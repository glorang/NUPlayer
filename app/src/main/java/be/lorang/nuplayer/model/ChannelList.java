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

import java.util.ArrayList;
import java.util.List;

import be.lorang.nuplayer.services.StreamService;

/**
 * This class represents a list of Live TV channels represented as Video objects
 * This is a Singleton class so we only create it once as this is a fixed list
 */
public class ChannelList {

    // Singleton instance
    private static ChannelList instance = null;

    private List<Video> mChannels = new ArrayList<Video>();

    // Define each Live TV channel as a statically defined video
    private ChannelList() {

        Video een = new Video(
                "één",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/een.jpg",
                "vualto_een_geo",
                "",
                "",
                "",
                "een",
                "",
                "",
                "",
                StreamService.STREAMTYPE_LIVETV
        );

        Video canvas = new Video(
                "Canvas",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/canvas.jpg",
                "vualto_canvas_geo",
                "",
                "",
                "",
                "canvas",
                "",
                "",
                "",
                StreamService.STREAMTYPE_LIVETV
        );

        Video ketnet = new Video(
                "Ketnet",
                "",
                "",
                1,
                0,
                "https://vrtnu-api.vrt.be/screenshots/ketnet.jpg",
                "vualto_ketnet_geo",
                "",
                "",
                "",
                "ketnet",
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

    // FIXME: feed EPG into description
    public void setDescription(Video video, String description) {
        for(Video channel : mChannels) {
            if(channel == video) {
                channel.setDescription(description);
            }
        }
    }

}