package com.az4.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.az4.app.entity.Book;
import com.az4.app.entity.BorrowRecord;
import com.az4.app.entity.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite 数据库帮助类。
 * 负责 t_user（用户表）、t_book（图书表）、t_borrow（借阅记录表）三张表的建表与完整 CRUD，
 * 以及借阅/归还的事务性处理。
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "library.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE_USER = "t_user";
    public static final String TABLE_BOOK = "t_book";
    public static final String TABLE_BORROW = "t_borrow";

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private static volatile DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /** 单例，避免多处持有多个连接 */
    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 用户表
        db.execSQL("CREATE TABLE " + TABLE_USER + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "account TEXT NOT NULL UNIQUE, "
                + "nickname TEXT, "
                + "password TEXT NOT NULL)");

        // 图书表
        db.execSQL("CREATE TABLE " + TABLE_BOOK + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL, "
                + "author TEXT, "
                + "category TEXT, "
                + "publisher TEXT, "
                + "isbn TEXT, "
                + "description TEXT, "
                + "cover TEXT, "
                + "stock INTEGER DEFAULT 0, "
                + "total INTEGER DEFAULT 0)");

        // 借阅记录表
        db.execSQL("CREATE TABLE " + TABLE_BORROW + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "book_id INTEGER NOT NULL, "
                + "book_title TEXT, "
                + "book_author TEXT, "
                + "borrow_time TEXT, "
                + "return_time TEXT, "
                + "status INTEGER DEFAULT 0)");

        seedBooks(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // v1 -> v2：统一封面路径为 "文件名.jpg"，历史数据为 "cover/xxx" 的做一次修正
            db.execSQL("UPDATE " + TABLE_BOOK + " SET cover = REPLACE(cover, 'cover/', '') "
                    + "WHERE cover LIKE 'cover/%'");
        }
    }

    // ==================== 用户相关 ====================

    /** 注册用户，返回新用户 id，失败返回 -1 */
    public long insertUser(User user) {
        ContentValues values = new ContentValues();
        values.put("account", user.getAccount());
        values.put("nickname", user.getNickname());
        values.put("password", user.getPassword());
        return getWritableDatabase().insert(TABLE_USER, null, values);
    }

    /** 账号是否已存在 */
    public boolean isAccountExist(String account) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_USER, new String[]{"id"},
                "account = ?", new String[]{account}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    /** 登录校验，成功返回用户对象，失败返回 null */
    public User login(String account, String password) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_USER,
                new String[]{"id", "account", "nickname", "password"},
                "account = ? AND password = ?", new String[]{account, password},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                User user = new User();
                user.setId(cursor.getInt(0));
                user.setAccount(cursor.getString(1));
                user.setNickname(cursor.getString(2));
                user.setPassword(cursor.getString(3));
                return user;
            }
        }
        return null;
    }

    // ==================== 图书相关 ====================

    /** 查询全部图书 */
    public List<Book> getAllBooks() {
        return queryBooks(null, null);
    }

    /**
     * 按关键词模糊搜索：书名、作者、分类三字段任一匹配。
     */
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        String like = "%" + keyword.trim() + "%";
        String where = "title LIKE ? OR author LIKE ? OR category LIKE ?";
        return queryBooks(where, new String[]{like, like, like});
    }

    private List<Book> queryBooks(String where, String[] args) {
        List<Book> books = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_BOOK, null, where, args,
                null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                books.add(readBook(cursor));
            }
        }
        return books;
    }

    /** 按 id 查询单本图书 */
    public Book getBookById(int bookId) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_BOOK, null, "id = ?",
                new String[]{String.valueOf(bookId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return readBook(cursor);
            }
        }
        return null;
    }

    private Book readBook(Cursor cursor) {
        Book book = new Book();
        book.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        book.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        book.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow("author")));
        book.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        book.setPublisher(cursor.getString(cursor.getColumnIndexOrThrow("publisher")));
        book.setIsbn(cursor.getString(cursor.getColumnIndexOrThrow("isbn")));
        book.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        book.setCover(cursor.getString(cursor.getColumnIndexOrThrow("cover")));
        book.setStock(cursor.getInt(cursor.getColumnIndexOrThrow("stock")));
        book.setTotal(cursor.getInt(cursor.getColumnIndexOrThrow("total")));
        return book;
    }

    /**
     * 更新库存：delta 为 +1 或 -1。
     * 借书时库存必须大于 0 才会更新成功。
     */
    public boolean updateBookStock(int bookId, int delta) {
        SQLiteDatabase db = getWritableDatabase();
        String sql;
        if (delta < 0) {
            // 借出：库存必须大于 0 才允许扣减
            sql = "UPDATE " + TABLE_BOOK + " SET stock = stock + " + delta
                    + " WHERE id = ? AND stock > 0";
        } else {
            sql = "UPDATE " + TABLE_BOOK + " SET stock = stock + " + delta + " WHERE id = ?";
        }
        SQLiteStatement statement = db.compileStatement(sql);
        statement.bindLong(1, bookId);
        int affected = statement.executeUpdateDelete();
        return affected > 0;
    }

    /** 批量写入图书（网络同步用） */
    public void insertBooksBatch(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Book book : books) {
                ContentValues values = new ContentValues();
                values.put("title", book.getTitle());
                values.put("author", book.getAuthor());
                values.put("category", book.getCategory());
                values.put("publisher", book.getPublisher());
                values.put("isbn", book.getIsbn());
                values.put("description", book.getDescription());
                values.put("cover", book.getCover());
                values.put("stock", book.getStock());
                values.put("total", book.getTotal());
                db.insert(TABLE_BOOK, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ==================== 借阅相关 ====================

    /** 插入借阅记录，返回记录 id */
    public long insertBorrowRecord(BorrowRecord record) {
        ContentValues values = new ContentValues();
        values.put("user_id", record.getUserId());
        values.put("book_id", record.getBookId());
        values.put("book_title", record.getBookTitle());
        values.put("book_author", record.getBookAuthor());
        values.put("borrow_time", record.getBorrowTime());
        values.put("status", record.getStatus());
        return getWritableDatabase().insert(TABLE_BORROW, null, values);
    }

    /** 更新借阅记录为已归还 */
    public boolean updateBorrowRecordReturned(int recordId, String returnTime) {
        ContentValues values = new ContentValues();
        values.put("status", BorrowRecord.STATUS_RETURNED);
        values.put("return_time", returnTime);
        int affected = getWritableDatabase().update(TABLE_BORROW, values, "id = ?",
                new String[]{String.valueOf(recordId)});
        return affected > 0;
    }

    /** 查询某用户的全部借阅记录（按借阅时间倒序） */
    public List<BorrowRecord> getBorrowRecords(int userId) {
        List<BorrowRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_BORROW, null, "user_id = ?",
                new String[]{String.valueOf(userId)}, null, null, "id DESC")) {
            while (cursor.moveToNext()) {
                BorrowRecord record = new BorrowRecord();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                record.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                record.setBookId(cursor.getInt(cursor.getColumnIndexOrThrow("book_id")));
                record.setBookTitle(cursor.getString(cursor.getColumnIndexOrThrow("book_title")));
                record.setBookAuthor(cursor.getString(cursor.getColumnIndexOrThrow("book_author")));
                record.setBorrowTime(cursor.getString(cursor.getColumnIndexOrThrow("borrow_time")));
                record.setReturnTime(cursor.getString(cursor.getColumnIndexOrThrow("return_time")));
                record.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
                records.add(record);
            }
        }
        return records;
    }

    /** 查询该用户对某本书"借阅中"的记录，没有则返回 null */
    public BorrowRecord getActiveBorrowRecord(int userId, int bookId) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_BORROW, null,
                "user_id = ? AND book_id = ? AND status = ?",
                new String[]{String.valueOf(userId), String.valueOf(bookId),
                        String.valueOf(BorrowRecord.STATUS_BORROWING)},
                null, null, "id DESC")) {
            if (cursor.moveToFirst()) {
                BorrowRecord record = new BorrowRecord();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                record.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
                record.setBookId(cursor.getInt(cursor.getColumnIndexOrThrow("book_id")));
                record.setBookTitle(cursor.getString(cursor.getColumnIndexOrThrow("book_title")));
                record.setBookAuthor(cursor.getString(cursor.getColumnIndexOrThrow("book_author")));
                record.setBorrowTime(cursor.getString(cursor.getColumnIndexOrThrow("borrow_time")));
                record.setReturnTime(cursor.getString(cursor.getColumnIndexOrThrow("return_time")));
                record.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
                return record;
            }
        }
        return null;
    }

    /**
     * 借书事务：库存 -1 且写入借阅记录，任一步失败则回滚。
     *
     * @return 成功返回 true
     */
    public boolean borrowBook(User user, Book book) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            SQLiteStatement borrowStatement = db.compileStatement(
                    "UPDATE " + TABLE_BOOK + " SET stock = stock - 1 WHERE id = ? AND stock > 0");
            borrowStatement.bindLong(1, book.getId());
            int affected = borrowStatement.executeUpdateDelete();
            if (affected == 0) {
                return false; // 库存不足，事务结束时会自动回滚
            }
            ContentValues values = new ContentValues();
            values.put("user_id", user.getId());
            values.put("book_id", book.getId());
            values.put("book_title", book.getTitle());
            values.put("book_author", book.getAuthor());
            values.put("borrow_time", now());
            values.put("status", BorrowRecord.STATUS_BORROWING);
            long recordId = db.insert(TABLE_BORROW, null, values);
            if (recordId <= 0) {
                return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 还书事务：库存 +1 且把借阅记录置为已归还，任一步失败则回滚。
     */
    public boolean returnBook(int recordId, int bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            SQLiteStatement returnStatement = db.compileStatement(
                    "UPDATE " + TABLE_BOOK + " SET stock = stock + 1 WHERE id = ?");
            returnStatement.bindLong(1, bookId);
            int affected = returnStatement.executeUpdateDelete();
            if (affected == 0) {
                return false;
            }
            ContentValues values = new ContentValues();
            values.put("status", BorrowRecord.STATUS_RETURNED);
            values.put("return_time", now());
            int updated = db.update(TABLE_BORROW, values, "id = ?",
                    new String[]{String.valueOf(recordId)});
            if (updated == 0) {
                return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** 当前时间，格式 yyyy-MM-dd HH:mm */
    public static String now() {
        return new SimpleDateFormat(TIME_PATTERN, Locale.CHINA).format(new Date());
    }

    // ==================== 预置数据 ====================

    /** 首次建库时写入 10 本预置图书 */
    private void seedBooks(SQLiteDatabase db) {
        insertSeed(db, "三体", "刘慈欣", "科幻", "重庆出版社", "9787536692930",
                "地球文明向宇宙发出的一声啼鸣，引来了三体文明的凝视。文化大革命期间，一次绝密的军事计划把人类推向了宇宙深处，也让一个文明走向了毁灭的边缘。", "st.jpg", 5);
        insertSeed(db, "百年孤独", "加西亚·马尔克斯", "文学", "南海出版公司", "9787544253994",
                "布恩迪亚家族七代人的传奇故事，魔幻现实主义文学的经典之作。马孔多小镇从兴起到消亡，写尽了拉丁美洲的百年历史。", "bngd.jpg", 4);
        insertSeed(db, "红楼梦", "曹雪芹", "古典文学", "人民文学出版社", "9787020002207",
                "中国古典小说的巅峰之作。以贾宝玉与林黛玉的爱情悲剧为主线，描绘了贾府由盛转衰的全过程。", "hlm.jpg", 6);
        insertSeed(db, "活着", "余华", "文学", "作家出版社", "9787506365437",
                "福贵一生历经家族败落、亲人离散，却始终以平静的姿态活着。一部关于苦难与承受的生命之书。", "hz.jpg", 5);
        insertSeed(db, "平凡的世界", "路遥", "文学", "北京十月文艺出版社", "9787530216781",
                "以孙少安、孙少平兄弟为中心，展现中国城乡社会在改革年代中的巨大变迁与普通人的奋斗。", "pfdsj.jpg", 3);
        insertSeed(db, "人类简史", "尤瓦尔·赫拉利", "历史", "中信出版社", "9787508647357",
                "从认知革命到科学革命，重新梳理智人如何一步步成为地球的主宰。一部视野宏大的历史读物。", "rljs.jpg", 4);
        insertSeed(db, "时间简史", "史蒂芬·霍金", "科普", "湖南科学技术出版社", "9787535732682",
                "从宇宙大爆炸到黑洞辐射，霍金以通俗的语言讲述时间与宇宙的起源和命运。", "sjjs.jpg", 2);
        insertSeed(db, "1984", "乔治·奥威尔", "文学", "北京十月文艺出版社", "9787530210291",
                "老大哥在看着你。一部关于极权社会与思想控制的经典反乌托邦小说。", "1984.jpg", 3);
        insertSeed(db, "围城", "钱钟书", "文学", "人民文学出版社", "9787020024759",
                "婚姻是一座围城，城外的人想进去，城里的人想出来。幽默而犀利的笔法写尽知识分子的困境。", "wc.jpg", 4);
        insertSeed(db, "西游记", "吴承恩", "古典文学", "人民文学出版社", "9787020008735",
                "唐僧师徒四人西天取经，历经九九八十一难。神魔小说的不朽典范。", "xwz.jpg", 5);
    }

    private void insertSeed(SQLiteDatabase db, String title, String author, String category,
                            String publisher, String isbn, String description,
                            String cover, int stock) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("author", author);
        values.put("category", category);
        values.put("publisher", publisher);
        values.put("isbn", isbn);
        values.put("description", description);
        values.put("cover", cover);
        values.put("stock", stock);
        values.put("total", stock);
        db.insert(TABLE_BOOK, null, values);
    }
}
