package com.example.mapp.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mapp.R;
import com.example.mapp.ui.teacher.TeacherMainActivity;
import com.example.mapp.util.PreferenceManager;

public class ProfileFragment extends Fragment {
    private TextView usernameText;
    private TextView nicknameText;
    private TextView roleText;
    private Button loginBtn;
    private Button guestBtn;
    private View loggedInLayout;
    private View loggedOutLayout;
    private PreferenceManager preferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferenceManager = PreferenceManager.getInstance(requireContext());

        usernameText = view.findViewById(R.id.text_username);
        nicknameText = view.findViewById(R.id.text_nickname);
        roleText = view.findViewById(R.id.text_role);
        loginBtn = view.findViewById(R.id.btn_login);
        guestBtn = view.findViewById(R.id.btn_guest);
        loggedInLayout = view.findViewById(R.id.logged_in_layout);
        loggedOutLayout = view.findViewById(R.id.logged_out_layout);

        View.OnClickListener openSettings = v ->
                startActivity(new Intent(getContext(), SettingsActivity.class));
        view.findViewById(R.id.btn_settings_in).setOnClickListener(openSettings);
        view.findViewById(R.id.btn_settings_out).setOnClickListener(openSettings);

        loginBtn.setOnClickListener(v ->
                startActivity(new Intent(getContext(), LoginActivity.class)));
        guestBtn.setOnClickListener(v -> {
            preferenceManager.setGuestMode(true);
            updateUI();
        });

        view.findViewById(R.id.btn_register).setOnClickListener(v ->
                startActivity(new Intent(getContext(), RegisterActivity.class)));

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            preferenceManager.clearUser();
            updateUI();
        });

        view.findViewById(R.id.btn_change_password).setOnClickListener(v ->
                startActivity(new Intent(getContext(), ChangePasswordActivity.class)));

        view.findViewById(R.id.btn_teacher_portal).setOnClickListener(v ->
                startActivity(new Intent(getContext(), TeacherMainActivity.class)));

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (!isAdded() || loggedInLayout == null) {
            return;
        }
        if (preferenceManager.isLoggedIn()) {
            loggedInLayout.setVisibility(View.VISIBLE);
            loggedOutLayout.setVisibility(View.GONE);
            usernameText.setText(preferenceManager.getUsername());
            nicknameText.setText(preferenceManager.getNickname());
            String role = preferenceManager.getRole();
            roleText.setText("teacher".equalsIgnoreCase(role)
                    ? getString(R.string.role_teacher) : getString(R.string.role_student));
            View root = getView();
            if (root != null) {
                root.findViewById(R.id.btn_teacher_portal).setVisibility(
                        "teacher".equalsIgnoreCase(role) ? View.VISIBLE : View.GONE);
            }
        } else if (preferenceManager.isGuestMode()) {
            loggedInLayout.setVisibility(View.GONE);
            loggedOutLayout.setVisibility(View.VISIBLE);
            loginBtn.setText(R.string.guest_mode);
        } else {
            loggedInLayout.setVisibility(View.GONE);
            loggedOutLayout.setVisibility(View.VISIBLE);
            loginBtn.setText(R.string.btn_login);
        }
    }
}
