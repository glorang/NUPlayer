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

import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import be.lorang.nuplayer.R;

public class MainFragment extends HorizontalMenuFragment {

    private static final String TAG = "MainFragment";

    private static final String[] menuItems = {
            "Home",
            "TV guide",
            "On demand",
            "Settings",
            "Search"
    };

    private static final int[] menuIcons = {
            R.drawable.ic_baseline_home,
            R.drawable.ic_baseline_calendar_today,
            R.drawable.ic_baseline_movie,
            R.drawable.ic_baseline_settings,
            R.drawable.ic_baseline_search,
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_horizontal, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout menuNavigationContainer = view.findViewById(R.id.menuNavigationContainer);
        LinearLayout mainContainerLayout = view.findViewById(R.id.mainContainerLayout);

        for(int i=0;i<menuItems.length;i++) {

            String menuItem = menuItems[i];

            Button button = createMenuButton(menuItem, menuIcons[i], 300);

            // Set click listener for search
            button.setOnClickListener(v -> {
                if (menuItem.equals("Search")) {
                    if (getActivity() == null) {
                        return;
                    }
                    startActivity(new Intent(getActivity(), SearchActivity.class));
                }
            });

            // Set focus listener for all (except search) menu items
            button.setOnFocusChangeListener((View.OnFocusChangeListener) (v, hasFocus) -> {

                if(hasFocus) {

                    setSelectedMenuButton(button);

                    // Fragment options
                    switch (menuItem) {
                        case "Home":
                            if(!(getSelectedFragment() instanceof HomeFragment)) {
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
            setSelectedFragment(new HomeFragment());
            loadFragment(R.id.mainContainerLayout);
        }
    }

}