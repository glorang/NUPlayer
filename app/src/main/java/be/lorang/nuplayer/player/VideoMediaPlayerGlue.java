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
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
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

    private PlaybackControlsRow.RewindAction mRewindAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;


    public VideoMediaPlayerGlue(Activity context, T impl) {
        super(context, impl);
        mRewindAction = new PlaybackControlsRow.RewindAction(context);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
        adapter.add(mRewindAction);
        adapter.add(mFastForwardAction);
    }

    @Override
    public void onActionClicked(Action action) {

        // FIXME: when you hit rewind at EOF the progress bar does not update until you press pause/play

        if (action.getId() == mFastForwardAction.getId()) {
            getPlayerAdapter().seekTo(getPlayerAdapter().getCurrentPosition() + (30 * 1000));
        } else if (action.getId() == mRewindAction.getId()) {
            getPlayerAdapter().seekTo(getPlayerAdapter().getCurrentPosition() - (30 * 1000));
        }

        /*
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }
         */

        super.onActionClicked(action);

    }

    private boolean shouldDispatchAction(Action action) {
        return action == mFastForwardAction || action == mRewindAction;
    }

    private void dispatchAction(Action action) {
        PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
        multiAction.nextIndex();
        notifyActionChanged(multiAction);
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

        Intent accessTokenIntent = new Intent(getContext(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    Intent resumePointsIntent = new Intent(getContext(), ResumePointsService.class);
                    resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_UPDATE_RESUME_POINT);
                    resumePointsIntent.putExtra("PLAYER_CURRENT_POSITION", (int)(getPlayerAdapter().getCurrentPosition() / 1000));
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

    @Override
    protected void onPlayCompleted() {
        super.onPlayCompleted();
        // FIXME: should we quit the video player automatically?
    }
}