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

import java.net.CookieHandler;
import java.net.CookieManager;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.AuthService;

public class MainActivity extends LeanbackActivity {

    public static String PREFERENCES_NAME = "VRTNUPreferences";
    public static String PREFERENCE_IS_APP_STARTUP = "isAppStartup";
    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

        // Set app startup boolean (see HomeFragment onViewCreated)
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PREFERENCE_IS_APP_STARTUP, true);
        editor.apply();

        // Check if user is authenticated
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        boolean isAuthenticated = prefs.getBoolean(AuthService.COMPLETED_AUTHENTICATION, false);

        // Start Login Activity if not
        if(!isAuthenticated) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        // setup application wide CookieManager
        CookieHandler.setDefault(new CookieManager());
    }

}
