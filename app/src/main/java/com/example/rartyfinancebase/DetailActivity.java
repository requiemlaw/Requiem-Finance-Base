package com.example.rartyfinancebase;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailActivity extends AppCompatActivity {

    private CandlestickChartView candlestickChart;
    private TextView detailName, detailPrice, tvDayRange, tvYearRange, tvVolume;
    private String currentSymbol, currentInterval, currentRange;
    private double lastKnownPrice = 0.0;

    // ── İndikatör Hafızası (Varsayılan Değerler) ──
    private boolean isRsiEnabled = true;
    private boolean isMacdEnabled = false;
    private boolean isMaEnabled = true;
    private boolean isBollingerEnabled = false;
    private boolean isVolumeEnabled = true;
    private String currentMaInput = "20, 50";
    private List<Integer> activeMaPeriods = new ArrayList<>();

    private BinanceApi binanceApi;
    private YahooApi yahooApi;
    private TextView selectedIntervalBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. TEMA AYARLARI
        SharedPreferences prefs = getSharedPreferences("RequiemPrefs", MODE_PRIVATE);
        boolean detailIsDarkMode = prefs.getBoolean("isDarkMode", true);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                detailIsDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        int bgColor = detailIsDarkMode ? Color.parseColor("#0B0E11") : Color.WHITE;
        getWindow().setStatusBarColor(bgColor);
        getWindow().setNavigationBarColor(bgColor);

        View mainScroll = findViewById(R.id.mainScrollView);
        if (mainScroll != null) {
            mainScroll.setBackgroundColor(bgColor);
        }

        // 2. HAFIZADAN İNDİKATÖR AYARLARINI ÇEK (STATE PERSISTENCE)
        SharedPreferences indPrefs = getSharedPreferences("IndicatorSettings", MODE_PRIVATE);
        isRsiEnabled = indPrefs.getBoolean("rsi", true);
        isMacdEnabled = indPrefs.getBoolean("macd", false);
        isMaEnabled = indPrefs.getBoolean("ma", true);
        isBollingerEnabled = indPrefs.getBoolean("bollinger", false);
        isVolumeEnabled = indPrefs.getBoolean("volume", true);
        currentMaInput = indPrefs.getString("maPeriods", "20, 50");

        parseMaPeriods(); // Metni listeye çevir

        candlestickChart = findViewById(R.id.candlestickChart);
        detailName = findViewById(R.id.detailName);
        detailPrice = findViewById(R.id.detailPrice);
        tvDayRange = findViewById(R.id.tvDayRange);
        tvYearRange = findViewById(R.id.tvYearRange);
        tvVolume = findViewById(R.id.tvVolume);

        ImageView btnSettings = findViewById(R.id.btnIndicatorSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> openIndicatorSettings());

        currentSymbol = getIntent().getStringExtra("SYMBOL");
        detailName.setText(getIntent().getStringExtra("NAME"));

        boolean isSlow = currentSymbol != null && (currentSymbol.equals("GC=F") || currentSymbol.contains("TRY") || currentSymbol.contains(".IS") || currentSymbol.contains("XAU"));
        currentInterval = isSlow ? "1d" : "15m";
        currentRange = isSlow ? "2y" : "5d";

        setupApis();
        setupCandleIntervalButtons();

        TextView defaultBtn = findViewById(isSlow ? R.id.btnCandle1M : R.id.btnCandle4h);
        if (defaultBtn != null) setSelectedButton(defaultBtn);
        fetchData();
    }

    private void parseMaPeriods() {
        activeMaPeriods.clear();
        if (!currentMaInput.trim().isEmpty()) {
            for (String p : currentMaInput.split(",")) {
                try { activeMaPeriods.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {}
            }
        }
    }

    private void openIndicatorSettings() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottom_sheet_indicators);

        android.widget.Switch switchRSI = dialog.findViewById(R.id.switchRSI);
        android.widget.Switch switchMACD = dialog.findViewById(R.id.switchMACD);
        android.widget.Switch switchMA = dialog.findViewById(R.id.switchMA);
        android.widget.Switch switchBollinger = dialog.findViewById(R.id.switchBollinger);
        android.widget.Switch switchVolume = dialog.findViewById(R.id.switchVolume);
        android.widget.EditText etMAPeriods = dialog.findViewById(R.id.etMAPeriods);
        android.widget.Button btnApply = dialog.findViewById(R.id.btnApplyIndicators);

        // Menü açıldığında şalterleri hafızadaki duruma göre ayarla
        if (switchRSI != null) switchRSI.setChecked(isRsiEnabled);
        if (switchMACD != null) switchMACD.setChecked(isMacdEnabled);
        if (switchMA != null) switchMA.setChecked(isMaEnabled);
        if (switchBollinger != null) switchBollinger.setChecked(isBollingerEnabled);
        if (switchVolume != null) switchVolume.setChecked(isVolumeEnabled);
        if (etMAPeriods != null) etMAPeriods.setText(currentMaInput);

        if (btnApply == null) return;

        btnApply.setOnClickListener(v -> {
            // Kullanıcının yeni seçimlerini al
            if (switchRSI != null) isRsiEnabled = switchRSI.isChecked();
            if (switchMACD != null) isMacdEnabled = switchMACD.isChecked();
            if (switchMA != null) isMaEnabled = switchMA.isChecked();
            if (switchBollinger != null) isBollingerEnabled = switchBollinger.isChecked();
            if (switchVolume != null) isVolumeEnabled = switchVolume.isChecked();
            if (etMAPeriods != null) currentMaInput = etMAPeriods.getText().toString();

            // 3. YENİ AYARLARI HAFIZAYA KAYDET (MÜHÜRLE)
            SharedPreferences.Editor editor = getSharedPreferences("IndicatorSettings", MODE_PRIVATE).edit();
            editor.putBoolean("rsi", isRsiEnabled);
            editor.putBoolean("macd", isMacdEnabled);
            editor.putBoolean("ma", isMaEnabled);
            editor.putBoolean("bollinger", isBollingerEnabled);
            editor.putBoolean("volume", isVolumeEnabled);
            editor.putString("maPeriods", currentMaInput);
            editor.apply();

            parseMaPeriods();
            fetchData();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void setupCandleIntervalButtons() {
        int[] ids = {R.id.btnCandle15m, R.id.btnCandle1h, R.id.btnCandle4h, R.id.btnCandle1d, R.id.btnCandle1w, R.id.btnCandle1M};
        String[] labels = {"1DK", "5DK", "15DK", "1S", "4S", "1G"};
        String[] intervals = {"1m", "5m", "15m", "1h", "4h", "1d"};
        String[] ranges = {"1d", "1d", "5d", "60d", "60d", "2y"};

        for (int i = 0; i < ids.length; i++) {
            TextView btn = findViewById(ids[i]);
            if (btn == null) continue;

            btn.setText(labels[i]);

            final String interval = intervals[i];
            final String range = ranges[i];
            btn.setOnClickListener(v -> {
                currentInterval = interval; currentRange = range;
                setSelectedButton((TextView) v);
                fetchData();
            });
        }
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

    private void fetchData() {
        if (currentSymbol != null && (currentSymbol.startsWith("BTC") || currentSymbol.startsWith("ETH"))) {
            fetchBinanceData();
        } else {
            fetchYahooData();
        }
    }

    private void fetchBinanceData() {
        String bSymbol = currentSymbol.endsWith("USDT") ? currentSymbol : currentSymbol + "USDT";
        binanceApi.getKlines(bSymbol, currentInterval, 1000).enqueue(new Callback<List<List<Object>>>() {
            @Override public void onResponse(Call<List<List<Object>>> call, Response<List<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) processBinanceAndRender(response.body());
            }
            @Override public void onFailure(Call<List<List<Object>>> call, Throwable t) {}
        });
    }

    private void processBinanceAndRender(List<List<Object>> body) {
        List<CandlestickChartView.Candle> candles = new ArrayList<>();
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        double totalVolume = 0.0;

        for (List<Object> kline : body) {
            try {
                long t = Double.valueOf(kline.get(0).toString()).longValue() / 1000;
                float o = Float.parseFloat(kline.get(1).toString());
                float h = Float.parseFloat(kline.get(2).toString());
                float l = Float.parseFloat(kline.get(3).toString());
                float c = Float.parseFloat(kline.get(4).toString());
                float v = Float.parseFloat(kline.get(5).toString());

                if (l < minPrice) minPrice = l;
                if (h > maxPrice) maxPrice = h;
                totalVolume += v;

                candles.add(new CandlestickChartView.Candle(o, h, l, c, v, t));
                lastKnownPrice = c;
            } catch (Exception ignored) {}
        }

        final double fMin = (minPrice == Double.MAX_VALUE) ? 0 : minPrice;
        final double fMax = (maxPrice == Double.MIN_VALUE) ? 0 : maxPrice;
        final double fVol = totalVolume;

        runOnUiThread(() -> {
            tvDayRange.setText(String.format(Locale.US, "%.2f - %.2f", fMin, fMax));
            tvYearRange.setText("--");
            tvVolume.setText(fVol > 0 ? String.format(Locale.US, "%.2f", fVol) : "--");
        });

        renderChart(candles);
    }

    private void fetchYahooData() {
        yahooApi.getChartData(currentSymbol, currentInterval, currentRange).enqueue(new Callback<YahooFinanceResponse>() {
            @Override public void onResponse(Call<YahooFinanceResponse> call, Response<YahooFinanceResponse> response) {
                try {
                    YahooFinanceResponse.Result result = response.body().chart.result.get(0);
                    updateStats(result.meta);
                    processYahooAndRender(result);
                } catch (Exception ignored) {}
            }
            @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {}
        });
    }

    private void processYahooAndRender(YahooFinanceResponse.Result result) {
        List<Double> closes = result.indicators.quote.get(0).close;
        List<Double> opens = result.indicators.quote.get(0).open;
        List<Double> highs = result.indicators.quote.get(0).high;
        List<Double> lows = result.indicators.quote.get(0).low;
        List<Double> vols = result.indicators.quote.get(0).volume;
        List<Long> times = result.timestamp;

        if (closes == null || opens == null) return;
        List<CandlestickChartView.Candle> candles = new ArrayList<>();

        for (int i = 0; i < closes.size(); i++) {
            if (closes.get(i) == null || opens.get(i) == null) continue;
            float o = opens.get(i).floatValue();
            float h = (highs != null && highs.get(i) != null) ? highs.get(i).floatValue() : o;
            float l = (lows != null && lows.get(i) != null) ? lows.get(i).floatValue() : o;
            float c = closes.get(i).floatValue();
            float v = (vols != null && vols.get(i) != null) ? vols.get(i).floatValue() : 0f;
            candles.add(new CandlestickChartView.Candle(o, h, l, c, v, times.get(i)));
            lastKnownPrice = c;
        }
        renderChart(candles);
    }

    private void renderChart(List<CandlestickChartView.Candle> candles) {
        List<Float> closes = new ArrayList<>();
        for (CandlestickChartView.Candle c : candles) closes.add(c.getClose());

        // Kullanıcı şalteri kapattıysa motoru yormuyoruz (Performans optimizasyonu)
        Map<Integer, List<Float>> dynamicMAs = isMaEnabled ? (Map) IndicatorEngine.INSTANCE.computeDynamicMAs(closes, activeMaPeriods) : null;
        List<Float> rsi14 = isRsiEnabled ? (List) IndicatorEngine.INSTANCE.rsi(closes, 14) : null;
        IndicatorEngine.MacdResult macd = isMacdEnabled ? IndicatorEngine.INSTANCE.computeMacd(closes, 12, 26, 9) : null;
        kotlin.Triple bbands = isBollingerEnabled ? IndicatorEngine.INSTANCE.computeBollingerBands(closes, 20, 2.0f) : null;

        runOnUiThread(() -> {
            candlestickChart.setData(candles, dynamicMAs, rsi14, macd, isRsiEnabled, isMacdEnabled, isMaEnabled, isBollingerEnabled, bbands, isVolumeEnabled);
            updatePriceDisplay(lastKnownPrice);
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

    private void updatePriceDisplay(double price) {
        String symbol = (currentSymbol != null && (currentSymbol.endsWith(".IS") || currentSymbol.equals("TRY=X"))) ? "₺" : "$";
        detailPrice.setText(symbol + String.format(Locale.US, "%.2f", price));
    }
}