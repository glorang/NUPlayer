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
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BaseFragment;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.app.ErrorFragment;
import androidx.leanback.app.RowsFragment;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.VrtPlayerTokenService;
import be.lorang.nuplayer.utils.Utils;

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

        // Map SharedPreference key to TextView in layout to update text fields (no formatting)
        Map<String, String> prefKeyToLabel = new HashMap<>();
        prefKeyToLabel.put("firstName", "text_firstName");
        prefKeyToLabel.put("lastName", "text_lastName");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS, "text_vrtPlayerTokenAnon");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY, "text_vrtPlayerTokenAnonDate");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED, "text_vrtPlayerTokenAuth");
        prefKeyToLabel.put(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY, "text_vrtPlayerTokenAuthDate");
        prefKeyToLabel.put("X-VRT-Token", "text_xvrttoken");
        prefKeyToLabel.put("vrtlogin-at", "text_vrtlogin_at");
        prefKeyToLabel.put("vrtlogin-rt", "text_vrtlogin_rt");
        prefKeyToLabel.put("vrtlogin-expiry", "text_vrtlogin_expiry");

        // Update TextFields
        for (Map.Entry<String, String> entry : prefKeyToLabel.entrySet()) {

            TextView destField = aboutFragmentView.findViewById(
                    getResources().getIdentifier(entry.getValue(),
                            "id", getActivity().getPackageName()));

            String value = prefs.getString(entry.getKey(), "None");

            // Format all date fields
            if(!value.equals("None")) {

                Instant date = null;

                // dates in ISO8601 format
                try {
                    if (entry.getKey().equals(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY) ||
                            entry.getKey().equals(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY)
                    ) {
                        date = Utils.parseDateISO8601(value);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

                // dates in a cookie (timestamp format)
                try {
                    if(entry.getKey().equals("X-VRT-Token") ||
                            entry.getKey().equals("vrtlogin-at") ||
                            entry.getKey().equals("vrtlogin-rt") ||
                            entry.getKey().equals("vrtlogin-expiry")
                    ) {
                        JSONObject cookieJSON = new JSONObject(value);
                        long expireDate = cookieJSON.getLong("whenCreated") +
                                (cookieJSON.getLong("maxAge")*1000); // Time in milliseconds
                        date = new Timestamp(expireDate).toInstant();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

                if(date != null) {
                    try {
                        if (Utils.isDateInPast(date.toString())) {
                            destField.setTextColor(ContextCompat.getColor(getContext(), R.color.vrtnu_red));
                        } else {
                            destField.setTextColor(ContextCompat.getColor(getContext(), R.color.vrtnu_green));
                        }

                        DateTimeFormatter formatter =
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                        .withLocale(Locale.UK)
                                        .withZone(ZoneId.of("Europe/Brussels"));

                        value = formatter.format(date);

                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }

            }

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