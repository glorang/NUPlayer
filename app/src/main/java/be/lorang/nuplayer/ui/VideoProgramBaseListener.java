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
 * Base listener class for Video and Program objects, used throughout different Fragments:
 * HomeFragment, SeriesFragment, LatestFragment, TVGuideFragment, ...
 */

public class VideoProgramBaseListener {
    private Activity activity;
    private static String TAG = "VideoProgramBaseListener";

    public VideoProgramBaseListener(Activity activity) {
        this.activity = activity;
    }

    public void startProgramIntent(Program program) {

        Intent programIntent = new Intent(activity.getBaseContext(), ProgramActivity.class);
        programIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle();
        activity.startActivity(programIntent, bundle);
    }

    public void startVideoIntent(Video video) {

        // Define the stream intent (this is the same for both Live TV as on demand video)
        // to get required stream info, but do not start it yet

        // return if activity got destroyed in the mean time
        if(activity == null) { return; }

        Intent streamIntent = new Intent(activity, StreamService.class);
        streamIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
        streamIntent.putExtra(StreamService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if(activity == null) { return; }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(activity, resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Start new Intent to play video
                    String videoURL = resultData.getString("MPEG_DASH_URL", "");
                    String drmToken = resultData.getString("VUALTO_TOKEN", "");
                    if(videoURL.length() > 0) {

                        Intent playbackIntent = new Intent(activity, VideoPlaybackActivity.class);
                        playbackIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
                        playbackIntent.putExtra("MPEG_DASH_URL", videoURL);
                        playbackIntent.putExtra("VUALTO_TOKEN", drmToken);
                        activity.startActivity(playbackIntent);

                    }
                }
            }
        });

        // get vrtPlayerToken
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
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

            Intent playerTokenIntent = new Intent(activity, VrtPlayerTokenService.class);
            playerTokenIntent.putExtra("TOKEN_TYPE", tokenType);

            playerTokenIntent.putExtra(VrtPlayerTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);

                    // return if activity got destroyed in the mean time
                    if(activity == null) { return; }

                    // show messages, if any
                    if(resultData.getString("MSG", "").length() > 0) {
                        Toast.makeText(activity, resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                    }

                    if (resultCode == Activity.RESULT_OK) {
                        // now that we have a vrtPlayerToken we can start the Stream Intent
                        activity.startService(streamIntent);
                    }
                }
            });

            // Get X-VRT-Player token for on-demand content
            if(tokenType.equals(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED)) {
                Intent accessTokenIntent = new Intent(activity, AccessTokenService.class);
                accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        if (resultCode == Activity.RESULT_OK) {

                            // return if activity got destroyed in the mean time
                            if(activity == null) { return; }

                            String token = resultData.getString("X-VRT-Token", "");
                            Log.d(TAG, "Successfully obtained X-VRT-Token:" + token);
                            playerTokenIntent.putExtra("X-VRT-Token", token);
                            activity.startService(playerTokenIntent);
                        }
                    }
                });

                activity.startService(accessTokenIntent);

            } else {
                // live-tv content, doesn't need an authenticated token,
                // start VrtPlayerTokenService immediately
                activity.startService(playerTokenIntent);
            }

        } else {
            // we already have a valid vrtPLayerToken, start Stream Intent immediately
            activity.startService(streamIntent);
        }
    }

}
