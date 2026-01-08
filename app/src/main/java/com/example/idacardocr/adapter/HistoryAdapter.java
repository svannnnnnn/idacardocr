package com.example.idacardocr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idacardocr.R;
import com.example.idacardocr.model.RecognitionHistory;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<RecognitionHistory> historyList = new ArrayList<>();
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(RecognitionHistory history);
        void onHistoryDelete(RecognitionHistory history, int position);
    }

    public HistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    public void setHistoryList(List<RecognitionHistory> list) {
        this.historyList = list;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < historyList.size()) {
            historyList.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecognitionHistory history = historyList.get(position);
        holder.tvSummary.setText(history.getSummary());
        holder.tvTime.setText(history.getFormattedTime());
        
        String sideDisplay = history.getCardSideDisplay();
        if (!sideDisplay.isEmpty()) {
            holder.tvCardSide.setText(sideDisplay);
            holder.tvCardSide.setVisibility(View.VISIBLE);
        } else {
            holder.tvCardSide.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryClick(history);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryDelete(history, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSummary, tvTime, tvCardSide;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvSummary = itemView.findViewById(R.id.tvSummary);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCardSide = itemView.findViewById(R.id.tvCardSide);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
