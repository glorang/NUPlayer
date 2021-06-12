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

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.ui.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * This class will obtain a new vrtPlayerToken
 *
 * There are two kinds of vrtPLayerTokens:
 *
 * - Anonymous tokens, those start with b0000@ and are used in (geolimited?) live TV
 * - Authenticated tokens, those start with b1000@ and are used for on-demand services
 *
 * The token and expiry date of each token is saved in a Shared Preference
 *
 * Via the LiveTVFragment and ProgramFragment the expiry is checked and they will call this
 * class to refresh them if required
 *
 */

public class VrtPlayerTokenService extends IntentService {

    private static final String TAG = "VrtPlayerTokenService";
    public final static String BUNDLED_LISTENER = "listener";

    public final static String VRTPLAYERTOKEN_ANONYMOUS = "vrtPlayerTokenAnonymous";
    public final static String VRTPLAYERTOKEN_AUTHENTICATED = "vrtPlayerTokenAuthenticated";
    public final static String VRTPLAYERTOKEN_ANONYMOUS_EXPIRY = "vrtPlayerTokenAnonymousExpiry";
    public final static String VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY = "vrtPlayerTokenAuthenticatedExpiry";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private JSONObject postData;
    private String sharedPrefTokenTarget = VRTPLAYERTOKEN_ANONYMOUS;
    private String sharedPrefExpiryTarget = VRTPLAYERTOKEN_ANONYMOUS_EXPIRY;

    public VrtPlayerTokenService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(VrtPlayerTokenService.BUNDLED_LISTENER);

        try {

            // get passed token type
            String tokenType = workIntent.getExtras().getString("TOKEN_TYPE", VRTPLAYERTOKEN_ANONYMOUS);
            String vrtnu_site_profile_vt = workIntent.getExtras().getString("vrtnu_site_profile_vt", "");

            postData = new JSONObject();

            if(tokenType.equals(VRTPLAYERTOKEN_AUTHENTICATED) && vrtnu_site_profile_vt.length() == 0) {
                resultData.putString("MSG", "Trying to get authenticated vrtPlayerToken without logging in.");
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            } else if(tokenType.equals(VRTPLAYERTOKEN_AUTHENTICATED) && vrtnu_site_profile_vt.length() > 0) {

                sharedPrefTokenTarget = VRTPLAYERTOKEN_AUTHENTICATED;
                sharedPrefExpiryTarget = VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY;

                // add vrtnu_site_profile_vt token
                postData.put("identityToken", vrtnu_site_profile_vt);
            }

            JSONObject returnData = httpClient.postRequest(
                    getString(R.string.service_playertoken_player_token_server),
                    "application/json", postData);

            if (httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            Log.d(TAG, "Obtained vrtPlayerToken. Data: " + returnData.toString());

            // store received token as SharedPreference
            editor = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.putString(sharedPrefTokenTarget, returnData.getString("vrtPlayerToken"));
            editor.putString(sharedPrefExpiryTarget, returnData.getString("expirationDate"));
            editor.apply();

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get vrtPlayerToken: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}
