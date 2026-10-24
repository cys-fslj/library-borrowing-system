package com.az4.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.az4.app.R;
import com.az4.app.adapter.BorrowRecordAdapter;
import com.az4.app.db.DatabaseHelper;
import com.az4.app.entity.BorrowRecord;
import com.az4.app.utils.SPUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * 个人中心：展示用户信息与全部借阅记录，支持退出登录。
 */
public class MineActivity extends AppCompatActivity {

    private TextView tvNickname;
    private TextView tvAccount;
    private RecyclerView rvRecords;
    private TextView tvEmptyRecords;
    private MaterialButton btnLogout;

    private BorrowRecordAdapter adapter;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine);

        db = DatabaseHelper.getInstance(this);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 页面回到前台时刷新借阅记录
        loadRecords();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvNickname = findViewById(R.id.tv_nickname);
        tvAccount = findViewById(R.id.tv_account);
        rvRecords = findViewById(R.id.rv_records);
        tvEmptyRecords = findViewById(R.id.tv_empty_records);
        btnLogout = findViewById(R.id.btn_logout);

        String nickname = SPUtils.getNickname(this);
        tvNickname.setText(nickname);
        tvAccount.setText("账号：" + SPUtils.getAccount(this));

        adapter = new BorrowRecordAdapter();
        rvRecords.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    /** 加载当前用户的借阅记录 */
    private void loadRecords() {
        List<BorrowRecord> records = db.getBorrowRecords(SPUtils.getUserId(this));
        adapter.setRecords(records);
        boolean empty = records.isEmpty();
        tvEmptyRecords.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvRecords.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("退出", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        SPUtils.clearLoginInfo(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
