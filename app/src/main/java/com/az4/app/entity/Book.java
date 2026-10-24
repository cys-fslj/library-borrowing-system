package com.az4.app.entity;

import java.io.Serializable;

/**
 * 图书实体类，对应数据库 t_book 表。
 */
public class Book implements Serializable {

    private int id;
    private String title;        // 书名
    private String author;       // 作者
    private String category;     // 分类
    private String publisher;    // 出版社
    private String isbn;         // ISBN
    private String description;  // 内容简介
    private String cover;        // 封面文件名（assets/book_covers 下）
    private int stock;           // 当前可借库存
    private int total;           // 馆藏总量

    public Book() {
    }

    public Book(String title, String author, String category, String publisher,
                String isbn, String description, String cover, int stock, int total) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.isbn = isbn;
        this.description = description;
        this.cover = cover;
        this.stock = stock;
        this.total = total;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    /** 是否还有库存可借 */
    public boolean hasStock() {
        return stock > 0;
    }
}
