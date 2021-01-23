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

import com.google.gson.Gson;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.ui.MainActivity;

import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * This class will provide valid vrtnutoken (X-VRT-Token) and vrtprofiletoken (vrtlogin-at) tokens
 *
 * If any of the tokens are expired they will be refreshed, given the refresh token (vrtlogin-rt)
 * is still valid, it this is not the case we return false and the calling Fragment should start
 * a new Auth intent
 *
 * See AuthService for a more detailed explanation
 *
 */

public class AccessTokenService extends IntentService {

    private static final String TAG = "AccessTokenService";
    public final static String BUNDLED_LISTENER = "listener";
    private HTTPClient httpClient = HTTPClient.getInstance();
    private Bundle resultData = new Bundle();

    private String xvrttokenJSON;
    private String vrtloginAtJSON;
    private String vrtloginRtJSON;
    private String vrtloginExpiryJSON;

    private SharedPreferences.Editor editor;
    private Iterator cookieIterator;
    private JSONObject returnObject;

    public AccessTokenService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            // Open SharedPreferences
            editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();

            // check current state
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
            xvrttokenJSON = prefs.getString("X-VRT-Token", null);
            vrtloginAtJSON = prefs.getString("vrtlogin-at", null);
            vrtloginRtJSON = prefs.getString("vrtlogin-rt", null);
            vrtloginExpiryJSON = prefs.getString("vrtlogin-expiry", null);

            // Check if existing X-VRT-Token contains Gson serialized Cookie, if not (<= 1.0.1-beta)
            // force user to re-authenticate
            if(xvrttokenJSON != null && !xvrttokenJSON.startsWith("{")) {
                xvrttokenJSON = null;
            }

            // If any of the cookies is missing force user to re-authenticate
            if(xvrttokenJSON == null || vrtloginAtJSON == null ||
                    vrtloginRtJSON == null || vrtloginExpiryJSON == null) {
                Log.d(TAG, "Cookies missing, user must re-authenticate");
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, false);
                editor.apply();
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            // Parse cookies
            HttpCookie xvrttoken = new Gson().fromJson(xvrttokenJSON, HttpCookie.class);
            HttpCookie vrtlogin_at = new Gson().fromJson(vrtloginAtJSON, HttpCookie.class);
            HttpCookie vrtlogin_rt = new Gson().fromJson(vrtloginRtJSON, HttpCookie.class);
            HttpCookie vrtlogin_expiry = new Gson().fromJson(vrtloginExpiryJSON, HttpCookie.class);

            // If our refresh token has expired we need to re-authenticate
            if(vrtlogin_rt.hasExpired()) {
                Log.d(TAG, "vrtlogin-rt expired, user must re-authenticate");
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, false);
                editor.apply();
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            // Refresh tokens if expired
            if(xvrttoken.hasExpired() || vrtlogin_at.hasExpired()) {

                Log.d(TAG, "Token expired, refreshing");
                Log.d(TAG, "xvrttoken = " + xvrttokenJSON);
                Log.d(TAG, "vrtlogin_at = " + vrtloginAtJSON);

                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", "vrtlogin-rt=" + vrtlogin_rt.getValue() + ";" +
                        "vrtlogin-expiry=" + vrtlogin_expiry.getValue());

                returnObject = httpClient.getRequest(
                        getString(R.string.service_access_token_refresh), headers);

                Log.d(TAG, "Refresh token result = " + returnObject.toString());

                if(httpClient.getResponseCode() != 200) {
                    resultData.putString("MSG", "Token refresh failed: " + httpClient.getResponseMessage());
                    receiver.send(Activity.RESULT_CANCELED, resultData);
                    return;
                }

                // Store cookies
                cookieIterator = httpClient.getCookies().getCookieStore().getCookies().iterator();
                while(cookieIterator.hasNext()) {
                    HttpCookie cookie = (HttpCookie)cookieIterator.next();
                    String cookieName = cookie.getName();
                    if(cookieName.equals("X-VRT-Token") ||
                            cookieName.equals("vrtlogin-at") ||
                            cookieName.equals("vrtlogin-rt") ||
                            cookieName.equals("vrtlogin-expiry")
                    ) {
                        Log.d(TAG, "Setting cookie as preference = " + new Gson().toJson(cookie, HttpCookie.class));
                        editor.putString(cookie.getName(), new Gson().toJson(cookie, HttpCookie.class));
                    }
                }

                // save SharedPreference in background
                editor.apply();
            }

            // Return X-VRT-Token and vrtlogin-at values
            xvrttokenJSON = prefs.getString("X-VRT-Token", null);
            vrtloginAtJSON = prefs.getString("vrtlogin-at", null);

            if(xvrttokenJSON != null) {
                HttpCookie resultToken = new Gson().fromJson(xvrttokenJSON, HttpCookie.class);
                resultData.putString("X-VRT-Token", resultToken.getValue());
            }

            if(vrtloginAtJSON != null) {
                HttpCookie resultToken = new Gson().fromJson(vrtloginAtJSON, HttpCookie.class);
                resultData.putString("vrtlogin-at", resultToken.getValue());
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            Log.e(TAG, "Could not obtain token");
            e.printStackTrace();
            resultData.putString("MSG", "Could not obtain token: " + e.getMessage());
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}