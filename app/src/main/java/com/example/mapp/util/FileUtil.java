package com.example.mapp.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtil {
    public static Uri saveBitmapToCache(Context context, Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            File file = new File(context.getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            out.close();
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 采样检测图片是否过暗，避免误识别 */
    public static boolean isImageTooDark(Context context, Uri uri) {
        if (uri == null) return true;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return true;
            Bitmap sample = BitmapFactory.decodeStream(input, null, options);
            input.close();
            if (sample == null) return true;
            int w = sample.getWidth();
            int h = sample.getHeight();
            long sum = 0;
            int step = Math.max(1, Math.min(w, h) / 32);
            int count = 0;
            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    int pixel = sample.getPixel(x, y);
                    sum += ((pixel >> 16) & 0xff) + ((pixel >> 8) & 0xff) + (pixel & 0xff);
                    count++;
                }
            }
            sample.recycle();
            if (count == 0) return true;
            double brightness = sum / (count * 3.0);
            return brightness < 28;
        } catch (Exception e) {
            return false;
        }
    }

    public static File getFileFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = sanitizeFileName(getFileName(context, uri));
            File tempFile = new File(context.getCacheDir(), fileName);

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileName(Context context, Uri uri) {
        String result = "upload_image.jpg";
        if (uri != null) {
            ContentResolver resolver = context.getContentResolver();
            android.database.Cursor cursor = resolver.query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return result;
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "upload_image.jpg";
        }
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!safe.contains(".")) {
            safe = safe + ".jpg";
        }
        return safe;
    }
}