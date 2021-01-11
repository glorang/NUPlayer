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

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a list of Videos, together they form a Program
 */
public class VideoList {

    final static String TAG = "VideoList";

    @SerializedName("videosLoaded") private int mVideosLoaded;
    @SerializedName("videosAvailable") private int mVideosAvailable;
    @SerializedName("videos") private List<Video> mVideos = new ArrayList<Video>();

    public VideoList() {
        mVideosLoaded = 0;
        mVideosAvailable = 0;
    }

    public List<Video> getVideos() {
        return mVideos;
    }
    public Video getVideo(int index) { return mVideos.get(index); }

    public void addVideo(Video v) {
        mVideos.add(v);
    }

    public int getVideosLoaded() {
        return mVideosLoaded;
    }

    public void setVideosLoaded(int videosLoaded) {
        this.mVideosLoaded = videosLoaded;
    }

    public int getVideosAvailable() {
        return mVideosAvailable;
    }

    public void setVideosAvailable(int videosAvailable) {
        this.mVideosAvailable = videosAvailable;
    }

    public boolean moreVideosAvailable() { return mVideosLoaded < mVideosAvailable; }

}