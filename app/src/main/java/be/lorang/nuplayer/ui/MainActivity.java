/*
 * Copyright 2021 Geert Lorang
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package be.lorang.nuplayer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.SearchEditText;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.AuthService;
import be.lorang.nuplayer.utils.HTTPClient;

public class MainActivity extends FragmentActivity {

    public static String PREFERENCES_NAME = "VRTNUPreferences";
    private static String TAG = "MainActivity";

    // Hour formatter
    private static final DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("Europe/Brussels"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

        // Check if user is authenticated
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        boolean isAuthenticated = prefs.getBoolean(AuthService.COMPLETED_AUTHENTICATION, false);

        // Temporary check to force re-authentication if using old cookie names
        String xvrttoken = prefs.getString("X-VRT-Token", null);
        if(xvrttoken != null && xvrttoken.length() > 0) {
            isAuthenticated = false;
        }

        // Start Login Activity if not
        if(!isAuthenticated) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        // setup application wide CookieManager
        CookieHandler.setDefault(new CookieManager());

        // Remove expired caches
        HTTPClient.clearExpiredCache(getCacheDir());

        // Setup clock ticker
        setupClockTicker();
    }

    private void setupClockTicker() {

        // Set initial value
        updateClock();

        // Update clock every 60 seconds, calculate initial delay to start of next minute
        int initialDelay = 60 - LocalDateTime.now().plusMinutes(1).getSecond();

        ScheduledThreadPoolExecutor executor =  new ScheduledThreadPoolExecutor(1);
        executor.scheduleWithFixedDelay(() -> {
            updateClock();
        }, initialDelay, 60L, TimeUnit.SECONDS);

    }

    private void updateClock() {

        // Update clock
        TextView menuClock = findViewById(R.id.menuClock);
        if(menuClock != null) {
            runOnUiThread(() -> {
                menuClock.setText(hourFormatter.format(Instant.now()));
            });
        }
    }

    @Override
    public void onBackPressed() {

        for(Fragment fragment : getSupportFragmentManager().getFragments()) {
            if(fragment instanceof CategoryProgramsFragment) {
                super.onBackPressed();
            } else if(fragment instanceof MainFragment) {
                MainFragment mainFragment = (MainFragment)fragment;

                if(mainFragment != null) {

                    LinearLayout menuNavigationContainer = mainFragment.getView().findViewById(R.id.menuNavigationContainer);
                    FrameLayout menuNavigationOverlay = mainFragment.getView().findViewById(R.id.menuNavigationOverlay);
                    if (menuNavigationContainer != null) {

                        // Check if any button has focus
                        boolean topNavHasFocus = false;
                        for (int i=0;i< menuNavigationContainer.getChildCount();i++) {
                            if (menuNavigationContainer.getChildAt(i) instanceof Button) {
                                Button menuButton = (Button) menuNavigationContainer.getChildAt(i);
                                if (menuButton.hasFocus()) {
                                    topNavHasFocus = true;
                                    break;
                                }
                            }
                        }

                        // exit app when focus is on top navigation
                        if(topNavHasFocus) {
                            super.onBackPressed();
                            return;
                        } else {

                            // Get last selected menu item from top nav
                            Button button = mainFragment.getSelectedMenuButton();

                            // Submenu overrules top nav, unless it already has focus
                            Fragment loadedFragment = getSupportFragmentManager().findFragmentById(R.id.mainContainerLayout);

                            if(loadedFragment instanceof HorizontalMenuFragment) {
                                HorizontalMenuFragment horizontalMenuFragment = (HorizontalMenuFragment)loadedFragment;
                                if(horizontalMenuFragment != null) {
                                    button = horizontalMenuFragment.getSelectedMenuButton();

                                    // return again one up (subnav -> main nav)
                                    if(button != null && button.hasFocus()) {
                                        button =  mainFragment.getSelectedMenuButton();
                                    }
                                }
                            }

                            // Set focus to latest selected (sub)menu button
                            if (button != null) {
                                button.requestFocus();
                            }

                            // Hide search bar
                            SearchEditText searchText = findViewById(R.id.searchText);
                            if(searchText != null && searchText.getWidth() > 0) {
                                searchText.getLayoutParams().width = 0;
                                menuNavigationOverlay.setFocusable(true);
                                menuNavigationContainer.setVisibility(View.VISIBLE);
                            }

                        }

                    }

                }
            }

        }
    }

}
