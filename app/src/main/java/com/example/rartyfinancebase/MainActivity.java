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
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LinearLayout containerCrypto, containerStocks, containerIndices, containerForex, containerCommodities, containerAlarms;
    private TextView tvActiveAlarm;
    private EditText etAlarmLevel;
    private Button btnSetAlarm;
    private Spinner spinnerAssets;

    private BinanceApi binanceApi;
    private YahooApi yahooApi;
    private Handler handler = new Handler();
    private Runnable updateTask;

    private final String CHANNEL_ID = "rarty_alerts";
    private List<Alarm> alarmList = new ArrayList<>();

    // VARLIK LİSTELERİ
    private final String[][] cryptoList = {
            {"BTCUSDT", "Bitcoin", "BTC"},
            {"ETHUSDT", "Ethereum", "ETH"},
            {"BNBUSDT", "BNB", "BNB"},
            {"SOLUSDT", "Solana", "SOL"},
            {"XRPUSDT", "Ripple", "XRP"}
    };
    private final String[][] mag7List = {{"AAPL", "Apple Inc."}, {"MSFT", "Microsoft"}, {"GOOGL", "Alphabet"}, {"AMZN", "Amazon"}, {"META", "Meta Platforms"}, {"TSLA", "Tesla"}, {"NVDA", "Nvidia Corp."}};
    private final String[][] indicesList = {{"XU100.IS", "BİST 100"}, {"^GSPC", "S&P 500"}, {"^IXIC", "Nasdaq"}, {"^DJI", "Dow Jones"}};

    // DÖVİZ LİSTESİ
    private final String[][] forexList = {
            {"TRY=X", "USD/TRY"},
            {"EURTRY=X", "EUR/TRY"},
            {"EURUSD=X", "EUR/USD"}
    };
    private final String[][] commoditiesList = {{"GC=F", "Altın (Ons)"}, {"SI=F", "Gümüş"}, {"HG=F", "Bakır"}, {"PA=F", "Paladyum"}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        containerCrypto = findViewById(R.id.containerCrypto);
        containerStocks = findViewById(R.id.containerStocks);
        containerIndices = findViewById(R.id.containerIndices);
        containerForex = findViewById(R.id.containerForex);
        containerCommodities = findViewById(R.id.containerCommodities);
        containerAlarms = findViewById(R.id.containerAlarms);
        tvActiveAlarm = findViewById(R.id.tvActiveAlarm);
        etAlarmLevel = findViewById(R.id.etAlarmLevel);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        spinnerAssets = findViewById(R.id.spinnerAssets);

        setupApis();
        createNotificationChannel();
        requestPermission();
        setupAlarmSpinner();
        buildMarketUI();

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

        updateTask = new Runnable() {
            @Override
            public void run() {
                refreshAllData();
                handler.postDelayed(this, 8000);
            }
        };
        handler.post(updateTask);
    }



    private void buildMarketUI() {
        for (String[] crypto : cryptoList) addAssetRow(crypto[0], crypto[1], crypto[2], containerCrypto, true);
        for (String[] stock : mag7List) addAssetRow(stock[0], stock[1], stock[0], containerStocks, false);
        for (String[] index : indicesList) addAssetRow(index[0], index[1], index[0], containerIndices, false);
        for (String[] forex : forexList) addAssetRow(forex[0], forex[1], forex[0], containerForex, false);
        for (String[] comm : commoditiesList) addAssetRow(comm[0], comm[1], comm[0], containerCommodities, false);
    }

    private void addAssetRow(String apiSymbol, String name, String displaySymbol, LinearLayout container, boolean isCrypto) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_asset, container, false);
        TextView tvSymbol = view.findViewById(R.id.tvSymbol);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvPrice = view.findViewById(R.id.tvPrice);
        tvPrice.setTag(apiSymbol);
        tvSymbol.setText(displaySymbol);
        tvName.setText(name);

        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("NAME", name);
                intent.putExtra("SYMBOL", isCrypto ? displaySymbol : apiSymbol);
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
        for(String[] a : cryptoList) names.add(a[1]);
        for(String[] a : mag7List) names.add(a[1]);
        for(String[] a : indicesList) names.add(a[1]);
        for(String[] a : forexList) names.add(a[1]);
        for(String[] a : commoditiesList) names.add(a[1]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssets.setAdapter(adapter);
    }

    private List<String> getAllSymbols() {
        List<String> symbols = new ArrayList<>();
        for(String[] a : cryptoList) symbols.add(a[0]);
        for(String[] a : mag7List) symbols.add(a[0]);
        for(String[] a : indicesList) symbols.add(a[0]);
        for(String[] a : forexList) symbols.add(a[0]);
        for(String[] a : commoditiesList) symbols.add(a[0]);
        return symbols;
    }

    private void setupApis() {
        binanceApi = new Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi.class);
        yahooApi = new Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi.class);
    }

    private void refreshAllData() {
        for (String[] crypto : cryptoList) fetchBinancePrice(crypto[0]);
        for (String[] stock : mag7List) fetchYahooPrice(stock[0]);
        for (String[] index : indicesList) fetchYahooPrice(index[0]);
        for (String[] forex : forexList) fetchYahooPrice(forex[0]);
        for (String[] comm : commoditiesList) fetchYahooPrice(comm[0]);
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
                for (String[] stock : mag7List) {
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