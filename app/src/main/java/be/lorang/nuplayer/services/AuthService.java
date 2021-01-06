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

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.ui.MainActivity;

import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.Iterator;

/*
 *  This class will obtain a X-VRT-Token valid for 1 year and store it in a (private) SharedPreference
 *
 *  Obtaining this token is a two-way process, first you need to authenticate your VRT account,
 *  then you can obtain a cookie that contains the X-VRT-Token that you need to stream videos.
 *
 *  The statically defined "gigyApiKey" is some static API key valid for everybody
 *  using the official VRT.NU app / website. It can be obtained from the backendData tag
 *  on the login page at:
 *  https://token.vrt.be/vrtnuinitlogin?provider=site&destination=https://www.vrt.be/vrtnu/
 *
 *  FIXME: this class needs to be split up with different methods as there are
 *   multiple 'X-VRT-Token' tokens:
 *
 *  There is :
 *  - one for video playback, valid for 1 year
 *  - one for user settings (favorites, time tracking, ...) valid for 1 hour
 *  - one for roaming - to check
 *
 */

public class AuthService extends IntentService {

    private static final String TAG = "AuthService";
    public final static String BUNDLED_LISTENER = "listener";
    private Bundle resultData = new Bundle();

    // processing variables
    private int statusCode;
    private JSONObject postData;
    private JSONObject returnObject;
    private SharedPreferences.Editor editor;
    private SharedPreferences prefs;

    // fields required to authenticate
    private String loginID;
    private String password;
    private final String gigyaApiKey = "3_qhEcPa5JGFROVwu5SWKqJ4mVOIkwlFNMSKwzPDAh8QZOtHqu6L4nD5Q7lk0eXOOG";
    private final String sessionExpiration = "-2";
    private final String targetEnv = "jssdk";

    // result fields from authentication (= fields required to obtain X-VRT-Token)
    private String UID;
    private String UIDSignature;
    private String signatureTimestamp;

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

            // authenticate
            loginID = workIntent.getExtras().getString("loginID");
            password = workIntent.getExtras().getString("password");

            Log.d(TAG, "Start authentication, username = " + loginID);

            postData = new JSONObject();
            postData.put("loginID", loginID);
            postData.put("password", password);
            postData.put("sessionExpiration", sessionExpiration);
            postData.put("APIKey", gigyaApiKey);
            postData.put("targetEnv", targetEnv);

            returnObject = new HTTPClient().postRequest(
                    getString(R.string.service_auth_authentication_server),
                    "application/x-www-form-urlencoded", postData);

            Log.d(TAG, "Result authentication = " + returnObject.toString());

            statusCode = returnObject.getInt("statusCode");
            if(statusCode != 200) {
                resultData.putString("MSG", returnObject.getString("errorDetails"));
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
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

            // get X-VRT-Token
            postData = new JSONObject();
            postData.put("email", loginID);
            postData.put("uid", UID);
            postData.put("uidsig", UIDSignature);
            postData.put("ts", signatureTimestamp);

            HTTPClient httpClient = new HTTPClient();
            httpClient.postRequest(getString(R.string.service_auth_token_server),
                    "application/json", postData);

            if(httpClient.getResponseCode() != 200) {
                resultData.putString("MSG", "Could not get X-VRT-Token cookie. HTTP code: "
                        + httpClient.getResponseCode()
                        + " message: " + httpClient.getResponseMessage());
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            Iterator cookieIterator = httpClient.getCookies().getCookieStore().getCookies().iterator();
            while(cookieIterator.hasNext()) {
                HttpCookie cookie = (HttpCookie)cookieIterator.next();
                if(cookie.getName().equals("X-VRT-Token")) {
                    editor.putString("X-VRT-Token", cookie.getValue());
                }
            }

            // save SharedPreference in background
            editor.apply();

            // read back SharedPreference to verify we were successful
            prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
            String xvrttoken = prefs.getString("X-VRT-Token", "");
            if(xvrttoken.length() > 0) {
                Log.d(TAG, "Successfully obtained X-VRT-Token: " + xvrttoken);
                resultData.putString("MSG", "Logged in successfully");
                receiver.send(Activity.RESULT_OK, resultData);
            } else {
                Log.d(TAG, "Failed to obtain X-VRT-Token");
                resultData.putString("MSG", "Failed to obtain VRT-Token");
                receiver.send(Activity.RESULT_CANCELED, resultData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Could not authenticate");
            resultData.putString("MSG", e.getMessage());
            e.printStackTrace();
            receiver.send(Activity.RESULT_CANCELED, resultData);
            return;
        }
    }
}
