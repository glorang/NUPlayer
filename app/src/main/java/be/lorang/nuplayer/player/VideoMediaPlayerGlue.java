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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.SeekBar;

import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.Map;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.ResumePointsService;

/**
 * PlayerGlue for video playback
 * @param <T>
 */
public class VideoMediaPlayerGlue<T extends PlayerAdapter> extends PlaybackTransportControlGlue<T> {

    private static final String TAG = "VideoMediaPlayerGlue";

    private PlaybackControlsRow.ClosedCaptioningAction closedCaptioningAction;
    private DebugAction debugAction;
    private SettingsAction settingsAction;

    private static final int TIMESEEK = 30; // in seconds
    private static final int MAX_MULTIPLIER = 5;
    private static final int MAX_BUTTON_COUNT = 5;
    private int prevKeyCode = -1;
    private int buttonCount = 0;
    private int currentMultiplier = 1;

    private int previousPosition = -1;

    private boolean developerModeEnabled;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public VideoMediaPlayerGlue(Activity context, T impl, boolean developerModeEnabled) {
        super(context, impl);
        this.developerModeEnabled = developerModeEnabled;

        closedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(context);
        debugAction = new DebugAction(context);
        settingsAction = new SettingsAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
        adapter.add(closedCaptioningAction);
        adapter.add(settingsAction);

        if(developerModeEnabled) {
            adapter.add(debugAction);
        }
    }

    @Override
    public void onActionClicked(Action action) {
        if (action == closedCaptioningAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            notifyActionChanged(multiAction);
            toggleClosedCaptions();
        } else if (action == debugAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            notifyActionChanged(multiAction);
            toggleDebug();
        } else if (action == settingsAction) {
            showSettingsDialog();
        } else {
            super.onActionClicked(action);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {

        View focusedView = v.findFocus();

        // Reset counters when other button is pressed
        if(prevKeyCode != keyCode) {
            currentMultiplier = 1;
            buttonCount = 0;
        }

        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if(focusedView instanceof SeekBar) {
                        togglePlayState();
                        prevKeyCode = -1;
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    if(focusedView instanceof SeekBar) {
                        updateMultiplier(keyCode);
                        fastForward();
                        getControlsRow().setCurrentPosition(getPlayerAdapter().isPrepared() ? getPlayerAdapter().getCurrentPosition() : -1);
                        prevKeyCode = keyCode;
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    if(focusedView instanceof SeekBar) {
                        updateMultiplier(keyCode);
                        rewind();
                        getControlsRow().setCurrentPosition(getPlayerAdapter().isPrepared() ? getPlayerAdapter().getCurrentPosition() : -1);
                        prevKeyCode = keyCode;
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    // Once the video is stopped we can no longer query it for its current position
                    // so we intercept KEYCODE_MEDIA_STOP here and update the video progress
                    // before the stop event is fired
                    if(buttonCount == 0) {
                        updateVideoProgress();
                    }
                    buttonCount++;
                    break;
            }
        }

        if(event.getAction() == KeyEvent.ACTION_UP) {

            // Reset all counters
            // cancel all previous timers
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // Reset all counters
                    prevKeyCode = -1;
                    currentMultiplier = 1;
                    buttonCount = 0;

                    // cancel all previous timers
                    handler.removeCallbacksAndMessages(null);
                }
            };

            handler.postDelayed(runnable, 30000);
        }

        prevKeyCode = keyCode;

        return super.onKey(v, keyCode, event);
    }

    // Every MAX_BUTTON_COUNT clicks (or long presses) of the same button (rewind | forward) we increase the multiplier
    // This makes progress skip forward | backwards between 30 seconds and 2.5 minutes intervals
    private void updateMultiplier(int keyCode) {
        if(currentMultiplier < MAX_MULTIPLIER && prevKeyCode == keyCode) {
            buttonCount++;
            if((buttonCount % MAX_BUTTON_COUNT) == 0) {
                currentMultiplier++;
            }
        }
    }

    public void togglePlayState() {
        if(getPlayerAdapter().isPlaying()) {
            getPlayerAdapter().pause();
        } else {
            getPlayerAdapter().play();
        }
    }

    public void toggleClosedCaptions() {
        ExoPlayerAdapter adapter = (ExoPlayerAdapter)getPlayerAdapter();
        if(adapter != null) {
            if(!adapter.getSubtitlesEnabled()) {
                adapter.enableSubtitles();
            } else {
                adapter.disableSubtitles();
            }
        }
    }

    public void toggleDebug() {
        ExoPlayerAdapter adapter = (ExoPlayerAdapter)getPlayerAdapter();
        if(adapter != null) {
            if(!adapter.getDebugEnabled()) {
                adapter.enableDebug();
            } else {
                adapter.disableDebug();
            }
        }
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

    public void showSettingsDialog() {
        ExoPlayerAdapter adapter = (ExoPlayerAdapter)getPlayerAdapter();
        LinkedHashMap<Integer, String> availableHeights = adapter.getAvailableHeights();
        int currentHeight = adapter.getCurrentMaxHeight();

        int[] keys = new int[availableHeights.size()];
        String[] values = new String[availableHeights.size()];

        int i=0;
        int selectedIndex = 0;
        for (Map.Entry<Integer, String> entry : availableHeights.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();

            if(entry.getKey() == currentHeight) {
                selectedIndex = i;
            }

            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.dialogAlertTheme));

        builder.setTitle(getContext().getString(R.string.player_select_quality))
                .setSingleChoiceItems(values, selectedIndex, (DialogInterface.OnClickListener) (dialog, index) -> {
                    adapter.setMaxHeight(keys[index]);
                    dialog.cancel();
                });
        builder.create().show();

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
        if(position == 0 || position == previousPosition) { return; }

        previousPosition = position;

        Log.d(TAG, "Setting position = " + position + " total = " + video.getDuration());

        Intent accessTokenIntent = new Intent(getContext(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    Intent resumePointsIntent = new Intent(getContext(), ResumePointsService.class);
                    resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_UPDATE_RESUME_POINT);
                    resumePointsIntent.putExtra("vrtnu_site_profile_vt", resultData.getString("vrtnu_site_profile_vt"));
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
        updateVideoProgress();

        super.onPlayStateChanged();
    }

}