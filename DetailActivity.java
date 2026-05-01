package com.example.rartyfinancebase;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
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
    private TextView detailName, detailPrice;
    private TextView tvDayRange, tvYearRange, tvVolume; // Yeni eklenenler
    private String currentSymbol, currentInterval, currentRange;
    private BinanceApi binanceApi;
    private YahooApi yahooApi;
    private double lastKnownPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // UI Bağlantıları
        lineChart = findViewById(R.id.lineChart);
        detailName = findViewById(R.id.detailName);
        detailPrice = findViewById(R.id.detailPrice);

        // İstatistik Bağlantıları
        tvDayRange = findViewById(R.id.tvDayRange);
        tvYearRange = findViewById(R.id.tvYearRange);
        tvVolume = findViewById(R.id.tvVolume);

        currentSymbol = getIntent().getStringExtra("SYMBOL");
        detailName.setText(getIntent().getStringExtra("NAME"));

        if (currentSymbol != null && (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") || currentSymbol.contains(".IS") || currentSymbol.contains("XAU"))) {
            currentInterval = "30m"; currentRange = "5d";
        } else {
            currentInterval = "15m"; currentRange = "1d";
        }

        setupApis();
        setupChart();
        setupButtons();
        fetchData();
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("Piyasa verisi çekiliyor...");

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) { updatePriceDisplay(e.getY()); }
            @Override public void onNothingSelected() { updatePriceDisplay(lastKnownPrice); }
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
                    List<Entry> entries = new ArrayList<>();
                    List<Long> timestamps = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.body().size(); i++) {
                            long ts = Double.valueOf(response.body().get(i).get(0).toString()).longValue() / 1000;
                            float close = Float.parseFloat(response.body().get(i).get(4).toString());
                            entries.add(new Entry(i, close));
                            timestamps.add(ts);
                            if (i == response.body().size() - 1) lastKnownPrice = close;
                        }
                        configureXAxis(timestamps);
                        updatePriceDisplay(lastKnownPrice);
                        updateChart(entries);
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
                        List<Double> prices = result.indicators.quote.get(0).close;
                        List<Long> timestamps = result.timestamp;

                        // İstatistikleri Güncelle
                        updateStats(result.meta);

                        List<Entry> entries = new ArrayList<>();
                        if (prices != null && timestamps != null) {
                            configureXAxis(timestamps);
                            for (int i = 0; i < prices.size(); i++) {
                                if (prices.get(i) != null) {
                                    entries.add(new Entry(i, prices.get(i).floatValue()));
                                    lastKnownPrice = prices.get(i);
                                }
                            }
                            updatePriceDisplay(lastKnownPrice);
                            updateChart(entries);
                        }
                    } else {
                        lineChart.setNoDataText("Veri bulunamadı.");
                    }
                } catch (Exception e) { Log.e("YAHOO_PARSE", e.getMessage()); }
            }
            @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {}
        });
    }

    private void updateStats(YahooFinanceResponse.Meta meta) {
        if (meta == null) return;
        runOnUiThread(() -> {
            tvDayRange.setText(String.format("%.2f - %.2f", meta.regularMarketDayLow, meta.regularMarketDayHigh));
            tvYearRange.setText(String.format("%.2f - %.2f", meta.fiftyTwoWeekLow, meta.fiftyTwoWeekHigh));
            if (meta.regularMarketVolume > 0) {
                double vol = meta.regularMarketVolume / 1_000_000.0;
                tvVolume.setText(String.format("%.2fM", vol));
            } else {
                tvVolume.setText("N/A");
            }
        });
    }

    private void configureXAxis(List<Long> timestamps) {
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
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
        lineChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setLabelCount(4, true);
        lineChart.getXAxis().setTextColor(Color.GRAY);
    }

    private void updateChart(List<Entry> entries) {
        LineDataSet set = new LineDataSet(entries, "Fiyat");
        set.setColor(Color.parseColor("#00D06C"));
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private void updatePriceDisplay(double price) {
        String cSymbol = (currentSymbol != null && (currentSymbol.endsWith(".IS") || currentSymbol.equals("TRY=X"))) ? "₺" : "$";
        String format = (price < 100 && !currentSymbol.contains("XAU") && !currentSymbol.contains("GC=F")) ? "%.4f" : "%.2f";
        detailPrice.setText(cSymbol + String.format(format, price));
    }
}