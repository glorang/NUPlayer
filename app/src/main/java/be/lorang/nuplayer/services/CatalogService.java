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

public class CatalogService extends IntentService {
    private static final String TAG = "CatalogService";
    public final static String BUNDLED_LISTENER = "listener";

    // Unfortunately programType is not available in the data returned by the Suggest API
    // So we define it here as a static list (shouldn't update too often) as we need it to query
    // the VideoList in the correct order (asc|desc sorting) in ProgramService
    private final static String[] programTypes = {"daily","oneoff","reeksaflopend", "reeksoplopend"};

    private HTTPClient httpClient = HTTPClient.getInstance();
    private String url;
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

    public CatalogService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        ProgramList programList = ProgramList.getInstance();
        if(programList.getPrograms().size() == 0) {

            try {

                // Get entire catalog of available Programs per programType
                for(String programType : programTypes) {

                    url = String.format(getString(R.string.service_catalog_catalog_url), programType);
                    Log.d(TAG, "Getting catalog part " + programType + " at " + url);
                    jsonObject = httpClient.getRequest(url);

                    if (httpClient.getResponseCode() != 200) {
                        continue;
                    }

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
                        String altImage = programJSON.optString(TAG_ALTIMAGE).replaceFirst("^(https:)?//images.vrt.be/orig/", "");
                        String imageServer = getString(R.string.model_image_server);

                        // we only use the 1st brand for now in the array
                        String brand = (String)brands.get(0);

                        Program program = new Program(
                                title,
                                description,
                                programName,
                                programType,
                                programUrl,
                                thumbnail,
                                altImage,
                                brand,
                                imageServer,
                                false,
                                false
                        );

                        programList.addProgram(program);

                        Log.d(TAG, "Adding to catalog : " + title + " " + programName + " " + programType + " " + programUrl);
                    }
                }

                // Sort catalog
                programList.sort();

                receiver.send(Activity.RESULT_OK, resultData);

            } catch (Exception e) {
                String message = "Could not download VRT.NU catalog:: " + e.getMessage();
                Log.e(TAG, message);
                e.printStackTrace();
                resultData.putString("MSG", message);
                receiver.send(Activity.RESULT_CANCELED, resultData);
            }
        } else {
            // we already have data in the catalog, receiving side can query the result via the Singleton ProgramList class
            // FIXME: handle catalog updates / ProgramList / Favorites refresh
            receiver.send(Activity.RESULT_OK, resultData);
        }

    }
}
