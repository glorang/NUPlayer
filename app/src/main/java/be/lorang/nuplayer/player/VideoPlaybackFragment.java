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

import android.net.Uri;
import android.os.Bundle;

import androidx.leanback.app.VideoFragment;
import androidx.leanback.app.VideoFragmentGlueHost;
import androidx.leanback.media.PlaybackGlue;

import be.lorang.nuplayer.model.Video;

public class VideoPlaybackFragment extends VideoFragment {

    public static final String TAG = "VideoPlaybackFragment";
    private VideoMediaPlayerGlue<ExoPlayerAdapter> mMediaPlayerGlue;
    private ExoPlayerAdapter playerAdapter;
    final VideoFragmentGlueHost mHost = new VideoFragmentGlueHost(this);

    private Video video;
    private String videoUrl;
    private String drmToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get Video object and videoUrl from VideoPlaybackActivity (still not sure this is how it's done)
        VideoPlaybackActivity vpa = (VideoPlaybackActivity) getActivity();

        video = vpa.getVideo();
        videoUrl = vpa.getVideoUrl();
        drmToken = vpa.getDrmToken();

        playerAdapter = new ExoPlayerAdapter(getActivity(), video.getTitle(), video.getDescription());
        mMediaPlayerGlue = new VideoMediaPlayerGlue(getActivity(), playerAdapter);
        mMediaPlayerGlue.setHost(mHost);
        mMediaPlayerGlue.setTitle(video.getTitle());
        mMediaPlayerGlue.setSubtitle(video.getDescription());
        mMediaPlayerGlue.getPlayerAdapter().setDrmToken(drmToken);
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(videoUrl));

        playWhenReady(mMediaPlayerGlue);

    }

    static void playWhenReady(PlaybackGlue glue) {
        if (glue.isPrepared()) {
            glue.play();
        } else {
            glue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    if (glue.isPrepared()) {
                        glue.removePlayerCallback(this);
                        glue.play();
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.pause();
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.getPlayerAdapter().setMediaSessionState(true);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.getPlayerAdapter().setMediaSessionState(false);
        }
        super.onStop();
    }

}