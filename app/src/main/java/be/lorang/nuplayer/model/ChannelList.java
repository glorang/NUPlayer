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

        Video een = new Video();
        een.setTitle("één");
        een.setEpisodeNumber(1);
        een.setDuration(0);
        een.setThumbnail("https://www.vrt.be/vrtnu-static/screenshots/een_geo.jpg");
        een.setVideoId("vualto_een_geo");
        een.setPubId("O8");
        een.setBrand("een");
        een.setStreamType(StreamService.STREAMTYPE_LIVETV);

        Video canvas = new Video();
        canvas.setTitle("Canvas");
        canvas.setEpisodeNumber(1);
        canvas.setDuration(0);
        canvas.setThumbnail("https://www.vrt.be/vrtnu-static/screenshots/canvas_geo.jpg");
        canvas.setVideoId("vualto_canvas_geo");
        canvas.setPubId("1H");
        canvas.setBrand("canvas");
        canvas.setStreamType(StreamService.STREAMTYPE_LIVETV);

        Video ketnet = new Video();
        ketnet.setTitle("Ketnet");
        ketnet.setEpisodeNumber(1);
        ketnet.setDuration(0);
        ketnet.setThumbnail("https://www.vrt.be/vrtnu-static/screenshots/ketnet_geo.jpg");
        ketnet.setVideoId("vualto_ketnet_geo");
        ketnet.setPubId("O9");
        ketnet.setBrand("ketnet");
        ketnet.setStreamType(StreamService.STREAMTYPE_LIVETV);

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

    public void setEPGInfo(String channel, String title, String subTitle, String timeSlot, int progress) {
        for(Video video : mChannels) {
            if(video.getPubId().equals(channel)) {
                video.setTitle(title);
                video.setSubTitle(subTitle);
                video.setOnTime(timeSlot);
                video.setProgressPct(progress);
            }
        }
    }

    public Video getLiveChannel(String channelID) {
        for(Video video : mChannels) {
            if(video.getPubId().equals(channelID)) {
                return video;
            }
        }
        return null;
    }

}