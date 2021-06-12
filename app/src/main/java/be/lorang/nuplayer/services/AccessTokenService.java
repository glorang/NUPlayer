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

import com.bumptech.glide.load.HttpException;
import com.google.gson.Gson;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.ui.MainActivity;

import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * This class will provide valid vrtnu-site_profile_{dt,vt} tokens
 *
 * If any of the tokens are expired they will be refreshed, given the refresh token (vrtnu-site_profile_rt)
 * is still valid, it this is not the case we return false and the calling Fragment should start
 * a new Auth intent
 *
 * See AuthService for a more detailed explanation
 *
 */

public class AccessTokenService extends IntentService {

    private static final String TAG = "AccessTokenService";
    public final static String BUNDLED_LISTENER = "listener";
    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    private String xvrttokenJSON;
    private String vrtloginAtJSON;
    private String vrtloginRtJSON;
    private String vrtloginExpiryJSON;

    private String vrtnu_site_profile_dt_json;
    private String vrtnu_site_profile_et_json;
    private String vrtnu_site_profile_rt_json;
    private String vrtnu_site_profile_vt_json;

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

            // Check for 'force' boolean
            Boolean forceRefresh = workIntent.getExtras().getBoolean("FORCE_REFRESH", false);

            // Open SharedPreferences
            editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();

            // check current state
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
            xvrttokenJSON = prefs.getString("X-VRT-Token", null);
            vrtloginAtJSON = prefs.getString("vrtlogin-at", null);
            vrtloginRtJSON = prefs.getString("vrtlogin-rt", null);
            vrtloginExpiryJSON = prefs.getString("vrtlogin-expiry", null);

            vrtnu_site_profile_dt_json = prefs.getString("vrtnu-site_profile_dt", null);
            vrtnu_site_profile_et_json = prefs.getString("vrtnu-site_profile_et", null);
            vrtnu_site_profile_rt_json = prefs.getString("vrtnu-site_profile_rt", null);
            vrtnu_site_profile_vt_json = prefs.getString("vrtnu-site_profile_vt", null);

            // Check if legacy cookies are set, if so, unset them. To be removed in a later NUPlayer version
            if(xvrttokenJSON != null) { editor.remove("X-VRT-Token"); }
            if(vrtloginAtJSON != null) { editor.remove("vrtlogin-at"); }
            if(vrtloginRtJSON != null) { editor.remove("vrtlogin-rt"); }
            if(vrtloginExpiryJSON != null) { editor.remove("vrtlogin-expiry"); }

            // If any of the cookies is missing force user to re-authenticate
            if(vrtnu_site_profile_dt_json == null || vrtnu_site_profile_et_json == null ||
                    vrtnu_site_profile_rt_json == null || vrtnu_site_profile_vt_json == null) {
                Log.d(TAG, "Cookies missing, user must re-authenticate");
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, false);
                editor.apply();
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            // Parse cookies
            HttpCookie vrtnu_site_profile_dt_cookie = new Gson().fromJson(vrtnu_site_profile_dt_json, HttpCookie.class);
            HttpCookie vrtnu_site_profile_et_cookie = new Gson().fromJson(vrtnu_site_profile_et_json, HttpCookie.class);
            HttpCookie vrtnu_site_profile_rt_cookie = new Gson().fromJson(vrtnu_site_profile_rt_json, HttpCookie.class);
            HttpCookie vrtnu_site_profile_vt_cookie = new Gson().fromJson(vrtnu_site_profile_vt_json, HttpCookie.class);

            // Add cookies to running CookieStore, this will make sure all Cookies are sent
            // on every HTTP request we make. CookieManager is non-persistent/in-memory only.
            CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
            CookieStore cookieStore = cookieManager.getCookieStore();
            cookieStore.add(URI.create(vrtnu_site_profile_dt_cookie.getDomain()), vrtnu_site_profile_dt_cookie);
            cookieStore.add(URI.create(vrtnu_site_profile_et_cookie.getDomain()), vrtnu_site_profile_et_cookie);
            cookieStore.add(URI.create(vrtnu_site_profile_rt_cookie.getDomain()), vrtnu_site_profile_rt_cookie);
            cookieStore.add(URI.create(vrtnu_site_profile_vt_cookie.getDomain()), vrtnu_site_profile_vt_cookie);

            // If our refresh token has expired we need to re-authenticate
            if(vrtnu_site_profile_rt_cookie.hasExpired()) {
                Log.d(TAG, "vrtnu_site_profile_rt_cookie expired, user must re-authenticate");
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, false);
                editor.apply();
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            // Refresh tokens if expired
            if(vrtnu_site_profile_vt_cookie.hasExpired() || forceRefresh) {

                Log.d(TAG, "Token expired, refreshing");
                Log.d(TAG, "Force = " + forceRefresh);
                Log.d(TAG, "vrtnu_site_profile_vt = " + vrtnu_site_profile_vt_json);

                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", getString(R.string.service_auth_referer_link));

                returnObject = httpClient.getRequest(getString(R.string.service_access_token_refresh), headers);

                Log.d(TAG, "Refresh token result = " + returnObject.toString());

                if(httpClient.getResponseCode() != 200) {
                    String errorMsg = "";
                    if(returnObject.has("error")) {
                        errorMsg = returnObject.getString("error");
                    }
                    errorMsg += " - " + httpClient.getResponseCode() + ": " + httpClient.getResponseMessage();
                    throw new HttpException(errorMsg);
                }

                // Store cookies
                cookieIterator = httpClient.getCookies().getCookieStore().getCookies().iterator();
                while(cookieIterator.hasNext()) {
                    HttpCookie cookie = (HttpCookie)cookieIterator.next();
                    String cookieName = cookie.getName();
                    if(cookieName.equals("vrtnu-site_profile_dt") ||
                            cookieName.equals("vrtnu-site_profile_et") ||
                            cookieName.equals("vrtnu-site_profile_rt") ||
                            cookieName.equals("vrtnu-site_profile_vt")
                    ) {
                        Log.d(TAG, "Setting cookie as preference = " + new Gson().toJson(cookie, HttpCookie.class));
                        editor.putString(cookie.getName(), new Gson().toJson(cookie, HttpCookie.class));
                    }
                }

                // save SharedPreference in background
                editor.apply();
            }

            // Return vrtnu-site_profile_vt token
            vrtnu_site_profile_vt_json = prefs.getString("vrtnu-site_profile_vt", null);

            if(vrtnu_site_profile_vt_json != null) {
                HttpCookie resultToken = new Gson().fromJson(vrtnu_site_profile_vt_json, HttpCookie.class);
                resultData.putString("vrtnu_site_profile_vt", resultToken.getValue());
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not obtain token: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}