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

/*
 * This class will download the list of series from the VRT.NU website and add them to the
 * ProgramList Series array
 *
 * As it is based on a semi-dynamic source (list_946984311.model.json - see plugin.video.vrt.nu > VRT NU API Wiki page)
 * I've included a backup source as well that will query the real search API which should
 * be more consistent but it cannot be filtered for 100% correct results or sorting.
 *
 */

public class SeriesService extends IntentService {
    private static final String TAG = "SeriesService";
    public final static String BUNDLED_LISTENER = "listener";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    public SeriesService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);
        ProgramList programList = ProgramList.getInstance();

        // Return immediately if series list already set
        if(programList.getSeriesCount() > 0){
            receiver.send(Activity.RESULT_OK, resultData);
            return;
        }

        try {

            // Get series from "primary" URL
            JSONObject returnObject = httpClient.getCachedRequest(getCacheDir(), getString(R.string.service_catalog_series_url), 1440);
            JSONArray items = null;
            if(httpClient.getResponseCode() == 200) {
                items = returnObject.getJSONArray("items");
            } else {
                // Get series from backup URL
                returnObject = httpClient.getCachedRequest(getCacheDir(), getString(R.string.service_catalog_series_backup_url), 1440);
                if(httpClient.getResponseCode() == 200) {
                    items = returnObject.getJSONArray("data");
                } else {
                    throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
                }
            }

            if(items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject programJSON = items.getJSONObject(i);
                    Log.d(TAG, "Setting isSerie = true for: " + programJSON.get("programName"));
                    programList.setIsSerie(programJSON.getString("programName"));
                }
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
