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

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class CatalogService extends IntentService {
    private static final String TAG = "CatalogService";
    public final static String BUNDLED_LISTENER = "listener";
    private Bundle resultData = new Bundle();
    private JSONObject jsonObject;
    private JSONArray items;

    private static final String TAG_TITLE = "title";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_PROGNAME = "programName";
    private static final String TAG_PROGURL = "programUrl";
    private static final String TAG_THUMBNAIL = "thumbnail";
    private static final String TAG_ALTIMAGE = "alternativeImage";
    private static final String TAG_BRANDS = "brands";

    private ArrayList<String> favorites = new ArrayList<String>();
    private ArrayList<String> timeLimitedSeries = new ArrayList<String>();

    public CatalogService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        ProgramList programList = ProgramList.getInstance();
        if(programList.getPrograms().size() == 0) {

            try {

                // If user is authenticated we first we get all favorites,
                // if there are any they will be marked as favorite when we create the Program object

                // This requires a X-VRT-Token "user" variant

                /*
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
                String xvrttoken = prefs.getString("X-VRT-Token", "");
                if(xvrttoken.length() > 0) {

                    String url = getString(R.string.service_catalog_favorites_url);
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + xvrttoken);

                    JSONObject returnObject = new HTTPClient().getRequest(url, "application/json", headers);
                    Log.d(TAG, returnObject.toString());

                    for (int i = 0; i < returnObject.names().length(); i++) {
                        String key = returnObject.names().getString(i);
                        JSONObject favorite = returnObject.getJSONObject(key).getJSONObject("value");
                        favorites.add(favorite.getString("title"));
                        Log.d(TAG, "Adding favorite: " + favorite.getString("title"));
                    }

                }
                */

                // Get all "time limited" series, we'll mark them as time limited when creating the Program object
                HTTPClient client = new HTTPClient();
                jsonObject = client.getRequest(getString(R.string.service_catalog_series_url));

                if(client.getResponseCode() != 200) {
                    resultData.putString("MSG", "Error occurred in downloading VRT.NU catalog: " + client.getResponseMessage());
                    receiver.send(Activity.RESULT_CANCELED, resultData);
                    return;
                }

                items = jsonObject.getJSONArray("data");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject programJSON = items.getJSONObject(i);
                    timeLimitedSeries.add(programJSON.getString("programName"));
                }

                // Get entire catalog of available Programs
                jsonObject = new HTTPClient().getRequest(getString(R.string.service_catalog_catalog_url));
                items = jsonObject.getJSONArray("data");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject programJSON = items.getJSONObject(i);

                    String title = programJSON.optString(TAG_TITLE);
                    String description = programJSON.optString(TAG_DESCRIPTION);
                    String programName = programJSON.optString(TAG_PROGNAME);
                    String programUrl = programJSON.optString(TAG_PROGURL);
                    JSONArray brands = programJSON.optJSONArray(TAG_BRANDS);

                    // we replace the image server with the one defined in urls.xml
                    // this prepends the (often) 'missing' 'https://' and allows us to query our own size ('orig' can go up to 18MB each(!))
                    String thumbnail = programJSON.optString(TAG_THUMBNAIL).replaceFirst("^(https:)?//images.vrt.be/orig/", "");
                    String altImage  = programJSON.optString(TAG_ALTIMAGE).replaceFirst("^(https:)?//images.vrt.be/orig/", "");;
                    String imageServer = getString(R.string.model_image_server);

                    // we only use the 1st brand for now in the array
                    String brand = (String)brands.get(0);

                    // Check if program is marked as favorite
                    boolean isFavorite = favorites.contains(title);

                    // Check if program is part of time limited series
                    boolean isTimeLimited = timeLimitedSeries.contains(programName);

                    Program program = new Program(title, description, programName, programUrl, thumbnail, altImage, brand, imageServer, isFavorite, isTimeLimited);
                    programList.addProgram(program);

                    Log.d(TAG, "Adding to catalog : " + title + " " + programName + " " + programUrl);
                }

                receiver.send(Activity.RESULT_OK, resultData);

            } catch (Exception e) {
                Log.e(TAG, "Error occurred in downloading VRT.NU catalog");
                resultData.putString("MSG", "Error occurred in downloading VRT.NU catalog: " + e.getMessage());
                e.printStackTrace();
                receiver.send(Activity.RESULT_CANCELED, resultData);
            }
        } else {
            // we already have data in the catalog, receiving side can query the result via the Singleton ProgramList class
            // FIXME: handle catalog updates / ProgramList / Favorites refresh
            receiver.send(Activity.RESULT_OK, resultData);
        }

    }
}
