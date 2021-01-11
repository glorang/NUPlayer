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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProgramService extends IntentService {
    private static final String TAG = "ProgramService";
    public final static String BUNDLED_LISTENER = "listener";
    private Bundle resultData = new Bundle();

    private HTTPClient httpClient = new HTTPClient();
    private Program program;
    private JSONObject returnObject;
    private LinkedHashMap<String,String> seasons;
    private int size = 10;

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


    // Try to make the best out of the season parsing, all in all it's not a real problem
    // if this fails, in this case it will return an empty map and we search for all episode
    // regardless of the season. Episodes might be mixed up because of this but seems accurate for now
    private LinkedHashMap<String,String> parseSeason(JSONObject inputData) {

        LinkedHashMap<String,String> result = new LinkedHashMap<>();

        try {

            // https://www.vrt.be/vrtnu/a-z/het-journaal.model.json
            // path1 : [":items"].parsys[":items"].container[":items"].banner[":items"].navigation[":itemsOrder"]               // generic, valid for most cases
            // path2 : [":items"].parsys[":items"].container[":items"]["episodes-list"][":items"].navigation[":itemsOrder"]     // la-theorie-du-y.model.json
            // path3 : [":items"].parsys[":items"].container[":items"].navigation[":items"].container.title (=trailer)          // trailer available
            // [":items"].parsys[":items"].container[":items"].banner[":items"] (empty)                                 // albatros.model.json, unreleased, no seasons/episodes yet

            JSONObject parseObject = null;

            // try path1
            try {
                parseObject = inputData
                        .getJSONObject(":items")
                        .getJSONObject("parsys")
                        .getJSONObject(":items")
                        .getJSONObject("container")
                        .getJSONObject(":items")
                        .getJSONObject("banner")
                        .getJSONObject(":items")
                        .getJSONObject("navigation");
            } catch(JSONException e) {
                e.printStackTrace();
            }

            // try path2
            if(parseObject == null) {
                try {
                    parseObject = inputData
                            .getJSONObject(":items")
                            .getJSONObject("parsys")
                            .getJSONObject(":items")
                            .getJSONObject("container")
                            .getJSONObject(":items")
                            .getJSONObject("episodes-list")
                            .getJSONObject(":items")
                            .getJSONObject("navigation");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(parseObject != null) {
                JSONArray itemsOrder = parseObject.getJSONArray(":itemsOrder");
                JSONObject items = parseObject.getJSONObject(":items");

                Log.d(TAG, "Seasons discovered: " + itemsOrder.toString());

                // Get season names
                Map<String, String> seasonNames = new HashMap<>();
                Iterator<String> keys = items.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    if (items.get(key) instanceof JSONObject) {
                        String value = items.getJSONObject(key).get("title").toString();
                        seasonNames.put(key, value);
                    }
                }

                // Return result in correct sort order
                for (int i = 0; i < items.length(); i++) {
                    String key = itemsOrder.get(i).toString();
                    result.put(key, seasonNames.get(key));
                }
            }

            // try path3 (trailer) to see if we should add a "Trailer season"
            try {
                parseObject = inputData
                        .getJSONObject(":items")
                        .getJSONObject("parsys")
                        .getJSONObject(":items")
                        .getJSONObject("container")
                        .getJSONObject(":items")
                        .getJSONObject("navigation")
                        .getJSONObject(":items")
                        .getJSONObject("container");

                if(parseObject instanceof JSONObject) {
                    if(parseObject.getString("title").equals("Trailer")) {
                        Log.d(TAG, "Adding Trailer season");
                        result.put("trailer", "Trailer");
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // if parseObject is still null here everything failed
            if(parseObject == null) {
                Log.d(TAG, "Cannot determine season or find trailer");
                return null;
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // define sort order based on programType
    private String defineSortOrder(String programType) {
        if(programType.equals("reeksaflopend") || programType.equals("daily")) {
            return "desc";
        } else {
            return "asc";
        }
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);
        String url;

        try {

            // Setup VideoList object
            VideoList videoList;

            // get passed Program object
            String programJson = workIntent.getExtras().getString("PROGRAM_OBJECT");
            program = new Gson().fromJson(programJson, Program.class);

            // get passed seasons Map
            String seasonJson = workIntent.getExtras().getString("SEASON_LIST");
            seasons = new Gson().fromJson(seasonJson, LinkedHashMap.class);

            // get passed season index, this maps to the index inside the map as keys are defined as strings (2020, 2020-nj)
            int seasonIndex = workIntent.getExtras().getInt("SEASON_INDEX");

            // get passed start index from where we should start loading videos (offset)
            int startIndex = workIntent.getExtras().getInt("START_INDEX");

            // Initiate videoList or get existing one if passed
            if(startIndex == 1) {
                videoList = new VideoList();
            } else {
                String videoListJson = workIntent.getExtras().getString("VIDEO_LIST");
                videoList = new Gson().fromJson(videoListJson, VideoList.class);

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
            }

            // Get list of available seasons
            if(seasons == null) {
                url = String.format(getString(R.string.service_program_seasons_url), program.getProgramName());
                Log.d(TAG, "Getting season info at " + url);
                returnObject = httpClient.getRequest(url);
                if (httpClient.getResponseCode() == 200) {
                    seasons = parseSeason(returnObject);
                }
                Log.d(TAG, "Set season to: " + seasons);
            }

            url = String.format(getString(R.string.service_program_program_url),
                    defineSortOrder(program.getProgramType()),
                    Integer.toString(size),
                    Integer.toString(startIndex),
                    program.getProgramUrl()
            );

            // get selected season
            if(seasons != null && seasons.size() > 0) {
                String seasonName = (new ArrayList<String>(seasons.keySet())).get(seasonIndex);
                url = url + "&facets[seasonName]=" + seasonName;
            } else {
                // If seasons is still null here we either failed to decode the JSON or failed to
                // fetch the model.json, create dummy season
                seasons = new LinkedHashMap<>();
                seasons.put("0", "Unknown");
            }

            Log.d(TAG, "Getting program details at: " + url);

            // Get program details
            returnObject = httpClient.getRequest(url);

            if (httpClient.getResponseCode() != 200) {
                resultData.putString("MSG", "Could not get details: " + httpClient.getResponseMessage());
                receiver.send(Activity.RESULT_CANCELED, resultData);
                return;
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

                // fields get with getString are mandatory and will throw a JSONException if not set
                // fields get with optString are optional and have a default value of ""
                String title = program.getString(TAG_TITLE);
                String description = program.optString(TAG_DESCRIPTION);
                String seasonName = program.optString(TAG_SEASONNAME);
                int episodeNumber = program.optInt(TAG_EPISODENR);
                int duration = program.optInt(TAG_DURATION);
                String videoId  = program.getString(TAG_VIDEOID);
                String pubId  = program.getString(TAG_PUBID);
                String formattedBroadcastDate = program.optString(TAG_BROADCASTDATE);

                // we replace the image server with the one defined in urls.xml
                // this prepends the (often) 'missing' 'https://' and allows us to query our own size ('orig' can go up to 18MB each(!))
                String thumbnail = program.optString(TAG_THUMBNAIL).replaceFirst("^(https:)?//images.vrt.be/orig/", "");
                String imageServer = getString(R.string.model_image_server);
                String streamType = StreamService.STREAMTYPE_ONDEMAND;

                Video video = new Video(title, description, seasonName, episodeNumber, duration, thumbnail, videoId, pubId, formattedBroadcastDate, imageServer, streamType);
                videoList.addVideo(video);

                Log.d(TAG, "Adding video : " + title + " " + episodeNumber);
            }

            resultData.putString("SEASON_LIST", (new Gson()).toJson(seasons));
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
