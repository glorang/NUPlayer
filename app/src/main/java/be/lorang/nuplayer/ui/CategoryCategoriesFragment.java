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
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Category;
import be.lorang.nuplayer.model.CategoryList;
import be.lorang.nuplayer.presenter.CategoryPresenter;
import be.lorang.nuplayer.presenter.CustomVerticalGridPresenter;
import be.lorang.nuplayer.services.CategoryService;

/*
 * Fragment that show Categories, initiated from CategoryMainFragment
 */

public class CategoryCategoriesFragment extends GridFragment implements OnItemViewClickedListener {

    private final static String TAG = "CategoryCategoriesFragment";
    private static final int COLUMNS = 5;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_MEDIUM;
    private ArrayObjectAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        loadData();
    }

    public void setupAdapter() {

        CustomVerticalGridPresenter presenter = new CustomVerticalGridPresenter(ZOOM_FACTOR, false);
        presenter.setPaddingTop(100);
        presenter.setNumberOfColumns(COLUMNS);

        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewClickedListener(this);
        setGridPresenter(presenter);

        mAdapter = new ArrayObjectAdapter(new CategoryPresenter(getContext()));
        setAdapter(mAdapter);

    }

    private void loadData() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return; }

        // start an Intent to get all categories
        Intent categoriesIntent = new Intent(getActivity(), CategoryService.class);
        categoriesIntent.putExtra("ACTION", CategoryService.ACTION_GET_CATEGORIES);
        categoriesIntent.putExtra(CategoryService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // return if activity got destroyed in the mean time
                if (getActivity() == null) {
                    return;
                }

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    for (Category category : CategoryList.getInstance().getCategories()) {
                        Log.d(TAG, "Adding category = " + category.getTitle());
                        mAdapter.add(category);
                    }
                }

            }
        });

        getActivity().startService(categoriesIntent);

    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof Category) {

            Category category = (Category) item;

            // Return if activity got destroyed in the mean time
            if(getActivity() == null) { return; }

            // Switch fragment to CategoryProgramsFragment
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

            // Set focus to submenu button "Categories"
            for(Fragment fragment : fragmentManager.getFragments()) {
                if(fragment instanceof MainFragment) {
                    MainFragment mainFragment = (MainFragment)fragment;
                    if(mainFragment.getView() != null) {
                        Button buttonSubOnDemandCategories = fragment.getView().findViewById(R.id.buttonSubOnDemandCategories);
                        if(buttonSubOnDemandCategories != null) {
                            buttonSubOnDemandCategories.requestFocus();
                        }
                    }
                }
            }

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.menuContentContainer, new CategoryProgramsFragment(category));
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }

    }

}