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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.presenter.SeriesPresenter;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.services.SeriesService;

import com.bumptech.glide.Glide;

public class SeriesFragment extends Fragment implements BrowseSupportFragment.MainFragmentAdapterProvider {

    private BrowseSupportFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseSupportFragment.MainFragmentAdapter(this);
    private static final String TAG = "SeriesFragment";

    private ArrayObjectAdapter mAdapter;
    private BackgroundManager mBackgroundManager;
    private Program program = null;

    private ImageView seriesBackgroundImageView = null;

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadData();
        //getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        seriesBackgroundImageView = view.findViewById(R.id.seriesBackgroundImageView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(program != null) {
            updateBackground(program.getThumbnail("w1920hx"));
        }
    }

    // Clearing the drawable here is required to make sure there are no references left
    // to the image as Glide might recycle it when the activity is paused
    @Override
    public void onPause() {
        super.onPause();
        if(seriesBackgroundImageView != null && seriesBackgroundImageView.getBackground() != null) {
            seriesBackgroundImageView.setBackground(null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unset background image
        if(seriesBackgroundImageView != null && seriesBackgroundImageView.getBackground() != null) {
            seriesBackgroundImageView.setBackground(null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_series, container, false);

        RowsSupportFragment rowsFragment = new RowsSupportFragment();

        // setup listeners
        rowsFragment.setOnItemViewClickedListener(new VideoProgramListener(getActivity()));

        rowsFragment.setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if(item instanceof Program) {
                Program program = (Program) item;
                this.program = program;
                setBrandLogo(program.getBrand());
                setTitle(program.getTitle());
                setDescription(program.getDescription());
                updateBackground(program.getThumbnail("w1920hx"));
            }
        });

        // setup adapter
        ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        rowsFragment.setAdapter(mRowsAdapter);

        SeriesPresenter seriesPresenter = new SeriesPresenter(getContext());
        mAdapter = new ArrayObjectAdapter(seriesPresenter);
        ListRow seriesListrow = new ListRow(null, mAdapter);
        mRowsAdapter.add(seriesListrow);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.seriesContainer, rowsFragment);
        fragmentTransaction.commit();

        return view;
    }

    private void loadData() {

        // return if activity got destroyed in the mean time
        if(getActivity() == null) { return ; }

        // start an Intent to get all complete series from the Catalog
        Intent seriesIntent = new Intent(getActivity(), SeriesService.class);
        seriesIntent.putExtra(SeriesService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
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

                    ProgramList programList = ProgramList.getInstance();
                    for(Program program : programList.getSeries()) {
                        mAdapter.add(program);
                    }
                }

            }
        });

        getActivity().startService(seriesIntent);

    }

    private void setBrandLogo(String brand) {
        ImageView brandImageView = getView().findViewById(R.id.brand_image);
        if (brandImageView != null && brand != null) {
            int resourceID = getContext().getResources().getIdentifier(
                    "ic_" + brand.replaceAll("-",""),
                    "drawable", getContext().getPackageName());
            if (resourceID > 0) {
                brandImageView.setImageResource(resourceID);
                brandImageView.setVisibility(View.VISIBLE);
            } else {
                brandImageView.setVisibility(View.GONE);
            }
        }
    }

    private void setTitle(String title) {
        TextView textViewTitle = getView().findViewById(R.id.seriesTitle);
        if(textViewTitle != null) {
            textViewTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT));
        }

    }

    private void setDescription(String description) {
        TextView textViewDescription = getView().findViewById(R.id.seriesDescription);
        if(textViewDescription != null) {
            if (description.length() > 350) {
                textViewDescription.setText(Html.fromHtml(description.substring(0, 350).concat("..."), Html.FROM_HTML_MODE_COMPACT));
            } else {
                textViewDescription.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT));
            }
        }
    }

    private void updateBackground(String uri) {
        Glide.with(this)
                .load(uri)
                .centerInside()
                .into(seriesBackgroundImageView);
    }
}