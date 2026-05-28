package com.example.mapp.ui.biology;



import android.content.Intent;

import android.net.Uri;

import android.os.Bundle;

import android.speech.tts.TextToSpeech;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.Button;

import android.widget.ImageView;

import android.widget.LinearLayout;

import android.widget.ProgressBar;

import android.widget.TextView;

import android.widget.Toast;

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

import com.example.mapp.model.BiologyDetection;

import com.example.mapp.util.BiologyImageLoader;
import com.example.mapp.util.FileUtil;

import com.example.mapp.util.ImagePickerHelper;

import com.example.mapp.util.NetworkUtil;

import com.google.android.material.card.MaterialCardView;

import java.io.File;

import java.util.List;

import java.util.Locale;

import okhttp3.MediaType;

import okhttp3.MultipartBody;

import okhttp3.RequestBody;

import retrofit2.Call;

import retrofit2.Callback;

import retrofit2.Response;



public class BiologyFragment extends Fragment {

    private ImageView imagePreview;

    private LinearLayout uploadPlaceholder;

    private Button btnTakePhoto;

    private Button btnSelectImage;

    private Button btnUpload;

    private Button btnSpeak;

    private ProgressBar progressBar;

    private MaterialCardView resultCard;

    private TextView textResult;

    private Button btnViewDetail;

    private RecyclerView recyclerView;

    private BiologyAdapter adapter;



    private Uri selectedImageUri;

    private String recognizedBiologyName;

    private String lastSpeakText;

    private ImagePickerHelper imagePickerHelper;

    private TextToSpeech textToSpeech;

    private Call<ApiResponse<BiologyDetection>> pendingDetectCall;

    private Call<ApiResponse<List<Biology>>> listCall;

    private Uri pendingImageUri;

    private boolean biologyListLoaded;



    @Override

    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        imagePickerHelper = new ImagePickerHelper(this, new ImagePickerHelper.Callback() {

            @Override

            public void onImageSelected(Uri uri) {

                pendingImageUri = uri;

                if (isAdded() && imagePreview != null) {

                    selectedImageUri = uri;

                    displaySelectedImage();

                }

            }



            @Override

            public void onCancelled() {

                if (isAdded()) {

                    Toast.makeText(requireContext(), R.string.select_image_cancelled, Toast.LENGTH_SHORT).show();

                }

            }

        });

    }



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

        btnTakePhoto = view.findViewById(R.id.btn_take_photo);

        btnSelectImage = view.findViewById(R.id.btn_select_image);

        btnUpload = view.findViewById(R.id.btn_upload);

        btnSpeak = view.findViewById(R.id.btn_speak);

        progressBar = view.findViewById(R.id.progress_bar);

        resultCard = view.findViewById(R.id.result_card);

        textResult = view.findViewById(R.id.text_result);

        btnViewDetail = view.findViewById(R.id.btn_view_detail);

        recyclerView = view.findViewById(R.id.recycler_view);



        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new BiologyAdapter();

        recyclerView.setAdapter(adapter);



        if (pendingImageUri != null) {

            selectedImageUri = pendingImageUri;

            displaySelectedImage();

        }



        setupClickListeners();

    }

    @Override

    public void onResume() {

        super.onResume();

        if (!biologyListLoaded) {

            loadBiologyList();

        }

    }



    private void setupClickListeners() {

        View.OnClickListener galleryClick = v -> {

            if (imagePickerHelper != null) {

                imagePickerHelper.pickFromGallery();

            }

        };

        uploadPlaceholder.setOnClickListener(galleryClick);

        imagePreview.setOnClickListener(galleryClick);

        btnSelectImage.setOnClickListener(galleryClick);



        btnTakePhoto.setOnClickListener(v -> {

            if (imagePickerHelper != null) {

                imagePickerHelper.takePhoto();

            }

        });



        btnUpload.setOnClickListener(v -> recognizeImage());



        btnSpeak.setOnClickListener(v -> {

            if (lastSpeakText != null) {

                ensureTextToSpeech();

                if (textToSpeech != null) {

                    textToSpeech.speak(lastSpeakText, TextToSpeech.QUEUE_FLUSH, null, "bio_speak");

                }

            }

        });



        btnViewDetail.setOnClickListener(v -> {

            if (recognizedBiologyName != null && !recognizedBiologyName.isEmpty()) {

                Intent intent = new Intent(getContext(), BiologyDetailActivity.class);

                intent.putExtra("biology_name", recognizedBiologyName);

                startActivity(intent);

            }

        });

    }



    private void displaySelectedImage() {

        if (selectedImageUri != null) {

            imagePreview.setVisibility(View.VISIBLE);

            uploadPlaceholder.setVisibility(View.GONE);

            btnUpload.setEnabled(true);

            resultCard.setVisibility(View.GONE);

            btnSpeak.setVisibility(View.GONE);



            Glide.with(this)

                    .load(selectedImageUri)

                    .centerCrop()

                    .into(imagePreview);

        }

    }



    private void recognizeImage() {

        if (selectedImageUri == null) {

            Toast.makeText(getContext(), R.string.select_image, Toast.LENGTH_SHORT).show();

            return;

        }



        if (FileUtil.isImageTooDark(requireContext(), selectedImageUri)) {

            Toast.makeText(getContext(), R.string.image_too_dark, Toast.LENGTH_LONG).show();

            return;

        }



        progressBar.setVisibility(View.VISIBLE);

        btnUpload.setEnabled(false);

        btnSelectImage.setEnabled(false);

        btnTakePhoto.setEnabled(false);



        File file = FileUtil.getFileFromUri(requireContext(), selectedImageUri);

        if (file == null) {

            progressBar.setVisibility(View.GONE);

            Toast.makeText(getContext(), R.string.select_image, Toast.LENGTH_SHORT).show();

            resetButtons();

            return;

        }



        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);

        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), requestBody);



        if (pendingDetectCall != null) {

            pendingDetectCall.cancel();

        }

        pendingDetectCall = ApiClient.getApiService().detectBiology(part);

        pendingDetectCall.enqueue(new Callback<ApiResponse<BiologyDetection>>() {

            @Override

            public void onResponse(Call<ApiResponse<BiologyDetection>> call, Response<ApiResponse<BiologyDetection>> response) {

                try {

                    if (!isAdded() || resultCard == null || textResult == null) {

                        return;

                    }

                    if (!response.isSuccessful() || response.body() == null) {

                        NetworkUtil.showErrorResponseToast(requireContext(), response);

                        return;

                    }

                    ApiResponse<BiologyDetection> body = response.body();

                    if (!body.isSuccess()) {

                        String msg = body.getMsg();

                        if (msg == null || msg.isEmpty()) {

                            msg = getString(R.string.recognize_failed);

                        }

                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();

                        return;

                    }

                    BiologyDetection detection = body.getData();

                    if (detection == null || detection.getBioName() == null
                            || detection.getBioName().trim().isEmpty()) {

                        Toast.makeText(requireContext(), R.string.recognize_failed, Toast.LENGTH_SHORT).show();

                        return;

                    }

                    String bioName = detection.getBioName().trim();

                    recognizedBiologyName = bioName;

                    showRecognitionResult(detection, null);

                    Toast.makeText(requireContext(), R.string.recognize_success, Toast.LENGTH_SHORT).show();

                    enrichRecognitionWithBiology(detection, bioName);

                } finally {

                    if (isAdded() && progressBar != null) {

                        progressBar.setVisibility(View.GONE);

                        resetButtons();

                    }

                }

            }



            @Override

            public void onFailure(Call<ApiResponse<BiologyDetection>> call, Throwable t) {

                if (!isAdded()) {

                    return;

                }

                progressBar.setVisibility(View.GONE);

                resetButtons();

                if (!call.isCanceled()) {

                    NetworkUtil.showFailureToast(requireContext(), t);

                }

            }

        });

    }



    private void enrichRecognitionWithBiology(BiologyDetection detection, String bioName) {

        ApiClient.getApiService().getBiologyByName(bioName).enqueue(new Callback<ApiResponse<Biology>>() {

            @Override

            public void onResponse(Call<ApiResponse<Biology>> call, Response<ApiResponse<Biology>> response) {

                if (!isAdded() || textResult == null) {

                    return;

                }

                Biology biology = null;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    biology = response.body().getData();

                }

                showRecognitionResult(detection, biology);

            }



            @Override

            public void onFailure(Call<ApiResponse<Biology>> call, Throwable t) {

                // 已展示识别结果，百科补充失败时保持当前界面

            }

        });

    }



    private void showRecognitionResult(BiologyDetection detection, @Nullable Biology biology) {

        if (!isAdded()) {

            return;

        }

        String category = detection.getBioName() != null ? detection.getBioName().trim() : null;

        if (category == null || category.isEmpty()) {

            return;

        }

        StringBuilder builder = new StringBuilder();

        appendField(builder, getString(R.string.label_detect_category), category);

        String en = biology != null && biology.getEnName() != null ? biology.getEnName() : detection.getEnName();

        if (en != null && !en.isEmpty()) {

            builder.append("\n").append(en);

        }

        if (detection.getIntro() != null && !detection.getIntro().isEmpty()) {

            builder.append("\n").append(detection.getIntro());

        }

        if (detection.getConfidence() != null) {

            builder.append("\n").append(getString(R.string.confidence_label, detection.getConfidence()));

        }

        appendField(builder, getString(R.string.label_habits),

                biology != null ? biology.getHabits() : null);

        appendField(builder, getString(R.string.label_distribution),

                biology != null ? biology.getDistribution() : null);

        appendField(builder, getString(R.string.label_protection),

                biology != null ? biology.getProtectionLevel() : null);

        String aiTip = biology != null ? biology.getAiFeatureTip() : null;

        if (aiTip == null && biology != null && biology.getIntro() != null) {

            aiTip = biology.getIntro();

        }

        appendField(builder, getString(R.string.label_ai_tip), aiTip);



        textResult.setText(builder.toString());

        resultCard.setVisibility(View.VISIBLE);

        btnSpeak.setVisibility(View.VISIBLE);



        lastSpeakText = buildSpeakScript(detection, biology);

        ensureTextToSpeech();

        if (textToSpeech != null) {

            textToSpeech.speak(lastSpeakText, TextToSpeech.QUEUE_FLUSH, null, "bio_speak_auto");

        }

    }



    private void ensureTextToSpeech() {

        if (textToSpeech != null || !isAdded()) {

            return;

        }

        textToSpeech = new TextToSpeech(requireContext(), status -> {

            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {

                textToSpeech.setLanguage(Locale.CHINA);

            }

        });

    }



    private void appendField(StringBuilder builder, String label, String value) {

        if (value != null && !value.isEmpty()) {

            builder.append("\n").append(label).append("：").append(value);

        }

    }



    private String buildSpeakScript(BiologyDetection detection, @Nullable Biology biology) {

        StringBuilder s = new StringBuilder();

        String speakName = detection.getBioName() != null ? detection.getBioName().trim() : "";

        s.append("识别成功，识别类别为").append(speakName).append("。");

        if (biology != null && biology.getIntro() != null) {

            s.append(biology.getIntro()).append("。");

        }

        if (biology != null && biology.getHabits() != null) {

            s.append("习性：").append(biology.getHabits()).append("。");

        }

        if (biology != null && biology.getProtectionLevel() != null) {

            s.append("保护等级：").append(biology.getProtectionLevel()).append("。");

        }

        if (detection.getConfidence() != null) {

            s.append("置信度").append(String.format(Locale.CHINA, "%.0f", detection.getConfidence())).append("个百分点。");

        }

        s.append("人工智能通过卷积网络提取外形特征完成识别。");

        return s.toString();

    }



    private void resetButtons() {

        btnUpload.setEnabled(selectedImageUri != null);

        btnSelectImage.setEnabled(true);

        btnTakePhoto.setEnabled(true);

    }



    private void loadBiologyList() {

        if (biologyListLoaded || listCall != null) {

            return;

        }

        listCall = ApiClient.getApiService().getBiologyList();

        listCall.enqueue(new Callback<ApiResponse<List<Biology>>>() {

            @Override

            public void onResponse(Call<ApiResponse<List<Biology>>> call, Response<ApiResponse<List<Biology>>> response) {

                listCall = null;

                if (!isAdded()) {

                    return;

                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    List<Biology> biologyList = response.body().getData();

                    if (biologyList != null && !biologyList.isEmpty()) {

                        adapter.setData(biologyList);

                        biologyListLoaded = true;

                    }

                }

            }



            @Override

            public void onFailure(Call<ApiResponse<List<Biology>>> call, Throwable t) {

                listCall = null;

                if (isAdded()) {

                    NetworkUtil.showFailureToast(requireContext(), t);

                }

            }

        });

    }



    @Override

    public void onDestroy() {

        if (pendingDetectCall != null) {

            pendingDetectCall.cancel();

            pendingDetectCall = null;

        }

        if (listCall != null) {

            listCall.cancel();

            listCall = null;

        }

        if (textToSpeech != null) {

            textToSpeech.stop();

            textToSpeech.shutdown();

            textToSpeech = null;

        }

        super.onDestroy();

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

            holder.bind(biologyList.get(position));

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
            private final ImageView imageView;



            ViewHolder(View itemView) {

                super(itemView);

                cardView = itemView.findViewById(R.id.card_view);

                imageView = itemView.findViewById(R.id.image_biology);

                nameText = itemView.findViewById(R.id.text_name);

                enNameText = itemView.findViewById(R.id.text_en_name);

                introText = itemView.findViewById(R.id.text_intro);

            }



            void bind(Biology biology) {

                nameText.setText(biology.getBioName());

                enNameText.setText(biology.getEnName());

                introText.setText(biology.getIntro());

                BiologyImageLoader.load(imageView, biology);

                cardView.setOnClickListener(v -> {

                    Intent intent = new Intent(getContext(), BiologyDetailActivity.class);

                    intent.putExtra("biology_name", biology.getBioName());

                    startActivity(intent);

                });

            }

        }

    }

}


