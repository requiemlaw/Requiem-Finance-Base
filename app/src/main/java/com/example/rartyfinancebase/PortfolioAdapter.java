package com.example.rartyfinancebase;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;
import android.content.Intent;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> {
    private List<MainActivity.PortfolioCategory> categoryList;
    private Context context;

    public PortfolioAdapter(List<MainActivity.PortfolioCategory> categoryList, Context context) {
        this.categoryList = categoryList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_portfolio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainActivity.PortfolioCategory item = categoryList.get(position);
        holder.tvEditName.setText(item.name);

        // Düzenle Emojisine (Kalem) tıklandığında isim değiştirme penceresi aç
        holder.btnRename.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Kategori Adını Değiştir");
            final EditText input = new EditText(context);
            input.setText(item.name);
            builder.setView(input);
            builder.setPositiveButton("Kaydet", (dialog, which) -> {
                item.name = input.getText().toString();
                notifyItemChanged(position); // Ekranı güncelle
            });
            builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // KANKA YENİ EKLENEN KISIM BURASI: Satıra tıklayınca içine gir
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditCategoryActivity.class);
            intent.putExtra("CATEGORY_INDEX", position); // Hangi kategoriye tıklandığını yolluyoruz
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return categoryList.size(); }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(categoryList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEditName, btnRename;
        public ViewHolder(View itemView) {
            super(itemView);
            tvEditName = itemView.findViewById(R.id.tvEditName);
            btnRename = itemView.findViewById(R.id.btnRename);
        }
    }
}