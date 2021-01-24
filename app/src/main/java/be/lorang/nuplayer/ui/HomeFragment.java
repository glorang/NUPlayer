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
import android.util.Log;
import android.widget.Toast;

import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.presenter.FavoritesPresenter;
import be.lorang.nuplayer.presenter.LiveTVPresenter;
import be.lorang.nuplayer.presenter.SeriesPresenter;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.SeriesService;

public class HomeFragment extends RowsFragment {

    private static String TAG = "HomeFragment";

    private ArrayObjectAdapter mRowsAdapter;
    private HeaderItem headerItem;
    private ArrayObjectAdapter adapter;

    private Intent seriesIntent;
    private Intent accessTokenIntent;
    private Intent favoritesIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        populateCatalog();
        addLiveTV();
        addCompleteSeries();
        addFavorites();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

    private void setupAdapter() {

        // setup listener
        setOnItemViewClickedListener(new VideoProgramListener(this));

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    private void addLiveTV() {

        // Live TV
        ChannelList channelList = ChannelList.getInstance();
        LiveTVPresenter liveTVpresenter = new LiveTVPresenter();
        headerItem = new HeaderItem("Live TV");
        adapter = new ArrayObjectAdapter(liveTVpresenter);

        for(Video channel : channelList.getChannels()) {
            adapter.add(channel);
        }

        mRowsAdapter.add(new ListRow(headerItem, adapter));

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

                    // series intent
                    getActivity().startService(seriesIntent);
                    // favorites intent (via accesstokenintent)
                    getActivity().startService(accessTokenIntent);

                }
            }
        });

        getActivity().startService(catalogIntent);
    }

    private void addCompleteSeries() {

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
                    SeriesPresenter seriesPresenter = new SeriesPresenter();
                    ProgramList programList = ProgramList.getInstance();

                    headerItem = new HeaderItem("Complete series");
                    adapter = new ArrayObjectAdapter(seriesPresenter);

                    for(Program program : programList.getSeries()) {
                        adapter.add(program);
                    }

                    mRowsAdapter.add(new ListRow(headerItem, adapter));

                }
            }
        });

    }

    private void addFavorites() {

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
                    FavoritesPresenter favoritesPresenter = new FavoritesPresenter();
                    ProgramList programList = ProgramList.getInstance();

                    headerItem = new HeaderItem("Favorites");
                    adapter = new ArrayObjectAdapter(favoritesPresenter);

                    for(Program program : programList.getFavorites()) {
                        adapter.add(program);
                    }

                    mRowsAdapter.add(new ListRow(headerItem, adapter));

                }
            }
        });

        accessTokenIntent = new Intent(getActivity(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    getActivity().startService(favoritesIntent);
                }
            }
        });

    }

    // Always show row titles
    @Override
    public void setExpand(boolean expand) {
        super.setExpand(true);
    }

}
