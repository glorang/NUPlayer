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
import android.widget.Toast;

import be.lorang.nuplayer.R;

public class MainActivity extends LeanbackActivity {

    public static String PREFERENCES_NAME = "VRTNUPreferences";
    private static String TAG = "MainActivity";
    private long backPressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //
        // Developer mode: Set fixed X-VRT-Cookie at start
        //

        // SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
        // editor.remove("X-VRT-Token");
        // editor.putString("X-VRT-Token", "<set your token here>");
        // editor.apply();

        // Start Login Activity if no X-VRT-Token is present //FIXME: token expired (or invalid?)
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String xvrttoken = prefs.getString("X-VRT-Token", "");
        Log.d(TAG, "Found X-VRT-Token at startup : " + xvrttoken);
        if(xvrttoken.length() == 0) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 1000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(),
                    "Press return again to exit.", Toast.LENGTH_SHORT)
                    .show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
