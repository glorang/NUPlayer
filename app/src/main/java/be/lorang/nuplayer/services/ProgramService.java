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
import be.lorang.nuplayer.model.ResumePoint;
import be.lorang.nuplayer.model.ResumePointList;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoList;

import com.bumptech.glide.load.HttpException;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * This class will load the 10 first Videos for a Program and parse the season info.
 *
 * Results are stored in VideoList class, using the START_INDEX parameter we load more
 * episodes dynamically
 *
 */

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
    public static final String TAG_SEASONTITLE = "seasonTitle";
    public static final String TAG_EPISODENR = "episodeNumber";
    public static final String TAG_DURATION = "duration";
    public static final String TAG_THUMBNAIL = "videoThumbnailUrl";
    public static final String TAG_VIDEOID = "videoId";
    public static final String TAG_PUBID = "publicationId";
    public static final String TAG_BROADCASTDATE = "formattedBroadcastDate";
    public static final String TAG_BROADCASTSHORTDATEDATE = "formattedBroadcastShortDate";
    public static final String TAG_BRANDS = "brands";
    public static final String TAG_PROGRAM = "program";
    public static final String TAG_ASSETPATH = "assetPath";
    public static final String TAG_URL = "url";
    public static final String TAG_WHATSONID = "whatsonId";
    public static final String TAG_PROGRAMWHATSONID = "programWhatsonId";
    public static final String TAG_ALLOWEDREGION = "allowedRegion";
    public static final String TAG_ASSETOFFTIME = "assetOffTime";
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

            JSONObject parseObject = null;
            JSONArray itemsOrder = null;
            JSONObject items = null;

            // try path1
            // [":items"].parsys[":items"].container[":items"].banner[":items"].navigation[":items"] && .navigation[":itemsOrder"]
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

                 itemsOrder = parseObject.getJSONArray(":itemsOrder");
                 items = parseObject.getJSONObject(":items");

            } catch(JSONException e) {
                e.printStackTrace();
            }

            // try path2
            // [":items"].parsys[":items"].container[":items"].banner[":items"] && banner[":itemsOrder"]
            if(itemsOrder == null || items == null) {
                try {
                    parseObject = inputData
                            .getJSONObject(":items")
                            .getJSONObject("parsys")
                            .getJSONObject(":items")
                            .getJSONObject("container")
                            .getJSONObject(":items")
                            .getJSONObject("banner");

                    itemsOrder = parseObject.getJSONArray(":itemsOrder");
                    items = parseObject.getJSONObject(":items");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // try path3 - la-theorie-du-y.model.json
            // [":items"].parsys[":items"].container[":items"]["episodes-list"][":items"].navigation[":items"] && .navigation[":itemsOrder"]
            if(itemsOrder == null || items == null) {
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

                    itemsOrder = parseObject.getJSONArray(":itemsOrder");
                    items = parseObject.getJSONObject(":items");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // try path4 - de-shaq.model.json.json (series?)
            // path4   : [":items"].parsys[":items"].container[":items"]["episodes-list"][":items"] && ["episodes-list"][":itemsOrder"]
            if(itemsOrder == null || items == null) {
                try {
                    parseObject = inputData
                            .getJSONObject(":items")
                            .getJSONObject("parsys")
                            .getJSONObject(":items")
                            .getJSONObject("container")
                            .getJSONObject(":items")
                            .getJSONObject("episodes-list");

                    itemsOrder = parseObject.getJSONArray(":itemsOrder");
                    items = parseObject.getJSONObject(":items");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // try path5 - salah.model.json, let's hope this is a rare exception
            // path5   : [":items"].parsys[":items"].container[":items"].episodes_list_268730743[":items"].navigation[":items"] && [":itemsOrder"]
            if(itemsOrder == null || items == null) {
                try {
                    parseObject = inputData
                            .getJSONObject(":items")
                            .getJSONObject("parsys")
                            .getJSONObject(":items")
                            .getJSONObject("container")
                            .getJSONObject(":items");

                    JSONObject parseObject2 = null;

                    Iterator<String> keys = parseObject.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        if(key.startsWith("episodes_list_")) {
                            parseObject2 = parseObject.getJSONObject(key)
                                    .getJSONObject(":items")
                                    .getJSONObject("navigation");
                        }
                    }

                    itemsOrder = parseObject2.getJSONArray(":itemsOrder");
                    items = parseObject2.getJSONObject(":items");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // try path6
            // details.data.program.seasons[0].title.raw (= key) + .value
            if(itemsOrder == null || items == null) {
                try {

                    items = new JSONObject();
                    itemsOrder = new JSONArray();

                    parseObject = inputData
                            .getJSONObject("details")
                            .getJSONObject("data")
                            .getJSONObject("program");

                    JSONArray seasons = parseObject.getJSONArray("seasons");

                    for (int i = 0; i < seasons.length(); i++) {
                        if (seasons.get(i) instanceof JSONObject) {
                            JSONObject entry = new JSONObject();
                            JSONObject title = seasons.getJSONObject(i).getJSONObject("title");
                            entry.put("title", title.getString("value"));
                            items.put(title.getString("raw"), entry);
                            itemsOrder.put(title.getString("raw"));
                        }
                    }

                    if (items.length() == 0 || itemsOrder.length() == 0) {
                        items = null;
                        itemsOrder = null;
                    }

                } catch (JSONException e) {
                    items = null;
                    itemsOrder = null;
                    e.printStackTrace();
                }
            }

            if(itemsOrder != null && items != null) {

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

            // try trailer path to see if we should add a "Trailer season"
            // [":items"].parsys[":items"].container[":items"].navigation[":items"].container.title
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

            // Get list of available seasons
            if(seasons == null) {
                url = String.format(getString(R.string.service_program_seasons_url), program.getProgramName());
                Log.d(TAG, "Getting season info at " + url);
                returnObject = httpClient.getCachedRequest(getCacheDir(), url, 1440);
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
                // Don't query our own dummy season
                if(!seasonName.equals("0")) {
                    url = url + "&facets[seasonName]=" + seasonName;
                }
            } else {
                // If seasons is still null here we either failed to decode the JSON or failed to
                // fetch the model.json, create dummy season
                seasons = new LinkedHashMap<>();
                seasons.put("0", "Unknown");
            }

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

                // Copy progress from ResumePointsList
                for(ResumePoint resumePoint : ResumePointList.getInstance().getResumePoints()) {
                    if(resumePoint.getUrl().equals(video.getURL()) && resumePoint.getProgress() > 0) {
                        Log.d(TAG, "Copying videoProgress for " + video.getTitle());
                        video.setProgressPct((int)resumePoint.getProgress());
                        video.setCurrentPosition((int)resumePoint.getPosition());
                    }
                }
                videoList.addVideo(video);
                Log.d(TAG, "Adding video : " + video.getTitle());
            }

            resultData.putString("SEASON_LIST", (new Gson()).toJson(seasons));
            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get program info: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }

    }

    public static Video parseVideoFromJSON(JSONObject inputObject, String imageServer) throws JSONException{

        // fields get with getString are mandatory and will throw a JSONException if not set
        // fields get with optString are optional and have a default value of ""
        String title = inputObject.getString(TAG_TITLE);
        String description = inputObject.optString(TAG_DESCRIPTION);
        String seasonName = inputObject.optString(TAG_SEASONNAME);
        String seasonTitle = inputObject.optString(TAG_SEASONTITLE);
        int episodeNumber = inputObject.optInt(TAG_EPISODENR);
        int duration = inputObject.optInt(TAG_DURATION, 0) * 60;
        String videoId  = inputObject.getString(TAG_VIDEOID);
        String pubId  = inputObject.getString(TAG_PUBID);
        String formattedBroadcastDate = inputObject.optString(TAG_BROADCASTDATE);
        String formattedBroadcastShortDate = inputObject.optString(TAG_BROADCASTSHORTDATEDATE);
        JSONArray brands = inputObject.optJSONArray(TAG_BRANDS);
        String program = inputObject.optString(TAG_PROGRAM);
        String assetPath = inputObject.optString(TAG_ASSETPATH);
        String url = inputObject.optString(TAG_URL);
        String whatsonId = inputObject.optString(TAG_WHATSONID);
        String programWhatsonId = inputObject.optString(TAG_PROGRAMWHATSONID);
        String allowedRegion = inputObject.optString(TAG_ALLOWEDREGION);
        String assetOffTime = inputObject.optString(TAG_ASSETOFFTIME);

        // we replace the image server with the one defined in urls.xml
        // this prepends the (often) 'missing' 'https://' and allows us to query our own size ('orig' can go up to 18MB each(!))
        String thumbnail = inputObject.optString(TAG_THUMBNAIL).replaceFirst("^(https:)?//images.vrt.be/orig/", "");
        String streamType = StreamService.STREAMTYPE_ONDEMAND;

        // we only use the 1st brand for now in the array
        String brand = (String)brands.get(0);

        Video video = new Video(
                title,
                description,
                seasonName,
                seasonTitle,
                episodeNumber,
                duration,
                thumbnail,
                videoId,
                pubId,
                formattedBroadcastDate,
                formattedBroadcastShortDate,
                brand,
                program,
                assetPath,
                url,
                whatsonId,
                programWhatsonId,
                allowedRegion,
                assetOffTime,
                imageServer,
                streamType
        );

        return video;
    }
}
