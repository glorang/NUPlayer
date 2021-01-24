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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * This class will take care of VRT authentication. Authentication is a multi-step process.
 *
 * Once authenticated you'll receive a X-VRT-Token, vrtlogin-at, vrtlogin-rt and vrtlogin-expiry cookie
 *
 *  - X-VRT-Token: used for all media playback, valid for 1 hour
 *  - vrtlogin-at (access token): used for accessing Resume Points, Favorites, Watch later etc, valid for 1 hour
 *  - vrtlogin-rt (refresh token): used to refresh vrtlogin-at and X-VRT-Token, valid for 1 year
 *  - vrtlogin-expiry, timestamp when X-VRT-Token and vrtlogin-at will expire, cookie itself is valid for 1 year
 *
 * Every time you refresh your token (AccessTokenService) vrtlogin-rt and vrtlogin-expiry are extended
 * Basically this means you need to use VRT.NU once before vrtlogin-rt expires and you never
 * need to re-authenticate
 *
 * X-VRT-Token and vrtlogin-at are always refreshed at the same time
 *
 * The statically defined "gigyApiKey" is some static API key valid for everybody
 * using the official VRT.NU app / website. It can be obtained from the backendData tag
 * on the login page at:
 * https://token.vrt.be/vrtnuinitlogin?provider=site&destination=https://www.vrt.be/vrtnu/
 *
 */

public class AuthService extends IntentService {

    private static final String TAG = "AuthService";
    public final static String BUNDLED_LISTENER = "listener";
    public final static String COMPLETED_AUTHENTICATION = "completedAuthentication";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    private String UID;
    private String UIDSignature;
    private String signatureTimestamp;
    private Iterator cookieIterator;
    private String xsrfCookie = "";
    private String sessionCookie = "";

    // processing variables
    private int statusCode;
    private JSONObject postData;
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

            // get passed data
            String loginID = workIntent.getExtras().getString("loginID");
            String password = workIntent.getExtras().getString("password");

            Log.d(TAG, "Start authentication, username = " + loginID);

            // Get OIDCXSRF and SESSION cookie from init login
            httpClient.getRequest(getString(R.string.service_auth_initlogin_server));

            if (httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            cookieIterator = httpClient.getCookies().getCookieStore().getCookies().iterator();
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
            postData = new JSONObject();
            postData.put("loginID", loginID);
            postData.put("password", password);
            postData.put("sessionExpiration", sessionExpiration);
            postData.put("APIKey", gigyaApiKey);
            postData.put("targetEnv", targetEnv);

            returnObject = httpClient.postRequest(
                    getString(R.string.service_auth_authentication_server),
                    "application/x-www-form-urlencoded", postData);

            Log.d(TAG, "Result authentication = " + returnObject.toString());

            statusCode = returnObject.getInt("statusCode");
            if(statusCode != 200) {
                String message = "";
                try {
                    message = returnObject.getString("errorDetails");
                }catch (JSONException e) {
                    message = statusCode + ": " + httpClient.getResponseMessage();
                    e.printStackTrace();
                }

                throw new HttpException(message);
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

            // get X-VRT-Token, vrtlogin-{at,rt,expiry} cookies
            postData = new JSONObject();
            postData.put("UID", UID);
            postData.put("UIDSignature", UIDSignature);
            postData.put("signatureTimestamp", signatureTimestamp);
            postData.put("client_id", clientID);
            postData.put("_csrf", xsrfCookie);

            httpClient.postRequest(getString(R.string.service_auth_performlogin_server),
                    "application/x-www-form-urlencoded", postData);

            if(httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            cookieIterator = httpClient.getCookies().getCookieStore().getCookies().iterator();
            int cookiesCount = 0;
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
                    cookiesCount++;
                }
            }

            // we expect 4 cookies: X-VRT-Token, vrtlogin-{at,rt,expiry} to be set at this point
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