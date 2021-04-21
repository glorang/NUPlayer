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
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.VerticalGridPresenter;

import be.lorang.nuplayer.model.Category;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.presenter.CatalogPresenter;
import be.lorang.nuplayer.services.CategoryService;

/*
 * Fragment that show Programs of a Category, initiated from CategoryCategoriesFragment
 */

public class CategoryProgramsFragment extends VerticalGridSupportFragment {

    private static final String TAG = "CategoryProgramsFragment";
    private static final int COLUMNS = 5;
    private static final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    private ArrayObjectAdapter mAdapter;
    private Category category;

    public CategoryProgramsFragment(Category category) {
        this.category = category;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        loadData();
    }

    private void setupAdapter() {

        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR, false);
        presenter.setNumberOfColumns(COLUMNS);

        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewClickedListener(new VideoProgramListener(getActivity()));
        setGridPresenter(presenter);

        mAdapter = new ArrayObjectAdapter(new CatalogPresenter(getContext()));
        setAdapter(mAdapter);
    }

    private void loadData() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return; }

        // start an Intent to set all categories in the Catalog (if not yet done)
        Intent categoriesIntent = new Intent(getActivity(), CategoryService.class);
        categoriesIntent.putExtra("ACTION", CategoryService.ACTION_SET_CATEGORIES);
        categoriesIntent.putExtra("CATEGORY_NAME", category.getName());
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

                    // get all programs from certain category from the catalog
                    for(Program program : ProgramList.getInstance().getProgramsByCategory(category.getName())) {
                        mAdapter.add(program);
                    }

                }

            }
        });

        getActivity().startService(categoriesIntent);

    }

}
