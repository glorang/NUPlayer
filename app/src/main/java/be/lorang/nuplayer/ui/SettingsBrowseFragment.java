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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Row;

import be.lorang.nuplayer.R;

/*
 * BrowseFragemnt to set and navigate through all Settings/About etc
 */

public class SettingsBrowseFragment extends BrowseFragment {
    private static final String TAG = "SettingsBrowseFragment";
    private static final String[] menuItems = {"Settings", "Token status", "About"};
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

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set menu background (left)
        setBrandColor(getResources().getColor(R.color.vrtnu_black_tint_1));

        // set content background (right)
        BackgroundManager.getInstance(getActivity()).setColor(getResources().getColor(R.color.vrtnu_black_tint_2));

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
    }

    private class PageRowFragmentFactory extends BrowseFragment.FragmentFactory {
        private final BackgroundManager mBackgroundManager;

        PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            mBackgroundManager.setDrawable(null);
            switch(row.getHeaderItem().getName()) {
                case "Settings":
                    return new SettingsFragment();
                case "Token status":
                    return new TokenStatusFragment();
                case "About":
                    return new AboutFragment();
                default:
                    Log.d(TAG, "Unknown row: " + row.getHeaderItem().getName());
                    return new SettingsFragment();
            }

        }
    }

}