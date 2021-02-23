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

package be.lorang.nuplayer.model;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.lorang.nuplayer.services.StreamService;

/**
 * This class represents a list of Live TV channels represented as Video objects
 * This is a Singleton class so we only create it once as this is a fixed list
 */
public class ChannelList {

    public final Map<String, String> channelMapping = ImmutableMap.of(
            "O8", "één",
            "1H", "Canvas",
            "O9", "Ketnet"
    );

    // Singleton instance
    private static ChannelList instance = null;

    private List<Video> mChannels = new ArrayList<Video>();

    // Define each Live TV channel as a statically defined video
    // Title, description, formattedBroadcastShortDate will be overwritten from EPGService
    // There is a slight attribute abuse in formattedBroadcastShortDate and pubId
    private ChannelList() {

        Video een = new Video(
                "één",
                "",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/een.jpg",
                "vualto_een_geo",
                "O8",
                "",
                "",
                "een",
                "",
                "",
                "",
                "",
                "",
                "",
                StreamService.STREAMTYPE_LIVETV
        );

        Video canvas = new Video(
                "Canvas",
                "",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/canvas.jpg",
                "vualto_canvas_geo",
                "1H",
                "",
                "",
                "canvas",
                "",
                "",
                "",
                "",
                "",
                "",
                StreamService.STREAMTYPE_LIVETV
        );

        Video ketnet = new Video(
                "Ketnet",
                "",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/ketnet.jpg",
                "vualto_ketnet_geo",
                "O9",
                "",
                "",
                "ketnet",
                "",
                "",
                "",
                "",
                "",
                "",
                StreamService.STREAMTYPE_LIVETV
        );

        mChannels.add(een);
        mChannels.add(canvas);
        mChannels.add(ketnet);

    }

    public static ChannelList getInstance() {
        if(instance == null) {
            instance = new ChannelList();
        }
        return instance;
    }

    public List<Video> getChannels() { return mChannels; }

    public void setEPGInfo(String channel, String title, String description, String timeslot, int progress) {
        for(Video video : mChannels) {
            if(video.getPubId().equals(channel)) {
                video.setTitle(title);
                video.setDescription(description);
                video.setFormattedBroadcastShortDate(timeslot);
                video.setProgressPct(progress);
            }
        }
    }

}