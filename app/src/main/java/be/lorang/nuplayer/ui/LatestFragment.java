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

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.model.VideoList;
import be.lorang.nuplayer.presenter.CustomVerticalGridPresenter;
import be.lorang.nuplayer.presenter.WideVideoPresenter;
import be.lorang.nuplayer.services.LatestService;

public class LatestFragment extends GridFragment implements OnItemViewSelectedListener {
    private final static String TAG = "LatestFragment";
    private static final int COLUMNS = 1;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_MEDIUM;
    private ArrayObjectAdapter mAdapter;
    private VideoList videoList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        loadData(1);
    }

    public void notifyDataReady() {
        if(getFragmentManager() != null) {
            for (Fragment fragment : getFragmentManager().getFragments()) {
                if (fragment instanceof OnDemandFragment) {
                    ((OnDemandFragment) fragment).hideProgressBar();
                }
            }
        }
    }

    private void setupAdapter() {
        CustomVerticalGridPresenter presenter = new CustomVerticalGridPresenter(ZOOM_FACTOR, false);
        presenter.setPaddingTop(100);
        presenter.setNumberOfColumns(COLUMNS);

        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewClickedListener(new VideoProgramListener(getActivity()));
        setOnItemViewSelectedListener(this);
        setGridPresenter(presenter);

        mAdapter = new ArrayObjectAdapter(new WideVideoPresenter(getContext(), WideVideoPresenter.CardType.LATEST));
        setAdapter(mAdapter);
    }

    private void loadData(int startIndex) {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        // start an Intent to download all latest videos as of startIndex
        Intent latestIntent = new Intent(getActivity(), LatestService.class);
        latestIntent.putExtra("START_INDEX", startIndex);

        latestIntent.putExtra(LatestService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
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
                    videoList = VideoList.getInstance();

                    // Add all new videos to the adapter
                    for(int i=(startIndex-1); i<videoList.getVideosLoaded(); i++) {
                        mAdapter.add(videoList.getVideo(i));
                    }

                }

                notifyDataReady();

            }
        });

        getActivity().startService(latestIntent);

    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof Video) {
            int selectedIndex = mAdapter.indexOf(item);
            if (selectedIndex != -1 && (mAdapter.size() - 1) == selectedIndex ) {

                // at last element, check if we need to load more data
                if(videoList != null && videoList.moreVideosAvailable()) {
                    // +1 for 0-based index -> 1-based index +1 to "start" at next episode = 2
                    Log.d(TAG, "More video's available. Trying to load videos as of: " + (selectedIndex+2));
                    loadData((selectedIndex+2));
                }
            }
        }
    }


}