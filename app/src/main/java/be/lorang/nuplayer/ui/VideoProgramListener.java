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

package be.lorang.nuplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.google.gson.Gson;

import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.player.VideoPlaybackActivity;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.StreamService;
import be.lorang.nuplayer.services.VrtPlayerTokenService;
import be.lorang.nuplayer.utils.Utils;

import static android.content.Context.MODE_PRIVATE;

/**
 *
 * Helper class to implement OnItemViewClickedListener when a Video or Program is selected
 *
 * Both HomeFragment and ProgramFragment show Programs and/or Videos,
 * as their listener implementations are identical the code is moved to this helper class.
 *
 * Ideally we would simply make a BaseFragment class but HomeFragment and ProgramFragment
 * extend different Fragment classes (RowFragment, GridFragment) already
 *
 * Unsure how "that's the way to do it" this is but this worksforme[tm]
 *
 */

public class VideoProgramListener implements OnItemViewClickedListener {
    private Fragment fragment;
    private static String TAG = "VideoProgramListener";

    public VideoProgramListener(Fragment fragment) {
        this.fragment = fragment;
    }

    private void startProgramIntent(Program program) {

        Intent programIntent = new Intent(fragment.getActivity().getBaseContext(), ProgramActivity.class);
        programIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(fragment.getActivity()).toBundle();
        fragment.startActivity(programIntent, bundle);
    }

    private void startVideoIntent(Video video) {

        // Define the stream intent (this is the same for both Live TV as on demand video)
        // to get required stream info, but do not start it yet

        Intent streamIntent = new Intent(fragment.getActivity(), StreamService.class);
        streamIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
        streamIntent.putExtra(StreamService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(fragment.getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Start new Intent to play video
                    String videoURL = resultData.getString("MPEG_DASH_URL", "");
                    String drmToken = resultData.getString("VUALTO_TOKEN", "");
                    if(videoURL.length() > 0) {

                        Intent playbackIntent = new Intent(fragment.getActivity(), VideoPlaybackActivity.class);
                        playbackIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
                        playbackIntent.putExtra("MPEG_DASH_URL", videoURL);
                        playbackIntent.putExtra("VUALTO_TOKEN", drmToken);
                        fragment.startActivity(playbackIntent);

                    }
                }
            }
        });

        // get vrtPlayerToken
        SharedPreferences prefs = fragment.getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        String vrtPlayerToken = "";
        String vrtPlayerTokenExpiry = null;
        String tokenType = VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS;

        // Check which token we need
        String streamType = video.getStreamType();

        if(streamType.equals(StreamService.STREAMTYPE_LIVETV)) {
            // get live tv (anonymous) vrtPlayerToken
            vrtPlayerToken = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS, "");
            vrtPlayerTokenExpiry = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY, "");
        } else if(streamType.equals(StreamService.STREAMTYPE_ONDEMAND)) {
            // get on demand (authenticated) vrtPlayerToken
            vrtPlayerToken = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED, "");
            vrtPlayerTokenExpiry = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY, "");
            tokenType = VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED;
        }

        Log.d(TAG, "Stream type = " + streamType);
        Log.d(TAG, "Token type = " + tokenType);
        Log.d(TAG, "Token expired = " + Utils.isDateInPast(vrtPlayerTokenExpiry));
        Log.d(TAG, "Token date = " + vrtPlayerTokenExpiry);

        // Start extra Intent if we don't have a vrtPlayerToken or if it's expired
        if(vrtPlayerToken.length() == 0 || Utils.isDateInPast(vrtPlayerTokenExpiry)) {

            Intent playerTokenIntent = new Intent(fragment.getActivity(), VrtPlayerTokenService.class);
            playerTokenIntent.putExtra("TOKEN_TYPE", tokenType);

            playerTokenIntent.putExtra(VrtPlayerTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);

                    // show messages, if any
                    if(resultData.getString("MSG", "").length() > 0) {
                        Toast.makeText(fragment.getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                    }

                    if (resultCode == Activity.RESULT_OK) {
                        // now that we have a vrtPlayerToken we can start the Stream Intent
                        fragment.getActivity().startService(streamIntent);
                    }
                }
            });

            // Get X-VRT-Player token for on-demand content
            if(tokenType.equals(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED)) {
                Intent accessTokenIntent = new Intent(fragment.getActivity(), AccessTokenService.class);
                accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        if (resultCode == Activity.RESULT_OK) {
                            String token = resultData.getString("X-VRT-Token", "");
                            Log.d(TAG, "Successfully obtained X-VRT-Token:" + token);
                            playerTokenIntent.putExtra("X-VRT-Token", token);
                            fragment.getActivity().startService(playerTokenIntent);
                        }
                    }
                });

                fragment.getActivity().startService(accessTokenIntent);

            } else {
                // live-tv content, doesn't need an authenticated token,
                // start VrtPlayerTokenService immediately
                fragment.getActivity().startService(playerTokenIntent);
            }

        } else {
            // we already have a valid vrtPLayerToken, start Stream Intent immediately
            fragment.getActivity().startService(streamIntent);
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof Video) {

            Video video = (Video) item;
            startVideoIntent(video);

        }else if(item instanceof Program) {

            Program program = (Program) item;
            startProgramIntent(program);
        }

    }


}
