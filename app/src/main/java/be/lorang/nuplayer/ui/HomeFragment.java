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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.presenter.FavoritesPresenter;
import be.lorang.nuplayer.presenter.LiveTVPresenter;
import be.lorang.nuplayer.presenter.SeriesPresenter;
import be.lorang.nuplayer.presenter.VideoPresenter;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.ResumePointsService;
import be.lorang.nuplayer.services.SeriesService;

public class HomeFragment extends RowsFragment {

    private static String TAG = "HomeFragment";

    private ArrayObjectAdapter mRowsAdapter;

    private ArrayObjectAdapter liveTVAdapter;
    private ArrayObjectAdapter seriesAdapter;
    private ArrayObjectAdapter favoritesAdapter;
    private ArrayObjectAdapter watchLaterAdapter;
    private ArrayObjectAdapter continueWatchingAdapter;

    private ListRow liveTVListRow;
    private ListRow seriesListRow;
    private ListRow favoritesListRow;
    private ListRow watchLaterListRow;
    private ListRow continueWatchingListRow;

    private Intent seriesIntent;
    private Intent accessTokenIntent;
    private Intent favoritesIntent;
    private Intent resumePointsIntent;

    private boolean seriesLoaded = false;
    private boolean favoritesLoaded = false;
    private boolean resumePointsLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup adapters with fixed order
        setupAdapters();

        // setup intents for all stuff we want to add to the front page
        setupSeriesIntent();
        setupResumePointsIntent();
        setupFavoritesIntent();
        setupAccessTokenIntent();

        // populate the catalog
        populateCatalog();

        // add live TV
        addLiveTV();

    }

    public void notifyDataReady() {
        if(seriesLoaded && favoritesLoaded && resumePointsLoaded) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    private void setupAdapters() {

        // setup listener
        setOnItemViewClickedListener(new VideoProgramListener(this));

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);

        // Live TV Adapter
        LiveTVPresenter liveTVpresenter = new LiveTVPresenter(getContext());
        HeaderItem liveTVHeader = new HeaderItem(getString(R.string.livetv_title));
        liveTVAdapter = new ArrayObjectAdapter(liveTVpresenter);
        liveTVListRow = new ListRow(liveTVHeader, liveTVAdapter);
        mRowsAdapter.add(liveTVListRow);

        // Favorites Adapter
        FavoritesPresenter favoritesPresenter = new FavoritesPresenter(getContext());
        HeaderItem favoritesHeader = new HeaderItem(getString(R.string.favorites_title));
        favoritesAdapter = new ArrayObjectAdapter(favoritesPresenter);
        favoritesListRow = new ListRow(favoritesHeader, favoritesAdapter);
        mRowsAdapter.add(favoritesListRow);

        // Continue watching Adapter
        VideoPresenter continueWatchingPresenter = new VideoPresenter(getContext());
        HeaderItem continueWatchingHeader = new HeaderItem(getString(R.string.continuewatching_title));
        continueWatchingAdapter = new ArrayObjectAdapter(continueWatchingPresenter);
        continueWatchingListRow = new ListRow(continueWatchingHeader, continueWatchingAdapter);
        mRowsAdapter.add(continueWatchingListRow);

        // Watch Later Adapter
        VideoPresenter watchLaterPresenter = new VideoPresenter(getContext());
        HeaderItem watchLaterHeader = new HeaderItem(getString(R.string.watchlater_title));
        watchLaterAdapter = new ArrayObjectAdapter(watchLaterPresenter);
        watchLaterListRow = new ListRow(watchLaterHeader, watchLaterAdapter);
        mRowsAdapter.add(watchLaterListRow);

        // Series Adapter
        SeriesPresenter seriesPresenter = new SeriesPresenter(getContext());
        HeaderItem seriesHeader = new HeaderItem(getString(R.string.series_title));
        seriesAdapter = new ArrayObjectAdapter(seriesPresenter);
        seriesListRow = new ListRow(seriesHeader, seriesAdapter);
        mRowsAdapter.add(seriesListRow);
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

    }

    private void populateCatalog() {

        // start an Intent to download the Catalog
        Intent catalogIntent = new Intent(getActivity(), CatalogService.class);
        catalogIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Start AccessTokenIntent which will populate Favorites, Watch Later and Resume Points
                    getActivity().startService(accessTokenIntent);

                    // Add Series
                    getActivity().startService(seriesIntent);

                }
            }
        });

        getActivity().startService(catalogIntent);
    }

    private void setupSeriesIntent() {

        // start an Intent to get all complete series from the Catalog
        // the intent will only start once the catalog has been downloaded, e.g. it is started
        // from populateCatalog() above

        seriesIntent = new Intent(getActivity(), SeriesService.class);
        seriesIntent.putExtra(SeriesService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // add "complete series" to the front page
                    ProgramList programList = ProgramList.getInstance();
                    for(Program program : programList.getSeries()) {
                        seriesAdapter.add(program);
                    }

                    if(seriesAdapter.size() == 0) {
                        mRowsAdapter.remove(seriesListRow);
                    }

                }

                seriesLoaded = true;
                notifyDataReady();
            }
        });

    }

    private void setupAccessTokenIntent() {
        accessTokenIntent = new Intent(getActivity(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {

                    // Get Favorites
                    getActivity().startService(favoritesIntent);

                    // Get Watch Later + Continue Watching
                    getActivity().startService(resumePointsIntent);

                }
            }
        });
    }

    private void setupFavoritesIntent() {

        // start an Intent to get all favorites from the Catalog
        // the intent will only start once the catalog has been downloaded, e.g. it is started
        // from populateCatalog() above

        favoritesIntent = new Intent(getActivity(), FavoriteService.class);
        favoritesIntent.putExtra(FavoriteService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // add favorites to the front page
                    ProgramList programList = ProgramList.getInstance();
                    for(Program program : programList.getFavorites()) {
                        favoritesAdapter.add(program);
                    }

                    if(favoritesAdapter.size() == 0) {
                        mRowsAdapter.remove(favoritesListRow);
                    }

                }

                favoritesLoaded = true;
                notifyDataReady();
            }
        });
    }

    private void setupResumePointsIntent() {

        // start an Intent to get all "Continue Watching" and "Watch Later" from VRT NU API
        // It does not depend on the Catalog to be populated but does require a valid token

        resumePointsIntent = new Intent(getActivity(), ResumePointsService.class);
        resumePointsIntent.putExtra("ACTION", "GET_CONTINUE_WATCHING_WATCH_LATER");
        resumePointsIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // add Continue Watching
                    VideoContinueWatchingList videoContinueWatchingList = VideoContinueWatchingList.getInstance();
                    for(Video video : videoContinueWatchingList.getVideos()) {
                        continueWatchingAdapter.add(video);
                    }

                    if(continueWatchingAdapter.size() == 0) {
                        mRowsAdapter.remove(continueWatchingListRow);
                    }

                    // add Watch Later
                    VideoWatchLaterList videoWatchLaterList = VideoWatchLaterList.getInstance();
                    for(Video video : videoWatchLaterList.getVideos()) {
                        watchLaterAdapter.add(video);
                    }

                    if(watchLaterAdapter.size() == 0) {
                        mRowsAdapter.remove(watchLaterListRow);
                    }

                }

                resumePointsLoaded = true;
                notifyDataReady();

            }
        });

    }

    // Always show row titles
    @Override
    public void setExpand(boolean expand) {
        super.setExpand(true);
    }

}
