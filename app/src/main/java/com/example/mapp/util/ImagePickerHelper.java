package com.example.mapp.util;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

/**
 * 相册/文件选图 + 系统相机拍照（Android 端识别用）。
 */
public final class ImagePickerHelper {

    public interface Callback {
        void onImageSelected(Uri uri);

        void onCancelled();
    }

    private final Fragment fragment;
    private final Callback callback;
    private final ActivityResultLauncher<String[]> openDocumentLauncher;
    private final ActivityResultLauncher<Void> takePictureLauncher;

    public ImagePickerHelper(Fragment fragment, Callback callback) {
        this.fragment = fragment;
        this.callback = callback;

        this.openDocumentLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            fragment.requireContext().getContentResolver()
                                    .takePersistableUriPermission(uri,
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                        }
                        callback.onImageSelected(uri);
                    } else {
                        callback.onCancelled();
                    }
                }
        );

        this.takePictureLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        Uri uri = FileUtil.saveBitmapToCache(fragment.requireContext(), bitmap);
                        if (uri != null) {
                            callback.onImageSelected(uri);
                        } else {
                            callback.onCancelled();
                        }
                    } else {
                        callback.onCancelled();
                    }
                }
        );
    }

    /** 从相册或文件管理器选择图片 */
    public void pickFromGallery() {
        openDocumentLauncher.launch(new String[]{"image/*"});
    }

    /** 打开系统相机拍照（对准海洋生物卡片） */
    public void takePhoto() {
        takePictureLauncher.launch(null);
    }
}
