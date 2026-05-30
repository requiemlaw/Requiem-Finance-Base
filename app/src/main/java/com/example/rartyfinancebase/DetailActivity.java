package com.example.rartyfinancebase;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import android.widget.ImageButton;
import android.view.View;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DetailActivity extends AppCompatActivity {
    private LineChart lineChart;
    private CandleStickChart candleChart;
    private ImageButton btnToggleChart;
    private boolean isLineChartActive = true;

    private TextView detailName, detailPrice;
    private TextView tvDayRange, tvYearRange, tvVolume;
    private String currentSymbol, currentInterval, currentRange;
    private BinanceApi binanceApi;
    private YahooApi yahooApi;

    private double lastKnownPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#0B0E11"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        lineChart = findViewById(R.id.lineChart);
        candleChart = findViewById(R.id.candleChart);
        lineChart.setBackgroundColor(Color.parseColor("#0B0E11"));
        candleChart.setBackgroundColor(Color.parseColor("#0B0E11"));
        btnToggleChart = findViewById(R.id.btnToggleChart);
        detailName = findViewById(R.id.detailName);
        detailPrice = findViewById(R.id.detailPrice);
        tvDayRange = findViewById(R.id.tvDayRange);
        tvYearRange = findViewById(R.id.tvYearRange);
        tvVolume = findViewById(R.id.tvVolume);

        // Mastermind Butonu Olayı
        btnToggleChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLineChartActive) {
                    lineChart.setVisibility(View.GONE);
                    candleChart.setVisibility(View.VISIBLE);
                    isLineChartActive = false;
                } else {
                    candleChart.setVisibility(View.GONE);
                    lineChart.setVisibility(View.VISIBLE);
                    isLineChartActive = true;
                }
            }
        });

        currentSymbol = getIntent().getStringExtra("SYMBOL");
        detailName.setText(getIntent().getStringExtra("NAME"));

        if (currentSymbol != null && (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") || currentSymbol.contains(".IS") || currentSymbol.contains("XAU"))) {
            currentInterval = "30m"; currentRange = "5d";
        } else {
            currentInterval = "15m"; currentRange = "1d";
        }

        setupApis();
        setupLineChart();
        setupCandleChart(); // YENİ EKLENDİ
        setupButtons();
        fetchData();
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void setupLineChart() {
        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("Piyasa verisi çekiliyor...");

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) { updatePriceDisplay(e.getY()); }
            @Override public void onNothingSelected() { updatePriceDisplay(lastKnownPrice); }
        });
    }

    private void setupCandleChart() {
        candleChart.getAxisLeft().setTextColor(Color.WHITE);
        candleChart.getXAxis().setTextColor(Color.WHITE);   
        candleChart.getDescription().setEnabled(false);
        candleChart.getAxisRight().setEnabled(false);
        candleChart.getXAxis().setDrawGridLines(false);
        candleChart.getAxisLeft().setDrawGridLines(false);
        candleChart.getLegend().setEnabled(false);
        candleChart.setNoDataText("OHLC verisi çekiliyor...");

        // YENİ: Parmağı kaydırınca fiyatın güncellenmesi (Dokunmatik Sensör)
        candleChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // Mum grafiğinde parmağın olduğu yerdeki kapanış (Close) fiyatını al
                if (e instanceof CandleEntry) {
                    updatePriceDisplay(((CandleEntry) e).getClose());
                } else {
                    updatePriceDisplay(e.getY());
                }
            }

            @Override
            public void onNothingSelected() {
                updatePriceDisplay(lastKnownPrice);
            }
        });
    }

    private void setupButtons() {
        findViewById(R.id.btn15m).setOnClickListener(v -> { currentInterval = "15m"; currentRange = "2d"; fetchData(); });
        findViewById(R.id.btn1d).setOnClickListener(v -> { currentInterval = "30m"; currentRange = "5d"; fetchData(); });
        findViewById(R.id.btn1w).setOnClickListener(v -> { currentInterval = "1h"; currentRange = "7d"; fetchData(); });
        findViewById(R.id.btn1M).setOnClickListener(v -> { currentInterval = "1d"; currentRange = "1mo"; fetchData(); });
    }

    private void fetchData() {
        lineChart.clear();
        candleChart.clear();
        if (currentSymbol != null && (currentSymbol.startsWith("BTC") || currentSymbol.startsWith("ETH"))) {
            fetchBinanceData();
        } else {
            fetchYahooData();
        }
    }

    private void fetchBinanceData() {
        String bSymbol = currentSymbol.endsWith("USDT") ? currentSymbol : currentSymbol + "USDT";
        String binanceInterval = "15m";
        if (currentInterval.equals("1h") || currentRange.equals("7d")) binanceInterval = "1h";
        if (currentInterval.equals("1d") || currentRange.equals("1mo")) binanceInterval = "1d";

        binanceApi.getKlines(bSymbol, binanceInterval, 100).enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Entry> lineEntries = new ArrayList<>();
                    List<CandleEntry> candleEntries = new ArrayList<>();
                    List<Long> timestamps = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.body().size(); i++) {
                            long ts = Double.valueOf(response.body().get(i).get(0).toString()).longValue() / 1000;
                            float open = Float.parseFloat(response.body().get(i).get(1).toString());
                            float high = Float.parseFloat(response.body().get(i).get(2).toString());
                            float low = Float.parseFloat(response.body().get(i).get(3).toString());
                            float close = Float.parseFloat(response.body().get(i).get(4).toString());

                            lineEntries.add(new Entry(i, close));
                            candleEntries.add(new CandleEntry(i, high, low, open, close));
                            timestamps.add(ts);
                            if (i == response.body().size() - 1) lastKnownPrice = close;
                        }
                        configureXAxis(lineChart, timestamps);
                        configureXAxis(candleChart, timestamps);
                        updatePriceDisplay(lastKnownPrice);
                        updateLineChart(lineEntries);
                        updateCandleChart(candleEntries);
                    } catch (Exception e) { Log.e("BINANCE_PARSE", e.getMessage()); }
                }
            }
            @Override public void onFailure(Call<List<List<Object>>> call, Throwable t) {}
        });
    }

    private void fetchYahooData() {
        yahooApi.getChartData(currentSymbol, currentInterval, currentRange).enqueue(new Callback<YahooFinanceResponse>() {
            @Override
            public void onResponse(Call<YahooFinanceResponse> call, Response<YahooFinanceResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && response.body().chart.result != null) {
                        YahooFinanceResponse.Result result = response.body().chart.result.get(0);
                        List<Double> closes = result.indicators.quote.get(0).close;
                        List<Double> opens = result.indicators.quote.get(0).open;
                        List<Double> highs = result.indicators.quote.get(0).high;
                        List<Double> lows = result.indicators.quote.get(0).low;
                        List<Long> timestamps = result.timestamp;

                        updateStats(result.meta);

                        List<Entry> lineEntries = new ArrayList<>();
                        List<CandleEntry> candleEntries = new ArrayList<>();

                        if (closes != null && timestamps != null) {
                            configureXAxis(lineChart, timestamps);
                            configureXAxis(candleChart, timestamps);

                            for (int i = 0; i < closes.size(); i++) {
                                // Yahoo bazen null veri atar, ondan koruyoruz
                                if (closes.get(i) != null && opens != null && opens.get(i) != null && highs.get(i) != null && lows.get(i) != null) {
                                    float c = closes.get(i).floatValue();
                                    float o = opens.get(i).floatValue();
                                    float h = highs.get(i).floatValue();
                                    float l = lows.get(i).floatValue();

                                    lineEntries.add(new Entry(i, c));
                                    candleEntries.add(new CandleEntry(i, h, l, o, c));
                                    lastKnownPrice = c;
                                }
                            }
                            updatePriceDisplay(lastKnownPrice);
                            updateLineChart(lineEntries);
                            updateCandleChart(candleEntries);
                        }
                    } else {
                        lineChart.setNoDataText("Veri bulunamadı.");
                        candleChart.setNoDataText("Veri bulunamadı.");
                    }
                } catch (Exception e) { Log.e("YAHOO_PARSE", e.getMessage()); }
            }
            @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {}
        });
    }

    private void updateStats(YahooFinanceResponse.Meta meta) {
        if (meta == null) return;
        runOnUiThread(() -> {
            tvDayRange.setText(String.format(Locale.US, "%.2f - %.2f", meta.regularMarketDayLow, meta.regularMarketDayHigh));
            tvYearRange.setText(String.format(Locale.US, "%.2f - %.2f", meta.fiftyTwoWeekLow, meta.fiftyTwoWeekHigh));
            if (meta.regularMarketVolume > 0) {
                double vol = meta.regularMarketVolume / 1_000_000.0;
                tvVolume.setText(String.format(Locale.US, "%.2fM", vol));
            } else {
                tvVolume.setText("N/A");
            }
        });
    }

    private void configureXAxis(com.github.mikephil.charting.charts.BarLineChartBase chart, List<Long> timestamps) {
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) {
                    Date date = new Date(timestamps.get(index) * 1000);
                    Locale tr = new Locale("tr", "TR");
                    SimpleDateFormat sdf;
                    if (currentRange != null && (currentRange.equals("7d") || currentRange.equals("1mo"))) {
                        sdf = new SimpleDateFormat("dd MMM", tr);
                    } else {
                        sdf = new SimpleDateFormat("HH:mm", tr);
                    }
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+3"));
                    return sdf.format(date);
                }
                return "";
            }
        });
        chart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setLabelCount(4, true);
        chart.getXAxis().setTextColor(Color.GRAY);
    }

    private void updateLineChart(List<Entry> entries) {
        LineDataSet set = new LineDataSet(entries, "Fiyat");
        set.setColor(Color.parseColor("#00D06C"));
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private void updateCandleChart(List<CandleEntry> entries) {
        CandleDataSet set = new CandleDataSet(entries, "Mum Fiyat");

        // YENİ: Kusursuz Mum Estetiği ve Wall Street Standartları
        set.setShadowColorSameAsCandle(true); // Fitillerin rengi gövdeyle aynı olsun
        set.setShadowWidth(1.2f); // Fitiller daha belirgin ve jilet gibi

        set.setDecreasingColor(Color.parseColor("#FF3B30")); // Düşüş Kırmızı
        set.setDecreasingPaintStyle(Paint.Style.FILL); // Gövde içi dolu

        set.setIncreasingColor(Color.parseColor("#00D06C")); // Yükseliş Senin Neon Yeşil
        set.setIncreasingPaintStyle(Paint.Style.FILL); // Gövde içi dolu

        set.setNeutralColor(Color.GRAY);

        // Parmakla dokununca çıkan artı (crosshair) çizgisinin estetiği
        set.setHighLightColor(Color.parseColor("#888888"));
        set.setHighlightLineWidth(1f);
        set.setDrawValues(false);

        candleChart.setData(new CandleData(set));
        candleChart.invalidate();
    }

    private void updatePriceDisplay(double price) {
        String cSymbol = (currentSymbol != null && (currentSymbol.endsWith(".IS") || currentSymbol.equals("TRY=X"))) ? "₺" : "$";
        String format = (price < 100 && !currentSymbol.contains("XAU") && !currentSymbol.contains("GC=F")) ? "%.4f" : "%.2f";
        detailPrice.setText(cSymbol + String.format(Locale.US, format, price));
    }
}