package com.example.rartyfinancebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class AssetEditAdapter extends RecyclerView.Adapter<AssetEditAdapter.ViewHolder> {
    private List<String[]> assetList;

    public AssetEditAdapter(List<String[]> assetList) {
        this.assetList = assetList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_portfolio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] item = assetList.get(position);
        holder.tvEditName.setText(item[1] + " (" + item[0] + ")"); // Örn: Apple Inc. (AAPL)
        holder.btnRename.setVisibility(View.GONE); // Varlıklarda kalem ikonuna gerek yok, gizliyoruz
    }

    @Override
    public int getItemCount() { return assetList.size(); }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(assetList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    // Sağa/Sola kaydırınca varlığı listeden silme
    public void onItemDismiss(int position) {
        assetList.remove(position);
        notifyItemRemoved(position);
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