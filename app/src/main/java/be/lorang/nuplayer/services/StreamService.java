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
import be.lorang.nuplayer.ui.MainActivity;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Video;

import com.bumptech.glide.load.HttpException;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * This class will obtain stream and DRM info for a given Video
 * For now we ignore the HLS streams and only use the MPEG-DASH streams
 */

public class StreamService extends IntentService {

    private static final String TAG = "StreamService";
    public final static String BUNDLED_LISTENER = "listener";
    public static final String STREAMTYPE_ONDEMAND = "ondemand";
    public static final String STREAMTYPE_LIVETV = "livetv";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    private String vrtPlayerToken;
    private String url;

    private static final String client = "vrtvideo@PROD";

    public StreamService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(VrtPlayerTokenService.BUNDLED_LISTENER);

        try {

            // get passed Video object
            String videoJson = workIntent.getExtras().getString("VIDEO_OBJECT");
            Video video = new Gson().fromJson(videoJson, Video.class);
            String streamType = video.getStreamType();

            // get vrtPlayerToken
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
            if(streamType.equals(STREAMTYPE_ONDEMAND)) {

                vrtPlayerToken = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED, "");

                url = getString(R.string.service_stream_stream_info_server) +
                        video.getPubId() + URLEncoder.encode("$", "utf-8") + video.getVideoId() +
                        "?vrtPlayerToken=" + vrtPlayerToken +
                        "&client=" + client;

            } else if(streamType.equals(STREAMTYPE_LIVETV)) {
                vrtPlayerToken = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS, "");

                url = getString(R.string.service_stream_stream_info_server) +
                        video.getVideoId() +
                        "?vrtPlayerToken=" + vrtPlayerToken +
                        "&client=" + client;

            }
            Log.d(TAG, "Requesting video info at " + url);
            JSONObject returnObject = httpClient.getRequest(url);

            if (httpClient.getResponseCode() != 200) {

                String message = "";
                try {
                    message = returnObject.getString("message");
                }catch (JSONException e) {
                    message = httpClient.getResponseCode() + "; " + httpClient.getResponseMessage();
                    e.printStackTrace();
                }

                throw new HttpException(message);
            }

            // Get DRM token if set
            String drm = returnObject.getString("drm");
            if(!drm.equals("null")) {
                Log.d(TAG, "Adding VUALTO token " + drm);
                resultData.putString("VUALTO_TOKEN", drm);
            }

            JSONArray targetUrls = returnObject.getJSONArray("targetUrls");
            for (int i = 0; i < targetUrls.length(); i++) {
                JSONObject streamUrl = targetUrls.getJSONObject(i);
                if (streamUrl.getString("type").equals("mpeg_dash")) {
                    Log.d(TAG, "Adding URL " + streamUrl.getString("url") + " for Video " + video.getTitle());
                    resultData.putString("MPEG_DASH_URL", streamUrl.getString("url"));
                }
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get stream info: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }
}
