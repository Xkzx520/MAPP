package com.example.mapp.ui;

import android.os.Bundle;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import com.example.mapp.R;
import com.example.mapp.ui.current.CurrentSimFragment;
import com.example.mapp.ui.biology.BiologyFragment;
import com.example.mapp.ui.course.CourseFragment;
import com.example.mapp.ui.user.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private VideoView backgroundVideo;
    private VideoBackgroundController videoController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);

        backgroundVideo = findViewById(R.id.background_video);
        videoController = new VideoBackgroundController(backgroundVideo);
        videoController.start();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new CourseFragment());
        }
    }

    @Override
    protected void onDestroy() {
        if (videoController != null) {
            videoController.release();
        }
        super.onDestroy();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_courses) {
                fragment = new CourseFragment();
            } else if (itemId == R.id.nav_recognition) {
                fragment = new BiologyFragment();
            } else if (itemId == R.id.nav_ai) {
                fragment = new CurrentSimFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToTab(int menuItemId) {
        bottomNavigationView.setSelectedItemId(menuItemId);
    }
}