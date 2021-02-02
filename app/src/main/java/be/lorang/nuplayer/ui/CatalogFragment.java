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

import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.presenter.CatalogPresenter;
import com.google.gson.Gson;


public class CatalogFragment extends GridFragment {
    private final static String TAG = "CatalogFragment";
    private static final int COLUMNS = 2;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_MEDIUM;
    private ArrayObjectAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        loadData();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

    private void setupAdapter() {

        VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR, false);
        presenter.setNumberOfColumns(COLUMNS);
        setGridPresenter(presenter);

        mAdapter = new ArrayObjectAdapter(new CatalogPresenter(getContext()));
        setAdapter(mAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(
                    Presenter.ViewHolder itemViewHolder,
                    Object item,
                    RowPresenter.ViewHolder rowViewHolder,
                    Row row) {

                        if (item instanceof Program) {
                            Program program = (Program) item;

                            Intent programIntent = new Intent(getActivity().getBaseContext(), ProgramActivity.class);
                            programIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
                            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                            startActivity(programIntent, bundle);
                        }
            }
        });
    }

    private void loadData() {

        // start an Intent to download the catalog
        // note that normally the catalog is already filled from the HomeFragment
        Intent serviceIntent = new Intent(getActivity(), CatalogService.class);
        serviceIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    ProgramList pl = ProgramList.getInstance();
                    mAdapter.clear();
                    mAdapter.addAll(0, pl.getPrograms());
                }
            }
        });

        getActivity().startService(serviceIntent);
    }
}