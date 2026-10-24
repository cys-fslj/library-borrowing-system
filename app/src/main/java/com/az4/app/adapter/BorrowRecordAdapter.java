package com.az4.app.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.az4.app.R;
import com.az4.app.entity.BorrowRecord;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * 借阅记录适配器，绑定 item_borrow_record 布局。
 * 通过状态图标与 Chip 颜色区分"借阅中"（橙色）与"已归还"（绿色）。
 */
public class BorrowRecordAdapter extends RecyclerView.Adapter<BorrowRecordAdapter.RecordViewHolder> {

    private final List<BorrowRecord> records = new ArrayList<>();

    public void setRecords(List<BorrowRecord> list) {
        records.clear();
        if (list != null) {
            records.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_borrow_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        BorrowRecord record = records.get(position);
        holder.tvBookTitle.setText(record.getBookTitle());
        holder.tvBookAuthor.setText(record.getBookAuthor());
        holder.tvBorrowTime.setText("借阅时间：" + record.getBorrowTime());

        boolean borrowing = record.isBorrowing();
        holder.ivStatusIcon.setImageResource(
                borrowing ? R.drawable.ic_borrow_active : R.drawable.ic_check_circle);

        // 归还时间：仅在已归还时显示
        if (!borrowing && record.getReturnTime() != null) {
            holder.tvReturnTime.setVisibility(View.VISIBLE);
            holder.tvReturnTime.setText("归还时间：" + record.getReturnTime());
        } else {
            holder.tvReturnTime.setVisibility(View.GONE);
        }

        // 状态 Chip 文案与配色
        holder.chipStatus.setText(borrowing ? "借阅中" : "已归还");
        int bgColor = borrowing ? R.color.chip_borrowed_bg : R.color.chip_returned_bg;
        int textColor = borrowing ? R.color.chip_borrowed_text : R.color.chip_returned_text;
        holder.chipStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), bgColor)));
        holder.chipStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), textColor));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivStatusIcon;
        final TextView tvBookTitle;
        final TextView tvBookAuthor;
        final TextView tvBorrowTime;
        final TextView tvReturnTime;
        final Chip chipStatus;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
            tvBookTitle = itemView.findViewById(R.id.tv_book_title);
            tvBookAuthor = itemView.findViewById(R.id.tv_book_author);
            tvBorrowTime = itemView.findViewById(R.id.tv_borrow_time);
            tvReturnTime = itemView.findViewById(R.id.tv_return_time);
            chipStatus = itemView.findViewById(R.id.chip_status);
        }
    }
}
