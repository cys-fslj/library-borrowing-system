package com.az4.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.az4.app.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * 图书封面加载工具类。
 * 封面图片预置在 assets/book_covers/ 目录下，读取时先取尺寸再按最大宽度采样，避免 OOM。
 */
public class ImageUtils {

    private static final String COVER_DIR = "book_covers/";
    private static final int MAX_WIDTH = 400;

    private ImageUtils() {
    }

    /**
     * 从 assets 加载图书封面到 ImageView。
     *
     * @param cover 封面文件名，例如 "st.jpg"
     */
    public static void loadBookCover(ImageView imageView, String cover) {
        if (imageView == null) {
            return;
        }
        if (cover == null || cover.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.ic_book_placeholder);
            return;
        }
        Context context = imageView.getContext();
        try {
            // 第一步：只读图片尺寸，不下采样，节省内存
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getAssets().open(COVER_DIR + cover)) {
                BitmapFactory.decodeStream(is, null, options);
            }
            // 第二步：按最大宽度计算采样率
            options.inSampleSize = calculateSampleSize(options.outWidth, MAX_WIDTH);
            options.inJustDecodeBounds = false;
            Bitmap bitmap;
            try (InputStream is = context.getAssets().open(COVER_DIR + cover)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }
            if (bitmap != null) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_book_placeholder);
            }
        } catch (IOException e) {
            // 封面缺失时降级为占位图，不影响功能
            imageView.setImageResource(R.drawable.ic_book_placeholder);
        }
    }

    /** 计算 2 的幂次采样率 */
    private static int calculateSampleSize(int originWidth, int targetWidth) {
        int sampleSize = 1;
        if (originWidth <= 0 || targetWidth <= 0) {
            return sampleSize;
        }
        while (originWidth / sampleSize > targetWidth) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}
