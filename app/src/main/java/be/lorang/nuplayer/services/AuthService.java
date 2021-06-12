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

import be.lorang.nuplayer.BuildConfig;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.ui.MainActivity;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.Iterator;

/*
 * This class will take care of VRT authentication. Authentication is a multi-step process:
 *  - First you need to get oidcstate, OIDCXSRF and SESSION cookies from "service_auth_initlogin_server"
 *  - Next, you need to authenticate the user's credentials against "service_auth_authentication_server"
 *      This will give you a JSON response of which you need UID, UIDSignature and signatureTimestamp
 *  - Finally you can get VRT.NU tokens (as cookies) from "service_auth_performlogin_server"
 *    using previously received data + value of OIDCXSRF + static clientID
 *
 * When the last step is completed you'll receive following cookies:
 *
 *  - vrtnu-site_profile_dt (name unknown) : contains your entire VRT profile, stored but unused for now, valid for 1 hour
 *  - vrtnu-site_profile_et (end time) : contains timestamp when _dt and _vt expire (in 1 hour), cookie itself is valid for 350(?) days
 *  - vrtnu-site_profile_rt (refresh token) : refresh token, valid for 1 year
 *  - vrtnu-site_profile_vt (vrt.nu token) : used for all video, watch later, resume point, ... requests. valid for 1 hour
 *
 * Every time you refresh your token (AccessTokenService) vrtnu-site_profile_dt and vrtnu-site_profile_vt are valid again for 1 hour.
 *
 * vrtnu-site_profile_dt and vrtnu-site_profile_vt are always refreshed at the same time
 *
 * The statically defined "gigyApiKey" is some static API key valid for everybody
 * using the official VRT.NU app / website. It can be obtained from the backendData tag
 * on the login page at:
 * https://token.vrt.be/vrtnuinitlogin?provider=site&destination=https://www.vrt.be/vrtnu/
 *
 * The project's HTTPClient from Utils was unable to authenticate anymore, it has been replaced by
 * OkHttp library, it is only in use here in AuthService. All other Services are still using our own
 * HTTPClient, the JSON caching feature is still heavily used but can probably be replaced by OkHttp's Caching
 *
 */

public class AuthService extends IntentService {

    private static final String TAG = "AuthService";
    public final static String BUNDLED_LISTENER = "listener";
    public final static String COMPLETED_AUTHENTICATION = "completedAuthentication";

    private JavaNetCookieJar cookieJar;
    private CookieManager cookieManager;
    private OkHttpClient httpClient;
    private Request request;

    private Bundle resultData = new Bundle();

    private String UID;
    private String UIDSignature;
    private String signatureTimestamp;
    private Iterator cookieIterator;
    private String xsrfCookie = "";
    private String sessionCookie = "";

    // processing variables
    private RequestBody postData;
    private JSONObject returnObject;
    private SharedPreferences.Editor editor;
    private SharedPreferences prefs;

    // Static authentication fields
    private static final String gigyaApiKey = "3_qhEcPa5JGFROVwu5SWKqJ4mVOIkwlFNMSKwzPDAh8QZOtHqu6L4nD5Q7lk0eXOOG";
    private static final String sessionExpiration = "0";
    private static final String targetEnv = "jssdk";
    private static final String clientID = "vrtnu-site";

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public AuthService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            // setup OkHttp client and cookiejar
            cookieManager = (CookieManager)CookieHandler.getDefault();
            cookieJar = new JavaNetCookieJar(cookieManager);
            httpClient = new OkHttpClient()
                    .newBuilder()
                    .cookieJar(cookieJar)
                    .build();

            // Remove all previous cookies
            cookieManager.getCookieStore().removeAll();

            // get passed data
            String loginID = workIntent.getExtras().getString("loginID");
            String password = workIntent.getExtras().getString("password");

            Log.d(TAG, "Start authentication, username = " + loginID);

            // Get OIDCXSRF cookie
            request = new Request.Builder()
                    .url(getString(R.string.service_auth_initlogin_server))
                    .addHeader("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME)
                    .addHeader("Referer", getString(R.string.service_auth_referer_link))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException(response.code() + ": " + response.message());
            }

            cookieIterator = cookieManager.getCookieStore().getCookies().iterator();
            while(cookieIterator.hasNext()) {
                HttpCookie cookie = (HttpCookie)cookieIterator.next();
                if(cookie.getName().equals("OIDCXSRF")) {
                    xsrfCookie = cookie.getValue();
                }
            }

            if(xsrfCookie.length() == 0) {
                Log.d(TAG, "Could not acquire OIDCXSRF cookie");
                resultData.putString("MSG", "Could not acquire OIDCXSRF cookie.");
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            Log.d(TAG, "Set xsrf = " + xsrfCookie);

            // Perform Login
            postData = new FormBody.Builder()
                    .add("loginID", loginID)
                    .add("password", password)
                    .add("sessionExpiration", sessionExpiration)
                    .add("APIKey", gigyaApiKey)
                    .add("targetEnv", targetEnv)
                    .build();

            request = new Request.Builder()
                    .url(getString(R.string.service_auth_authentication_server))
                    .addHeader("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME)
                    .post(postData)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException(response.code() + ": " + response.message());
                returnObject = new JSONObject(response.body().string());
            }

            UID = returnObject.getString("UID");
            UIDSignature = returnObject.getString("UIDSignature");
            signatureTimestamp = returnObject.getString("signatureTimestamp");
            JSONObject profile = returnObject.getJSONObject("profile");

            Log.d(TAG, "Successfully authenticated, UID =" + UID +
                    " Sig = " + UIDSignature +
                    " ts = " + signatureTimestamp +
                    " profile = " + profile.toString());

            // store name as preference
            editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.putString("firstName", profile.getString("firstName"));
            editor.putString("lastName", profile.getString("lastName"));

            // get vrtnu-site_profile_{dt,et,rt,vt} cookies
            postData = new FormBody.Builder()
                    .add("UID", UID)
                    .add("UIDSignature", UIDSignature)
                    .add("signatureTimestamp", signatureTimestamp)
                    .add("client_id", clientID)
                    .add("_csrf", xsrfCookie)
                    .build();

            request = new Request.Builder()
                    .url(getString(R.string.service_auth_performlogin_server))
                    .addHeader("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME)
                    .post(postData)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException(response.code() + ": " + response.message());
            }

            cookieIterator = cookieManager.getCookieStore().getCookies().iterator();
            int cookiesCount = 0;
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
                    cookiesCount++;
                }
            }

            // we expect 4 cookies: vrtnu-site_profile_{dt,et,rt,vt} to be set at this point
            // any missing cookie is considered a failure
            if(cookiesCount == 4) {
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, true);
                resultData.putString("MSG", "Logged in successfully");
                receiver.send(Activity.RESULT_OK, resultData);
            } else {
                resultData.putString("MSG", "Not all required cookies were set. Found: " + cookiesCount + ", expected 4");
                receiver.send(Activity.RESULT_CANCELED, resultData);
            }

            // save SharedPreference in background
            editor.apply();

        } catch (Exception e) {
            String message = "Could not authenticate: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}