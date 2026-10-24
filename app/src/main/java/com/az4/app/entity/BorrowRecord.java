package com.az4.app.entity;

/**
 * 借阅记录实体类，对应数据库 t_borrow 表。
 * 为了列表查询性能，冗余存储了书名与作者。
 */
public class BorrowRecord {

    public static final int STATUS_BORROWING = 0; // 借阅中
    public static final int STATUS_RETURNED = 1;  // 已归还

    private int id;
    private int userId;
    private int bookId;
    private String bookTitle;   // 冗余字段：书名
    private String bookAuthor;  // 冗余字段：作者
    private String borrowTime;  // 借阅时间 yyyy-MM-dd HH:mm
    private String returnTime;  // 归还时间，未归还时为空
    private int status;         // 0 借阅中 / 1 已归还

    public BorrowRecord() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getBorrowTime() {
        return borrowTime;
    }

    public void setBorrowTime(String borrowTime) {
        this.borrowTime = borrowTime;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /** 是否仍在借阅中 */
    public boolean isBorrowing() {
        return status == STATUS_BORROWING;
    }
}
