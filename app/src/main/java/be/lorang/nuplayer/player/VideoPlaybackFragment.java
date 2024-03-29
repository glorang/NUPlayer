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

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.PlaybackGlue;

import com.google.android.exoplayer2.ExoPlayer;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.services.StreamService;
import be.lorang.nuplayer.ui.MainActivity;
import be.lorang.nuplayer.ui.SettingsFragment;

import static android.content.Context.MODE_PRIVATE;

public class VideoPlaybackFragment extends VideoSupportFragment {

    public static final String TAG = "VideoPlaybackFragment";
    private VideoMediaPlayerGlue<ExoPlayerAdapter> mMediaPlayerGlue;
    private ExoPlayerAdapter playerAdapter;
    final VideoSupportFragmentGlueHost mHost = new VideoSupportFragmentGlueHost(this);

    private Video video;
    private String videoUrl;
    private String drmToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // return if Activity got destroyed in the mean time
        if(getActivity() == null) { return; }

        // get Video object and videoUrl from VideoPlaybackActivity (still not sure this is how it's done)
        if (getActivity() instanceof VideoPlaybackActivity) {
            VideoPlaybackActivity vpa = (VideoPlaybackActivity) getActivity();
            video = vpa.getVideo();
            videoUrl = vpa.getVideoUrl();
            drmToken = vpa.getDrmToken();
        }

        // return if video null
        if(video == null) { return; }

        // Check if developer mode is enabled
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        boolean developerModeEnabled = prefs.getBoolean(SettingsFragment.SETTING_DEVELOPER_MODE, false);

        playerAdapter = new ExoPlayerAdapter(getActivity(), video.getProgramTitle(), video.getTitle());
        mMediaPlayerGlue = new VideoMediaPlayerGlue(getActivity(), playerAdapter, developerModeEnabled);
        mMediaPlayerGlue.setHost(mHost);
        mMediaPlayerGlue.setTitle(video.getTitle());
        mMediaPlayerGlue.setSubtitle(Html.fromHtml(video.getSubTitle(), Html.FROM_HTML_MODE_COMPACT));
        mMediaPlayerGlue.getPlayerAdapter().setDrmToken(drmToken);
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(videoUrl));
        mMediaPlayerGlue.setControlsOverlayAutoHideEnabled(true);

        // Exit player when playback is completed or stopped
        mMediaPlayerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
            @Override
            public void onPlayCompleted(PlaybackGlue glue) {
                super.onPlayCompleted(glue);
                getActivity().finishAfterTransition();
            }
        });

        // Resume video, restart from beginning if progress > 95%
        if(video.getProgressPct() > 0 && video.getProgressPct() < 95
                && !video.getStreamType().equals(StreamService.STREAMTYPE_LIVETV))  {
            Log.d(TAG, "Setting start position = " + video.getCurrentPosition());
            mMediaPlayerGlue.getPlayerAdapter().seekTo(video.getCurrentPosition() * 1000);
        }

        playWhenReady(mMediaPlayerGlue);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playerAdapter.setSubtitleView(view.findViewById(R.id.leanback_subtitles));
        playerAdapter.setDebugTextView(view.findViewById(R.id.exoPlayerDebug));
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