package com.example.rartyfinancebase;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EditCategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Arayüz olarak kategorilerin arayüzünü (activity_edit_portfolio) kullanmaya devam ediyoruz
        setContentView(R.layout.activity_edit_portfolio);

        // Hangi kategoriye tıklandığını al
        int catIndex = getIntent().getIntExtra("CATEGORY_INDEX", -1);
        if (catIndex == -1) { finish(); return; }

        // O kategoriyi ve içindeki hisseleri bul
        MainActivity.PortfolioCategory category = MainActivity.categoryList.get(catIndex);
        List<String[]> assets = category.assets;

        RecyclerView rvEditPortfolio = findViewById(R.id.rvEditPortfolio);
        Button btnSavePortfolio = findViewById(R.id.btnSavePortfolio);
        btnSavePortfolio.setText(category.name + " KAYDET VE ÇIK"); // Buton ismini kategoriye özel yap

        // Hisseleri listelemek için yeni yazdığımız adapter'ı bağlıyoruz
        AssetEditAdapter adapter = new AssetEditAdapter(assets);
        rvEditPortfolio.setLayoutManager(new LinearLayoutManager(this));
        rvEditPortfolio.setAdapter(adapter);

        // Hem Sürükle (Yukarı/Aşağı) Hem Sil (Sağa/Sola Kaydır)
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                adapter.onItemDismiss(viewHolder.getAdapterPosition());
            }
        });

        helper.attachToRecyclerView(rvEditPortfolio);
        btnSavePortfolio.setOnClickListener(v -> finish());
    }
}