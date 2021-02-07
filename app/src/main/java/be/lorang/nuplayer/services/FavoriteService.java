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
import com.google.gson.Gson;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
 * This class will manage Favorites in the Catalog and send Favorite updates to VRT
 */

public class FavoriteService extends IntentService {
    private static final String TAG = "FavoriteService";
    public final static String BUNDLED_LISTENER = "listener";

    private ProgramList programList = ProgramList.getInstance();

    public final static String ACTION_GET = "getFavorites";
    public final static String ACTION_UPDATE_FAVORITE = "updateFavorite";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();
    private String xvrttoken = "";

    public FavoriteService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            String action = workIntent.getExtras().getString("ACTION");
            xvrttoken = workIntent.getExtras().getString("X-VRT-Token", "");

            switch (action) {
                case FavoriteService.ACTION_GET:
                    populateFavorites();
                    break;
                case FavoriteService.ACTION_UPDATE_FAVORITE:
                    Program program = new Gson().fromJson(workIntent.getExtras().getString("PROGRAM_OBJECT"), Program.class);
                    boolean isFavorite = workIntent.getExtras().getBoolean("IS_FAVORITE", false);
                    String whatsonId = workIntent.getExtras().getString("WHATSONID", "");
                    updateFavorite(program, isFavorite, whatsonId);
                    break;
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not download/update VRT.NU favorites: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }

    // Get all favorites
    private void populateFavorites() throws IOException, JSONException {

        // Only run once as we track Favorites ourselves once initialized
        if(programList.getFavoritesCount() > 0){
            return;
        }

        Map<String, String> headers = new HashMap<>();
        if(xvrttoken.length() > 0) {
            headers.put("authorization", "Bearer " + xvrttoken);
        }

        JSONObject returnObject = httpClient.getRequest(getString(R.string.service_catalog_favorites_url), headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        // Match on Title, it's the best option available
        // whatsonId is missing in Program
        // programUrl is used inconsistently by VRT API (starting with (https://)?<base url>, ending in .html or /)
        for (int i = 0; i < returnObject.names().length(); i++) {
            String key = returnObject.names().getString(i);
            JSONObject favorite = returnObject.getJSONObject(key).getJSONObject("value");
            programList.setIsFavorite(favorite.getString("title"), favorite.getBoolean("isFavorite"));
            Log.d(TAG, "Adding favorite: " + favorite.getString("title"));
        }
    }

    // Update favorites (add / remove)
    private void updateFavorite(Program program, Boolean isFavorite, String whatsonId) throws JSONException, IOException {

        if(program == null) { return; }

        // Set new state in ProgramList
        programList.setIsFavorite(program.getTitle(), isFavorite);

        // Set Favorite at VRT side

        // Remove base url if present
        String programUrl = program.getProgramUrl();
        programUrl = programUrl.replaceFirst("^(https:)?//www.vrt.be", "");

        JSONObject postData = new JSONObject();
        postData.put("isFavorite", isFavorite);
        postData.put("programUrl", programUrl);
        postData.put("title", program.getTitle());
        if(whatsonId.length() > 0) {
            postData.put("whatsonId", whatsonId);
        }

        String assetPath = programUrl.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String url = getString(R.string.service_catalog_favorites_url) + "/" + assetPath;

        Log.d(TAG, "Updating Favorite, url = " + url + " post data = " + postData);

        Map<String, String> headers = new HashMap<>();
        if(xvrttoken.length() > 0) {
            headers.put("authorization", "Bearer " + xvrttoken);
        }

        httpClient.postRequest(url, "application/json", postData, headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        Log.d(TAG, "Favorite updated successfully");

    }
}
