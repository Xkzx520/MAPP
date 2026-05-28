package com.example.mapp.util;

import android.content.Context;
import android.widget.Toast;

public final class ToastUtil {

    private ToastUtil() {
    }

    public static void showShort(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
