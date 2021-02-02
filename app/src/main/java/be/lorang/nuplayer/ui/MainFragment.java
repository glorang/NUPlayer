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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Row;

import android.view.View;

import be.lorang.nuplayer.R;

public class MainFragment extends BrowseFragment {

    private static final long HEADER_ID_1 = 1;
    private static final String HEADER_NAME_1 = "Home";
    private static final long HEADER_ID_2 = 2;
    private static final String HEADER_NAME_2 = "Catalog";
    private static final long HEADER_ID_3 = 3;
    private static final String HEADER_NAME_3 = "Series";
    private static final long HEADER_ID_4 = 4;
    private static final String HEADER_NAME_4 = "About";

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

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        prepareEntranceTransition();
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
        createRows();
    }

    private void createRows() {
        HeaderItem headerItem1 = new HeaderItem(HEADER_ID_1, HEADER_NAME_1);
        PageRow pageRow1 = new PageRow(headerItem1);
        mRowsAdapter.add(pageRow1);

        HeaderItem headerItem2 = new HeaderItem(HEADER_ID_2, HEADER_NAME_2);
        PageRow pageRow2 = new PageRow(headerItem2);
        mRowsAdapter.add(pageRow2);

        HeaderItem headerItem3 = new HeaderItem(HEADER_ID_3, HEADER_NAME_3);
        PageRow pageRow3 = new PageRow(headerItem3);
        mRowsAdapter.add(pageRow3);

        HeaderItem headerItem4 = new HeaderItem(HEADER_ID_4, HEADER_NAME_4);
        PageRow pageRow4 = new PageRow(headerItem4);
        mRowsAdapter.add(pageRow4);

        startEntranceTransition();
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
            if (row.getHeaderItem().getId() == HEADER_ID_1) {
                return new HomeFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_2) {
                return new CatalogFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_3) {
                return new SeriesFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_4) {
                return new AboutFragment();
            }

            throw new IllegalArgumentException(String.format("Invalid row %s", rowObj));
        }
    }

}
