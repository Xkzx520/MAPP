package com.example.mapp.ui.course;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.Course;
import com.example.mapp.ui.MainActivity;
import com.example.mapp.util.NetworkUtil;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseFragment extends Fragment {

    private static final int TYPE_RECOGNITION = 1;
    private static final int TYPE_CURRENT = 2;

    private ProgressBar progressBar;
    private TextView emptyView;
    private View scrollContent;

    private TextView recognitionNameText;
    private TextView recognitionIntroText;
    private TextView currentNameText;
    private TextView currentIntroText;
    private MaterialCardView sectionRecognition;
    private MaterialCardView sectionCurrent;
    private Button btnGoRecognition;
    private Button btnGoCurrent;

    private Course recognitionCourse;
    private Course currentCourse;
    private Call<ApiResponse<List<Course>>> coursesCall;
    private boolean coursesLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        scrollContent = view.findViewById(R.id.scroll_content);

        recognitionNameText = view.findViewById(R.id.text_recognition_name);
        recognitionIntroText = view.findViewById(R.id.text_recognition_intro);
        currentNameText = view.findViewById(R.id.text_current_name);
        currentIntroText = view.findViewById(R.id.text_current_intro);
        sectionRecognition = view.findViewById(R.id.section_recognition);
        sectionCurrent = view.findViewById(R.id.section_current);
        btnGoRecognition = view.findViewById(R.id.btn_go_recognition);
        btnGoCurrent = view.findViewById(R.id.btn_go_current);

        btnGoRecognition.setOnClickListener(v -> openRecognition());
        sectionRecognition.setOnClickListener(v -> openRecognition());

        btnGoCurrent.setOnClickListener(v -> openCurrentCourseDetail());
        sectionCurrent.setOnClickListener(v -> openCurrentCourseDetail());

        setupHeroSection(view);

        if (!coursesLoaded) {
            loadCourses();
        }
    }

    @Override
    public void onDestroyView() {
        if (coursesCall != null) {
            coursesCall.cancel();
            coursesCall = null;
        }
        super.onDestroyView();
    }

    private void loadCourses() {
        if (coursesCall != null) {
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        // 先展示默认文案，避免等待接口时整屏空白
        scrollContent.setVisibility(View.VISIBLE);
        bindCourses(java.util.Collections.emptyList());

        ApiService apiService = ApiClient.getApiService();
        coursesCall = apiService.getCourseList();
        coursesCall.enqueue(new Callback<ApiResponse<List<Course>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Course>>> call,
                                   Response<ApiResponse<List<Course>>> response) {
                coursesCall = null;
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                coursesLoaded = true;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Course> courses = response.body().getData();
                    if (courses != null && !courses.isEmpty()) {
                        bindCourses(courses);
                        scrollContent.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    NetworkUtil.showErrorResponseToast(requireContext(), response);
                    emptyView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Course>>> call, Throwable t) {
                coursesCall = null;
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                NetworkUtil.showFailureToast(requireContext(), t);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindCourses(List<Course> courses) {
        recognitionCourse = findCourse(courses, TYPE_RECOGNITION);
        currentCourse = findCourse(courses, TYPE_CURRENT);

        if (recognitionCourse != null) {
            recognitionNameText.setText(recognitionCourse.getCourseName());
            recognitionIntroText.setText(recognitionCourse.getIntro());
        } else {
            recognitionNameText.setText(R.string.section_recognition_fallback_name);
            recognitionIntroText.setText(R.string.section_recognition_fallback_intro);
        }

        if (currentCourse != null) {
            currentNameText.setText(currentCourse.getCourseName());
            currentIntroText.setText(currentCourse.getIntro());
        } else {
            currentNameText.setText(R.string.section_current_fallback_name);
            currentIntroText.setText(R.string.section_current_fallback_intro);
        }
    }

    private Course findCourse(List<Course> courses, int courseType) {
        for (Course course : courses) {
            if (course.getCourseType() == courseType) {
                return course;
            }
        }
        for (Course course : courses) {
            String name = course.getCourseName() != null ? course.getCourseName() : "";
            if (courseType == TYPE_RECOGNITION && (name.contains("识别") || name.contains("生物"))) {
                return course;
            }
            if (courseType == TYPE_CURRENT && (name.contains("洋流") || name.contains("模拟"))) {
                return course;
            }
        }
        return null;
    }

    private void setupHeroSection(View root) {
        TextView title = root.findViewById(R.id.hero_title);
        if (title == null || !isAdded()) {
            return;
        }
        try {
            android.graphics.Typeface tf = ResourcesCompat.getFont(requireContext(), R.font.instrument_serif);
            if (tf != null) {
                title.setTypeface(tf);
            }
        } catch (Exception ignored) {
            title.setTypeface(android.graphics.Typeface.SERIF);
        }
        int width = getResources().getDisplayMetrics().widthPixels;
        if (width >= getResources().getDimensionPixelSize(R.dimen.hero_nav_max_width) / 2) {
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.hero_title_size_large));
        }
    }

    private void openRecognition() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToTab(R.id.nav_recognition);
        }
    }

    private void openCurrentCourseDetail() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToTab(R.id.nav_ai);
        }
    }
}
