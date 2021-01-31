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
 *
 * As it is hard to pass object references between fragments and activities
 * this is a Singleton class. You can never have more than 1 Program in the UI visible
 * at the same in any case so this shouldn't be too much of a problem.
 *
 */
public class VideoList {

    // Singleton instance
    private static VideoList instance = null;

    final static String TAG = "VideoList";

    @SerializedName("videosLoaded") private int mVideosLoaded;
    @SerializedName("videosAvailable") private int mVideosAvailable;
    @SerializedName("videos") private List<Video> mVideos = new ArrayList<Video>();

    private VideoList() {
        mVideosLoaded = 0;
        mVideosAvailable = 0;
    }

    public static VideoList getInstance() {
        if(instance == null) {
            instance = new VideoList();
        }
        return instance;
    }

    public void clear() {
        mVideos.clear();
        mVideosAvailable = 0;
        mVideosLoaded = 0;
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

    public boolean setProgress(Video video, int position) {
        Log.d(TAG, "Setting progress, position = " + position + " total = " + video.getDuration());
        int progress = (int) (((double) position / video.getDuration()) * 100);

        for (Video mVideo : mVideos) {
            if (mVideo.getVideoId().equals(video.getVideoId()) &&
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

    // Duration is set in ProgramService in minutes as that's how it is returned by VRT's JSON
    // Once we start playback we can update it with a more accurate number (in seconds)
    // which is useful to generate a more accurate progressbar
    public boolean setDuration(Video video, int duration) {
        Log.d(TAG, "Setting duration for video " + video.getTitle());

        for (Video mVideo : mVideos) {
            if (mVideo.getVideoId().equals(video.getVideoId()) &&
                    mVideo.getPubId().equals(video.getPubId())
            ) {
                mVideo.setDuration(duration);
                return true;
            }
        }

        return false;
    }



}