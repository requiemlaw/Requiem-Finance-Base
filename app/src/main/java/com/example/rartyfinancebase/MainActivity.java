package com.example.rartyfinancebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import android.content.SharedPreferences;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatDelegate;

// --- V2 KÖPRÜ İMPORTLARI ---
import androidx.compose.ui.platform.ComposeView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // UI Bileşenleri
    private LinearLayout containerAlarms;
    private TextView tvActiveAlarm;
    private EditText etAlarmLevel;
    private Button btnSetAlarm;
    private Spinner spinnerAssets;

    // --- V2 SUPER-APP KÖPRÜSÜ BİLEŞENLERİ ---
    private BottomNavigationView bottomNavigation;
    private ComposeView composeContainer;
    private LinearLayout terminalContainer;

    // Dark Mode için ekliyorum
    private ImageButton btnThemeToggle;
    private RelativeLayout transitionOverlay;
    private boolean isDarkMode = false;
    private SharedPreferences prefs;

    // API ve Arka Plan Görevleri
    private BinanceApi binanceApi;
    private YahooApi yahooApi;
    private Handler handler = new Handler();
    private Runnable updateTask;

    private final String CHANNEL_ID = "rarty_alerts";
    private List<Alarm> alarmList = new ArrayList<>();

    // --- YENİ DİNAMİK KATEGORİ SİSTEMİ ---
    public static class PortfolioCategory {
        public String name;
        public List<String[]> assets;
        public boolean isCrypto;
        public PortfolioCategory(String name, List<String[]> assets, boolean isCrypto) {
            this.name = name;
            this.assets = assets;
            this.isCrypto = isCrypto;
        }
    }
    public static List<PortfolioCategory> categoryList = new ArrayList<>();

    // VARLIK LİSTELERİ
    public static List<String[]> cryptoList = new ArrayList<>(Arrays.asList(
            new String[]{"BTCUSDT", "Bitcoin", "BTC"},
            new String[]{"ETHUSDT", "Ethereum", "ETH"},
            new String[]{"BNBUSDT", "BNB", "BNB"},
            new String[]{"SOLUSDT", "Solana", "SOL"},
            new String[]{"XRPUSDT", "Ripple", "XRP"}
    ));
    public static List<String[]> mag7List = new ArrayList<>(Arrays.asList(
            new String[]{"AAPL", "Apple Inc."},
            new String[]{"MSFT", "Microsoft"},
            new String[]{"GOOGL", "Alphabet"},
            new String[]{"AMZN", "Amazon"},
            new String[]{"META", "Meta Platforms"},
            new String[]{"TSLA", "Tesla"},
            new String[]{"NVDA", "Nvidia Corp."}
    ));
    public static List<String[]> indicesList = new ArrayList<>(Arrays.asList(
            new String[]{"XU100.IS", "BİST 100"},
            new String[]{"^GSPC", "S&P 500"},
            new String[]{"^IXIC", "Nasdaq"},
            new String[]{"^DJI", "Dow Jones"}
    ));
    public static List<String[]> forexList = new ArrayList<>(Arrays.asList(
            new String[]{"TRY=X", "USD/TRY"},
            new String[]{"EURTRY=X", "EUR/TRY"},
            new String[]{"EURUSD=X", "EUR/USD"}
    ));
    public static List<String[]> commoditiesList = new ArrayList<>(Arrays.asList(
            new String[]{"GC=F", "Altın (Ons)"},
            new String[]{"SI=F", "Gümüş"},
            new String[]{"HG=F", "Bakır"},
            new String[]{"PA=F", "Paladyum"}
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- MASTERMIND DOKUNUŞU: Tema kararı ekran çizilmeden ÖNCE verilir! ---
        prefs = getSharedPreferences("RequiemPrefs", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("isDarkMode", true);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // ----------------------------------------------------------------------

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Bağlantıları
        containerAlarms = findViewById(R.id.containerAlarms);
        tvActiveAlarm = findViewById(R.id.tvActiveAlarm);
        etAlarmLevel = findViewById(R.id.etAlarmLevel);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        spinnerAssets = findViewById(R.id.spinnerAssets);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);

        // --- V2 BAĞLANTILARI ---
        terminalContainer = findViewById(R.id.terminalContainer);
        composeContainer = findViewById(R.id.composeContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        int bgColor = isDarkMode ? android.graphics.Color.parseColor("#0B0E11") : android.graphics.Color.parseColor("#FFFFFF");
        terminalContainer.setBackgroundColor(bgColor);
        
        // Kategori Havuzunu Doldur
        if (categoryList.isEmpty()) {
            categoryList.add(new PortfolioCategory("US-STOCKS", mag7List, false));
            categoryList.add(new PortfolioCategory("CRYPTO", cryptoList, true));
            categoryList.add(new PortfolioCategory("ENDEKSLER", indicesList, false));
            categoryList.add(new PortfolioCategory("FOREX", forexList, false));
            categoryList.add(new PortfolioCategory("EMTİALAR", commoditiesList, false));
        }

        setupApis();
        createNotificationChannel();
        requestPermission();
        setupAlarmSpinner();
        buildMarketUI();

        // --- V2 ALT MENÜ TETİKLEYİCİSİ ---
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_terminal) {
                // 1. Sekme: Eski Terminal (Java) Arayüzü
                composeContainer.setVisibility(View.GONE);
                terminalContainer.setVisibility(View.VISIBLE);
                return true;
            }
            else if (itemId == R.id.nav_depth) {
                // 2. Sekme: Yeni Derinlik Arayüzü (Compose)
                terminalContainer.setVisibility(View.GONE);
                composeContainer.setVisibility(View.VISIBLE);

                // Çevirmen Köprü üzerinden Kotlin motorunu ateşle (0 = Derinlik)
                ComposeBridge.setScreen(composeContainer, 0);

                return true;
            }
            else if (itemId == R.id.nav_portfolio) {
                // 3. Sekme: Yeni Portföy Arayüzü (Compose)
                terminalContainer.setVisibility(View.GONE);
                composeContainer.setVisibility(View.VISIBLE);

                // Çevirmen Köprü üzerinden Kotlin motorunu ateşle (1 = Portföy)
                ComposeBridge.setScreen(composeContainer, 1);

                return true;
            }

            return false;
        });

        btnThemeToggle.setOnClickListener(v -> {
            isDarkMode = !isDarkMode;
            prefs.edit().putBoolean("isDarkMode", isDarkMode).apply();

            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // İşte bu komut o meşhur önizlemedeki geçişi tetikler
            recreate();
        });

        // Düzenleme Butonu
        Button btnEditPortfolio = findViewById(R.id.btnEditPortfolio);
        btnEditPortfolio.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditPortfolioActivity.class);
            startActivity(intent);
        });

        // --- ARAMA ÇUBUĞU VE S&P 500 HAVUZU ---
        AutoCompleteTextView searchTickerInput = findViewById(R.id.searchTickerInput);

        String[] usStocks = {
                "AAPL - Apple Inc.", "MSFT - Microsoft", "NVDA - Nvidia Corp.", "GOOGL - Alphabet (Class A)",
                "GOOG - Alphabet (Class C)", "AMZN - Amazon", "META - Meta Platforms", "BRK.B - Berkshire Hathaway",
                "LLY - Eli Lilly", "AVGO - Broadcom", "TSLA - Tesla", "JPM - JPMorgan Chase", "WMT - Walmart",
                "BEN - Franklin Resources", "IVZ - Invesco", "TROW - T. Rowe Price", "PEGI - Pattern Energy"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, usStocks);
        searchTickerInput.setAdapter(adapter);

        searchTickerInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            String[] parts = selectedItem.split(" - ");
            String tickerSymbol = parts[0].trim();
            String name = parts.length > 1 ? parts[1].trim() : tickerSymbol;

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("TICKER_SYMBOL", tickerSymbol);
            intent.putExtra("NAME", name);
            intent.putExtra("SYMBOL", tickerSymbol);
            startActivity(intent);

            searchTickerInput.setText("");
        });

        // Alarm Kurma İşlemi
        btnSetAlarm.setOnClickListener(v -> {
            String val = etAlarmLevel.getText().toString();
            if (!val.isEmpty()) {
                double price = Double.parseDouble(val);
                int selectedIdx = spinnerAssets.getSelectedItemPosition();
                String symbol = getAllSymbols().get(selectedIdx);
                String name = spinnerAssets.getSelectedItem().toString();

                alarmList.add(new Alarm(symbol, name, price));
                updateAlarmUI();
                etAlarmLevel.setText("");
                Toast.makeText(this, name + " listeye eklendi!", Toast.LENGTH_SHORT).show();
            }
        });

        // Veri Güncelleme Döngüsü
        updateTask = new Runnable() {
            @Override
            public void run() {
                refreshAllData();
                handler.postDelayed(this, 8000);
            }
        };
        handler.post(updateTask);
    } // onCreate BURADA BİTİYOR

    // DİNAMİK ARAYÜZ OLUŞTURUCU (onCreate dışında)
    private void buildMarketUI() {
        LinearLayout mainContainer = findViewById(R.id.containerAllMarkets);
        if (mainContainer == null) return;
        mainContainer.removeAllViews();

        for (PortfolioCategory cat : categoryList) {
            TextView tvTitle = new TextView(this);
            tvTitle.setText(cat.name);
            tvTitle.setTextSize(12);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
            tvTitle.setPadding(0, 40, 0, 10);
            tvTitle.setAllCaps(true);
            mainContainer.addView(tvTitle);

            LinearLayout assetContainer = new LinearLayout(this);
            assetContainer.setOrientation(LinearLayout.VERTICAL);
            for (String[] asset : cat.assets) {
                String displaySymbol = asset.length > 2 ? asset[2] : asset[0];
                addAssetRow(asset[0], asset[1], displaySymbol, assetContainer, cat.isCrypto);
            }
            mainContainer.addView(assetContainer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildMarketUI(); // Geri dönünce listeyi tazele
    }

    private void addAssetRow(String apiSymbol, String name, String displaySymbol, LinearLayout container, boolean isCrypto) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_asset, container, false);
        TextView tvSymbol = view.findViewById(R.id.tvSymbol);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvPrice = view.findViewById(R.id.tvPrice);

        tvPrice.setTag(apiSymbol);
        tvSymbol.setText(displaySymbol);
        tvName.setText(name);
        tvPrice.setText("..."); // VERİ GELENE KADAR BOŞ VEYA 0.00 DURMASIN, ŞIK DURSUN

        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("NAME", name);
                intent.putExtra("SYMBOL", isCrypto ? displaySymbol : apiSymbol);
                intent.putExtra("TICKER_SYMBOL", isCrypto ? displaySymbol : apiSymbol);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "DetailActivity hatası!", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(view);
    }

    private void updateAlarmUI() {
        containerAlarms.removeAllViews();
        if (alarmList.isEmpty()) {
            tvActiveAlarm.setVisibility(View.VISIBLE);
            tvActiveAlarm.setText("Aktif Alarm Yok");
        } else {
            tvActiveAlarm.setVisibility(View.GONE);
            for (Alarm alarm : alarmList) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER);
                row.setPadding(0, 8, 0, 8);

                TextView txtInfo = new TextView(this);
                txtInfo.setText(alarm.displayName + " : " + alarm.targetPrice);
                txtInfo.setTextColor(android.graphics.Color.parseColor("#0F172A"));
                txtInfo.setTextSize(14);
                txtInfo.setClickable(false);

                TextView btnEdit = new TextView(this);
                btnEdit.setText(" ✏️");
                btnEdit.setPadding(15, 0, 15, 0);
                btnEdit.setOnClickListener(v -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle(alarm.displayName + " Düzenle");
                    final EditText input = new EditText(this);
                    input.setInputType(android.view.inputmethod.EditorInfo.TYPE_CLASS_NUMBER | android.view.inputmethod.EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setText(String.valueOf(alarm.targetPrice));
                    builder.setView(input);
                    builder.setPositiveButton("Güncelle", (dialog, which) -> {
                        String newPriceStr = input.getText().toString();
                        if (!newPriceStr.isEmpty()) {
                            alarm.targetPrice = Double.parseDouble(newPriceStr);
                            updateAlarmUI();
                        }
                    });
                    builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
                    builder.show();
                });

                TextView btnDelete = new TextView(this);
                btnDelete.setText(" ❌");
                btnDelete.setPadding(15, 0, 15, 0);
                btnDelete.setOnClickListener(v -> {
                    alarmList.remove(alarm);
                    updateAlarmUI();
                });

                row.addView(txtInfo);
                row.addView(btnEdit);
                row.addView(btnDelete);
                containerAlarms.addView(row);
            }
        }
    }

    private void setupAlarmSpinner() {
        List<String> names = new ArrayList<>();
        for(PortfolioCategory cat : categoryList) {
            for(String[] a : cat.assets) {
                names.add(a[1]);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssets.setAdapter(adapter);
    }

    private List<String> getAllSymbols() {
        List<String> symbols = new ArrayList<>();
        for(PortfolioCategory cat : categoryList) {
            for(String[] a : cat.assets) {
                symbols.add(a[0]);
            }
        }
        return symbols;
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void refreshAllData() {
        for(PortfolioCategory cat : categoryList) {
            for (String[] asset : cat.assets) {
                if (cat.isCrypto) {
                    fetchBinancePrice(asset[0]);
                } else {
                    fetchYahooPrice(asset[0]);
                }
            }
        }
    }

    private void fetchBinancePrice(String symbol) {
        binanceApi.getPrice(symbol).enqueue(new Callback<BinanceTickerResponse>() {
            @Override
            public void onResponse(Call<BinanceTickerResponse> call, Response<BinanceTickerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double currentPrice = Double.parseDouble(response.body().getPrice());
                    YahooFinanceResponse.Result cryptoResult = new YahooFinanceResponse.Result();
                    cryptoResult.meta = new YahooFinanceResponse.Meta();
                    cryptoResult.meta.regularMarketPrice = currentPrice;
                    updatePriceWithExtra(symbol, cryptoResult);
                }
            }
            @Override public void onFailure(Call<BinanceTickerResponse> call, Throwable t) {}
        });
    }

    private void fetchYahooPrice(String symbol) {
        yahooApi.getChartData(symbol, "1m", "1d").enqueue(new Callback<YahooFinanceResponse>() {
            @Override
            public void onResponse(Call<YahooFinanceResponse> call, Response<YahooFinanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().chart.result != null) {
                    updatePriceWithExtra(symbol, response.body().chart.result.get(0));
                }
            }
            @Override public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {}
        });
    }

    private void updatePriceWithExtra(String symbol, YahooFinanceResponse.Result resultData) {
        View root = findViewById(android.R.id.content);
        TextView tvPrice = root.findViewWithTag(symbol);

        if (tvPrice != null) {
            LinearLayout parent = (LinearLayout) tvPrice.getParent();
            TextView tvExtra = parent.findViewById(R.id.tvExtra);
            YahooFinanceResponse.Meta meta = resultData.meta;
            double currentPrice = meta.regularMarketPrice;
            double prevClose = meta.chartPreviousClose > 0 ? meta.chartPreviousClose : meta.previousClose;

            // EURTRY=X İÇİN ₺ SİMGESİ GÜNCELLEMESİ
            tvPrice.setText((symbol.endsWith(".IS") || symbol.equals("TRY=X") || symbol.equals("EURTRY=X") ? "₺" : "$") + String.format("%.2f", currentPrice));

            double livePrice = currentPrice;
            if (resultData.indicators != null && resultData.indicators.quote != null && !resultData.indicators.quote.isEmpty()) {
                List<Double> closePrices = resultData.indicators.quote.get(0).close;
                if (closePrices != null) {
                    for (int i = closePrices.size() - 1; i >= 0; i--) {
                        if (closePrices.get(i) != null) { livePrice = closePrices.get(i); break; }
                    }
                }
            }

            Iterator<Alarm> it = alarmList.iterator();
            while (it.hasNext()) {
                Alarm alarm = it.next();
                if (symbol.equals(alarm.symbol)) {
                    if (Math.abs(livePrice - alarm.targetPrice) < (livePrice * 0.0005)) {
                        sendNotification(alarm.displayName + " Hedefe Ulaştı!", "Fiyat: " + livePrice);
                        it.remove();
                        runOnUiThread(() -> {
                            updateAlarmUI();
                            Toast.makeText(this, alarm.displayName + " HEDEFE GELDİ!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            if (tvExtra != null) {
                boolean isStock = false;
                for (String[] stock : mag7List) { // Şimdilik sadece mag7 de ekstra göster
                    if (stock[0].trim().equalsIgnoreCase(symbol.trim())) { isStock = true; break; }
                }

                if (isStock) {
                    double diff = 0; double percent = 0; String prefix = "";
                    if (Math.abs(livePrice - currentPrice) > 0.001) {
                        prefix = "Pre: "; diff = livePrice - currentPrice; percent = (diff / currentPrice) * 100;
                    } else if (prevClose > 0.01) {
                        diff = currentPrice - prevClose; percent = (diff / prevClose) * 100;
                    }
                    String sign = diff > 0 ? "+" : (diff < 0 ? "-" : "");
                    int color = diff >= 0 ? android.graphics.Color.parseColor("#00D06C") : android.graphics.Color.RED;
                    tvExtra.setText(String.format("%s%s%.2f (%s%.2f%%)", prefix, sign, Math.abs(diff), sign, Math.abs(percent)));
                    tvExtra.setTextColor(color);
                    tvExtra.setVisibility(View.VISIBLE);
                } else {
                    tvExtra.setVisibility(View.GONE);
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Borsa Alarm", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void sendNotification(String title, String msg) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(msg).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1, b.build());
        }
    }

    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacks(updateTask); }
}