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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import android.os.Handler;
import android.os.ResultReceiver;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.presenter.SeriesPresenter;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.services.SeriesService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

public class SeriesFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {

    private BrowseFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseFragment.MainFragmentAdapter(this);
    private static final String TAG = "SeriesFragment";

    private ArrayObjectAdapter mAdapter;
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaulBackgroundImage;

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadData();

        // set default background
        mDefaulBackgroundImage = getResources().getDrawable(R.drawable.default_background, null);
        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_series, container, false);

        RowsFragment rowsFragment = new RowsFragment();

        // setup listeners
        rowsFragment.setOnItemViewClickedListener(new VideoProgramListener(this));

        rowsFragment.setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if(item instanceof Program) {
                Program program = (Program) item;
                setBrandLogo(program.getBrand());
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

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.seriesContainer, rowsFragment);
        fragmentTransaction.commit();

        return view;
    }

    private void loadData() {

        // start an Intent to get all complete series from the Catalog
        Intent seriesIntent = new Intent(getActivity(), SeriesService.class);
        seriesIntent.putExtra(SeriesService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

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

    private void updateBackground(String uri) {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        if(!mBackgroundManager.isAttached()) {
            mBackgroundManager.attach(getActivity().getWindow());
        }
        DisplayMetrics mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .errorOf(mDefaulBackgroundImage)
                .centerCrop();

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }
}