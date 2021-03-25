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
 * This class represents a list of ResumePoints
 *
 * Whenever a Program is opened all Videos are matched against this list to set progress
 *
 */
public class ResumePointList {

    // Singleton instance
    private static ResumePointList instance = null;

    final static String TAG = "ResumePointList";
    private List<ResumePoint> resumePoints = new ArrayList<>();

    private ResumePointList() {}

    public static ResumePointList getInstance() {
        if (instance == null) {
            instance = new ResumePointList();
        }
        return instance;
    }

    public List<ResumePoint> getResumePoints() {
        return resumePoints;
    }

    public void clear() {
        resumePoints.clear();
    }

    public void add(ResumePoint resumePoint) {
        resumePoints.add(resumePoint);
    }

    public void remove(ResumePoint resumePoint) {
        resumePoints.remove(resumePoint);
    }

    public boolean setProgress(Video video, int position) {
        int progress = (int) (((double) position / video.getDuration()) * 100);

        for (ResumePoint resumePoint : resumePoints) {
            if(resumePoint.getUrl().equals(video.getURL())) {
                Log.d(TAG, "Setting progress for video " + video.getVideoId() + " to: " + progress);
                resumePoint.setPosition(position);
                resumePoint.setProgress(progress);
                return true;
            }
        }
        return false;
    }

}