package com.example.mapp.util;

import android.content.Context;
import android.widget.Toast;

import com.example.mapp.R;
import com.example.mapp.api.ApiConfig;
import com.example.mapp.model.ApiResponse;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Response;

public final class NetworkUtil {

    private NetworkUtil() {
    }

    public static void showFailureToast(Context context, Throwable t) {
        String detail = t != null && t.getMessage() != null ? t.getMessage() : "";
        String message;
        if (t instanceof UnknownHostException || t instanceof ConnectException) {
            message = context.getString(R.string.error_connect, ApiConfig.getBaseUrl());
        } else if (t instanceof SocketTimeoutException) {
            message = context.getString(R.string.error_timeout);
        } else {
            message = context.getString(R.string.error_network) + (detail.isEmpty() ? "" : ": " + detail);
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static <T> void showErrorResponseToast(Context context, Response<ApiResponse<T>> response) {
        String msg = context.getString(R.string.error_network);
        if (response != null && response.errorBody() != null) {
            try {
                msg = msg + " (" + response.code() + ")";
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static <T> String getApiErrorMessage(Context context, Response<ApiResponse<T>> response) {
        if (response != null && response.body() != null && response.body().getMsg() != null) {
            return response.body().getMsg();
        }
        if (response != null) {
            return context.getString(R.string.error_network) + " (" + response.code() + ")";
        }
        return context.getString(R.string.error_network);
    }
}
