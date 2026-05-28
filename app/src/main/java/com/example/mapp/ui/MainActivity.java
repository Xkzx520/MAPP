package com.example.mapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import com.example.mapp.R;
import com.example.mapp.util.LearningTracker;
import com.example.mapp.ui.biology.BiologyFragment;
import com.example.mapp.ui.course.CourseFragment;
import com.example.mapp.ui.current.CurrentSimFragment;
import com.example.mapp.ui.user.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private VideoView backgroundVideo;
    private ImageView backgroundImage;
    private VideoBackgroundController videoController;

    private CourseFragment courseFragment;
    private BiologyFragment biologyFragment;
    private CurrentSimFragment currentSimFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;
    private int lastMenuId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);

        backgroundVideo = findViewById(R.id.background_video);
        backgroundImage = findViewById(R.id.background_image);
        videoController = new VideoBackgroundController(backgroundVideo);
        videoController.setOnErrorFallback(() -> {
            backgroundImage.setVisibility(View.VISIBLE);
            backgroundVideo.setVisibility(View.GONE);
        });
        videoController.start();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        if (savedInstanceState == null) {
            courseFragment = new CourseFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, courseFragment, tagFor(R.id.nav_courses))
                    .setMaxLifecycle(courseFragment, Lifecycle.State.RESUMED)
                    .commit();
            activeFragment = courseFragment;
            lastMenuId = R.id.nav_courses;
            trackTab(R.id.nav_courses);
        } else {
            courseFragment = (CourseFragment) getSupportFragmentManager()
                    .findFragmentByTag(tagFor(R.id.nav_courses));
            biologyFragment = (BiologyFragment) getSupportFragmentManager()
                    .findFragmentByTag(tagFor(R.id.nav_recognition));
            currentSimFragment = (CurrentSimFragment) getSupportFragmentManager()
                    .findFragmentByTag(tagFor(R.id.nav_ai));
            profileFragment = (ProfileFragment) getSupportFragmentManager()
                    .findFragmentByTag(tagFor(R.id.nav_profile));
            activeFragment = courseFragment;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoController != null) {
            videoController.resume();
        }
    }

    @Override
    protected void onPause() {
        LearningTracker.flush(this);
        if (videoController != null) {
            videoController.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (videoController != null) {
            videoController.release();
            videoController = null;
        }
        super.onDestroy();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == lastMenuId) {
                return true;
            }
            switchTab(itemId);
            return true;
        });
    }

    private void switchTab(int menuId) {
        Fragment target = getOrCreateFragment(menuId);
        if (target == null) {
            return;
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
        if (activeFragment != null && activeFragment != target) {
            tx.hide(activeFragment);
            tx.setMaxLifecycle(activeFragment, Lifecycle.State.STARTED);
        }
        if (target.isAdded()) {
            tx.show(target);
        } else {
            tx.add(R.id.fragment_container, target, tagFor(menuId));
        }
        tx.setMaxLifecycle(target, Lifecycle.State.RESUMED);
        tx.commitAllowingStateLoss();

        activeFragment = target;
        lastMenuId = menuId;
        trackTab(menuId);
    }

    private void trackTab(int menuId) {
        if (menuId == R.id.nav_courses) {
            LearningTracker.onTabOpened(this, "course", "课程首页");
        } else if (menuId == R.id.nav_recognition) {
            LearningTracker.onTabOpened(this, "biology", "海洋识别");
        } else if (menuId == R.id.nav_ai) {
            LearningTracker.onTabOpened(this, "current", "洋流模拟");
        } else if (menuId == R.id.nav_profile) {
            LearningTracker.onTabOpened(this, "profile", "个人中心");
        }
    }

    private Fragment getOrCreateFragment(int menuId) {
        if (menuId == R.id.nav_courses) {
            if (courseFragment == null) {
                courseFragment = new CourseFragment();
            }
            return courseFragment;
        }
        if (menuId == R.id.nav_recognition) {
            if (biologyFragment == null) {
                biologyFragment = new BiologyFragment();
            }
            return biologyFragment;
        }
        if (menuId == R.id.nav_ai) {
            if (currentSimFragment == null) {
                currentSimFragment = new CurrentSimFragment();
            }
            return currentSimFragment;
        }
        if (menuId == R.id.nav_profile) {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
            }
            return profileFragment;
        }
        return null;
    }

    private static String tagFor(int menuId) {
        return "main_tab_" + menuId;
    }

    public void navigateToTab(int menuItemId) {
        if (bottomNavigationView.getSelectedItemId() != menuItemId) {
            bottomNavigationView.setSelectedItemId(menuItemId);
        } else {
            switchTab(menuItemId);
        }
    }
}
