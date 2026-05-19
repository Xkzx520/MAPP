package com.example.mapp.ui.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.AIInfo;
import com.example.mapp.model.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIFragment extends Fragment {
    private ProgressBar progressBar;
    private View contentLayout;
    private TextView modelNameText;
    private TextView versionText;
    private TextView classCountText;
    private TextView updateTimeText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_bar);
        contentLayout = view.findViewById(R.id.content_layout);
        modelNameText = view.findViewById(R.id.text_model_name);
        versionText = view.findViewById(R.id.text_version);
        classCountText = view.findViewById(R.id.text_class_count);
        updateTimeText = view.findViewById(R.id.text_update_time);

        loadAIInfo();
    }

    private void loadAIInfo() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getApiService();
        apiService.getAIInfo().enqueue(new Callback<ApiResponse<AIInfo>>() {
            @Override
            public void onResponse(Call<ApiResponse<AIInfo>> call, Response<ApiResponse<AIInfo>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AIInfo aiInfo = response.body().getData();
                    if (aiInfo != null) {
                        displayAIInfo(aiInfo);
                        contentLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AIInfo>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAIInfo(AIInfo aiInfo) {
        modelNameText.setText(aiInfo.getModelName());
        versionText.setText(aiInfo.getVersion());
        classCountText.setText(String.valueOf(aiInfo.getClassCount()));
        updateTimeText.setText(aiInfo.getUpdateTime());
    }
}