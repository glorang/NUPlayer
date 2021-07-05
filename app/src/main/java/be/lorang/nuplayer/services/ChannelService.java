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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.utils.Utils;

/*
 * Service to add/remove the NUPlayer channel to/from the Android TV Home Screen
 *
 * The name might be a bit unfortunate as this has nothing to do with a (VRT NU) TV channel
 *
 */

public class ChannelService extends IntentService {

    private static final String TAG = "ChannelService";
    private static final String CHANNEL_NAME = "Live channels";

    public final static String ACTION_ADD_REMOVE = "addRemove";
    public final static String BUNDLED_LISTENER = "listener";

    private Bundle resultData = new Bundle();

    public ChannelService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(VrtPlayerTokenService.BUNDLED_LISTENER);

        try {

            boolean channelState = workIntent.getExtras().getBoolean("CHANNEL_STATE", false);
            String action = workIntent.getExtras().getString("ACTION");

            switch (action) {
                case ACTION_ADD_REMOVE:
                    if(channelState) {
                        addChannel();
                    } else {
                        removeChannel();
                    }
                    break;
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not add/remove channel from home screen: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }
    }


    private long getChannelId() {
        Cursor cursor = getContentResolver().query(TvContractCompat.Channels.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            Channel channel = Channel.fromCursor(cursor);
            if(channel.getDisplayName().equals(CHANNEL_NAME)) {
                return channel.getId();
            }
        }
        return -1;
    }

    private void addChannel() {
        Long channelId = getChannelId();

        // channel already added
        if(channelId > 0) { return; }

        // Create NUPlayer channel
        Channel.Builder builder = new Channel.Builder();
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(CHANNEL_NAME)
                .setAppLinkIntentUri(Uri.parse("nuplayer://be.lorang.nuplayer/startapp"));

        Uri channelUri = getContentResolver().insert(
                TvContractCompat.Channels.CONTENT_URI, builder.build().toContentValues());

        channelId = ContentUris.parseId(channelUri);

        // Set Icon
        Bitmap icon = Utils.drawableToBitmap(getResources().getDrawable(R.drawable.ic_logo_channel_nuplayer, null));
        ChannelLogoUtils.storeChannelLogo(this, channelId, icon);

        // Set as default channel
        TvContractCompat.requestChannelBrowsable(this, channelId);

        // Add live streams
        for(Video channel : ChannelList.getInstance().getChannels()) {

            int logoID = getResources().getIdentifier(
                    "logo_" + channel.getBrand().replaceAll("-","") + "_16_9",
                    "drawable", getPackageName());

            Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                    + getResources().getResourcePackageName(logoID) + '/'
                    + getResources().getResourceTypeName(logoID) + '/'
                    + String.valueOf(logoID));

            PreviewProgram program = new PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setTitle(ChannelList.getInstance().channelMapping.get(channel.getPubId()))
                    .setPosterArtAspectRatio(TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9)
                    .setPosterArtUri(imageUri)
                    .setIntentUri(Uri.parse("nuplayer://be.lorang.nuplayer/playvideo/" + channel.getPubId()))
                    .setType(TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE)
                    .build();

            Uri programUri = getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI,
                    program.toContentValues());

        }

    }

    private void removeChannel() {
        Long channelId = getChannelId();
        if(channelId > 0) {
            getContentResolver().delete(TvContractCompat.buildChannelUri(channelId), null, null);
        }
    }

}