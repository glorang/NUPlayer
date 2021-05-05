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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.ProgramService;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.VideoList;
import be.lorang.nuplayer.presenter.CustomVerticalGridPresenter;
import be.lorang.nuplayer.presenter.WideVideoPresenter;
import be.lorang.nuplayer.services.ResumePointsService;

import com.google.gson.Gson;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgramFragment extends VerticalGridSupportFragment implements OnItemViewSelectedListener {

    private static final int COLUMNS = 1;
    private static final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    private static final String TAG = "ProgramFragment";

    private Program program;
    private VideoList videoList;
    private LinkedHashMap<String,String> seasons;
    private int selectedSeasonIndex = 0;

    private ArrayObjectAdapter mAdapter;
    private BackgroundManager mBackgroundManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get Program object from ProgramActivity (why is it so complicated to pass objects :-\)
        ProgramActivity pa = (ProgramActivity)getActivity();
        program = pa.getProgram();

        setupAdapter();
        loadData(1);
    }

    private void setupUi() {
        setTitle(program.getTitle());
        updateBackground(program.getThumbnail("w1920hx"));
        setBrandImage(program.getBrand().replaceAll("-", ""));
        setFavoritesButton();
    }

    private void setBrandImage(String brand) {
        try {

            ImageView brandImageLogo = (ImageView) getActivity().findViewById(R.id.brandImageLogo);
            if (brandImageLogo != null) {
                int resourceID = getResources().getIdentifier("ic_" + brand, "drawable", getActivity().getPackageName());
                if (resourceID > 0) {
                    brandImageLogo.setImageDrawable(getResources().getDrawable(resourceID, null));
                }
            }

        } catch(Exception e) {
            // don't crash if we can't find / load brand image
            e.printStackTrace();
        }
    }

    private void setEpisodeCount(int episodeCount, int episodesLoaded) {
        try {
            if(episodeCount > 0) {
                TextView episodeCountText = (TextView) getActivity().findViewById(R.id.episodeCountText);
                episodeCountText.setText("Available: " + episodeCount + "\nLoaded: " + episodesLoaded);
            }
        } catch(Exception e) {
            // don't crash if we can't find / load brand image
            e.printStackTrace();
        }
    }

    private void setAssetOffTime(String assetOffTime) {
        try {
            DateTimeFormatter assetOffTimeFormatter = DateTimeFormatter.ofPattern("dd LLL yyyy")
                    .withZone(ZoneId.of("Europe/Brussels"));
            ZonedDateTime assetOffTimeZdt = ZonedDateTime.parse(assetOffTime,
                    DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ssZ"));

            TextView assetOffDateText = (TextView) getActivity().findViewById(R.id.assetOffDateText);
            assetOffDateText.setText(assetOffTimeFormatter.format(assetOffTimeZdt));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void setRegion(String region) {
        try {

            String icon = "";
            switch(region) {
                case "WORLD":
                    icon = "ic_globe";
                    break;
                case "BE":
                    icon = "ic_flag_be";
                    break;
            }

            ImageView allowedRegionImage = (ImageView) getActivity().findViewById(R.id.allowedRegionImage);
            if (allowedRegionImage != null) {
                Log.d(TAG, "Setting image = " + icon);
                int resourceID = getResources().getIdentifier(icon, "drawable", getActivity().getPackageName());
                if (resourceID > 0) {
                    allowedRegionImage.setImageDrawable(getResources().getDrawable(resourceID, null));
                }
            }

        } catch(Exception e) {
            // don't crash if we can't find / load region flag
            e.printStackTrace();
        }
    }

    private void setupAdapter() {

        CustomVerticalGridPresenter videoGridPresenter = new CustomVerticalGridPresenter(ZOOM_FACTOR, false);
        videoGridPresenter.setOffset(340);
        videoGridPresenter.setNumberOfColumns(COLUMNS);

        // note: The click listeners must be called before setGridPresenter for the event listeners
        // to be properly registered on the viewholders.
        setOnItemViewClickedListener(new VideoProgramListener(getActivity()));
        setOnItemViewSelectedListener(this);
        setGridPresenter(videoGridPresenter);

        mAdapter = new ArrayObjectAdapter(new WideVideoPresenter(getContext(), WideVideoPresenter.CardType.PROGRAM));
        setAdapter(mAdapter);
        prepareEntranceTransition();
    }

    private void loadData(int startIndex) {

        // return if Activity got destroyed in the mean time
        if(getActivity() == null) { return; }

        // start an Intent to download all videos for a specific program
        Intent serviceIntent = new Intent(getActivity(), ProgramService.class);
        serviceIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
        serviceIntent.putExtra("START_INDEX", startIndex);
        serviceIntent.putExtra("SEASON_INDEX", selectedSeasonIndex);

        //
        // Between this ProgramFragment and ProgramService we pass seasons Map back and forth
        // This way we can don't have to parse the seasons each time we load more episodes
        // or switch seasons
        //

        if(seasons != null) {
            serviceIntent.putExtra("SEASON_LIST", (new Gson()).toJson(seasons));
        }

        serviceIntent.putExtra(ProgramService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    videoList = VideoList.getInstance();
                    seasons = new Gson().fromJson(resultData.getString("SEASON_LIST"), LinkedHashMap.class);

                    setEpisodeCount(videoList.getVideosAvailable(), videoList.getVideosLoaded());
                    updateSeasons(seasons);

                    // Set region from first result
                    if(videoList.getVideosAvailable() > 0) {
                        Video video = videoList.getVideo(0);
                        setRegion(video.getAllowedRegion());
                    }

                    // Add all new videos to the adapter
                    for(int i=(startIndex-1); i<videoList.getVideosLoaded(); i++) {
                        mAdapter.add(videoList.getVideo(i));
                    }

                    // Set focus on Video list (iso on Favorites button or Season selector)
                    getView().requestFocus();

                }

                startEntranceTransition();
            }
        });

        getActivity().startService(serviceIntent);

    }

    private void updateSeasons(LinkedHashMap<String,String> seasons) {

        if(getActivity() == null) { return; }

        Spinner spinner = (Spinner)getActivity().findViewById(R.id.spinner_seasons);

        if(spinner == null || seasons == null || seasons.size() == 0 || spinner.getAdapter() != null) {
            return;
        }

        List<String> spinnerArray =  new ArrayList<String>();
        for (Map.Entry<String, String> season : seasons.entrySet()) {
            spinnerArray.add(season.getValue());
        }

        // add all discovered seasons
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // this avoids triggering the listener event onStart()
        spinner.setSelection(0, false);

        // add listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
              @Override
              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                  // clear existing videos
                  mAdapter.clear();
                  // set season index
                  selectedSeasonIndex = position;
                  // load new data
                  loadData(1);
              }

              @Override
              public void onNothingSelected(AdapterView<?> parent) {

              }
          }
        );

    }

    private void setFavoritesButtonState(Button button, String state) {
        Drawable img;
        String text;

        switch(state) {
            case "Remove":
                img = ResourcesCompat.getDrawable(getResources(),
                        android.R.drawable.ic_menu_delete, getContext().getTheme());
                text = "Remove";
                break;
            default:
            case "Add":
                img = ResourcesCompat.getDrawable(getResources(),
                        android.R.drawable.ic_menu_add, getContext().getTheme());
                text = "Add";
                break;
        }

        button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        button.setText(text);
    }


    private void setFavoritesButton() {

        Button favoriteButton = (Button)getActivity().findViewById(R.id.buttonFavorite);

        if(favoriteButton == null) {
            return;
        }

        if(program.isFavorite()) {
            setFavoritesButtonState(favoriteButton,"Remove");
        } else {
            setFavoritesButtonState(favoriteButton,"Add");
        }

        // add listener
        favoriteButton.setOnClickListener(v -> {
            boolean newState = !program.isFavorite();

            // switch state
            if(program.isFavorite()) {
                setFavoritesButtonState(favoriteButton,"Add");
            } else {
                setFavoritesButtonState(favoriteButton,"Remove");
            }

            // update our own instance
            program.setIsFavorite(newState);

            // update at VRT
            Intent accessTokenIntent = new Intent(getContext(), AccessTokenService.class);
            accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == Activity.RESULT_OK) {

                        Intent favoritesIntent = new Intent(getContext(), FavoriteService.class);
                        favoritesIntent.putExtra("ACTION", FavoriteService.ACTION_UPDATE_FAVORITE);
                        favoritesIntent.putExtra("X-VRT-Token", resultData.getString("X-VRT-Token"));
                        favoritesIntent.putExtra("PROGRAM_OBJECT", new Gson().toJson(program));
                        favoritesIntent.putExtra("IS_FAVORITE", newState);

                        // Our Program object doesn't hold the whatsonId as it's not returned by
                        // the suggest API so we pass it on from the first Video object
                        // This is maybe a little hacky but should do the job
                        videoList = VideoList.getInstance();
                        if(videoList.getVideosLoaded() > 0) {
                            String whatsonId = videoList.getVideo(0).getProgramWhatsonId();
                            if(whatsonId.length() > 0) {
                                favoritesIntent.putExtra("WHATSONID", whatsonId);
                            }
                        }

                        favoritesIntent.putExtra(ResumePointsService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                super.onReceiveResult(resultCode, resultData);

                                // show messages, if any
                                if (resultData.getString("MSG", "").length() > 0) {
                                    Toast.makeText(getContext(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        getContext().startService(favoritesIntent);

                    }
                }
            });

            getContext().startService(accessTokenIntent);

        });

    }

    private void updateBackground(String uri) {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        if(!mBackgroundManager.isAttached()) {
            mBackgroundManager.attach(getActivity().getWindow());
        }

        Glide.with(getActivity())
                .asBitmap()
                .centerCrop()
                .load(uri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    // Clearing the drawable here is required to make sure there are no references left
    // to the image as Glide might recycle it when the activity is paused
    @Override
    public void onPause() {
        super.onPause();
        if(mBackgroundManager.getDrawable() != null) {
            mBackgroundManager.clearDrawable();
            //mBackgroundManager.release();
         }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unset image and release memory
        if(mBackgroundManager.getDrawable() != null) {
            mBackgroundManager.clearDrawable();
            mBackgroundManager.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // reset background as it was removed in onStop() | onPause()
        setupUi();

        // Update adapter with latest progress for all videos
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
            }
        }, 2000);

    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof Video) {
            int selectedIndex = mAdapter.indexOf(item);
            if (selectedIndex != -1 && (mAdapter.size() - 1) == selectedIndex ) {

                // at last element, check if we need to load more data
                if(videoList != null && videoList.moreVideosAvailable()) {
                    // +1 for 0-based index -> 1-based index +1 to "start" at next episode = 2
                    Log.d(TAG, "More video's available. Trying to load videos as of: " + (selectedIndex+2));
                    loadData((selectedIndex+2));
                }
            }

            setAssetOffTime(((Video) item).getAssetOffTime());
        }
    }


}
