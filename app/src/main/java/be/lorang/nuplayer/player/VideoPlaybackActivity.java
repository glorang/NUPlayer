/*
 * Copyright 2021 Geert Lorang
 * Copyright 2016 The Android Open Source Project
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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;

import com.google.gson.Gson;

public class VideoPlaybackActivity extends Activity {

    public static final String TAG = "VideoPlaybackActivity";

    private Video video;
    private String videoUrl;
    private String drmToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayback);

        // get passed Video object and URL
        videoUrl = getIntent().getExtras().getString("MPEG_DASH_URL");
        String videoJson = getIntent().getExtras().getString("VIDEO_OBJECT");
        if(videoJson != null) {
            video = new Gson().fromJson(videoJson, Video.class);
        }

        drmToken = getIntent().getExtras().getString("VUALTO_TOKEN");

        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.videoFragment, new VideoPlaybackFragment(), VideoPlaybackFragment.TAG);
            ft.commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // This part is necessary to ensure that getIntent returns the latest intent when
        // VideoExampleActivity is started. By default, getIntent() returns the initial intent
        // that was set from another activity that started VideoExampleActivity. However, we need
        // to update this intent when for example, user clicks on another video when the currently
        // playing video is in PIP mode, and a new video needs to be started.
        setIntent(intent);
    }

    public Video getVideo() { return video; }
    public String getVideoUrl() { return videoUrl; }
    public String getDrmToken() { return drmToken; }

}