package com.az4.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 轻量级持久化工具类。
 * 负责保存/读取登录状态与当前用户信息，实现"下次打开自动免登录"。
 */
public class SPUtils {

    private static final String SP_NAME = "library_app";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_IS_LOGIN = "is_login";

    private SPUtils() {
    }

    private static SharedPreferences getSp(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存登录信息。
     */
    public static void saveLoginInfo(Context context, int userId, String account, String nickname) {
        getSp(context).edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_ACCOUNT, account)
                .putString(KEY_NICKNAME, nickname)
                .putBoolean(KEY_IS_LOGIN, true)
                .apply();
    }

    /** 当前登录用户 id，未登录返回 -1 */
    public static int getUserId(Context context) {
        return getSp(context).getInt(KEY_USER_ID, -1);
    }

    public static String getAccount(Context context) {
        return getSp(context).getString(KEY_ACCOUNT, "");
    }

    public static String getNickname(Context context) {
        return getSp(context).getString(KEY_NICKNAME, "");
    }

    /** 是否已登录 */
    public static boolean isLogin(Context context) {
        return getSp(context).getBoolean(KEY_IS_LOGIN, false) && getUserId(context) > 0;
    }

    /** 退出登录时清除登录信息 */
    public static void clearLoginInfo(Context context) {
        getSp(context).edit().clear().apply();
    }
}
