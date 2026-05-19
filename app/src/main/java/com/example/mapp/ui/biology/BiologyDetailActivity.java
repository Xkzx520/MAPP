package com.example.mapp.ui.biology;

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
import com.example.mapp.model.Biology;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BiologyDetailActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView nameText;
    private TextView enNameText;
    private TextView introText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biology_detail);

        progressBar = findViewById(R.id.progress_bar);
        nameText = findViewById(R.id.text_name);
        enNameText = findViewById(R.id.text_en_name);
        introText = findViewById(R.id.text_intro);

        String biologyName = getIntent().getStringExtra("biology_name");
        if (biologyName != null && !biologyName.isEmpty()) {
            loadBiologyDetail(biologyName);
        } else {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
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
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Biology biology = response.body().getData();
                    if (biology != null) {
                        displayBiology(biology);
                    }
                } else {
                    Toast.makeText(BiologyDetailActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Biology>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BiologyDetailActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBiology(Biology biology) {
        nameText.setText(biology.getBioName());
        enNameText.setText(biology.getEnName());
        introText.setText(biology.getIntro());
    }
}