package com.az4.app.utils;

import android.os.Handler;
import android.os.Looper;

import com.az4.app.entity.Book;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络请求工具类，基于 OkHttp3 封装。
 * 仅用于后台同步图书数据，属于 P1 增强功能：
 * 网络不可用时静默降级，核心功能始终由本地 SQLite 支撑。
 */
public class HttpClientUtils {

    /** 图书数据接口地址（示例接口，失败时自动降级到本地数据） */
    private static final String BOOKS_URL = "https://example.com/api/library/books";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private HttpClientUtils() {
    }

    /** 网络回调，始终在主线程回调 */
    public interface BookCallback {
        void onSuccess(List<Book> books);

        void onFailure(Exception e);
    }

    /**
     * 异步拉取远程图书数据。子线程执行请求，主线程回调结果。
     */
    public static void fetchBooksFromNetwork(final BookCallback callback) {
        Request request = new Request.Builder()
                .url(BOOKS_URL)
                .get()
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onFailure(e));
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response resp = response) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        throw new IOException("HTTP " + resp.code());
                    }
                    final List<Book> books = parseBooks(resp.body().string());
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onSuccess(books));
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onFailure(e));
                    }
                }
            }
        });
    }

    /** 解析 JSON 数组为图书列表 */
    private static List<Book> parseBooks(String json) throws Exception {
        List<Book> list = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            Book book = new Book();
            book.setTitle(o.optString("title"));
            book.setAuthor(o.optString("author"));
            book.setCategory(o.optString("category"));
            book.setPublisher(o.optString("publisher"));
            book.setIsbn(o.optString("isbn"));
            book.setDescription(o.optString("description"));
            book.setCover(o.optString("cover"));
            book.setStock(o.optInt("stock"));
            book.setTotal(o.optInt("total", o.optInt("stock")));
            list.add(book);
        }
        return list;
    }
}
