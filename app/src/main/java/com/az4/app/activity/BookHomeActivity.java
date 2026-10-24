package com.az4.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.az4.app.R;
import com.az4.app.adapter.BookAdapter;
import com.az4.app.db.DatabaseHelper;
import com.az4.app.entity.Book;
import com.az4.app.utils.HttpClientUtils;

import java.util.List;

/**
 * 首页：图书列表 + 实时搜索 + 个人中心入口。
 * 数据加载策略为"本地优先 + 后台静默更新"，网络不可用时不影响使用。
 */
public class BookHomeActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "book_id";

    private RecyclerView rvBooks;
    private EditText etSearch;
    private TextView tvEmpty;
    private ProgressBar progressLoading;

    private BookAdapter adapter;
    private DatabaseHelper db;
    private String currentKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_home);

        db = DatabaseHelper.getInstance(this);
        initViews();
        initListeners();

        // 本地数据先上屏，保证秒开
        loadBooks(currentKeyword);
        // 后台同步网络数据，失败静默降级
        syncFromNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从详情页返回时刷新，库存可能已经变化
        loadBooks(currentKeyword);
    }

    private void initViews() {
        rvBooks = findViewById(R.id.rv_books);
        etSearch = findViewById(R.id.et_search);
        tvEmpty = findViewById(R.id.tv_empty);
        progressLoading = findViewById(R.id.progress_loading);
        findViewById(R.id.iv_mine).setOnClickListener(v -> startActivity(new Intent(this, MineActivity.class)));

        adapter = new BookAdapter(book -> {
            Intent intent = new Intent(BookHomeActivity.this, BookDetailActivity.class);
            intent.putExtra(EXTRA_BOOK_ID, book.getId());
            startActivity(intent);
        });
        rvBooks.setAdapter(adapter);
    }

    private void initListeners() {
        // 输入即搜，无需点击搜索按钮
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentKeyword = s == null ? "" : s.toString().trim();
                loadBooks(currentKeyword);
            }
        });
    }

    /** 从本地数据库加载列表 */
    private void loadBooks(String keyword) {
        List<Book> books = db.searchBooks(keyword);
        adapter.setBooks(books);
        boolean empty = books.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBooks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** 后台拉取远程图书数据，成功后入库并刷新列表 */
    private void syncFromNetwork() {
        HttpClientUtils.fetchBooksFromNetwork(new HttpClientUtils.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                progressLoading.setVisibility(View.GONE);
                if (books != null && !books.isEmpty()) {
                    db.insertBooksBatch(books);
                    loadBooks(currentKeyword);
                    Toast.makeText(BookHomeActivity.this, "已同步最新图书数据", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 网络不可用时静默降级，本地数据已展示
                progressLoading.setVisibility(View.GONE);
            }
        });
    }
}
