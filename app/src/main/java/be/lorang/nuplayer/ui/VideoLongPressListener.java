package be.lorang.nuplayer.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import android.app.Fragment;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.ResumePointsService;

public class VideoLongPressListener implements View.OnLongClickListener {

    private static final String TAG = "VideoLongPressListener";

    private Context context;
    private Video video;

    public VideoLongPressListener(Context context, Video video) {
        this.context = context;
        this.video = video;
    }

    @Override
    public boolean onLongClick(View v) {

        ArrayList<String> menuOptions = new ArrayList<>();

        VideoWatchLaterList videoWatchLaterList = VideoWatchLaterList.getInstance();
        if(video.getProgressPct() > 0) {
            menuOptions.add(context.getString(R.string.context_remove_continue_watching));
        } else if(videoWatchLaterList.getVideos().contains(video)) {
            menuOptions.add(context.getString(R.string.context_remove_watch_later));
        } else if(!videoWatchLaterList.getVideos().contains(video)){
            menuOptions.add(context.getString(R.string.context_add_watch_later));
        }

        menuOptions.add(context.getString(R.string.context_cancel));

        String[] test = new String[menuOptions.size()];
        for(int i=0;i<menuOptions.size();i++) {
            test[i] = menuOptions.get(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.context_title))
                .setItems(test, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int index) {

                        String item = menuOptions.get(index);
                        // Comparing on the label value is maybe not the best but should work...

                        if(item.equals(context.getString(R.string.context_cancel))) {
                            return;
                        }

                        if(item.equals(context.getString(R.string.context_remove_continue_watching))) {
                            removeFromContinueWatching();
                        }

                        if(item.equals(context.getString(R.string.context_add_watch_later))) {
                            updateWatchLater(true);
                        }

                        if(item.equals(context.getString(R.string.context_remove_watch_later))) {
                            updateWatchLater(false);
                        }

                        // Update adapters on HomeFragemnt
                        notifyHomeFragment();

                    }
                });
        builder.create().show();

        return false;
    }

    // Refresh adapters on HomeFragment, there are probably better ways to do this but worksforme[tm]
    // This function looks for:
    // MainActivity -> Fragments -> MainFragment -> ChildFragments -> HomeFragment and then calls
    // refreshAdapters() to make sure we remove adapters if they are (now) empty
    private void notifyHomeFragment() {

        if(context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            List<Fragment> mainFragments = activity.getFragmentManager().getFragments();
            for (Fragment fragment : mainFragments) {
                if (fragment instanceof MainFragment) {
                    MainFragment m = (MainFragment) fragment;
                    List<Fragment> childFragments = m.getChildFragmentManager().getFragments();
                    for (Fragment childFragment : childFragments) {
                        if (childFragment instanceof HomeFragment) {
                            HomeFragment homeFragment = (HomeFragment) childFragment;
                            homeFragment.refreshAdapters();
                        }
                    }
                }
            }
        }
    }

    // remove from continue watching by settings progress == video length (this is what VRT.NU does)
    private void removeFromContinueWatching() {

        if(context == null || video == null) { return; }

        Intent accessTokenIntent = new Intent(context, AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    Intent resumePointsIntent = new Intent(context, ResumePointsService.class);
                    resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_UPDATE_RESUME_POINT);
                    resumePointsIntent.putExtra("X-VRT-Token", resultData.getString("X-VRT-Token"));
                    resumePointsIntent.putExtra("PLAYER_CURRENT_POSITION", video.getDuration());
                    resumePointsIntent.putExtra("VIDEO_OBJECT", new Gson().toJson(video));

                    resumePointsIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            // show messages, if any
                            if (resultData.getString("MSG", "").length() > 0) {
                                Toast.makeText(context, resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    context.startService(resumePointsIntent);

                }
            }
        });

        context.startService(accessTokenIntent);
    }

    // Add/remove watch later
    private void updateWatchLater(boolean watchLater) {

        if(context == null || video == null) { return; }

        Intent accessTokenIntent = new Intent(context, AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    Intent resumePointsIntent = new Intent(context, ResumePointsService.class);
                    resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_UPDATE_WATCH_LATER);
                    resumePointsIntent.putExtra("X-VRT-Token", resultData.getString("X-VRT-Token"));
                    resumePointsIntent.putExtra("VIDEO_OBJECT", new Gson().toJson(video));
                    resumePointsIntent.putExtra("WATCH_LATER", watchLater);

                    resumePointsIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            // show messages, if any
                            if (resultData.getString("MSG", "").length() > 0) {
                                Toast.makeText(context, resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    context.startService(resumePointsIntent);

                }
            }
        });

        context.startService(accessTokenIntent);
    }


}
