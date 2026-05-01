package com.example.rartyfinancebase;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EditPortfolioActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_portfolio);

        RecyclerView rvEditPortfolio = findViewById(R.id.rvEditPortfolio);
        Button btnSavePortfolio = findViewById(R.id.btnSavePortfolio);

        // Adapter'a MainActivity'deki Kategori Listemizi (Referans olarak) veriyoruz
        PortfolioAdapter adapter = new PortfolioAdapter(MainActivity.categoryList, this);
        rvEditPortfolio.setLayoutManager(new LinearLayoutManager(this));
        rvEditPortfolio.setAdapter(adapter);

        // Sadece Aşağı-Yukarı Sürükleme (Kategori silmeyi kapattım ki veriler kaybolmasın)
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
        });

        helper.attachToRecyclerView(rvEditPortfolio);
        btnSavePortfolio.setOnClickListener(v -> finish()); // Çıkışta MainActivity'e döner
    }
}