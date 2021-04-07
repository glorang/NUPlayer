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
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoList;

import com.bumptech.glide.load.HttpException;

import org.json.JSONArray;
import org.json.JSONObject;

import static be.lorang.nuplayer.services.ProgramService.parseVideoFromJSON;

/*
 * This class will load the 10 most recent published Videos
 *
 * Results are stored in VideoList class, using the START_INDEX parameter we load more
 * episodes dynamically
 *
 */

public class LatestService extends IntentService {
    private static final String TAG = "LatestService";
    public final static String BUNDLED_LISTENER = "listener";
    private Bundle resultData = new Bundle();

    private HTTPClient httpClient = new HTTPClient();
    private JSONObject returnObject;
    private int size = 10;
    private int maxSize = 100;

    public LatestService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);
        String url;

        try {

            // get passed start index from where we should start loading videos (offset)
            int startIndex = workIntent.getExtras().getInt("START_INDEX");

            // Initiate videoList
            VideoList videoList = VideoList.getInstance();
            if(startIndex == 1) {
                // Clear VideoList on first load
                videoList.clear();
            }

            // Add an additional check to make sure we don't create a loading loop
            // This should never happen: we already catch any non-200 HTTP response
            // and any JSON parsing exception but better be sure we don't flood VRT.NU search API
            // This could theoretically happen when the first request is successful but any
            // next request returns an unwanted, but valid JSON, response
            int va = videoList.getVideosAvailable();
            if(va > 0 && startIndex > va) {
                resultData.putString("MSG", "Start index (" + startIndex + ") > videos available (" + va + "). This should never happen.");
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            int vl = videoList.getVideosLoaded();
            if(vl > maxSize) {
                resultData.putString("MSG", "Not loading more than "  + maxSize + " videos!");
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
            }

            url = String.format(getString(R.string.service_program_latest_url),
                    Integer.toString(size),
                    Integer.toString(startIndex)
            );

            Log.d(TAG, "Getting program details at: " + url);

            // Get program details
            returnObject = httpClient.getCachedRequest(getCacheDir(), url);

            if (httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            JSONObject meta = returnObject.getJSONObject("meta");
            JSONArray items = returnObject.getJSONArray("results");

            // Update VideoList object with latest numbers of available / loaded
            int videosAvailable = meta.getInt("total_results");
            int videosLoaded = videoList.getVideosLoaded() + items.length();

            videoList.setVideosAvailable(videosAvailable);
            videoList.setVideosLoaded(videosLoaded);

            Log.d(TAG, "Available = " + videosAvailable);
            Log.d(TAG, "Loaded = " + videosLoaded);

            for (int i = 0; i < items.length(); i++) {
                JSONObject program = items.getJSONObject(i);
                String imageServer = getString(R.string.model_image_server);
                Video video = parseVideoFromJSON(program, imageServer);
                videoList.addVideo(video);
                Log.d(TAG, "Adding video : " + video.getTitle());
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get latest videos: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }

    }

}
