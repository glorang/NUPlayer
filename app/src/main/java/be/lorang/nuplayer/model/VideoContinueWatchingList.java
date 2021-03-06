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
import java.util.List;

/**
 * This class represents the list of Videos that are marked as "Continue Watching"
 *
 * This is a Singleton class to reduce VRT.NU REST API calls
 *
 */
public class VideoContinueWatchingList {

    final static String TAG = "VideoContinueWatchingList";
    // Singleton instance
    private static VideoContinueWatchingList instance = null;
    private boolean videoListInitialized = false;

    @SerializedName("videos") private List<Video> mVideos = new ArrayList<Video>();

    private VideoContinueWatchingList() { }
    public static VideoContinueWatchingList getInstance() {
        if(instance == null) {
            instance = new VideoContinueWatchingList();
        }
        return instance;
    }

    public List<Video> getVideos() {
        return mVideos;
    }
    public Video getVideo(int index) { return mVideos.get(index); }

    public void addVideo(Video v) {
        mVideos.add(v);
        videoListInitialized = true;
    }

    public void removeVideo(Video video) {
        Video videoToRemove = null;
        for(Video mVideo : mVideos) {
            if(mVideo.getVideoId().equals(video.getVideoId()) &&
                    mVideo.getPubId().equals(video.getPubId())
            ) {
                videoToRemove = mVideo;
            }
        }

        if(videoToRemove != null) {
            mVideos.remove(videoToRemove);
        }
    }

    public void clear() {
        mVideos.clear();
        videoListInitialized = false;
    }

    public boolean isVideoListInitialized() {
        return videoListInitialized;
    }

    public boolean setProgress(Video video, int position) {
        Log.d(TAG, "Setting progress, position = " + position + " total = " + video.getDuration());
        int progress = (int)(((double)position / video.getDuration()) * 100);

        for(Video mVideo : mVideos) {
            if(mVideo.getVideoId().equals(video.getVideoId()) &&
                    mVideo.getPubId().equals(video.getPubId())
            ) {
                Log.d(TAG, "Setting progress for video " + video.getVideoId() + " to: " + progress);
                mVideo.setCurrentPosition(position);
                mVideo.setProgressPct(progress);
                return true;
            }
        }

        return false;
    }
}