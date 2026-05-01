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

public class MainActivity extends AppCompatActivity {

    // UI Bileşenleri
    private LinearLayout containerAlarms;
    private TextView tvActiveAlarm;
    private EditText etAlarmLevel;
    private Button btnSetAlarm;
    private Spinner spinnerAssets;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Bağlantıları
        containerAlarms = findViewById(R.id.containerAlarms);
        tvActiveAlarm = findViewById(R.id.tvActiveAlarm);
        etAlarmLevel = findViewById(R.id.etAlarmLevel);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        spinnerAssets = findViewById(R.id.spinnerAssets);

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
                "UNH - UnitedHealth Group", "V - Visa Inc.", "XOM - Exxon Mobil", "MA - Mastercard", "JNJ - Johnson & Johnson",
                "PG - Procter & Gamble", "HD - Home Depot", "COST - Costco", "MRK - Merck & Co.", "ABBV - AbbVie",
                "CRM - Salesforce", "AMD - Advanced Micro Devices", "CVX - Chevron", "NFLX - Netflix",
                "KO - Coca-Cola", "PEP - PepsiCo", "BAC - Bank of America", "TMO - Thermo Fisher Scientific",
                "LIN - Linde plc", "MCD - McDonald's", "DIS - Walt Disney", "ADBE - Adobe Inc.", "CSCO - Cisco Systems",
                "ABT - Abbott Laboratories", "INTU - Intuit", "QCOM - Qualcomm", "IBM - International Business Machines",
                "CMCSA - Comcast", "CAT - Caterpillar", "VZ - Verizon", "PFE - Pfizer", "BA - Boeing",
                "INTC - Intel", "NKE - Nike", "UBER - Uber Technologies", "PLTR - Palantir Technologies",
                "COIN - Coinbase", "MSTR - MicroStrategy", "SMCI - Super Micro Computer", "NOW - ServiceNow",
                "AMAT - Applied Materials", "TXN - Texas Instruments", "GE - General Electric", "ISRG - Intuitive Surgical",
                "PM - Philip Morris International", "AMGN - Amgen", "COP - ConocoPhillips", "HON - Honeywell",
                "UNP - Union Pacific", "SYK - Stryker Corp.", "SPGI - S&P Global", "AXP - American Express",
                "LMT - Lockheed Martin", "RTX - RTX Corporation", "BKNG - Booking Holdings", "T - AT&T",
                "PGR - Progressive Corp.", "MDT - Medtronic", "VRTX - Vertex Pharmaceuticals", "BSX - Boston Scientific",
                "GS - Goldman Sachs", "C - Citigroup", "MS - Morgan Stanley", "BLK - BlackRock", "SCHW - Charles Schwab",
                "CB - Chubb Limited", "MMC - Marsh & McLennan", "FI - Fiserv", "CVS - CVS Health", "CI - Cigna",
                "ELV - Elevance Health", "REGN - Regeneron Pharmaceuticals", "VLO - Valero Energy", "MPC - Marathon Petroleum",
                "PSX - Phillips 66", "SLB - Schlumberger", "EOG - EOG Resources", "PXD - Pioneer Natural Resources",
                "OXY - Occidental Petroleum", "FCX - Freeport-McMoRan", "NEM - Newmont", "APD - Air Products",
                "ECL - Ecolab", "SHW - Sherwin-Williams", "CTVA - Corteva", "NUE - Nucor", "NEE - NextEra Energy",
                "DUK - Duke Energy", "SO - Southern Company", "SRE - Sempra", "AEP - American Electric Power",
                "D - Dominion Energy", "EXC - Exelon", "WM - Waste Management", "RSG - Republic Services",
                "CSX - CSX Corp.", "NSC - Norfolk Southern", "FDX - FedEx", "UPS - United Parcel Service",
                "DAL - Delta Air Lines", "UAL - United Airlines", "LUV - Southwest Airlines", "DE - Deere & Company",
                "PCAR - PACCAR", "EMR - Emerson Electric", "ETN - Eaton", "PH - Parker-Hannifin", "TT - Trane Technologies",
                "CARR - Carrier Global", "JCI - Johnson Controls", "IR - Ingersoll Rand", "ROK - Rockwell Automation",
                "A - Agilent Technologies", "ILMN - Illumina", "IQV - IQVIA", "MTD - Mettler-Toledo", "TMO - Thermo Fisher",
                "WAT - Waters Corp.", "ZTS - Zoetis", "IDXX - IDEXX Laboratories", "DHR - Danaher", "BDX - Becton Dickinson",
                "EW - Edwards Lifesciences", "BSX - Boston Scientific", "ALGN - Align Technology", "RMD - ResMed",
                "STE - STERIS", "GILD - Gilead Sciences", "BIIB - Biogen", "SGEN - Seagen", "MRNA - Moderna",
                "INCY - Incyte", "VRTX - Vertex", "REGN - Regeneron", "BMY - Bristol-Myers Squibb", "SNY - Sanofi",
                "GSK - GSK plc", "NVS - Novartis", "AZN - AstraZeneca", "TTE - TotalEnergies", "BP - BP plc",
                "SHEL - Shell plc", "EQNR - Equinor", "ENB - Enbridge", "TRP - TC Energy", "CNQ - Canadian Natural",
                "SU - Suncor Energy", "BNS - Bank of Nova Scotia", "BMO - Bank of Montreal", "RY - Royal Bank of Canada",
                "TD - Toronto-Dominion", "CM - Canadian Imperial", "BAM - Brookfield Asset", "BX - Blackstone",
                "KKR - KKR & Co.", "APO - Apollo Global", "CG - Carlyle Group", "ARES - Ares Management",
                "O - Realty Income", "SPG - Simon Property Group", "PLD - Prologis", "AMT - American Tower",
                "CCI - Crown Castle", "EQIX - Equinix", "DLR - Digital Realty", "PSA - Public Storage",
                "EXR - Extra Space", "AVB - AvalonBay", "EQR - Equity Residential", "INVH - Invitation Homes",
                "MAA - Mid-America", "CPT - Camden Property", "SUI - Sun Communities", "ELS - Equity Lifestyle",
                "WELL - Welltower", "VTR - Ventas", "PEAK - Healthpeak", "ARE - Alexandria Real",
                "BXP - Boston Properties", "SLG - SL Green", "VNO - Vornado", "KIM - Kimco", "REG - Regency Centers",
                "FRT - Federal Realty", "NEM - Newmont", "GOLD - Barrick Gold", "AEM - Agnico Eagle",
                "KGC - Kinross Gold", "WPM - Wheaton Precious", "FNV - Franco-Nevada", "RGLD - Royal Gold",
                "ALB - Albemarle", "SQM - Sociedad Quimica", "LTHM - Livent", "LAC - Lithium Americas",
                "RIO - Rio Tinto", "BHP - BHP Group", "VALE - Vale S.A.", "CLF - Cleveland-Cliffs",
                "X - United States Steel", "STLD - Steel Dynamics", "NUE - Nucor", "RS - Reliance Steel",
                "AA - Alcoa", "CENX - Century Aluminum", "FCX - Freeport-McMoRan", "SCCO - Southern Copper",
                "TECK - Teck Resources", "AG - First Majestic", "PAAS - Pan American", "HL - Hecla Mining",
                "CDE - Coeur Mining", "MTA - Metalla Royalty", "OR - Osisko Gold", "SAND - Sandstorm Gold",
                "MUX - McEwen Mining", "IAG - IAMGOLD", "NGD - New Gold", "EGO - Eldorado Gold",
                "EQX - Equinox Gold", "GFI - Gold Fields", "HMY - Harmony Gold", "AU - AngloGold",
                "SBSW - Sibanye Stillwater", "IMPUY - Impala Platinum", "ANGPY - Anglo American Platinum",
                "AMKBY - A.P. Moller-Maersk", "ZIM - ZIM Integrated", "DAC - Danaos", "SBLK - Star Bulk",
                "GOGL - Golden Ocean", "SFL - SFL Corp", "FRO - Frontline", "STNG - Scorpio Tankers",
                "INSW - International Seaways", "TNK - Teekay Tankers", "DHT - DHT Holdings",
                "EURN - Euronav", "TRMD - TORM", "NAT - Nordic American", "GNK - Genco Shipping",
                "EGLE - Eagle Bulk", "CPLP - Capital Product", "CMRE - Costamare", "ATCO - Atlas Corp",
                "MPLX - MPLX LP", "EPD - Enterprise Products", "ET - Energy Transfer", "WMB - Williams Companies",
                "OKE - ONEOK", "KMI - Kinder Morgan", "TRGP - Targa Resources", "PAA - Plains All American",
                "MMP - Magellan Midstream", "WES - Western Midstream", "CQP - Cheniere Energy",
                "LNG - Cheniere Energy Inc", "SRE - Sempra", "SWX - Southwest Gas", "NJR - New Jersey Resources",
                "SJI - South Jersey Industries", "CPK - Chesapeake Utilities", "ATO - Atmos Energy",
                "NI - NiSource", "UGI - UGI Corp", "OGE - OGE Energy", "PNW - Pinnacle West",
                "IDA - IDACORP", "AVA - Avista", "ALE - ALLETE", "MGEE - MGE Energy",
                "POR - Portland General", "NWN - Northwest Natural", "SR - Spire", "BKH - Black Hills",
                "MDU - MDU Resources", "CNP - CenterPoint Energy", "CMS - CMS Energy", "DTE - DTE Energy",
                "WEC - WEC Energy", "LNT - Alliant Energy", "XEL - Xcel Energy", "AEE - Ameren",
                "ES - Eversource", "FE - FirstEnergy", "PEG - Public Service", "ED - Consolidated Edison",
                "AWK - American Water", "WTRG - Essential Utilities", "CWT - California Water",
                "SJW - SJW Group", "MSEX - Middlesex Water", "YORW - York Water", "PRK - Park National",
                "CBU - Community Bank", "NBTB - NBT Bancorp", "TMP - Tompkins Financial",
                "FNB - F.N.B. Corp", "FULT - Fulton Financial", "WSFS - WSFS Financial",
                "UBSI - United Bankshares", "CFR - Cullen/Frost", "TCBI - Texas Capital",
                "CMA - Comerica", "ZION - Zions Bancorp", "KEY - KeyCorp", "FITB - Fifth Third",
                "HBAN - Huntington Bancshares", "CFG - Citizens Financial", "RF - Regions Financial",
                "TFC - Truist Financial", "PNC - PNC Financial", "USB - U.S. Bancorp",
                "WFC - Wells Fargo", "BAC - Bank of America", "JPM - JPMorgan Chase",
                "C - Citigroup", "MS - Morgan Stanley", "GS - Goldman Sachs",
                "SCHW - Charles Schwab", "BLK - BlackRock", "STT - State Street",
                "BK - Bank of New York Mellon", "NTRS - Northern Trust", "AMP - Ameriprise",
                "RJF - Raymond James", "LPLA - LPL Financial", "SF - Stifel Financial",
                "HLI - Houlihan Lokey", "MC - Moelis", "PJT - PJT Partners",
                "EVR - Evercore", "LAZ - Lazard", "CG - Carlyle Group",
                "APO - Apollo Global", "KKR - KKR & Co.", "BX - Blackstone",
                "BAM - Brookfield Asset", "ARES - Ares Management", "OAK - Oaktree",
                "BEN - Franklin Resources", "IVZ - Invesco", "TROW - T. Rowe Price",
                "JHG - Janus Henderson", "AMG - Affiliated Managers", "AB - AllianceBernstein",
                "FCNCA - First Citizens", "SIVB - SVB Financial", "SBNY - Signature Bank",
                "FRC - First Republic", "PACW - PacWest Bancorp", "WAL - Western Alliance",
                "EWBC - East West Bancorp", "PINC - Premier Inc.",
                "IQV - IQVIA Holdings", "PRAH - PRA Health Sciences", "SYNH - Syneos Health",
                "ICLR - ICON plc", "MEDP - Medpace Holdings", "CRL - Charles River Labs",
                "WST - West Pharmaceutical", "BIO - Bio-Rad Laboratories", "TMO - Thermo Fisher",
                "DHR - Danaher Corp", "A - Agilent Technologies", "WAT - Waters Corp",
                "PKI - PerkinElmer", "MTD - Mettler-Toledo", "ILMN - Illumina",
                "PACB - Pacific Biosciences", "TXG - 10x Genomics", "NSTG - NanoString Technologies",
                "FLDM - Fluidigm Corp", "QDEL - Quidel Corp", "HOLX - Hologic",
                "DXCM - DexCom", "PODD - Insulet", "TNDM - Tandem Diabetes",
                "MDT - Medtronic", "SYK - Stryker", "BSX - Boston Scientific",
                "ZBH - Zimmer Biomet", "ABT - Abbott Labs", "JNJ - Johnson & Johnson",
                "PFE - Pfizer", "MRK - Merck & Co.", "LLY - Eli Lilly",
                "BMY - Bristol-Myers Squibb", "ABBV - AbbVie", "AMGN - Amgen",
                "GILD - Gilead Sciences", "BIIB - Biogen", "REGN - Regeneron",
                "VRTX - Vertex Pharmaceuticals", "INCY - Incyte", "ALXN - Alexion Pharmaceuticals",
                "SGEN - Seagen", "EXEL - Exelixis", "HALO - Halozyme Therapeutics",
                "BMRN - BioMarin", "SRPT - Sarepta Therapeutics", "PTCT - PTC Therapeutics",
                "UTHR - United Therapeutics", "FOLD - Amicus Therapeutics", "RARE - Ultragenyx",
                "ALNY - Alnylam", "IONS - Ionis Pharmaceuticals", "CRSP - CRISPR Therapeutics",
                "NTLA - Intellia Therapeutics", "EDIT - Editas Medicine", "BEAM - Beam Therapeutics",
                "FATE - Fate Therapeutics", "ALLO - Allogene Therapeutics", "CABA - Cabaletta Bio",
                "PRLD - Prelude Therapeutics", "KROS - Keros Therapeutics", "RLAY - Relay Therapeutics",
                "KRTX - Karuna Therapeutics", "CERE - Cerevel Therapeutics", "AXSM - Axsome Therapeutics",
                "ITCI - Intra-Cellular Therapies", "CYTK - Cytokinetics", "MYOV - Myovant Sciences",
                "ESPR - Esperion Therapeutics", "AKRO - Akero Therapeutics", "MDGL - Madrigal Pharmaceuticals",
                "VKTX - Viking Therapeutics", "NGM - NGM Biopharmaceuticals", "ALT - Altimmune",
                "ARNA - Arena Pharmaceuticals", "GLPG - Galapagos NV", "ARGX - argenx SE",
                "ASND - Ascendis Pharma", "GWPH - GW Pharmaceuticals", "ZLAB - Zai Lab",
                "BGNE - BeiGene", "INMD - InMode", "NUVA - NuVasive", "GMED - Globus Medical",
                "PEN - Penumbra", "SIBN - Sientra", "EBS - Emergent BioSolutions",
                "KNSA - Kiniksa Pharmaceuticals", "RXRX - Recursion Pharmaceuticals", "ABSI - Absci",
                "DNA - Ginkgo Bioworks", "ZY - Zymergen", "TWST - Twist Bioscience",
                "BLI - Berkeley Lights", "QSI - Quantum-Si", "MAXN - Maxeon Solar",
                "RUN - Sunrun", "NOVA - Sunnova", "SPWR - SunPower",
                "ENPH - Enphase Energy", "SEDG - SolarEdge", "FSLR - First Solar",
                "CSIQ - Canadian Solar", "JKS - JinkoSolar", "DQ - Daqo New Energy",
                "PLUG - Plug Power", "FCEL - FuelCell Energy", "BLDP - Ballard Power",
                "BE - Bloom Energy", "QS - QuantumScape", "ENVX - Enovix",
                "SLDP - Solid Power", "MVST - Microvast", "CHPT - ChargePoint",
                "BLNK - Blink Charging", "EVGO - EVgo", "VLTA - Volta",
                "WBX - Wallbox", "STEM - Stem", "FLNC - Fluence Energy",
                "NEP - NextEra Energy Partners", "CWEN - Clearway Energy", "HASI - Hannon Armstrong",
                "AY - Atlantica Sustainable", "BEP - Brookfield Renewable", "PEGI - Pattern Energy"
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