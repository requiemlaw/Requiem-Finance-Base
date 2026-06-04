package com.example.rartyfinancebase;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // ── UI ──────────────────────────────────────────────────────────────────
    private CandlestickChartView candlestickChart;
    private TextView detailName, detailPrice;
    private TextView tvDayRange, tvYearRange, tvVolume;

    // ── State ───────────────────────────────────────────────────────────────
    private String currentSymbol, currentInterval, currentRange;
    private double lastKnownPrice = 0.0;
    private boolean detailIsDarkMode;

    // ── Network ─────────────────────────────────────────────────────────────
    private BinanceApi binanceApi;
    private YahooApi yahooApi;

    // ── Seçili buton takibi ─────────────────────────────────────────────────
    private TextView selectedIntervalBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("RequiemPrefs", MODE_PRIVATE);
        detailIsDarkMode = prefs.getBoolean("isDarkMode", true);

        if (detailIsDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // ── Renkler ──────────────────────────────────────────────────────────
        int bgColor  = detailIsDarkMode ? Color.parseColor("#0B0E11") : Color.WHITE;
        int boxColor = detailIsDarkMode ? Color.parseColor("#0B0E11") : Color.parseColor("#F1F5F9");
        int textColor = detailIsDarkMode ? Color.WHITE : Color.BLACK;

        View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (rootView != null) rootView.setBackgroundColor(bgColor);

        android.view.Window window = getWindow();
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);

        // ── View bağlantıları ─────────────────────────────────────────────────
        candlestickChart = findViewById(R.id.candlestickChart);
        detailName       = findViewById(R.id.detailName);
        detailPrice      = findViewById(R.id.detailPrice);
        tvDayRange       = findViewById(R.id.tvDayRange);
        tvYearRange      = findViewById(R.id.tvYearRange);
        tvVolume         = findViewById(R.id.tvVolume);

        // ── Stats container ───────────────────────────────────────────────────
        LinearLayout statsContainer = findViewById(R.id.statsContainer);
        if (statsContainer != null) {
            statsContainer.setBackgroundColor(bgColor);
            statsContainer.setElevation(0);
        }

        // ── Eski butonlar (btn15m vb.) — renk ayarı ───────────────────────────
        int[] oldBtnIds = {R.id.btn15m, R.id.btn1d, R.id.btn1w, R.id.btn1M};
        for (int id : oldBtnIds) {
            Button b = findViewById(id);
            if (b != null) {
                b.setBackgroundColor(bgColor);
                b.setTextColor(textColor);
                b.setElevation(0);
                b.setStateListAnimator(null);
            }
        }

        android.widget.HorizontalScrollView hsv = findViewById(R.id.btnScrollView);
        if (hsv != null) hsv.setBackgroundColor(bgColor);

        // ── Intent ────────────────────────────────────────────────────────────
        currentSymbol = getIntent().getStringExtra("SYMBOL");
        detailName.setText(getIntent().getStringExtra("NAME"));

        boolean isSlow = currentSymbol != null &&
                (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") ||
                        currentSymbol.contains(".IS") || currentSymbol.contains("XAU"));
        currentInterval = isSlow ? "30m" : "15m";
        currentRange    = isSlow ? "5d"  : "1d";

        setupApis();
        setupCandleIntervalButtons();
        setupOldButtons();
        fetchData();
    }

    // ── API setup ────────────────────────────────────────────────────────────
    private void setupApis() {
        binanceApi = new Retrofit.Builder()
                .baseUrl("https://api.binance.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(BinanceApi.class);

        yahooApi = new Retrofit.Builder()
                .baseUrl("https://query1.finance.yahoo.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(YahooApi.class);
    }

    // ── Yeni candlestick zaman aralığı butonları ─────────────────────────────
    private void setupCandleIntervalButtons() {
        int[] ids = {
                R.id.btnCandle15m,
                R.id.btnCandle1h,
                R.id.btnCandle4h,
                R.id.btnCandle1d,
                R.id.btnCandle1w,
                R.id.btnCandle1M
        };
        String[] intervals = {"15m", "1h",  "4h",  "1d",  "1wk", "1mo"};
        String[] ranges    = {"2d",  "5d",  "14d", "1mo", "6mo", "1y"};

        for (int i = 0; i < ids.length; i++) {
            TextView btn = findViewById(ids[i]);
            if (btn == null) continue;
            final String interval = intervals[i];
            final String range    = ranges[i];
            btn.setOnClickListener(v -> {
                currentInterval = interval;
                currentRange    = range;
                setSelectedButton((TextView) v);
                fetchData();
            });
        }

        // Varsayılan seçili buton
        TextView defaultBtn = findViewById(R.id.btnCandle15m);
        if (defaultBtn != null) setSelectedButton(defaultBtn);
    }

    private void setSelectedButton(TextView btn) {
        if (selectedIntervalBtn != null) {
            selectedIntervalBtn.setSelected(false);
            selectedIntervalBtn.setTextColor(Color.parseColor("#848E9C"));
        }
        selectedIntervalBtn = btn;
        selectedIntervalBtn.setSelected(true);
        selectedIntervalBtn.setTextColor(Color.parseColor("#F0B90B"));
    }

    // ── Eski butonlar (hâlâ çalışıyor, opsiyonel) ────────────────────────────
    private void setupOldButtons() {
        findViewById(R.id.btn15m).setOnClickListener(v -> { currentInterval = "15m"; currentRange = "2d";  fetchData(); });
        findViewById(R.id.btn1d) .setOnClickListener(v -> { currentInterval = "30m"; currentRange = "5d";  fetchData(); });
        findViewById(R.id.btn1w) .setOnClickListener(v -> { currentInterval = "1h";  currentRange = "7d";  fetchData(); });
        findViewById(R.id.btn1M) .setOnClickListener(v -> { currentInterval = "1d";  currentRange = "1mo"; fetchData(); });
    }

    // ── Veri çekme ───────────────────────────────────────────────────────────
    private void fetchData() {
        if (currentSymbol != null &&
                (currentSymbol.startsWith("BTC") || currentSymbol.startsWith("ETH"))) {
            fetchBinanceData();
        } else {
            fetchYahooData();
        }
    }

    // ── Binance ──────────────────────────────────────────────────────────────
    private void fetchBinanceData() {
        String bSymbol   = currentSymbol.endsWith("USDT") ? currentSymbol : currentSymbol + "USDT";
        String bInterval = currentInterval;

        binanceApi.getKlines(bSymbol, bInterval, 150).enqueue(new Callback<List<List<Object>>>() {
            @Override
            public void onResponse(Call<List<List<Object>>> call,
                                   Response<List<List<Object>>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                processBinanceAndRender(response.body());
            }
            @Override public void onFailure(Call<List<List<Object>>> call, Throwable t) {
                Log.e("BINANCE", t.getMessage());
            }
        });
    }

    private void processBinanceAndRender(List<List<Object>> body) {
        List<CandlestickChartView.Candle> candles = new ArrayList<>();

        for (List<Object> kline : body) {
            try {
                long  t = Double.valueOf(kline.get(0).toString()).longValue() / 1000;
                float o = Float.parseFloat(kline.get(1).toString());
                float h = Float.parseFloat(kline.get(2).toString());
                float l = Float.parseFloat(kline.get(3).toString());
                float c = Float.parseFloat(kline.get(4).toString());
                float v = Float.parseFloat(kline.get(5).toString());
                candles.add(new CandlestickChartView.Candle(o, h, l, c, v, t));
                lastKnownPrice = c;
            } catch (Exception e) {
                Log.w("BINANCE_PARSE", "Skipped malformed kline");
            }
        }

        renderChart(candles);
    }

    // ── Yahoo Finance ────────────────────────────────────────────────────────
    private void fetchYahooData() {
        yahooApi.getChartData(currentSymbol, currentInterval, currentRange)
                .enqueue(new Callback<YahooFinanceResponse>() {
                    @Override
                    public void onResponse(Call<YahooFinanceResponse> call,
                                           Response<YahooFinanceResponse> response) {
                        try {
                            if (!response.isSuccessful() || response.body() == null
                                    || response.body().chart.result == null) return;

                            YahooFinanceResponse.Result result = response.body().chart.result.get(0);
                            updateStats(result.meta);
                            processYahooAndRender(result);
                        } catch (Exception e) {
                            Log.e("YAHOO_PARSE", e.getMessage());
                        }
                    }
                    @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {
                        Log.e("YAHOO", t.getMessage());
                    }
                });
    }

    private void processYahooAndRender(YahooFinanceResponse.Result result) {
        List<Double> closes = result.indicators.quote.get(0).close;
        List<Double> opens  = result.indicators.quote.get(0).open;
        List<Double> highs  = result.indicators.quote.get(0).high;
        List<Double> lows   = result.indicators.quote.get(0).low;
        List<Double> vols   = result.indicators.quote.get(0).volume;
        List<Long>   times  = result.timestamp;

        if (closes == null || opens == null) return;

        List<CandlestickChartView.Candle> candles = new ArrayList<>();

        for (int i = 0; i < closes.size(); i++) {
            if (closes.get(i) == null || opens.get(i) == null) continue;

            float o = opens.get(i).floatValue();
            float h = (highs != null && i < highs.size() && highs.get(i) != null)
                    ? highs.get(i).floatValue() : o;
            float l = (lows  != null && i < lows.size()  && lows.get(i)  != null)
                    ? lows.get(i).floatValue()  : o;
            float c = closes.get(i).floatValue();
            float v = (vols  != null && i < vols.size()  && vols.get(i)  != null)
                    ? vols.get(i).floatValue()  : 0f;
            long  t = (times != null && i < times.size() && times.get(i) != null)
                    ? times.get(i) : 0L;

            candles.add(new CandlestickChartView.Candle(o, h, l, c, v, t));
            lastKnownPrice = c;
        }

        renderChart(candles);
    }

    // ── Ortak render ─────────────────────────────────────────────────────────
    /**
     * IndicatorEngine ile MA20, MA50, RSI14 hesaplayıp CandlestickChartView'a besler.
     */
    private void renderChart(List<CandlestickChartView.Candle> candles) {
        kotlin.Triple<List<Float>, List<Float>, List<Float>> indicators =
                IndicatorEngine.INSTANCE.computeAll(candles);

        List<Float> ma20  = (List<Float>) indicators.getFirst();
        List<Float> ma50  = (List<Float>) indicators.getSecond();
        List<Float> rsi14 = (List<Float>) indicators.getThird();

        runOnUiThread(() -> {
            candlestickChart.setData(candles, ma20, ma50, rsi14);
            updatePriceDisplay(lastKnownPrice);
        });
    }

    // ── Yardımcı metodlar ────────────────────────────────────────────────────
    private void updateStats(YahooFinanceResponse.Meta m) {
        if (m == null) return;
        runOnUiThread(() -> {
            tvDayRange.setText(String.format(Locale.US, "%.2f - %.2f",
                    m.regularMarketDayLow, m.regularMarketDayHigh));
            tvYearRange.setText(String.format(Locale.US, "%.2f - %.2f",
                    m.fiftyTwoWeekLow, m.fiftyTwoWeekHigh));
            tvVolume.setText(m.regularMarketVolume > 0
                    ? String.format(Locale.US, "%.2fM", m.regularMarketVolume / 1_000_000.0)
                    : "N/A");
        });
    }

    private void updatePriceDisplay(double price) {
        String symbol = (currentSymbol != null &&
                (currentSymbol.endsWith(".IS") || currentSymbol.equals("TRY=X"))) ? "₺" : "$";
        String format = (price < 100 && currentSymbol != null &&
                !currentSymbol.contains("XAU") && !currentSymbol.contains("GC=F"))
                ? "%.4f" : "%.2f";
        detailPrice.setText(symbol + String.format(Locale.US, format, price));
    }
}
