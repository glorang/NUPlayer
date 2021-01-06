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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.VerticalGridFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;

import android.os.ResultReceiver;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.ProgramService;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.VideoList;
import be.lorang.nuplayer.presenter.CustomVerticalGridPresenter;
import be.lorang.nuplayer.presenter.ProgramPresenter;

import com.google.gson.Gson;

public class ProgramFragment extends VerticalGridFragment {

    private static final int COLUMNS = 1;
    private static final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    private static final String TAG = "ProgramFragment";

    private Program program;
    private ArrayObjectAdapter mAdapter;
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaulBackgroundImage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get Program object from ProgramActivity (why is it so complicated to pass objects :-\)
        ProgramActivity pa = (ProgramActivity)getActivity();
        program = pa.getProgram();

        // set default background
        mDefaulBackgroundImage = getResources().getDrawable(R.drawable.default_background, null);

        setupAdapter();
        setupUi();
        loadData();
    }

    private void setupUi() {
        setTitle(program.getTitle());
        updateBackground(program.getThumbnail("w1920hx"));
    }

    private void setupAdapter() {

        CustomVerticalGridPresenter videoGridPresenter = new CustomVerticalGridPresenter(ZOOM_FACTOR, false);
        videoGridPresenter.setNumberOfColumns(COLUMNS);

        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewClickedListener(new VideoProgramListener(this));
        setGridPresenter(videoGridPresenter);

        mAdapter = new ArrayObjectAdapter(new ProgramPresenter(getContext()));
        setAdapter(mAdapter);

        prepareEntranceTransition();
    }

    private void loadData() {

        // start an Intent to download all videos for a specific program

        Intent serviceIntent = new Intent(getActivity(), ProgramService.class);
        serviceIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
        serviceIntent.putExtra(ProgramService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    VideoList videoList = new Gson().fromJson(resultData.getString("VIDEO_LIST"), VideoList.class);
                    mAdapter.clear();
                    mAdapter.addAll(0, videoList.getVideos());
                    startEntranceTransition();
                }
            }
        });

        getActivity().startService(serviceIntent);

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
                        //Bitmap b = Utils.adjustOpacity(resource,40);
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    @Override
    public void onDestroy() {
        // If the background image of a program is missing it will show the background of the
        // previous program, make sure to unset it
        mBackgroundManager.clearDrawable();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        // reset background as it was removed in onDestroy()
        setupUi();
        super.onResume();
    }

}
