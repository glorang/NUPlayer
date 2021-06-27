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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.leanback.widget.SearchEditText;

import be.lorang.nuplayer.R;

public class MainFragment extends HorizontalMenuFragment implements TextWatcher {

    private static final String TAG = "MainFragment";

    private static final String[] menuItems = {
            "Home",
            "TV guide",
            "On demand",
            "Settings"
    };

    private static final int[] menuIcons = {
            R.drawable.ic_baseline_home,
            R.drawable.ic_baseline_calendar_today,
            R.drawable.ic_baseline_movie,
            R.drawable.ic_baseline_settings
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_horizontal, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout menuNavigationContainer = view.findViewById(R.id.menuNavigationContainer);
        LinearLayout mainContainerLayout = view.findViewById(R.id.mainContainerLayout);
        FrameLayout menuNavigationOverlay = view.findViewById(R.id.menuNavigationOverlay);
        setContentContainer(mainContainerLayout);

        ImageButton searchButton = view.findViewById(R.id.searchButton);
        SearchEditText searchText = view.findViewById(R.id.searchText);

        // Set click listener for Search icon, this will slide in|out the search bar
        if(searchButton != null && searchText != null) {
            searchButton.setOnClickListener(v -> {

                // Slide in search bar
                if(searchText.getWidth() == 0) {

                    // unset any previous search text
                    searchText.setText(null);

                    menuNavigationOverlay.setFocusable(false);
                    menuNavigationContainer.setVisibility(View.GONE);

                    ValueAnimator slideAnimator = ValueAnimator
                            .ofInt(0, menuNavigationContainer.getWidth())
                            .setDuration(200);

                    slideAnimator.addUpdateListener(animation1 -> {
                        Integer value = (Integer) animation1.getAnimatedValue();
                        searchText.getLayoutParams().width = value.intValue();
                        searchText.requestLayout();
                    });

                    slideAnimator.addListener(new AnimatorListenerAdapter() {
                         @Override
                         public void onAnimationEnd(Animator animation) {
                             searchText.requestFocus();
                         }
                    });

                    AnimatorSet animationSet = new AnimatorSet();
                    animationSet.setInterpolator(new AccelerateInterpolator());
                    animationSet.play(slideAnimator);
                    animationSet.start();

                    setSelectedFragment(new SearchFragment());
                    loadFragment(R.id.mainContainerLayout);

                // Slide out search bar
                } else {

                    ValueAnimator slideAnimator = ValueAnimator
                            .ofInt(searchText.getWidth(), 0)
                            .setDuration(200);

                    slideAnimator.addUpdateListener(animation1 -> {
                        Integer value = (Integer) animation1.getAnimatedValue();
                        searchText.getLayoutParams().width = value.intValue();
                        searchText.requestLayout();
                    });

                    slideAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            menuNavigationOverlay.setFocusable(true);
                            menuNavigationContainer.setVisibility(View.VISIBLE);
                        }
                    });

                    AnimatorSet animationSet = new AnimatorSet();
                    animationSet.setInterpolator(new DecelerateInterpolator());
                    animationSet.play(slideAnimator);
                    animationSet.start();

                }

            });
        }

        // Set search text listener to update search results
        searchText.addTextChangedListener(this);

        // show soft keyboard when searchText has focus
        searchText.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                InputMethodManager in = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                in.showSoftInput(searchText, 0);
            }
        });

        // hide soft keyboard when search is pressed
        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager in = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
            }
            return false;
        });

        for(int i=0;i<menuItems.length;i++) {

            String menuItem = menuItems[i];

            Button button = createMenuButton(menuItem, menuIcons[i], 280);

            // Set focus listener for all (except search) menu items
            button.setOnFocusChangeListener((View.OnFocusChangeListener) (v, hasFocus) -> {

                if(hasFocus) {

                    setSelectedMenuButton(button);

                    // Fragment options
                    switch (menuItem) {
                        case "Home":
                            if(!(getSelectedFragment() instanceof HomeFragment)) {
                                showProgressBar();
                                setSelectedFragment(new HomeFragment());
                            }
                            break;
                        case "TV guide":
                            if(!(getSelectedFragment() instanceof TVGuideFragment)) {
                                setSelectedFragment(new TVGuideFragment());
                            }
                            break;
                        case "On demand":
                            if(!(getSelectedFragment() instanceof OnDemandFragment)) {
                                setSelectedFragment(new OnDemandFragment());
                            }
                            break;
                        case "Settings":
                            if(!(getSelectedFragment() instanceof SettingsMainFragment)) {
                                setSelectedFragment(new SettingsMainFragment());
                            }
                            break;
                        default:
                            setSelectedFragment(null);
                            break;
                    }


                    if(mainContainerLayout != null) {
                        loadFragment(R.id.mainContainerLayout);
                        if(getSelectedFragment() == null) {
                            mainContainerLayout.removeAllViews();
                        }
                    }

                }

            });

            // Set focus to first menu item
            if(i==0) {
                button.requestFocus();
            }

            // Set button ids && setNextFocusDownId for items with a submenu
            switch(menuItems[i]) {
                case "Home":
                    button.setId(R.id.buttonTopNavHome);
                    break;
                case "TV guide":
                    button.setId(R.id.buttonTopNavTVGuide);
                    break;
                case "On demand":
                    button.setId(R.id.buttonTopNavOnDemand);
                    button.setNextFocusDownId(R.id.buttonSubOnDemandLatest);
                    break;
                case "Settings":
                    button.setId(R.id.buttonTopNavSettings);
                    button.setNextFocusDownId(R.id.buttonSubSettingsSettings);
                    break;
            }

            if(menuNavigationContainer != null) {
                menuNavigationContainer.addView(button);
            }
        }

        // add default Fragment
        if(mainContainerLayout != null) {
            showProgressBar();
            setSelectedFragment(new HomeFragment());
            loadFragment(R.id.mainContainerLayout);
        }
    }

    // TextWatcher listener methods
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(getLoadedFragment() instanceof SearchFragment) {
            ((SearchFragment)getLoadedFragment()).setSearchQuery(s.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public void fadeInMainMenuBar() {

        if(getView() == null) { return; }

        LinearLayout menuNavigation = getView().findViewById(R.id.menuNavigation);

        if(menuNavigation != null) {

            int height = menuNavigation.getLayoutParams().height;

            if(menuNavigation.getVisibility() == View.INVISIBLE) {

                // Set height to 0
                menuNavigation.getLayoutParams().height = 0;
                menuNavigation.requestLayout();

                // Make nav visible
                menuNavigation.setVisibility(View.VISIBLE);

                // Fade in
                ValueAnimator slideAnimator = ValueAnimator
                        .ofInt(0, height)
                        .setDuration(500);

                slideAnimator.addUpdateListener(animation -> {
                    Integer value = (Integer) animation.getAnimatedValue();
                    menuNavigation.getLayoutParams().height = value.intValue();
                    menuNavigation.requestLayout();
                });

                AnimatorSet animationSet = new AnimatorSet();
                animationSet.setInterpolator(new AccelerateInterpolator());
                animationSet.setStartDelay(500);
                animationSet.play(slideAnimator);
                animationSet.start();

            }

        }

    }

}