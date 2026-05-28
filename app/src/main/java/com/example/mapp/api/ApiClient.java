package com.example.mapp.api;

import android.util.Log;
import com.example.mapp.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                Log.i(NetworkLogInterceptor.TAG, "API baseUrl=" + ApiConfig.getBaseUrl());
                clientBuilder.addInterceptor(new NetworkLogInterceptor());
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                        message -> Log.d(NetworkLogInterceptor.TAG, message));
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                clientBuilder.addInterceptor(logging);
            }

            Gson gson = new GsonBuilder()
                    .serializeNulls()
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.getBaseUrl())
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(ApiService.class);
        }
        return apiService;
    }
}
