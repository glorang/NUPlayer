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

import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Row;

import android.util.Log;
import android.view.View;

import be.lorang.nuplayer.R;

public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";
    private static final String[] menuItems = {"Home", "Latest", "Series", "Catalog" ,"Settings"};
    private BackgroundManager mBackgroundManager;

    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUi();
        loadData();
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        getMainFragmentRegistry().registerFragment(PageRow.class,
                new PageRowFragmentFactory(mBackgroundManager));
    }

    private void setupUi() {

        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_logo, null));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED); // Should be HEADERS_HIDDEN
        setHeadersTransitionOnBackEnabled(true);

        // set menu background (left)
        setBrandColor(getResources().getColor(R.color.vrtnu_black_tint_1));

        // set content background (right)
        BackgroundManager.getInstance(getActivity()).setColor(getResources().getColor(R.color.vrtnu_black_tint_2));

        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.vrtnu_blue));
        setDefaultSearchListener();

        prepareEntranceTransition();
    }

    private void setDefaultSearchListener() {
        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });
    }

    /*
     * The search orb will always try to steal focus so we cannot navigate our own form controls anymore
     * Disable setOnFocusSearchListener
     */

    @Override
    public void onViewCreated(View b, Bundle savedInstanceState) {
        super.onViewCreated(b, savedInstanceState);
        BrowseFrameLayout mBrowseFrame = b.findViewById(R.id.browse_frame);
        if(mBrowseFrame != null) {
            mBrowseFrame.setOnFocusSearchListener(null);
        }
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
        createRows();
    }

    private void createRows() {
        mRowsAdapter.removeItems(0, mRowsAdapter.size());
        for(int i=0;i<menuItems.length;i++) {
            HeaderItem headerItem = new HeaderItem(i, menuItems[i]);
            PageRow pageRow = new PageRow(headerItem);
            mRowsAdapter.add(pageRow);
        }

        startEntranceTransition();
    }

    private class PageRowFragmentFactory extends BrowseSupportFragment.FragmentFactory {
        private final BackgroundManager mBackgroundManager;

        PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            mBackgroundManager.setDrawable(null);
            setDefaultSearchListener();
            switch(row.getHeaderItem().getName()) {
                case "Home":
                    return new HomeFragment();
                case "Catalog":
                    return new CatalogFragment();
                case "Series":
                    setOnSearchClickedListener(null);
                    return new SeriesFragment();
                case "Latest":
                    return new LatestFragment();
                case "Settings":
                    setOnSearchClickedListener(null);
                    return new SettingsMainFragment();
                default:
                    Log.d(TAG, "Unknown row: " + row.getHeaderItem().getName());
                    return new HomeFragment();
            }

        }
    }

}