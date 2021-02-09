package be.lorang.nuplayer.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.bumptech.glide.load.HttpException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.utils.HTTPClient;

/*
 * Very basic Service to fetch EPG data and feed it into ChannelList object
 *
 */

public class EPGService extends IntentService {
    private static final String TAG = "EPGService";
    public final static String BUNDLED_LISTENER = "listener";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    public EPGService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            // Current time
            ZonedDateTime currentDateTime = ZonedDateTime.now();

            // Get ChannelList
            ChannelList channelList = ChannelList.getInstance();

            // Get EPG data (once a day)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneId.of("Europe/Brussels"));

            String url = String.format(getString(R.string.service_epg_url),
                    formatter.format(currentDateTime));

            JSONObject returnObject = httpClient.getCachedRequest(getCacheDir(), url, 1440);
            if(httpClient.getResponseCode() != 200) {
                throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
            }

            Iterator<String> keys = returnObject.keys();
            while(keys.hasNext()) {
                String channel = keys.next();
                if (returnObject.get(channel) instanceof JSONArray) {
                    JSONArray channelItems = returnObject.getJSONArray(channel);

                    for(int i=0; i<channelItems.length(); i++) {
                        JSONObject epgEntry = channelItems.getJSONObject(i);

                        String startTimeString = epgEntry.getString("startTime");
                        String endTimeString = epgEntry.getString("endTime");

                        ZonedDateTime startTime = ZonedDateTime.parse(startTimeString);
                        ZonedDateTime endTime = ZonedDateTime.parse(endTimeString);

                        if(currentDateTime.isAfter(startTime) && currentDateTime.isBefore(endTime)) {

                            DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
                                    .withZone(ZoneId.of("Europe/Brussels"));

                            String title = epgEntry.getString("title");
                            String description = epgEntry.getString("subtitle");
                            String timeslot = hourFormatter.format(startTime) + " - " + hourFormatter.format(endTime);

                            long duration = endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli();
                            int progress = (int)(((double)(currentDateTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()) / duration) * 100);
                            Log.d(TAG, "Setting EPG for channel " + channel + " to " + title + " " + timeslot);
                            channelList.setEPGInfo(channel, title, description, timeslot, progress);
                        }
                    }
                }
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not download EPG data: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }

    }
}