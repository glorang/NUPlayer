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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.presenter.CatalogPresenter;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CatalogFragment extends GridFragment implements View.OnClickListener {
    private final static String TAG = "CatalogFragment";
    private static final int COLUMNS = 4;
    private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_MEDIUM;
    private List<String> selectedBrands = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();
        loadData();
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, container, false);

        ProgramList programList = ProgramList.getInstance();
        LinearLayout checkBoxView = view.findViewById(R.id.brandCheckBoxes);


        if(checkBoxView != null) {
            // Add list of all brands (channels)
            for (String brand : programList.getBrands()) {
                FrameLayout brandCheckbox = createBrandCheckbox(brand);
                if (brandCheckbox == null) {
                    continue;
                }
                checkBoxView.addView(brandCheckbox);
            }
        }

        return view;
    }

    private FrameLayout createBrandCheckbox(String brand) {

        // Create FrameLayout
        FrameLayout frameLayout = new FrameLayout(getContext());
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frameLayout.setPadding(5, 5, 5, 5);

        // Add checkbox + listener
        CheckBox checkBox = new CheckBox(getContext());

        // slight abuse but whatever. How hard is it again to pass a simple String? DONOTWANT
        checkBox.setContentDescription(brand);

        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        checkBox.setBackground(getResources().getDrawable(R.drawable.button_default, null));

        checkBox.setOnClickListener(this);

        // Add image view with brand logo
        ImageView imageView = new ImageView(getContext());
        LinearLayout.LayoutParams imageViewLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 30);
        imageViewLayoutParams.setMargins(0, 15, 0, 15);
        imageView.setLayoutParams(imageViewLayoutParams);

        int resourceID = getContext().getResources().getIdentifier(
                "ic_" + brand.replaceAll("-", ""),
                "drawable", getContext().getPackageName());

        // Only add Brands for which we have a logo
        if(resourceID <= 0) {
            return null;
        }

        imageView.setImageResource(resourceID);

        // Add checkbox + brand logo to framelayout
        frameLayout.addView(checkBox);
        frameLayout.addView(imageView);

        return frameLayout;
    }

    @Override
    public void onClick(View view) {

        if(view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox)view;
            String selectedBrand = checkBox.getContentDescription().toString();

            if(((CheckBox) view).isChecked()){
                if(!selectedBrands.contains(selectedBrand)) {
                    selectedBrands.add(selectedBrand);
                }
            } else {
                if(selectedBrands.contains(selectedBrand)) {
                    selectedBrands.remove(selectedBrand);
                }
            }
            loadData();
        }
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

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        Intent serviceIntent = new Intent(getActivity(), CatalogService.class);
        serviceIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
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
                    ProgramList pl = ProgramList.getInstance();
                    mAdapter.clear();
                    if(selectedBrands.size() == 0) {
                        mAdapter.addAll(0, pl.getPrograms());
                    } else {
                        for(Program program : pl.getPrograms()) {
                            if(selectedBrands.contains(program.getBrand())) {
                                mAdapter.add(program);
                            }
                        }
                    }
                }
            }
        });

        getActivity().startService(serviceIntent);
    }
}