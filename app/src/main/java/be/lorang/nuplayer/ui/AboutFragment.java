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

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.app.BaseFragment;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.app.ErrorFragment;
import androidx.leanback.app.RowsFragment;

import java.util.HashMap;
import java.util.Map;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.VrtPlayerTokenService;

import static android.content.Context.MODE_PRIVATE;

/*
 * Simple fragment to show some general info and debug output, should be cleaned up for final release
 */

public class AboutFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {

    private BrowseFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseFragment.MainFragmentAdapter(this);
    private static final String TAG = "AboutFragment";

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMainFragmentAdapter().getFragmentHost().showTitleView(false);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View aboutFragmentView = inflater.inflate(R.layout.about_fragment, container, false);

        // get shared preferences
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);

        // Map SharedPreference key to TextView in layout
        Map<String, String> prefKeyToLabel = new HashMap<>();
        prefKeyToLabel.put("firstName", "text_firstName");
        prefKeyToLabel.put("lastName", "text_lastName");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS, "text_vrtPlayerTokenAnon");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY, "text_vrtPlayerTokenAnonDate");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED, "text_vrtPlayerTokenAuth");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY, "text_vrtPlayerTokenAuthDate");
        prefKeyToLabel.put("X-VRT-Token", "text_xvrttoken");

        // Update TextFields
        for (Map.Entry<String, String> entry : prefKeyToLabel.entrySet()) {

            TextView destField = aboutFragmentView.findViewById(
                    getResources().getIdentifier(entry.getValue(),
                            "id", getActivity().getPackageName()));

            String value = prefs.getString(entry.getKey(), "None");
            if(value.length() > 40) { value = value.substring(0,40) + " [...]"; }

            destField.setText(value);
        }

        return aboutFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
    }

}