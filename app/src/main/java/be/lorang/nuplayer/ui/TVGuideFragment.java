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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.EPGEntry;
import be.lorang.nuplayer.model.EPGList;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.player.VideoPlaybackActivity;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.EPGService;
import be.lorang.nuplayer.services.StreamService;
import be.lorang.nuplayer.services.VrtPlayerTokenService;
import be.lorang.nuplayer.utils.Utils;

import static android.content.Context.MODE_PRIVATE;

/*
 * Fragment to generate a (basic) TV guide
 *
 * The Fragment's XML layout has 3 hardcoded FrameLayouts (epgListEen|Ketnet|Canvas), one for each channel
 * Each EPG entry is generated as a button and is positioned on the correct time via its left-margin.
 * Something-something with a RecyclerView is probably better and more performant but as we have
 * little data this works just as good
 * 
 * Hacky stuff in this Fragment:
 *  - EPG <-> Catalog is matched via title, we don't have a better field available
 *  - Marker is generated via 3 separate blocks (one for each channel)
 *
 */

public class TVGuideFragment extends Fragment implements BrowseSupportFragment.MainFragmentAdapterProvider {

    private BrowseSupportFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseSupportFragment.MainFragmentAdapter(this);
    private static final String TAG = "TVGuideFragment";

    private static final int WIDTH_DIVIDER = 4; // defines how long an EPG entry visually is
    private static final int START_HOUR = 6;
    private static final int TIMELINE_INCREMENT = 30 * 60; // 30 minutes

    // EPG Date selector formatter
    private final static DateTimeFormatter epgDialogFormatter = DateTimeFormatter.ofPattern("EE dd/MM")
            .withZone(ZoneId.of("Europe/Brussels"));

    // Hour formatter
    private final static DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("Europe/Brussels"));

    private Button markerEen;
    private Button markerKetnet;
    private Button markerCanvas;

    private ZonedDateTime startDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).with(LocalTime.of(START_HOUR, 0));
    private ZonedDateTime selectedDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_tvguide, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        setupButtonListeners();
        createEPGTimeline();

        // EPG Data after midnight is stored in the JSON file of the day before,
        // we make "today" a little bit longer by faking "today" as "yesterday"
        ZonedDateTime epgDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
        if(isAfterMidnightBeforeStart()) {
            selectedDate = epgDate.minusDays(1);
            startDate = startDate.minusDays(1);
        }
        updateEPGData();
    }

    /*
    @Override
    public void onResume() {
        super.onResume();
        updateMarkers();
        scrollToNow();
    }
    */

    private void setupButtonListeners() {
        Button today = getView().findViewById(R.id.buttonToday);
        Button backToNow = getView().findViewById(R.id.buttonBackToNow);

        if(today != null) {
            today.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ZonedDateTime dialogStartDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
                    if(isAfterMidnightBeforeStart()) {
                        dialogStartDate = dialogStartDate.minusDays(1);
                    }

                    String[] keys = new String[15];
                    String[] values = new String[15];
                    ZonedDateTime start = dialogStartDate.minusDays(7);

                    for(int i=0;i<keys.length;i++) {
                        keys[i] = start.toString();
                        values[i] = epgDialogFormatter.format(start);
                        start = start.plusDays(1);
                    }

                    values[6] = "Yesterday";
                    values[7] = "Today";
                    values[8] = "Tomorrow";

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            new ContextThemeWrapper(getActivity(), R.style.dialogAlertTheme));

                    builder.setTitle(getActivity().getString(R.string.epg_dialog_title))
                            .setSingleChoiceItems(values, 7, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int index) {
                                    selectedDate = ZonedDateTime.parse(keys[index]);
                                    today.setText(values[index]);
                                    updateEPGData();
                                    dialog.cancel();
                                }
                            });
                    builder.create().show();

                }
            });
        }

        if(backToNow!= null) {
            backToNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));

                    if(isAfterMidnightBeforeStart()) {
                        selectedDate = selectedDate.minusDays(1);
                    }

                    today.setText("Today");
                    updateEPGData();
                    scrollToNow();
                }
            });
        }

    }

    private void createEPGTimeline() {

        LinearLayout linearLayoutTimeline = getView().findViewById(R.id.linearLayoutTimeline);

        for(int i=0; i<(24 * 60 * 60) / TIMELINE_INCREMENT; i++) {

            int increment = i * TIMELINE_INCREMENT;
            Instant timeInterval = startDate.toInstant().plusSeconds(increment);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((TIMELINE_INCREMENT / WIDTH_DIVIDER),  LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView textView = new TextView(getActivity());
            textView.setLayoutParams(params);
            textView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            textView.setText("| " + hourFormatter.format(timeInterval));
            linearLayoutTimeline.addView(textView);
        }

    }

    // This will generate the marker on the current time and scroll to the current time
    // It's a bit hacky as we do it in 3 times, once for each channel
    private void createMarkers() {

        markerEen = createMarker();
        markerKetnet = createMarker();
        markerCanvas = createMarker();

        FrameLayout epgListEen = getView().findViewById(R.id.epgListEen);
        FrameLayout epgListKetnet = getView().findViewById(R.id.epgListKetnet);
        FrameLayout epgListCanvas = getView().findViewById(R.id.epgListCanvas);

        if(epgListEen != null) { epgListEen.addView(markerEen); }
        if(epgListKetnet != null) { epgListKetnet.addView(markerKetnet); }
        if(epgListCanvas != null) { epgListCanvas.addView(markerCanvas); }

        updateMarkers();
    }

    private Button createMarker() {
        Button button = new Button(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(10, ViewGroup.LayoutParams.MATCH_PARENT);
        button.setLayoutParams(params);
        button.setBackgroundColor(getResources().getColor(R.color.vrtnu_white_alpha_60, null));
        return button;
    }

    private void updateMarkers() {
        int xAtCurrentTime = (int)(ZonedDateTime.now().toEpochSecond() - startDate.toEpochSecond()) / WIDTH_DIVIDER;
        if(markerEen != null) { markerEen.setX(xAtCurrentTime); }
        if(markerKetnet != null) { markerKetnet.setX(xAtCurrentTime); }
        if(markerCanvas != null) { markerCanvas.setX(xAtCurrentTime); }
    }

    private void updateEPGData() {

        startDate = selectedDate.with(LocalTime.of(START_HOUR,0));

        // start an Intent to fetch EPG data
        Intent epgIntent = new Intent(getActivity(), EPGService.class);
        epgIntent.putExtra("ACTION", EPGService.ACTION_GET_EPG);
        epgIntent.putExtra("EPG_DATE", selectedDate.toString());
        epgIntent.putExtra(EPGService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                String returnData = resultData.getString("EPG_DATA", "");
                EPGList epgList = new Gson().fromJson(returnData,EPGList.class);

                FrameLayout channelLayoutEen = getView().findViewById(R.id.epgListEen);
                FrameLayout channelLayoutKetnet = getView().findViewById(R.id.epgListKetnet);
                FrameLayout channelLayoutCanvas = getView().findViewById(R.id.epgListCanvas);

                if(channelLayoutEen != null) { channelLayoutEen.removeAllViews(); }
                if(channelLayoutKetnet != null) { channelLayoutKetnet.removeAllViews(); }
                if(channelLayoutCanvas != null) { channelLayoutCanvas.removeAllViews(); }

                if(epgList != null) {
                    for(int i=0;i<epgList.getEpgData().size();i++) {
                        EPGEntry epgEntry = epgList.getEpgData().get(i);

                        EPGEntry epgEntryNext = null;
                        if((i+1) < epgList.getEpgData().size()) {
                            epgEntryNext = epgList.getEpgData().get(i+1);
                        }

                        Button button = generateEPGEntry(epgEntry, epgEntryNext);
                        switch (epgEntry.getChannelID()) {
                            case "O8": // een
                                if(channelLayoutEen != null) { channelLayoutEen.addView(button); }
                                break;
                            case "1H": // canvas
                                if(channelLayoutCanvas != null) { channelLayoutCanvas.addView(button); }
                                break;
                            case "O9": // ketnet
                                if(channelLayoutKetnet != null) { channelLayoutKetnet.addView(button); }
                                break;
                        }

                        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                TextView epgDescription = getView().findViewById(R.id.epgDescription);
                                TextView epgTitle = getView().findViewById(R.id.epgTitle);
                                TextView epgTime = getView().findViewById(R.id.epgTime);
                                ImageView epgImage = getView().findViewById(R.id.epgImage);

                                if(epgTitle != null) {
                                    epgTitle.setText(Html.fromHtml(epgEntry.getTitle(), Html.FROM_HTML_MODE_COMPACT));
                                }

                                if(epgTime != null) {
                                    ZonedDateTime startTime = ZonedDateTime.parse(epgEntry.getStartTime());
                                    ZonedDateTime endTime = ZonedDateTime.parse(epgEntry.getEndTime());
                                    String timeslot = hourFormatter.format(startTime) + " - " + hourFormatter.format(endTime);
                                    epgTime.setText(timeslot);
                                }

                                if(epgDescription != null) {
                                    epgDescription.setText(Html.fromHtml(epgEntry.getDescription(), Html.FROM_HTML_MODE_COMPACT));
                                }

                                if (epgImage != null) {
                                    Glide.with(getActivity())
                                            .asBitmap()
                                            .load(epgEntry.getThumbnail())
                                            .into(epgImage);
                                }

                            }
                        });
                    }

                    // Add timeline marker
                    // they should be created after the data is loaded to make sure they appear on top
                    createMarkers();

                    // Scroll to current time
                    scrollToNow();
                }
            }
        });

        getActivity().startService(epgIntent);
    }

    private Button generateEPGEntry(EPGEntry epgEntry, EPGEntry epgEntryNext) {

        ZonedDateTime startTime = ZonedDateTime.parse(epgEntry.getStartTime());
        ZonedDateTime endTimeReal = ZonedDateTime.parse(epgEntry.getEndTime());
        ZonedDateTime endTime = ZonedDateTime.parse(epgEntry.getEndTime());

        // Set endTime to startTime of next extry, this will visually close all gaps
        if(epgEntryNext != null) {
            endTime = ZonedDateTime.parse(epgEntryNext.getStartTime()).minusSeconds(30);
            // Check for channel wrap (epgList contains items for all channels)
            if(endTime.isBefore(startTime)) {
                endTime = endTimeReal;
            }
        }

        long duration = endTime.toEpochSecond() - startTime.toEpochSecond();
        int width = (int)(duration / WIDTH_DIVIDER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT);

        // calculate left margin offset from START_HOUR
        int leftMargin = (int)(startTime.toEpochSecond() - startDate.toEpochSecond()) / WIDTH_DIVIDER;
        params.setMargins(leftMargin,5,5,5);

        //Log.d(TAG, epgEntry.getTitle() + " width = " + width + " margin = " + leftMargin);

        Button button = new Button(getActivity());
        button.setLayoutParams(params);
        button.setBackground(getResources().getDrawable(R.drawable.button_epg, null));
        button.setAllCaps(false);
        button.setGravity(Gravity.TOP);
        button.setMaxLines(1);
        button.setStateListAnimator(null);
        button.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        button.setText(Html.fromHtml(epgEntry.getTitle(), Html.FROM_HTML_MODE_COMPACT));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
                // start Live TV
                if (currentTime.isAfter(startTime) && currentTime.isBefore(endTimeReal)) {
                    Video video = ChannelList.getInstance().getLiveChannel(epgEntry.getChannelID());
                    if (video != null) {
                        startVideoIntent(video);
                    }
                } else {
                    // start Program display
                    Program program = ProgramList.getInstance().getProgram(epgEntry.getTitle());
                    if (program != null) {
                        startProgramIntent(program);
                    } else {
                        Toast.makeText(getActivity(), "Program not found in catalog", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return button;
    }

    private void scrollToNow() {

        int xAtCurrentTime = (int)(ZonedDateTime.now().toEpochSecond() - startDate.toEpochSecond()) / WIDTH_DIVIDER;

        if(xAtCurrentTime < 0 || xAtCurrentTime > (86400 / WIDTH_DIVIDER)) { return; }

        HorizontalScrollView horizontalScrollView = getView().findViewById(R.id.epgScrollView);
        if(horizontalScrollView != null) {
            int scrollTo = (xAtCurrentTime - 600) > 0 ? xAtCurrentTime - 600 : 0;
            horizontalScrollView.scrollTo(scrollTo, 0);
        }
    }

    private boolean isAfterMidnightBeforeStart() {
        ZonedDateTime epgDate = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
        return epgDate.getHour() >= 0 && epgDate.getHour() < START_HOUR;
    }

    // FIXME: copy/paste from VideoProgramListener
    private void startProgramIntent(Program program) {
        Intent programIntent = new Intent(getActivity().getBaseContext(), ProgramActivity.class);
        programIntent.putExtra("PROGRAM_OBJECT", (new Gson()).toJson(program));
        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
        startActivity(programIntent, bundle);
    }

    // FIXME: simplified (Live TV only) copy/paste from VideoProgramListener
    private void startVideoIntent(Video video) {

        // Define the stream intent (this is the same for both Live TV as on demand video)
        // to get required stream info, but do not start it yet

        Intent streamIntent = new Intent(getActivity(), StreamService.class);
        streamIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
        streamIntent.putExtra(StreamService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {

                    // Start new Intent to play video
                    String videoURL = resultData.getString("MPEG_DASH_URL", "");
                    String drmToken = resultData.getString("VUALTO_TOKEN", "");
                    if(videoURL.length() > 0) {

                        Intent playbackIntent = new Intent(getActivity(), VideoPlaybackActivity.class);
                        playbackIntent.putExtra("VIDEO_OBJECT", (new Gson()).toJson(video));
                        playbackIntent.putExtra("MPEG_DASH_URL", videoURL);
                        playbackIntent.putExtra("VUALTO_TOKEN", drmToken);
                        startActivity(playbackIntent);

                    }
                }
            }
        });

        // get vrtPlayerToken
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        String vrtPlayerToken = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS, "");
        String vrtPlayerTokenExpiry = prefs.getString(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY, "");
        String tokenType = VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS;

        // Start extra Intent if we don't have a vrtPlayerToken or if it's expired
        if(vrtPlayerToken.length() == 0 || Utils.isDateInPast(vrtPlayerTokenExpiry)) {

            Intent playerTokenIntent = new Intent(getActivity(), VrtPlayerTokenService.class);
            playerTokenIntent.putExtra("TOKEN_TYPE", tokenType);

            playerTokenIntent.putExtra(VrtPlayerTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);

                    // show messages, if any
                    if(resultData.getString("MSG", "").length() > 0) {
                        Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                    }

                    if (resultCode == Activity.RESULT_OK) {
                        // now that we have a vrtPlayerToken we can start the Stream Intent
                        getActivity().startService(streamIntent);
                    }
                }
            });

            getActivity().startService(playerTokenIntent);

        } else {
            // we already have a valid vrtPLayerToken, start Stream Intent immediately
            getActivity().startService(streamIntent);
        }
    }

}