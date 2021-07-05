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

package be.lorang.nuplayer.player;

/*
 * Simple helper class to handle video playback from NUPlayer channel on the home screen
 *
 * We "simulate" a button press which will start a new VideoPlaybackActivity and end this activity
 *
 */

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import java.util.List;

import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.ui.VideoProgramBaseListener;

public class VideoPlaybackHomeScreenActivity extends FragmentActivity {

    // Helper function to get channel id (returns O8,O9 or 1H from URL nuplayer://be.lorang.nuplayer/playvideo/{O8,O9,1H}
    private String getVideoID(Uri uri) {
        List<String> paths = uri.getPathSegments();
        if (paths.size() == 2 && TextUtils.equals(paths.get(0), "playvideo")) {
            return paths.get(1);
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Video video = null;

        Uri videoUri = getIntent().getData();
        if (videoUri != null) {

            // Get passed channel (O8, O9, 1H)
            String pubId = getVideoID(getIntent().getData());

            // Get Video object from ChannelList
            for (Video channel : ChannelList.getInstance().getChannels()) {
                if (channel.getPubId().equals(pubId)) {
                    video = channel;
                    break;
                }
            }

            // Start live stream
            if(video != null) {

                // Reset title and description, it might contain outdated EPG info
                video.setTitle(ChannelList.getInstance().channelMapping.get(video.getPubId()));
                video.setDescription("");

                VideoProgramBaseListener videoProgramBaseListener = new VideoProgramBaseListener(this);
                videoProgramBaseListener.startVideoIntent(video);
            }

            finish();
        }
    }

}
