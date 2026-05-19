package com.example.mapp.ui.course;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.Course;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseDetailActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView nameText;
    private TextView typeText;
    private TextView introText;
    private TextView timeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        progressBar = findViewById(R.id.progress_bar);
        nameText = findViewById(R.id.text_name);
        typeText = findViewById(R.id.text_type);
        introText = findViewById(R.id.text_intro);
        timeText = findViewById(R.id.text_time);

        int courseId = getIntent().getIntExtra("course_id", -1);
        if (courseId != -1) {
            loadCourseDetail(courseId);
        } else {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadCourseDetail(int courseId) {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();
        apiService.getCourseDetail(courseId).enqueue(new Callback<ApiResponse<Course>>() {
            @Override
            public void onResponse(Call<ApiResponse<Course>> call, Response<ApiResponse<Course>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Course course = response.body().getData();
                    if (course != null) {
                        displayCourse(course);
                    }
                } else {
                    Toast.makeText(CourseDetailActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Course>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CourseDetailActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCourse(Course course) {
        nameText.setText(course.getCourseName());
        typeText.setText(course.getCourseTypeName());
        introText.setText(course.getIntro());
        timeText.setText(course.getCreateTime());
    }
}