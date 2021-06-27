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

import be.lorang.nuplayer.BuildConfig;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ResumePoint;
import be.lorang.nuplayer.model.ResumePointList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.ProgramList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
 * ResumePoint service is responsible for:
 * - Getting all ResumePoints
 * - Getting all Watch Later items (they are returned in the same API call)
 *
 * It will:
 * - Add all ResumePoints of last year to ResumePointsList so ProgramService can track progress of ALL videos (of last year)
 * - Add all watch later items as Video to VideoWatchLaterList
 * - Add all ResumePoints as Videos with progress > 5 & < 95 from last month to VideoContinueWatchList
 *     For those last two this class will parse a ResumePoint to a Video, this requires
 *     an extra API call for each Video so we try to reduce the elements as much as possible / cache them for a long time.
 *     A single video object shouldn't update in any case
 *
 * - Update ResumePoints / Watch Later entries (either on user request or when a video is stopped / paused)
 *
 */

public class ResumePointsService extends IntentService {
    private static final String TAG = "ResumePointService";
    public final static String BUNDLED_LISTENER = "listener";

    public final static String ACTION_GET = "getContinueWatchingWatchLater";
    public final static String ACTION_UPDATE_RESUME_POINT = "updateResumePoint";
    public final static String ACTION_DELETE_RESUME_POINT = "deleteResumePoint";
    public final static String ACTION_UPDATE_WATCH_LATER = "updateWatchLater";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();
    private String vrtnu_site_profile_vt = "";
    private Video video;

    public ResumePointsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            String action = workIntent.getExtras().getString("ACTION");
            vrtnu_site_profile_vt = workIntent.getExtras().getString("vrtnu_site_profile_vt", "");

            switch(action) {
                case ResumePointsService.ACTION_GET:
                    populateVideoLists();
                    break;
                case ResumePointsService.ACTION_UPDATE_RESUME_POINT:
                    video = new Gson().fromJson(workIntent.getExtras().getString("VIDEO_OBJECT"), Video.class);
                    int position = workIntent.getExtras().getInt("PLAYER_CURRENT_POSITION");
                    updateResumePoint(video, position);
                    break;
                case ResumePointsService.ACTION_DELETE_RESUME_POINT:
                    video = new Gson().fromJson(workIntent.getExtras().getString("VIDEO_OBJECT"), Video.class);
                    deleteResumePoint(video);
                    break;
                case ResumePointsService.ACTION_UPDATE_WATCH_LATER:
                    video = new Gson().fromJson(workIntent.getExtras().getString("VIDEO_OBJECT"), Video.class);
                    boolean watchLater = workIntent.getExtras().getBoolean("WATCH_LATER");
                    updateWatchLater(video, watchLater);
                    break;
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get/set resume point(s)/watch later: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }

    // Get all resume points and store them in classes VideoContinueWatchingList and VideoWatchLaterList
    private void populateVideoLists() throws IOException, JSONException {

        ResumePointList resumePointList = ResumePointList.getInstance();

        // Only run once as we track resume points / watch later ourselves once initialized
        if(resumePointList.getResumePoints().size() > 0) {
            return;
        }

        // Get all resume points from VRT
        Map<String, String> headers = new HashMap<>();
        if(vrtnu_site_profile_vt.length() > 0){
            headers.put("authorization", "Bearer " + vrtnu_site_profile_vt);
        }

        JSONObject returnObject = httpClient.getRequest(getString(R.string.service_resumepoints_url), headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        // only track resume points of last year
        Instant lastMonth = ZonedDateTime.now().minusMonths(1).toInstant();
        Instant lastYear = ZonedDateTime.now().minusYears(1).toInstant();

        for (int i = 0; i < returnObject.names().length(); i++) {
            String assetPath = returnObject.names().getString(i);
            JSONObject resumePointObject = returnObject.getJSONObject(assetPath);

            // Parse JSON to ResumePoint object
            long created = resumePointObject.getLong("created");

            // Skip if entry if older than 1 year
            Instant createdInstant = new Timestamp(created).toInstant();
            if (createdInstant.isBefore(lastYear)) {
                continue;
            }

            // Get remaining fields from JSONObject
            long updated = resumePointObject.getLong("updated");
            JSONObject value = resumePointObject.getJSONObject("value");
            String url = value.getString("url");
            Double position = value.getDouble("position");
            Double total = value.getDouble("total");
            Double progress = (position / total) * 100;
            boolean watchLater = false;


            // add base url if missing
            if(url.startsWith("/vrtnu/a-z/")) {
                url = "//www.vrt.be" + url;
            }

            // Create ResumePoint object
            ResumePoint resumePoint = new ResumePoint(
                    created,
                    updated,
                    url,
                    position,
                    total,
                    progress
            );

            // Check if watchLater is set
            if(value.has("watchLater")) {
                watchLater = value.getBoolean("watchLater");
                resumePoint.setWatchLater(watchLater);
            }

            // A video can be both a "Continue Watching" as a "Watch Later" video
            // If progress >= 5% "Continue Watching" takes precedence
            if(watchLater && progress >= 5) {
                watchLater = false;
            }

            // Add to ResumePointList
            resumePointList.add(resumePoint);

            // Parse ResumePoints of last month with with 5 < progress > 95 to a Video object
            // for display on HomeFragment's Watch Later | Continue Watching ListRows

            // Skip resume points who are created more than a month ago
            if (!watchLater && createdInstant.isBefore(lastMonth)) {
                Log.d(TAG, "Skipping " + url + " as created > 1 month " + createdInstant.toString());
                continue;
            }

            // skip videos with 5 < progress > 95
            if (!watchLater && (progress < 5 || progress > 95)) {
                Log.d(TAG, "Skipping " + url + " as progress =  " + progress);
                continue;
            }

            // Create Video object of result
            String queryURL = String.format(getString(R.string.service_resumepoints_video_url), url);
            Log.d(TAG, "Getting video info at: " + queryURL);
            HTTPClient videoHTTPClient = new HTTPClient();
            // Cache single video object (size=1) for 30 days, worst case some title or thumbnail is off
            JSONObject videoReturnObject = videoHTTPClient.getCachedRequest(getCacheDir(), queryURL, 43200);
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
            String imageServer = getString(R.string.model_image_server);
            Video video = ProgramService.parseVideoFromJSON(program, imageServer);
            video.setProgressPct(progress.intValue());
            video.setCurrentPosition(position.intValue());

            // (Some) Videos that are currently live are already available but their asset path might
            // change once finished so always use the asset path provided by the resume point (JSON key)
            video.setAssetPath(assetPath);

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

        // Calculate progress
        int progress = (int) (((double) position / video.getDuration()) * 100);

        // Update progress in VideoContinueWatchingList
        if(!VideoContinueWatchingList.getInstance().setProgress(video, position)) {
            // Video not found, add it and set progress
            VideoContinueWatchingList.getInstance().addVideo(video);
            VideoContinueWatchingList.getInstance().setProgress(video, position);
        }

        // Remove from VideoWatchLaterList if present as it is in VideoContinueWatchingList if progress > 5%
        if(progress > 5) {
            Log.d(TAG, "Removing from video watchlater, progress = " + progress);
            VideoWatchLaterList.getInstance().removeVideo(video);
        }

        // Update progress in ResumePointList
        if(!ResumePointList.getInstance().setProgress(video, position)) {
            // ResumePoint not found, create new one and set progress
            ResumePoint resumePoint = new ResumePoint(
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    video.getURL(),
                    video.getCurrentPosition(),
                    video.getDuration(),
                    progress
            );
            ResumePointList.getInstance().add(resumePoint);
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
        if(vrtnu_site_profile_vt.length() > 0) {
            headers.put("authorization", "Bearer " + vrtnu_site_profile_vt);
        }

        httpClient.postRequest(url, "application/json", postData, headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        Log.d(TAG, "Resume point updated successfully");

    }

    // Delete resume point
    private void deleteResumePoint(Video video) throws IOException {

        if(video == null) { return; }

        // Remove from VideoWatchLaterList
        VideoWatchLaterList.getInstance().removeVideo(video);

        // Remove from VideoContinueWatchList
        VideoContinueWatchingList.getInstance().removeVideo(video);

        // Remove from ResumePointList
        ResumePointList.getInstance().remove(video);

        // Remove at VRT
        String assetPath = video.getAssetPath()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        Log.d(TAG, "Deleting resume point at = " + assetPath);

        OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
                .url(getString(R.string.service_resumepoints_url) + "/" + assetPath)
                .addHeader("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", "Bearer " + vrtnu_site_profile_vt)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException(response.code() + ": " + response.message());
        }

        Log.d(TAG, "Resume point deleted successfully");

    }

    // Toggle watch later
    private void updateWatchLater(Video video, boolean watchLater) throws JSONException, IOException {

        if(video == null) { return; }

        // Don't add Live tv to watch later (no listener attached in any case)
        if(video.getStreamType().equals(StreamService.STREAMTYPE_LIVETV)) {
            return;
        }

        // Add to/remove from watch later list
        VideoWatchLaterList videoWatchLaterList = VideoWatchLaterList.getInstance();
        if(watchLater) {
            video.setProgressPct(0);
            videoWatchLaterList.addVideo(video);
        } else {
            videoWatchLaterList.removeVideo(video);
        }

        // Add / remove on VRT side
        // Note that it takes multiple minutes before this change is visible @ VRT side
        String assetPath = video.getAssetPath()
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        // Remove base url if present
        String videoUrl = video.getURL();
        videoUrl = videoUrl.replaceFirst("^(https:)?//www.vrt.be", "");

        JSONObject postData = new JSONObject();
        postData.put("url", videoUrl);
        postData.put("position", 0);
        postData.put("total", 100);
        postData.put("watchLater", watchLater);

        Log.d(TAG, "Updating watch later, post data = " + postData);
        String url = getString(R.string.service_resumepoints_url) + "/" + assetPath;

        Map<String, String> headers = new HashMap<>();
        if(vrtnu_site_profile_vt.length() > 0) {
            headers.put("authorization", "Bearer " + vrtnu_site_profile_vt);
        }

        httpClient.postRequest(url, "application/json", postData, headers);
        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        Log.d(TAG, "Watch later updated successfully");

    }
}
