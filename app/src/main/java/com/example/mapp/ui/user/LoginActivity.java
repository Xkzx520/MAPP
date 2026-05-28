package com.example.mapp.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mapp.R;
import com.example.mapp.api.ApiClient;
import com.example.mapp.api.ApiService;
import com.example.mapp.model.ApiResponse;
import com.example.mapp.model.LoginRequest;
import com.example.mapp.model.User;
import com.example.mapp.ui.teacher.TeacherMainActivity;
import com.example.mapp.util.NetworkUtil;
import com.example.mapp.util.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private Spinner roleSpinner;
    private Button loginBtn;
    private ProgressBar progressBar;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferenceManager = PreferenceManager.getInstance(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        usernameInput = findViewById(R.id.input_username);
        passwordInput = findViewById(R.id.input_password);
        roleSpinner = findViewById(R.id.spinner_role);
        loginBtn = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        TextView registerLink = findViewById(R.id.link_register);

        setupRoleSpinner();
        loginBtn.setOnClickListener(v -> performLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void setupRoleSpinner() {
        String[] labels = {getString(R.string.role_student), getString(R.string.role_teacher)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
    }

    private String selectedRole() {
        return roleSpinner.getSelectedItemPosition() == 1 ? "teacher" : "student";
    }

    private void performLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String role = selectedRole();

        if (username.isEmpty()) {
            Toast.makeText(this, R.string.username_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginBtn.setEnabled(false);

        LoginRequest request = new LoginRequest(username, password, role);
        ApiService apiService = ApiClient.getApiService();
        apiService.login(request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                progressBar.setVisibility(View.GONE);
                loginBtn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        preferenceManager.saveUser(user.getUserId(), user.getUsername(),
                                user.getNickname(), user.getRole());
                        Toast.makeText(LoginActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                        if ("teacher".equalsIgnoreCase(user.getRole())) {
                            startActivity(new Intent(LoginActivity.this, TeacherMainActivity.class));
                        }
                        finish();
                    }
                } else {
                    NetworkUtil.showErrorResponseToast(LoginActivity.this, response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loginBtn.setEnabled(true);
                NetworkUtil.showFailureToast(LoginActivity.this, t);
            }
        });
    }
}
