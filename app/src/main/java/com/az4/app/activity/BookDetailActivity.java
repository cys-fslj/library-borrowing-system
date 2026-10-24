package com.az4.app.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.az4.app.R;
import com.az4.app.db.DatabaseHelper;
import com.az4.app.entity.Book;
import com.az4.app.entity.BorrowRecord;
import com.az4.app.entity.User;
import com.az4.app.utils.ImageUtils;
import com.az4.app.utils.SPUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

/**
 * 图书详情页：展示完整图书信息，并在底部完成借阅/归还操作。
 * 借阅与归还均为事务性操作：库存变更与记录写入要么全部成功，要么回滚。
 */
public class BookDetailActivity extends AppCompatActivity {

    /** 按钮模式：借阅 */
    private static final int MODE_BORROW = 0;
    /** 按钮模式：归还 */
    private static final int MODE_RETURN = 1;

    private MaterialToolbar toolbar;
    private ImageView ivCover;
    private TextView tvBookTitle;
    private TextView tvBookAuthor;
    private TextView tvBookPublisher;
    private TextView tvBookIsbn;
    private TextView tvBookDescription;
    private TextView tvStockInfo;
    private Chip chipStock;
    private View cardBorrowStatus;
    private MaterialButton btnAction;

    private DatabaseHelper db;
    private Book book;
    private BorrowRecord activeRecord;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        db = DatabaseHelper.getInstance(this);
        userId = SPUtils.getUserId(this);

        int bookId = getIntent().getIntExtra(BookHomeActivity.EXTRA_BOOK_ID, -1);
        book = db.getBookById(bookId);
        if (book == null) {
            Toast.makeText(this, "图书不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        populateUi();
        setButtonMode();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivCover = findViewById(R.id.iv_cover);
        tvBookTitle = findViewById(R.id.tv_book_title);
        tvBookAuthor = findViewById(R.id.tv_book_author);
        tvBookPublisher = findViewById(R.id.tv_book_publisher);
        tvBookIsbn = findViewById(R.id.tv_book_isbn);
        tvBookDescription = findViewById(R.id.tv_book_description);
        tvStockInfo = findViewById(R.id.tv_stock_info);
        chipStock = findViewById(R.id.chip_stock);
        cardBorrowStatus = findViewById(R.id.card_borrow_status);
        btnAction = findViewById(R.id.btn_action);

        toolbar.setNavigationOnClickListener(v -> finish());
        btnAction.setOnClickListener(v -> onActionClick());
    }

    /** 填充界面数据 */
    private void populateUi() {
        tvBookTitle.setText(book.getTitle());
        tvBookAuthor.setText("作者：" + safe(book.getAuthor()));
        tvBookPublisher.setText("出版社：" + safe(book.getPublisher()));
        tvBookIsbn.setText("ISBN：" + safe(book.getIsbn()));
        tvBookDescription.setText(safe(book.getDescription()));
        tvStockInfo.setText("库存 " + book.getStock() + " / 共 " + book.getTotal() + " 本");

        chipStock.setText(book.hasStock() ? "可借阅" : "暂无库存");
        int bg = book.hasStock() ? R.color.chip_stock_bg : R.color.logout_red;
        int fg = book.hasStock() ? R.color.primary : R.color.white;
        chipStock.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, bg)));
        chipStock.setTextColor(ContextCompat.getColor(this, fg));

        ImageUtils.loadBookCover(ivCover, book.getCover());
    }

    /** 根据当前用户是否已借阅本书，切换按钮模式 */
    private void setButtonMode() {
        activeRecord = db.getActiveBorrowRecord(userId, book.getId());
        if (activeRecord != null) {
            btnAction.setText("归还");
            btnAction.setTag(MODE_RETURN);
            btnAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.logout_red)));
            cardBorrowStatus.setVisibility(View.VISIBLE);
        } else {
            btnAction.setText("借阅");
            btnAction.setTag(MODE_BORROW);
            btnAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary)));
            cardBorrowStatus.setVisibility(View.GONE);
        }
    }

    private void onActionClick() {
        Object tag = btnAction.getTag();
        int mode = tag instanceof Integer ? (Integer) tag : MODE_BORROW;
        if (mode == MODE_RETURN) {
            doReturn();
        } else {
            doBorrow();
        }
    }

    /** 借书：库存 -1 + 生成借阅记录（同一事务） */
    private void doBorrow() {
        if (!book.hasStock()) {
            Toast.makeText(this, "该书暂时没有库存", Toast.LENGTH_SHORT).show();
            return;
        }
        User user = new User();
        user.setId(userId);
        boolean success = db.borrowBook(user, book);
        if (success) {
            Toast.makeText(this, "借阅成功，请按时归还", Toast.LENGTH_SHORT).show();
            refreshBook();
        } else {
            Toast.makeText(this, "借阅失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /** 还书：库存 +1 + 记录置为已归还（同一事务） */
    private void doReturn() {
        if (activeRecord == null) {
            Toast.makeText(this, "未找到借阅记录", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success = db.returnBook(activeRecord.getId(), book.getId());
        if (success) {
            Toast.makeText(this, "归还成功，感谢使用", Toast.LENGTH_SHORT).show();
            refreshBook();
        } else {
            Toast.makeText(this, "归还失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /** 重新从数据库读取图书，保证界面与库存一致 */
    private void refreshBook() {
        Book latest = db.getBookById(book.getId());
        if (latest != null) {
            book = latest;
        }
        populateUi();
        setButtonMode();
    }

    private String safe(String text) {
        return TextUtils.isEmpty(text) ? "未知" : text;
    }
}
