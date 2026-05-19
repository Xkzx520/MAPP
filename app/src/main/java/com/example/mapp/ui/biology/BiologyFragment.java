package com.example.mapp.ui.biology;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.Biology;
import com.example.mapp.model.UploadResponse;
import com.example.mapp.util.FileUtil;
import com.google.android.material.card.MaterialCardView;
import java.io.File;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BiologyFragment extends Fragment {
    private ImageView imagePreview;
    private LinearLayout uploadPlaceholder;
    private Button btnSelectImage;
    private Button btnUpload;
    private ProgressBar progressBar;
    private MaterialCardView resultCard;
    private TextView textResult;
    private Button btnViewDetail;
    private RecyclerView recyclerView;
    private BiologyAdapter adapter;

    private Uri selectedImageUri;
    private String recognizedBiologyName;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        displaySelectedImage();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biology, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imagePreview = view.findViewById(R.id.image_preview);
        uploadPlaceholder = view.findViewById(R.id.upload_placeholder);
        btnSelectImage = view.findViewById(R.id.btn_select_image);
        btnUpload = view.findViewById(R.id.btn_upload);
        progressBar = view.findViewById(R.id.progress_bar);
        resultCard = view.findViewById(R.id.result_card);
        textResult = view.findViewById(R.id.text_result);
        btnViewDetail = view.findViewById(R.id.btn_view_detail);
        recyclerView = view.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new BiologyAdapter();
        recyclerView.setAdapter(adapter);

        setupClickListeners();
        loadBiologyList();
    }

    private void setupClickListeners() {
        View.OnClickListener selectImageClickListener = v -> openImagePicker();
        uploadPlaceholder.setOnClickListener(selectImageClickListener);
        imagePreview.setOnClickListener(selectImageClickListener);
        btnSelectImage.setOnClickListener(selectImageClickListener);

        btnUpload.setOnClickListener(v -> uploadImage());

        btnViewDetail.setOnClickListener(v -> {
            if (recognizedBiologyName != null && !recognizedBiologyName.isEmpty()) {
                Intent intent = new Intent(getContext(), BiologyDetailActivity.class);
                intent.putExtra("biology_name", recognizedBiologyName);
                startActivity(intent);
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            imagePreview.setVisibility(View.VISIBLE);
            uploadPlaceholder.setVisibility(View.GONE);
            btnUpload.setEnabled(true);
            resultCard.setVisibility(View.GONE);

            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(imagePreview);
        }
    }

    private void uploadImage() {
        if (selectedImageUri == null) {
            Toast.makeText(getContext(), R.string.select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);
        btnSelectImage.setEnabled(false);

        File file = FileUtil.getFileFromUri(requireContext(), selectedImageUri);
        if (file == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            resetButtons();
            return;
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), requestBody);

        ApiService apiService = ApiClient.getApiService();
        apiService.uploadFile(part).enqueue(new Callback<ApiResponse<UploadResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UploadResponse>> call, Response<ApiResponse<UploadResponse>> response) {
                progressBar.setVisibility(View.GONE);
                resetButtons();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UploadResponse uploadResponse = response.body().getData();
                    if (uploadResponse != null) {
                        Toast.makeText(getContext(), R.string.upload_success, Toast.LENGTH_SHORT).show();
                        recognizedBiologyName = "小丑鱼";
                        showRecognitionResult(recognizedBiologyName);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.upload_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UploadResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                resetButtons();
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRecognitionResult(String biologyName) {
        textResult.setText(biologyName);
        resultCard.setVisibility(View.VISIBLE);
    }

    private void resetButtons() {
        btnUpload.setEnabled(selectedImageUri != null);
        btnSelectImage.setEnabled(true);
    }

    private void loadBiologyList() {
        ApiService apiService = ApiClient.getApiService();
        apiService.getBiologyList().enqueue(new Callback<ApiResponse<List<Biology>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Biology>>> call, Response<ApiResponse<List<Biology>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Biology> biologyList = response.body().getData();
                    if (biologyList != null && !biologyList.isEmpty()) {
                        adapter.setData(biologyList);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Biology>>> call, Throwable t) {
            }
        });
    }

    private class BiologyAdapter extends RecyclerView.Adapter<BiologyAdapter.ViewHolder> {
        private List<Biology> biologyList;

        void setData(List<Biology> biologyList) {
            this.biologyList = biologyList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_biology, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Biology biology = biologyList.get(position);
            holder.bind(biology);
        }

        @Override
        public int getItemCount() {
            return biologyList == null ? 0 : biologyList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final MaterialCardView cardView;
            private final TextView nameText;
            private final TextView enNameText;
            private final TextView introText;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_view);
                nameText = itemView.findViewById(R.id.text_name);
                enNameText = itemView.findViewById(R.id.text_en_name);
                introText = itemView.findViewById(R.id.text_intro);
            }

            void bind(Biology biology) {
                nameText.setText(biology.getBioName());
                enNameText.setText(biology.getEnName());
                introText.setText(biology.getIntro());

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), BiologyDetailActivity.class);
                    intent.putExtra("biology_name", biology.getBioName());
                    startActivity(intent);
                });
            }
        }
    }
}