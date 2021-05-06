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

package be.lorang.nuplayer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import java.util.ArrayList;
import java.util.List;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.presenter.FavoritesPresenter;
import be.lorang.nuplayer.presenter.LiveTVPresenter;
import be.lorang.nuplayer.presenter.VideoPresenter;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.services.EPGService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.ResumePointsService;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends RowsSupportFragment {

    public static String TAG = "HomeFragment";

    private ArrayObjectAdapter mRowsAdapter;

    private ArrayObjectAdapter liveTVAdapter;
    private ArrayObjectAdapter favoritesAdapter;
    private ArrayObjectAdapter watchLaterAdapter;
    private ArrayObjectAdapter continueWatchingAdapter;

    private HeaderItem liveTVHeader;
    private HeaderItem favoritesHeader;
    private HeaderItem continueWatchingHeader;
    private HeaderItem watchLaterHeader;

    private ListRow liveTVListRow;
    private ListRow favoritesListRow;
    private ListRow watchLaterListRow;
    private ListRow continueWatchingListRow;

    private boolean favoritesListRowAdded = false;
    private boolean watchLaterListRowAdded = false;
    private boolean continueWatchingListRowAdded = false;

    private Intent accessTokenIntent;
    private Intent favoritesIntent;
    private Intent resumePointsIntent;

    private boolean liveTVLoaded = false;
    private boolean favoritesLoaded = false;
    private boolean resumePointsLoaded = false;

    private String xvrttoken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup adapters with fixed order
        setupAdapters();

        // setup intents for all stuff we want to add to the front page
        setupResumePointsIntent();
        setupFavoritesIntent();
        setupAccessTokenIntent();

        // add live TV
        addLiveTV();

        // Populate catalog
        populateCatalog();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setPadding(0, 80, 0, 0);
        return view;
    }

    /*
     * When we open the app we want to hide the menu and only exit when the menu is visible (press return twice)
     * This is *exactly* what:
     *   setHeadersState(HEADERS_HIDDEN);
     *   setHeadersTransitionOnBackEnabled(true);
     * in MainFragment is supposed to do but doesn't work for on first load, I think there is some
     * initial state bug, hence we solve this with a quick-and-dirty SharedPreference ¯\_(ツ)_/¯
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences viewer = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        if(viewer.getBoolean(MainActivity.PREFERENCE_IS_APP_STARTUP, true)) {
            view.requestFocus();
            SharedPreferences.Editor editor = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(MainActivity.PREFERENCE_IS_APP_STARTUP, false);
            editor.apply();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapters();
    }

    // Refresh all adapters with latest status:
    // - Update progress bar(s)
    // - Add/remove Favorites
    // - Add/remove Continue Watching
    // - Add/remove Watch later
    public void refreshAdapters() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if(liveTVLoaded) {
                    updateEPGData();
                }

                if(favoritesLoaded) {
                    favoritesAdapter.setItems(ProgramList.getInstance().getFavorites(), null);
                    favoritesAdapter.notifyArrayItemRangeChanged(0, favoritesAdapter.size());
                    showHideFavorites();
                }

                if(resumePointsLoaded) {
                    continueWatchingAdapter.setItems(getContinueWatchingList(), null);
                    continueWatchingAdapter.notifyArrayItemRangeChanged(0, continueWatchingAdapter.size());
                    showHideContinueWatching();

                    watchLaterAdapter.setItems(VideoWatchLaterList.getInstance().getVideos(), null);
                    watchLaterAdapter.notifyArrayItemRangeChanged(0, watchLaterAdapter.size());
                    showHideWatchLater();
                }

            }
        }, 1000);
    }

    public void notifyDataReady() {
        if(favoritesLoaded && resumePointsLoaded && liveTVLoaded) {
            //getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    private void setupAdapters() {

        // setup listener
        setOnItemViewClickedListener(new VideoProgramListener(getActivity()));

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);

        // Live TV Adapter
        LiveTVPresenter liveTVpresenter = new LiveTVPresenter(getContext());
        liveTVHeader = new HeaderItem(getString(R.string.livetv_title));
        liveTVAdapter = new ArrayObjectAdapter(liveTVpresenter);
        liveTVListRow = new ListRow(liveTVHeader, liveTVAdapter);
        mRowsAdapter.add(0, liveTVListRow);

        // Favorites Adapter
        FavoritesPresenter favoritesPresenter = new FavoritesPresenter(getContext());
        favoritesHeader = new HeaderItem(getString(R.string.favorites_title));
        favoritesAdapter = new ArrayObjectAdapter(favoritesPresenter);
        favoritesListRow = new ListRow(favoritesHeader, favoritesAdapter);
        mRowsAdapter.add(1, favoritesListRow);
        favoritesListRowAdded = true;

        // Continue watching Adapter
        VideoPresenter continueWatchingPresenter = new VideoPresenter(getContext());
        continueWatchingHeader = new HeaderItem(getString(R.string.continuewatching_title));
        continueWatchingAdapter = new ArrayObjectAdapter(continueWatchingPresenter);
        continueWatchingListRow = new ListRow(continueWatchingHeader, continueWatchingAdapter);
        mRowsAdapter.add(2, continueWatchingListRow);
        continueWatchingListRowAdded = true;

        // Watch Later Adapter
        VideoPresenter watchLaterPresenter = new VideoPresenter(getContext());
        watchLaterHeader = new HeaderItem(getString(R.string.watchlater_title));
        watchLaterAdapter = new ArrayObjectAdapter(watchLaterPresenter);
        watchLaterListRow = new ListRow(watchLaterHeader, watchLaterAdapter);
        mRowsAdapter.add(3, watchLaterListRow);
        watchLaterListRowAdded = true;
    }

    private void addLiveTV() {

        // Live TV
        ChannelList channelList = ChannelList.getInstance();

        for(Video channel : channelList.getChannels()) {
            liveTVAdapter.add(channel);
        }

        if(liveTVAdapter.size() == 0) {
            mRowsAdapter.remove(liveTVListRow);
        }

        // Add EPG data
        updateEPGData();

    }

    private void updateEPGData() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        // start an Intent to fetch EPG data
        Intent epgIntent = new Intent(getActivity(), EPGService.class);
        epgIntent.putExtra("ACTION", EPGService.ACTION_UPDATE_LIVE_TV_EPG);
        epgIntent.putExtra(EPGService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if(getActivity() == null) { return ; }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                // Update adapter
                if (resultCode == Activity.RESULT_OK) {
                    liveTVAdapter.notifyArrayItemRangeChanged(0, liveTVAdapter.size());
                }

                liveTVLoaded = true;
                notifyDataReady();
            }
        });

        getActivity().startService(epgIntent);
    }

    private void populateCatalog() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        // start an Intent to download the Catalog
        Intent catalogIntent = new Intent(getActivity(), CatalogService.class);
        catalogIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if(getActivity() == null) { return ; }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    // Start AccessTokenIntent which will populate Favorites, Watch Later and Resume Points
                    getActivity().startService(accessTokenIntent);
                }
            }
        });

        getActivity().startService(catalogIntent);
    }

    private void setupAccessTokenIntent() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        accessTokenIntent = new Intent(getActivity(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    // return if activity got destroyed in the mean time
                    if(getActivity() == null) { return ; }

                    // Store X-VRT-Token
                    xvrttoken = resultData.getString("X-VRT-Token", "");

                    // Get Favorites
                    getActivity().startService(favoritesIntent);

                    // Get Watch Later + Continue Watching
                    getActivity().startService(resumePointsIntent);

                } else {
                    // User not authenticated
                    favoritesLoaded = true;
                    resumePointsLoaded = true;

                    showHideFavorites();
                    showHideContinueWatching();
                    showHideWatchLater();

                    notifyDataReady();
                }
            }
        });
    }

    private void setupFavoritesIntent() {

        // start an Intent to get all favorites from the Catalog
        // the intent will only start once the catalog has been downloaded, e.g. it is started
        // from populateCatalog() above

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        favoritesIntent = new Intent(getActivity(), FavoriteService.class);
        favoritesIntent.putExtra("ACTION", FavoriteService.ACTION_GET);
        favoritesIntent.putExtra("X-VRT-Token", xvrttoken);
        favoritesIntent.putExtra(FavoriteService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if(getActivity() == null) { return ; }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Set favorites
                    favoritesAdapter.setItems(ProgramList.getInstance().getFavorites(), null);
                }

                favoritesLoaded = true;
                showHideFavorites();
                notifyDataReady();
            }
        });
    }

    private void setupResumePointsIntent() {

        // start an Intent to get all "Continue Watching" and "Watch Later" from VRT NU API
        // Require valid VRT token, started via AccessToken intent once Catalog is downloaded

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        resumePointsIntent = new Intent(getActivity(), ResumePointsService.class);
        resumePointsIntent.putExtra("ACTION", ResumePointsService.ACTION_GET);
        resumePointsIntent.putExtra("X-VRT-Token", xvrttoken);
        resumePointsIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if(getActivity() == null) { return ; }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Set Continue watching Later
                    continueWatchingAdapter.setItems(getContinueWatchingList(), null);

                    // Set Watch Later
                    watchLaterAdapter.setItems(VideoWatchLaterList.getInstance().getVideos(), null);
                }

                resumePointsLoaded = true;
                showHideContinueWatching();
                showHideWatchLater();
                notifyDataReady();

            }
        });

    }

    private List<Video> getContinueWatchingList() {
        List<Video> result = new ArrayList<>();

        // Get Continue Watching
        VideoContinueWatchingList videoContinueWatchingList = VideoContinueWatchingList.getInstance();

        // Add results
        for(Video video : videoContinueWatchingList.getVideos()) {
            if(video.getProgressPct() > 5 && video.getProgressPct() < 95) {
                result.add(video);
            }
        }

        return result;
    }

    // Always show row titles
    @Override
    public void setExpand(boolean expand) {
        super.setExpand(true);
    }

    private void showHideFavorites() {
        int index = 1;

        if(favoritesAdapter.size() == 0 && favoritesListRowAdded) {
            mRowsAdapter.remove(favoritesListRow);
            favoritesListRowAdded = false;
        } else if(favoritesAdapter.size() > 0 && !favoritesListRowAdded) {
            mRowsAdapter.add(index, favoritesListRow);
            favoritesListRowAdded = true;
        }
    }

    private void showHideContinueWatching() {
        int index = 2;
        if(favoritesAdapter.size() == 0) { index--; }

        if(continueWatchingAdapter.size() == 0 && continueWatchingListRowAdded) {
            mRowsAdapter.remove(continueWatchingListRow);
            continueWatchingListRowAdded = false;
        } else if(continueWatchingAdapter.size() > 0 && !continueWatchingListRowAdded) {
            mRowsAdapter.add(index, continueWatchingListRow);
            continueWatchingListRowAdded = true;
        }
    }

    private void showHideWatchLater() {
        int index = 3;
        if(favoritesAdapter.size() == 0) { index--; }
        if(continueWatchingAdapter.size() == 0) { index--; }

        if(watchLaterAdapter.size() == 0 && watchLaterListRowAdded) {
            mRowsAdapter.remove(watchLaterListRow);
            watchLaterListRowAdded = false;
        } else if(watchLaterAdapter.size() > 0 && !watchLaterListRowAdded) {
            mRowsAdapter.add(index, watchLaterListRow);
            watchLaterListRowAdded = true;
        }
    }
}
