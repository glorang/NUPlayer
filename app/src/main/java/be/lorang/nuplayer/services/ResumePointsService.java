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
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.ProgramList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/*
 * This class will get or update Resume Points for a Video object and query for all "Watch Later"
 * Videos.
 *
 * It will populate / manage the VideoContinueWatchList and VideoWatchLaterList singleton classes
 *
 */

public class ResumePointsService extends IntentService {
    private static final String TAG = "ResumePointService";
    public final static String BUNDLED_LISTENER = "listener";

    public final static String ACTION_GET = "getContinueWatchingWatchLater";
    public final static String ACTION_UPDATE_RESUME_POINT = "updateResumePoint";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();
    private String xvrttoken = "";

    public ResumePointsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            String action = workIntent.getExtras().getString("ACTION");
            xvrttoken = workIntent.getExtras().getString("X-VRT-Token", "");

            switch(action) {
                case ResumePointsService.ACTION_GET:
                    populateVideoLists();
                    break;
                case ResumePointsService.ACTION_UPDATE_RESUME_POINT:
                    Video video = new Gson().fromJson(workIntent.getExtras().getString("VIDEO_OBJECT"), Video.class);
                    int position = workIntent.getExtras().getInt("PLAYER_CURRENT_POSITION");
                    updateResumePoint(video, position);
                    break;
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not download get resume points: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }

    // Get all resume points and store them in classes VideoContinueWatchingList and VideoWatchLaterList
    private void populateVideoLists() throws IOException, JSONException {

        // Only run once as we track resume points / watch later ourselves once initialized
        if(VideoWatchLaterList.getInstance().isVideoListInitialized() &&
                VideoContinueWatchingList.getInstance().isVideoListInitialized())  {
            return;
        }

        Map<String, String> headers = new HashMap<>();
        if(xvrttoken.length() > 0){
            headers.put("authorization", "Bearer " + xvrttoken);
        }

        JSONObject returnObject = httpClient.getRequest(getString(R.string.service_resumepoints_url), headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        // only show resume points of last month (FIXME: add setting if we ever have a settings menu)
        Instant lastMonth = ZonedDateTime.now().minusMonths(1).toInstant();

        for (int i = 0; i < returnObject.names().length(); i++) {
            String key = returnObject.names().getString(i);
            JSONObject resumePointObject = returnObject.getJSONObject(key);
            JSONObject value = resumePointObject.getJSONObject("value");

            Instant created = new Timestamp(resumePointObject.getLong("created")).toInstant();
            String url = value.getString("url");

            Log.d(TAG, "url =" + url);

            // add base url if missing
            if(url.startsWith("/vrtnu/a-z/")) {
                url = "//www.vrt.be" + url;
            }

            // Check if watchLater or resume point
            boolean watchLater = false;
            if(value.has("watchLater")) {
                watchLater = value.getBoolean("watchLater");
                Log.d(TAG,"watch later = " + watchLater + " for url " + url);
            }

            Log.d(TAG, "Parsing resume point entry: " + resumePointObject.toString());

            // Skip resume points who are created more than a month ago
            if (!watchLater && created.isBefore(lastMonth)) {
                Log.d(TAG, "Skipping " + url + " as created > 1 month " + created.toString());
                continue;
            }

            // Get current position / total / progress
            Double position = value.getDouble("position");
            Double total = value.getDouble("total");
            Double progress = (position / total) * 100;

            // skip videos with 5 < progress > 95
            if (!watchLater && (progress < 5 || progress > 95)) {
                Log.d(TAG, "Skipping " + url + " as progress =  " + progress);
                continue;
            }

            // Create Video object of result
            String queryURL = String.format(getString(R.string.service_resumepoints_video_url), url);
            Log.d(TAG, "Getting video info at: " + queryURL);
            HTTPClient videoHTTPClient = new HTTPClient();
            JSONObject videoReturnObject = videoHTTPClient.getCachedRequest(getCacheDir(), queryURL, 1440);
            if(videoHTTPClient.getResponseCode() != 200) {
                continue;
            }

            Log.d(TAG, "Video result = " + videoReturnObject.toString());

            JSONObject meta = videoReturnObject.getJSONObject("meta");
            if(meta.getInt("total_results") != 1) {
                continue;
            }

            // Parse (first) result to Video object
            JSONArray items = videoReturnObject.getJSONArray("results");
            JSONObject program = items.getJSONObject(0);
            String imageServer = getString(R.string.model_image_server); // FIXME: quick hack
            Video video = ProgramService.parseVideoFromJSON(program, imageServer);
            video.setProgressPct(progress.intValue());
            video.setCurrentPosition(position.intValue());

            if(watchLater) {
                VideoWatchLaterList.getInstance().addVideo(video);
            } else {
                video.setDuration(total.intValue());
                VideoContinueWatchingList.getInstance().addVideo(video);
            }

        }
    }

    // Update resume point with new position
    private void updateResumePoint(Video video, int position) throws JSONException, IOException {

        if(video == null || position == 0) { return; }

        // Don't track Live TV progress
        if(video.getStreamType().equals(StreamService.STREAMTYPE_LIVETV)) {
            return;
        }

        // Update progress in VideoContinueWatchingList
        if(!VideoContinueWatchingList.getInstance().setProgress(video, position)) {
            // Video not found, add it and set progress
            VideoContinueWatchingList.getInstance().addVideo(video);
            VideoContinueWatchingList.getInstance().setProgress(video, position);
        }

        // Update progress in VideoList
        VideoList.getInstance().setProgress(video, position);

        // Update progress at VRT side
        // Note that it takes multiple minutes before this change is visible @ VRT side
        String assetPath = video.getAssetPath()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        // Remove base url if present
        String videoUrl = video.getURL();
        videoUrl = videoUrl.replaceFirst("^(https:)?//www.vrt.be", "");

        JSONObject postData = new JSONObject();
        postData.put("url", videoUrl);
        postData.put("position", position);
        postData.put("total", video.getDuration());
        postData.put("whatsonId", video.getWhatsonId());

        Log.d(TAG, "Updating resume point, post data = " + postData);
        String url = getString(R.string.service_resumepoints_url) + "/" + assetPath;

        Map<String, String> headers = new HashMap<>();
        if(xvrttoken.length() > 0) {
            headers.put("authorization", "Bearer " + xvrttoken);
        }

        httpClient.postRequest(url, "application/json", postData, headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        Log.d(TAG, "Resume point updated successfully");

    }
}
