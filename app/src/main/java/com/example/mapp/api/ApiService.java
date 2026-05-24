package com.example.mapp.api;

import com.example.mapp.model.AIInfo;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.Biology;
import com.example.mapp.model.BiologyDetection;
import com.example.mapp.model.Course;
import com.example.mapp.model.LoginRequest;
import com.example.mapp.model.UploadResponse;
import com.example.mapp.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @GET("course/list")
    Call<ApiResponse<List<Course>>> getCourseList();

    @GET("course/detail/{id}")
    Call<ApiResponse<Course>> getCourseDetail(@Path("id") int id);

    @GET("biology/list")
    Call<ApiResponse<List<Biology>>> getBiologyList();

    @GET("biology/name/{name}")
    Call<ApiResponse<Biology>> getBiologyByName(@Path("name") String name);

    @GET("ai/info")
    Call<ApiResponse<AIInfo>> getAIInfo();

    @GET("ai/health")
    Call<ApiResponse<String>> healthCheck();

    @Multipart
    @POST("ai/detect")
    Call<ApiResponse<BiologyDetection>> detectBiology(@Part MultipartBody.Part file);

    @POST("user/login")
    Call<ApiResponse<User>> login(@Body LoginRequest request);

    @GET("user/info/{userId}")
    Call<ApiResponse<User>> getUserInfo(@Path("userId") int userId);

    @Multipart
    @POST("file/upload")
    Call<ApiResponse<UploadResponse>> uploadFile(@Part MultipartBody.Part file);
}