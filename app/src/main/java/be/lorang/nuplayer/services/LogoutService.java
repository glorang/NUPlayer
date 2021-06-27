/*
 * Copyright 2021 Geert Lorang
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


package be.lorang.nuplayer.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;

import be.lorang.nuplayer.BuildConfig;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.ResumePointList;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.ui.MainActivity;
import be.lorang.nuplayer.utils.HTTPClient;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Service to handle logout
 *
 * It will clear all internal cookies + stored preferences and query VRT NU's logout URL
 */

public class LogoutService extends IntentService {

    private static final String TAG = "LogoutService";
    public final static String BUNDLED_LISTENER = "listener";

    private Bundle resultData = new Bundle();

    public LogoutService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(VrtPlayerTokenService.BUNDLED_LISTENER);

        try {

            // Clear catalog
            ProgramList.getInstance().clear();

            // Clear Resume Points
            ResumePointList.getInstance().clear();

            // Clear continue watching
            VideoContinueWatchingList.getInstance().clear();

            // Clear Watch Later
            VideoWatchLaterList.getInstance().clear();

            // Clear all caches
            HTTPClient.clearCache(getCacheDir());

            // Unset all shared pref keys
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();

            // Logout at VRT side

            // setup OkHttp client and cookiejar
            CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
            JavaNetCookieJar cookieJar = new JavaNetCookieJar(cookieManager);
            OkHttpClient httpClient = new OkHttpClient()
                    .newBuilder()
                    .cookieJar(cookieJar)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.service_logout_url))
                    .addHeader("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME)
                    .addHeader("Referer", getString(R.string.service_auth_referer_link))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                Log.d(TAG, "Logout response = " + response.toString());
                if (!response.isSuccessful()) throw new IOException(response.code() + ": " + response.message());
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not logout: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}