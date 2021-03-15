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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import com.google.gson.Gson;

import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.ResumePointsService;

/**
 * PlayerGlue for video playback
 * @param <T>
 */
public class VideoMediaPlayerGlue<T extends PlayerAdapter> extends PlaybackTransportControlGlue<T> {

    private static final String TAG = "VideoMediaPlayerGlue";

    private static final int TIMESEEK = 30; // in seconds
    private static final int MAX_MULTIPLIER = 5;
    private int prevKeyCode = -1;
    private int buttonCount = 0;
    private int currentMultiplier = 1;

    private PlaybackControlsRow.RewindAction mRewindAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;

    public VideoMediaPlayerGlue(Activity context, T impl) {
        super(context, impl);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {

        // Every 5 clicks (or long presses) in the same 'direction' (rewind | forward) we increase
        // the multiplier, this makes progress skip forward|backwards between 30 seconds and 2.5 minutes
        if(event.getAction() == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {

            if(currentMultiplier < MAX_MULTIPLIER && prevKeyCode == keyCode) {
                buttonCount++;
                if((buttonCount % 5) == 0) {
                    currentMultiplier++;
                }
            }
        }

        if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            fastForward();
            getControlsRow().setCurrentPosition(getPlayerAdapter().isPrepared() ? getPlayerAdapter().getCurrentPosition() : -1);
        }

        if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            rewind();
            getControlsRow().setCurrentPosition(getPlayerAdapter().isPrepared() ? getPlayerAdapter().getCurrentPosition() : -1);
        }

        if(prevKeyCode != keyCode) {
            currentMultiplier = 1;
            buttonCount = 0;
        }

        prevKeyCode = keyCode;

        return super.onKey(v, keyCode, event);
    }

    public void rewind() {
        long newPosition = getCurrentPosition() - (TIMESEEK * 1000 * currentMultiplier);
        newPosition = (newPosition < 0) ? 0 : newPosition;
        getPlayerAdapter().seekTo(newPosition);
    }

    public void fastForward() {
        if (getDuration() > -1) {
            long newPosition = getCurrentPosition() + (TIMESEEK * 1000 * currentMultiplier);
            newPosition = (newPosition > getDuration()) ? getDuration() : newPosition;
            getPlayerAdapter().seekTo(newPosition);
        }
    }

    private void notifyActionChanged(PlaybackControlsRow.MultiAction action) {
        int index = -1;
        if (getPrimaryActionsAdapter() != null) {
            index = getPrimaryActionsAdapter().indexOf(action);
        }
        if (index >= 0) {
            getPrimaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
        } else {
            if (getSecondaryActionsAdapter() != null) {
                index = getSecondaryActionsAdapter().indexOf(action);
                if (index >= 0) {
                    getSecondaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
                }
            }
        }
    }

    private ArrayObjectAdapter getPrimaryActionsAdapter() {
        if (getControlsRow() == null) {
            return null;
        }
        return (ArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    private ArrayObjectAdapter getSecondaryActionsAdapter() {
        if (getControlsRow() == null) {
            return null;
        }
        return (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
    }

    private void updateVideoProgress() {

        VideoPlaybackActivity vpa = (VideoPlaybackActivity) getContext();
        Video video = vpa.getVideo();
        int position = (int)(getPlayerAdapter().getCurrentPosition() / 1000);
        if(position == 0) { return; }

        Log.d(TAG, "Setting position = " + position + " total = " + video.getDuration());

        Intent accessTokenIntent = new Intent(getContext(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    Intent resumePointsIntent = new Intent(getContext(), ResumePointsService.class);
                    resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_UPDATE_RESUME_POINT);
                    resumePointsIntent.putExtra("X-VRT-Token", resultData.getString("X-VRT-Token"));
                    resumePointsIntent.putExtra("PLAYER_CURRENT_POSITION", position);
                    resumePointsIntent.putExtra("VIDEO_OBJECT", new Gson().toJson(video));

                    resumePointsIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            // show messages, if any
                            if (resultData.getString("MSG", "").length() > 0) {
                                Toast.makeText(getContext(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    getContext().startService(resumePointsIntent);

                }
            }
        });

        getContext().startService(accessTokenIntent);

    }

    @Override
    protected void onPlayStateChanged() {
        Log.d(TAG, "In onPlayStateChanged");

        // Stopped or paused, update progress
        if(!getPlayerAdapter().isPlaying()) {
                updateVideoProgress();
        }

        super.onPlayStateChanged();
    }

    @Override
    protected void onDetachedFromHost() {
        Log.d(TAG, "On detached from host");

        // Return from ExoPLayer, update Video progress
        updateVideoProgress();

        super.onDetachedFromHost();
    }

}