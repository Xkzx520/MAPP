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
import com.example.mapp.util.PreferenceManager;

public class ProfileFragment extends Fragment {
    private TextView usernameText;
    private TextView nicknameText;
    private TextView roleText;
    private Button loginBtn;
    private View loggedInLayout;
    private View loggedOutLayout;

    private PreferenceManager preferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        loggedInLayout = view.findViewById(R.id.logged_in_layout);
        loggedOutLayout = view.findViewById(R.id.logged_out_layout);

        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        });

        View logoutBtn = view.findViewById(R.id.btn_logout);
        logoutBtn.setOnClickListener(v -> {
            preferenceManager.clearUser();
            updateUI();
        });

        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (preferenceManager.isLoggedIn()) {
            loggedInLayout.setVisibility(View.VISIBLE);
            loggedOutLayout.setVisibility(View.GONE);

            usernameText.setText(preferenceManager.getUsername());
            nicknameText.setText(preferenceManager.getNickname());
            roleText.setText(preferenceManager.getRole());
        } else {
            loggedInLayout.setVisibility(View.GONE);
            loggedOutLayout.setVisibility(View.VISIBLE);
        }
    }
}