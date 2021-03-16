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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BrowseSupportFragment;

import be.lorang.nuplayer.R;


/*
 * Fragment that manages the Settings (sub)menu
 */

public class SettingsMainFragment extends Fragment implements BrowseSupportFragment.MainFragmentAdapterProvider {

    private BrowseSupportFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseSupportFragment.MainFragmentAdapter(this);
    private static final String TAG = "SettingsDummyFragment";

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_settings_main, container, false);

        // get container layout where we show our settings
        LinearLayout settingsContainer = view.findViewById(R.id.settingsContainerLayout);

        // get form controls
        Button settingsButtonSettings = view.findViewById(R.id.buttonSettingsMainSettings);
        Button settingsButtonTokenStatus = view.findViewById(R.id.buttonSettingsMainTokenStatus);
        Button settingsButtonAbout = view.findViewById(R.id.buttonSettingsMainAbout);

        // add default Fragment
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.settingsContainerLayout, new SettingsFragment());
        fragmentTransaction.commit();

        // Listeners to switch layout
        settingsButtonSettings.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                settingsContainer.removeAllViews();
                FragmentTransaction fragmentInnerTransaction = fragmentManager.beginTransaction();
                fragmentInnerTransaction.add(R.id.settingsContainerLayout, new SettingsFragment());
                fragmentInnerTransaction.commit();
            }
        });

        settingsButtonTokenStatus.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                settingsContainer.removeAllViews();
                FragmentTransaction fragmentInnerTransaction = fragmentManager.beginTransaction();
                fragmentInnerTransaction.add(R.id.settingsContainerLayout, new TokenStatusFragment());
                fragmentInnerTransaction.commit();
            }
        });

        settingsButtonAbout.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                settingsContainer.removeAllViews();
                FragmentTransaction fragmentInnerTransaction = fragmentManager.beginTransaction();
                fragmentInnerTransaction.add(R.id.settingsContainerLayout, new AboutFragment());
                fragmentInnerTransaction.commit();
            }
        });

        return view;
    }

}