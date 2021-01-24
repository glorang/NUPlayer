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
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.bumptech.glide.load.HttpException;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * This class will download the all favorites and mark the program as favorite in the Catalog
 *
 * The list is only shown on the Home Fragment.
 *
 */

public class FavoriteService extends IntentService {
    private static final String TAG = "FavoriteService";
    public final static String BUNDLED_LISTENER = "listener";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    public FavoriteService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);
        ProgramList programList = ProgramList.getInstance();

        // Return immediately if favorites list already set
        if(programList.getFavoritesCount() > 0){
            receiver.send(Activity.RESULT_OK, resultData);
            return;
        }

        try {

            // Get all favorites
            //
            // Note that we need to have a valid vrtlogin-{at,rt,expiry} cookies for this call
            // to succeed. As they are passed automatically by the application wide CookieHandler
            // there is no explicit reference to them here
            JSONObject returnObject = httpClient.getRequest(getString(R.string.service_catalog_favorites_url));
            if(httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            // FIXME: matching on Title might not be the best (worksforme[tm] though)
            //  Might be better to match on "whatsonId"
            for (int i = 0; i < returnObject.names().length(); i++) {
                String key = returnObject.names().getString(i);
                JSONObject favorite = returnObject.getJSONObject(key).getJSONObject("value");
                programList.setIsFavorite(favorite.getString("title"));
                Log.d(TAG, "Adding favorite: " + favorite.getString("title"));
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not download VRT.NU Series list: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }

    }
}
