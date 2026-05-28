package com.example.mapp.ui.biology;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.Biology;
import com.example.mapp.util.BiologyImageLoader;
import com.example.mapp.util.NetworkUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BiologyDetailActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private ImageView imageView;
    private TextView nameText;
    private TextView enNameText;
    private TextView introText;
    private TextView habitsText;
    private TextView distributionText;
    private TextView protectionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biology_detail);

        progressBar = findViewById(R.id.progress_bar);
        imageView = findViewById(R.id.image_biology);
        nameText = findViewById(R.id.text_name);
        enNameText = findViewById(R.id.text_en_name);
        introText = findViewById(R.id.text_intro);
        habitsText = findViewById(R.id.text_habits);
        distributionText = findViewById(R.id.text_distribution);
        protectionText = findViewById(R.id.text_protection);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String biologyName = getIntent().getStringExtra("biology_name");
        if (biologyName != null && !biologyName.isEmpty()) {
            loadBiologyDetail(biologyName);
        } else {
            NetworkUtil.showFailureToast(this, new IllegalArgumentException(getString(R.string.error_recognize_param)));
            finish();
        }
    }

    private void loadBiologyDetail(String name) {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService();
        apiService.getBiologyByName(name).enqueue(new Callback<ApiResponse<Biology>>() {
            @Override
            public void onResponse(Call<ApiResponse<Biology>> call, Response<ApiResponse<Biology>> response) {
                progressBar.setVisibility(View.GONE);
                if (!isFinishing() && response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Biology biology = response.body().getData();
                    if (biology != null) {
                        displayBiology(biology);
                        return;
                    }
                }
                NetworkUtil.showErrorResponseToast(BiologyDetailActivity.this, response);
            }

            @Override
            public void onFailure(Call<ApiResponse<Biology>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                NetworkUtil.showFailureToast(BiologyDetailActivity.this, t);
            }
        });
    }

    private void displayBiology(Biology biology) {
        nameText.setText(biology.getBioName());
        enNameText.setText(biology.getEnName());
        introText.setText(biology.getIntro());
        setOptionalLine(habitsText, getString(R.string.label_habits), biology.getHabits());
        setOptionalLine(distributionText, getString(R.string.label_distribution), biology.getDistribution());
        setOptionalLine(protectionText, getString(R.string.label_protection), biology.getProtectionLevel());

        BiologyImageLoader.load(imageView, biology);
    }

    private void setOptionalLine(TextView view, String label, String value) {
        if (value != null && !value.isEmpty()) {
            view.setVisibility(View.VISIBLE);
            view.setText(label + "：" + value);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}
