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
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoList;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProgramService extends IntentService {
    private static final String TAG = "ProgramService";
    public final static String BUNDLED_LISTENER = "listener";
    private Bundle resultData = new Bundle();

    private Program program;
    private String sortOrder = "";
    private JSONObject returnObject;
    private String season = "";

    public static final String TAG_TITLE = "title";
    public static final String TAG_DESCRIPTION = "shortDescription";
    public static final String TAG_SEASONNAME = "seasonName";
    public static final String TAG_EPISODENR = "episodeNumber";
    public static final String TAG_DURATION = "duration";
    public static final String TAG_THUMBNAIL = "videoThumbnailUrl";
    public static final String TAG_VIDEOID = "videoId";
    public static final String TAG_PUBID = "publicationId";
    public static final String TAG_BROADCASTDATE = "formattedBroadcastDate";
    public static final String TAG_PROGRAMTYPE = "programType";

    public ProgramService() {
        super(TAG);
    }

    private String parseSeason(JSONObject inputData) {

        String result = "";

        try {

            // https://www.vrt.be/vrtnu/a-z/het-journaal.model.json
            // [":items"].parsys[":items"].container[":items"].banner[":items"].navigation[":itemsOrder"]               // generic, valid for most cases
            // [":items"].parsys[":items"].container[":items"]["episodes-list"][":items"].navigation[":itemsOrder"]     // la-theorie-du-y.model.json
            // [":items"].parsys[":items"].container[":items"].banner[":items"] (empty)                                 // albatros.model.json, unreleased, no seasons/episodes yet

            JSONObject level1 = (JSONObject) inputData.get(":items");
            JSONObject level2 = (JSONObject) level1.get("parsys");
            JSONObject level3 = (JSONObject) level2.get(":items");
            JSONObject level4 = (JSONObject) level3.get("container");
            JSONObject level5 = (JSONObject) level4.get(":items");

            JSONObject level6;
            try {
                level6 = (JSONObject) level5.get("banner");
            } catch(JSONException e) {
                e.printStackTrace();
                // if banner is not set, try with "episodes-list"
                try {
                    level6 = (JSONObject) level5.get("episodes-list");
                } catch(JSONException ee) {
                    ee.printStackTrace();
                    Log.d(TAG, "Can not determine season");
                    return "";
                }
            }

            JSONObject level7 = (JSONObject) level6.get(":items");
            JSONObject level8;
            try {
                 level8 = (JSONObject) level7.get("navigation");
            } catch(JSONException e) {
                e.printStackTrace();
                // No episodes or seasons probably exist for this show
                return "";
            }
            JSONArray itemsOrder = (JSONArray) level8.get(":itemsOrder");

            // FIXME: for now we only support current season
            //  To check how we can implement this in the GUI
            Log.d(TAG, "Seasons discovered: " + itemsOrder.toString());
            result = itemsOrder.get(0).toString().replaceFirst("^0","");

        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);
        VideoList videoList = new VideoList();
        String url;

        try {

            // get passed Program object
            String programJson = workIntent.getExtras().getString("PROGRAM_OBJECT");
            program = new Gson().fromJson(programJson, Program.class);

            // Get list of available seasons
            url = String.format(getString(R.string.service_program_seasons_url), program.getProgramName());
            Log.d(TAG, "Getting season info at " + url);
            HTTPClient client = new HTTPClient();
            returnObject = client.getRequest(url);
            if(client.getResponseCode() == 200) {
                season = parseSeason(returnObject);
            }
            Log.d(TAG, "Set season to: " + season);

            url = String.format(getString(R.string.service_program_program_url), program.getProgramUrl());
            if(season.length() > 0) {
                url = url + "&facets[seasonTitle]=" + season;
            }
            Log.d(TAG, "Getting program details at: " + url);
            returnObject = new HTTPClient().getRequest(url);
            JSONArray items = returnObject.getJSONArray("results");

            for (int i = 0; i < items.length(); i++) {
                JSONObject program = items.getJSONObject(i);

                String title = program.optString(TAG_TITLE);
                String description = program.optString(TAG_DESCRIPTION);
                String seasonName = program.optString(TAG_SEASONNAME);
                int episodeNumber = program.optInt(TAG_EPISODENR);
                int duration = program.optInt(TAG_DURATION);
                String videoId  = program.optString(TAG_VIDEOID);
                String pubId  = program.optString(TAG_PUBID);
                String formattedBroadcastDate = program.optString(TAG_BROADCASTDATE);

                // get sort order (data is not available in Program)
                sortOrder = program.optString(TAG_PROGRAMTYPE);

                // we replace the image server with the one defined in urls.xml
                // this prepends the (often) 'missing' 'https://' and allows us to query our own size ('orig' can go up to 18MB each(!))
                String thumbnail = program.optString(TAG_THUMBNAIL).replaceFirst("^(https:)?//images.vrt.be/orig/", "");
                String imageServer = getString(R.string.model_image_server);
                String streamType = StreamService.STREAMTYPE_ONDEMAND;

                Video video = new Video(title, description, seasonName, episodeNumber, duration, thumbnail, videoId, pubId, formattedBroadcastDate, imageServer, streamType);
                videoList.addVideo(video);

                Log.d(TAG, "Adding video : " + title + " " + episodeNumber);
            }

            // sort result
            videoList.sort(sortOrder);

            resultData.putString("VIDEO_LIST", (new Gson()).toJson(videoList));
            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            resultData.putString("MSG", "Could not get program info: " + e.getMessage());
            e.printStackTrace();
            receiver.send(Activity.RESULT_CANCELED, resultData);
            return;
        }

    }
}
