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
import com.az4.app.entity.Book;
import com.az4.app.utils.ImageUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * 图书列表适配器，绑定 item_book 布局。
 */
public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    /** 条目点击回调 */
    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private final List<Book> books = new ArrayList<>();
    private final OnBookClickListener listener;

    public BookAdapter(OnBookClickListener listener) {
        this.listener = listener;
    }

    /** 刷新数据源 */
    public void setBooks(List<Book> list) {
        books.clear();
        if (list != null) {
            books.addAll(list);
        }
        notifyDataSetChanged();
    }

    public int getItemCountValue() {
        return books.size();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthor());
        holder.tvCategory.setText(book.getCategory());

        // 库存标签：有货蓝色，缺货红色
        holder.chipStock.setText("库存 " + book.getStock());
        int bgColor = book.hasStock() ? R.color.chip_stock_bg : R.color.logout_red;
        int textColor = book.hasStock() ? R.color.primary : R.color.white;
        holder.chipStock.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), bgColor)));
        holder.chipStock.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), textColor));

        // 封面从 assets 加载，带采样压缩
        ImageUtils.loadBookCover(holder.ivCover, book.getCover());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final TextView tvTitle;
        final TextView tvAuthor;
        final TextView tvCategory;
        final Chip chipStock;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_book_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvCategory = itemView.findViewById(R.id.tv_category);
            chipStock = itemView.findViewById(R.id.chip_stock);
        }
    }
}
