package com.az4.app.entity;

/**
 * 用户实体类，对应数据库 t_user 表。
 */
public class User {

    private int id;
    private String account;   // 登录账号
    private String nickname;  // 昵称
    private String password;  // 密码

    public User() {
    }

    public User(String account, String nickname, String password) {
        this.account = account;
        this.nickname = nickname;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
