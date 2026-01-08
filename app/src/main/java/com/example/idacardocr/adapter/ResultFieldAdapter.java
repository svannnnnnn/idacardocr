package com.example.idacardocr.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idacardocr.R;
import com.example.idacardocr.model.ResultField;

import java.util.ArrayList;
import java.util.List;

public class ResultFieldAdapter extends RecyclerView.Adapter<ResultFieldAdapter.ViewHolder> {

    private List<ResultField> fields = new ArrayList<>();
    private Context context;
    private OnRevealRequestListener revealListener;

    public interface OnRevealRequestListener {
        void onRevealRequest(int position, ResultField field);
    }

    public ResultFieldAdapter(Context context, OnRevealRequestListener listener) {
        this.context = context;
        this.revealListener = listener;
    }

    public void setFields(List<ResultField> fields) {
        this.fields = fields;
        notifyDataSetChanged();
    }

    public void revealField(int position) {
        if (position >= 0 && position < fields.size()) {
            fields.get(position).setRevealed(true);
            notifyItemChanged(position);
        }
    }

    public void hideAllFields() {
        for (ResultField field : fields) {
            field.setRevealed(false);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result_field, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResultField field = fields.get(position);
        
        holder.tvFieldName.setText(field.getFieldName());
        holder.tvFieldValue.setText(field.getDisplayValue());

        // 敏感字段显示切换按钮
        if (field.isSensitive()) {
            holder.btnToggle.setVisibility(View.VISIBLE);
            holder.btnToggle.setImageResource(
                field.isRevealed() ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off
            );
            
            holder.btnToggle.setOnClickListener(v -> {
                if (field.isRevealed()) {
                    // 隐藏
                    field.setRevealed(false);
                    notifyItemChanged(position);
                } else {
                    // 请求认证后显示
                    if (revealListener != null) {
                        revealListener.onRevealRequest(position, field);
                    }
                }
            });
        } else {
            holder.btnToggle.setVisibility(View.GONE);
        }

        // 复制按钮
        holder.btnCopy.setOnClickListener(v -> {
            String valueToCopy = field.getFullValue();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(field.getFieldName(), valueToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, field.getFieldName() + " 已复制", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFieldName;
        TextView tvFieldValue;
        ImageView btnToggle;
        ImageView btnCopy;

        ViewHolder(View itemView) {
            super(itemView);
            tvFieldName = itemView.findViewById(R.id.tvFieldName);
            tvFieldValue = itemView.findViewById(R.id.tvFieldValue);
            btnToggle = itemView.findViewById(R.id.btnToggle);
            btnCopy = itemView.findViewById(R.id.btnCopy);
        }
    }
}
