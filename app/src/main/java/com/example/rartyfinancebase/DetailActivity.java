package com.example.rartyfinancebase;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
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
import android.view.ViewGroup;

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
    private boolean detailIsDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences detailPrefs = getSharedPreferences("RequiemPrefs", MODE_PRIVATE);
        detailIsDarkMode = detailPrefs.getBoolean("isDarkMode", true);

        if (detailIsDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // --- RENKLER ---
        int zifiriSiyah = Color.parseColor("#0B0E11");
        int bgColor = detailIsDarkMode ? zifiriSiyah : Color.WHITE;
        int boxColor = detailIsDarkMode ? zifiriSiyah : Color.parseColor("#F1F5F9");
        int textColor = detailIsDarkMode ? Color.WHITE : Color.BLACK;

        View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (rootView != null) { rootView.setBackgroundColor(bgColor); }

        android.view.Window window = getWindow();
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);

        lineChart = findViewById(R.id.lineChart);
        candleChart = findViewById(R.id.candleChart);
        btnToggleChart = findViewById(R.id.btnToggleChart);
        detailName = findViewById(R.id.detailName);
        detailPrice = findViewById(R.id.detailPrice);
        tvDayRange = findViewById(R.id.tvDayRange);
        tvYearRange = findViewById(R.id.tvYearRange);
        tvVolume = findViewById(R.id.tvVolume);

        lineChart.setBackgroundColor(Color.TRANSPARENT);
        candleChart.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout statsContainer = findViewById(R.id.statsContainer);
        if(statsContainer != null) {
            statsContainer.setBackgroundColor(bgColor);
            statsContainer.setElevation(0);
        }


        android.widget.HorizontalScrollView hsv = findViewById(R.id.btnScrollView);
        if (hsv != null) {
            hsv.setBackgroundColor(bgColor);
        }


        Button[] buttons = { findViewById(R.id.btn15m), findViewById(R.id.btn1d), findViewById(R.id.btn1w), findViewById(R.id.btn1M) };
        for(Button b : buttons) {
            if(b != null) {
                b.setBackgroundColor(bgColor); // Artık butonlar da background ile aynı
                b.setTextColor(textColor);
                b.setElevation(0);
                b.setStateListAnimator(null);
            }
        }


        View container = (View) findViewById(R.id.btn15m).getParent();
        if (container != null) {
            ((View) container.getParent()).setBackgroundColor(bgColor);
        }

        btnToggleChart.setOnClickListener(v -> {
            if (isLineChartActive) { lineChart.setVisibility(View.GONE); candleChart.setVisibility(View.VISIBLE); isLineChartActive = false; }
            else { candleChart.setVisibility(View.GONE); lineChart.setVisibility(View.VISIBLE); isLineChartActive = true; }
        });

        currentSymbol = getIntent().getStringExtra("SYMBOL");
        detailName.setText(getIntent().getStringExtra("NAME"));

        currentInterval = (currentSymbol != null && (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") || currentSymbol.contains(".IS") || currentSymbol.contains("XAU"))) ? "30m" : "15m";
        currentRange = (currentSymbol != null && (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") || currentSymbol.contains(".IS") || currentSymbol.contains("XAU"))) ? "5d" : "1d";

        setupApis(); setupLineChart(); setupCandleChart(); setupButtons(); fetchData();
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void setupLineChart() {
        int ct = detailIsDarkMode ? Color.WHITE : Color.BLACK;
        lineChart.getAxisLeft().setTextColor(ct); lineChart.getXAxis().setTextColor(ct);
        lineChart.getDescription().setEnabled(false); lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false); lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getLegend().setEnabled(false); lineChart.setNoDataText("Yükleniyor...");
        lineChart.getPaint(LineChart.PAINT_INFO).setColor(ct);
    }

    private void setupCandleChart() {
        int ct = detailIsDarkMode ? Color.WHITE : Color.BLACK;
        candleChart.getAxisLeft().setTextColor(ct); candleChart.getXAxis().setTextColor(ct);
        candleChart.getDescription().setEnabled(false); candleChart.getAxisRight().setEnabled(false);
        candleChart.getXAxis().setDrawGridLines(false); candleChart.getAxisLeft().setDrawGridLines(false);
        candleChart.getLegend().setEnabled(false); candleChart.setNoDataText("Yükleniyor...");
        candleChart.getPaint(CandleStickChart.PAINT_INFO).setColor(ct);
    }

    private void setupButtons() {
        findViewById(R.id.btn15m).setOnClickListener(v -> { currentInterval = "15m"; currentRange = "2d"; fetchData(); });
        findViewById(R.id.btn1d).setOnClickListener(v -> { currentInterval = "30m"; currentRange = "5d"; fetchData(); });
        findViewById(R.id.btn1w).setOnClickListener(v -> { currentInterval = "1h"; currentRange = "7d"; fetchData(); });
        findViewById(R.id.btn1M).setOnClickListener(v -> { currentInterval = "1d"; currentRange = "1mo"; fetchData(); });
    }

    private void fetchData() {
        lineChart.clear(); candleChart.clear();
        if (currentSymbol != null && (currentSymbol.startsWith("BTC") || currentSymbol.startsWith("ETH"))) { fetchBinanceData(); }
        else { fetchYahooData(); }
    }

    private void fetchBinanceData() {
        String bS = currentSymbol.endsWith("USDT") ? currentSymbol : currentSymbol + "USDT";
        String bI = currentInterval.equals("1h") ? "1h" : (currentInterval.equals("1d") ? "1d" : "15m");
        binanceApi.getKlines(bS, bI, 100).enqueue(new Callback<List<List<Object>>>() {
            @Override public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Entry> le = new ArrayList<>(); List<CandleEntry> ce = new ArrayList<>(); List<Long> ts = new ArrayList<>();
                    for (int i = 0; i < response.body().size(); i++) {
                        long t = Double.valueOf(response.body().get(i).get(0).toString()).longValue() / 1000;
                        float o = Float.parseFloat(response.body().get(i).get(1).toString()); float h = Float.parseFloat(response.body().get(i).get(2).toString());
                        float l = Float.parseFloat(response.body().get(i).get(3).toString()); float c = Float.parseFloat(response.body().get(i).get(4).toString());
                        le.add(new Entry(i, c)); ce.add(new CandleEntry(i, h, l, o, c)); ts.add(t);
                        if (i == response.body().size() - 1) lastKnownPrice = c;
                    }
                    configureXAxis(lineChart, ts); configureXAxis(candleChart, ts); updatePriceDisplay(lastKnownPrice);
                    updateLineChart(le); updateCandleChart(ce);
                }
            }
            @Override public void onFailure(Call<List<List<Object>>> call, Throwable t) {}
        });
    }

    private void fetchYahooData() {
        yahooApi.getChartData(currentSymbol, currentInterval, currentRange).enqueue(new Callback<YahooFinanceResponse>() {
            @Override public void onResponse(Call<YahooFinanceResponse> call, Response<YahooFinanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().chart.result != null) {
                    YahooFinanceResponse.Result r = response.body().chart.result.get(0);
                    updateStats(r.meta);
                    List<Entry> le = new ArrayList<>(); List<CandleEntry> ce = new ArrayList<>();
                    for (int i = 0; i < r.indicators.quote.get(0).close.size(); i++) {
                        if (r.indicators.quote.get(0).close.get(i) != null) {
                            float c = r.indicators.quote.get(0).close.get(i).floatValue();
                            le.add(new Entry(i, c));
                            ce.add(new CandleEntry(i, r.indicators.quote.get(0).high.get(i).floatValue(), r.indicators.quote.get(0).low.get(i).floatValue(), r.indicators.quote.get(0).open.get(i).floatValue(), c));
                            lastKnownPrice = c;
                        }
                    }
                    configureXAxis(lineChart, r.timestamp); configureXAxis(candleChart, r.timestamp); updatePriceDisplay(lastKnownPrice);
                    updateLineChart(le); updateCandleChart(ce);
                }
            }
            @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {}
        });
    }

    private void updateStats(YahooFinanceResponse.Meta m) {
        if (m == null) return;
        runOnUiThread(() -> {
            tvDayRange.setText(String.format(Locale.US, "%.2f - %.2f", m.regularMarketDayLow, m.regularMarketDayHigh));
            tvYearRange.setText(String.format(Locale.US, "%.2f - %.2f", m.fiftyTwoWeekLow, m.fiftyTwoWeekHigh));
            tvVolume.setText(m.regularMarketVolume > 0 ? String.format(Locale.US, "%.2fM", m.regularMarketVolume / 1_000_000.0) : "N/A");
        });
    }

    private void configureXAxis(com.github.mikephil.charting.charts.BarLineChartBase chart, List<Long> ts) {
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int i = (int) value;
                if (i >= 0 && i < ts.size()) {
                    SimpleDateFormat sdf = new SimpleDateFormat((currentRange != null && (currentRange.equals("7d") || currentRange.equals("1mo"))) ? "dd MMM" : "HH:mm", new Locale("tr", "TR"));
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+3")); return sdf.format(new Date(ts.get(i) * 1000));
                }
                return "";
            }
        });
        chart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setLabelCount(4, true);
        chart.getXAxis().setTextColor(detailIsDarkMode ? Color.GRAY : Color.BLACK);
    }

    private void updateLineChart(List<Entry> e) {
        LineDataSet s = new LineDataSet(e, "Fiyat");
        s.setColor(Color.parseColor("#00D06C")); s.setLineWidth(2.5f); s.setDrawCircles(false);
        s.setMode(LineDataSet.Mode.CUBIC_BEZIER); s.setDrawValues(false);
        lineChart.setData(new LineData(s)); lineChart.invalidate();
    }

    private void updateCandleChart(List<CandleEntry> e) {
        CandleDataSet s = new CandleDataSet(e, "Mum Fiyat");
        s.setDecreasingColor(Color.parseColor("#FF3B30")); s.setIncreasingColor(Color.parseColor("#00D06C"));
        s.setShadowColorSameAsCandle(true); s.setDrawValues(false);
        candleChart.setData(new CandleData(s)); candleChart.invalidate();
    }

    private void updatePriceDisplay(double p) {
        String s = (currentSymbol != null && (currentSymbol.endsWith(".IS") || currentSymbol.equals("TRY=X"))) ? "₺" : "$";
        detailPrice.setText(s + String.format(Locale.US, (p < 100 && !currentSymbol.contains("XAU") && !currentSymbol.contains("GC=F")) ? "%.4f" : "%.2f", p));
    }
}