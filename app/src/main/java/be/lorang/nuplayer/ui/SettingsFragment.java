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

/*
 * Settings fragment, used to logout, refresh catalog etc
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BrowseSupportFragment;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.ResumePointList;
import be.lorang.nuplayer.model.VideoContinueWatchingList;
import be.lorang.nuplayer.model.VideoWatchLaterList;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.AuthService;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.SeriesService;
import be.lorang.nuplayer.services.VrtPlayerTokenService;
import be.lorang.nuplayer.utils.HTTPClient;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends Fragment implements BrowseSupportFragment.MainFragmentAdapterProvider {

    private BrowseSupportFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseSupportFragment.MainFragmentAdapter(this);
    private static final String TAG = "SettingsFragment";

    public final static String SETTING_DEVELOPER_MODE = "developerMode";

    private Intent catalogIntent;
    private Intent seriesIntent;
    private Intent accessTokenIntent;
    private Intent favoritesIntent;

    private boolean catalogLoaded;
    private boolean seriesLoaded;
    private boolean favoritesLoaded;

    private TextView catalogField;
    private TextView JSONcacheField;
    private TextView loggedinField;

    private Switch developerModeSwitch;
    private Button catalogButton;
    private Button cacheButton;
    private Button loginButton;

    private LinearLayout catalogContainer;
    private LinearLayout jsonCacheContainer;

    private ProgressBar catalogProgressBar;

    private String xvrttoken;

    @Override
    public BrowseSupportFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareIntents();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // get form text fields
        catalogField = view.findViewById(R.id.valueSettingsCatalog);
        JSONcacheField = view.findViewById(R.id.valueSettingsJSONCache);
        loggedinField = view.findViewById(R.id.valueSettingsLoggedIn);

        // get form controls
        developerModeSwitch = view.findViewById(R.id.switchDeveloperMode);
        catalogButton = view.findViewById(R.id.buttonSettingsCatalogRefresh);
        catalogProgressBar = view.findViewById(R.id.progressBarSettingsCatalogRefresh);
        cacheButton = view.findViewById(R.id.buttonSettingsJSONCache);
        loginButton = view.findViewById(R.id.buttonSettingsLoggedIn);

        // get layouts only visible when developer mode enabled
        catalogContainer = view.findViewById(R.id.catalogContainer);
        jsonCacheContainer = view.findViewById(R.id.jsonCacheContainer);

        // set initial values
        setDeveloperMode();
        setCatalogText(catalogField);
        setJSONCacheText(JSONcacheField);
        setLoginText(loggedinField);
        setLoginButtonState();

        // developer mode switch listener
        developerModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(SETTING_DEVELOPER_MODE, isChecked);
            editor.apply();

            // Reload fragment
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.mainContainerLayout, new SettingsMainFragment());
            fragmentTransaction.commit();

        });

        // Catalog listener
        catalogButton.setOnClickListener(v -> {

            // Clear catalog
            ProgramList.getInstance().clear();

            // Clear cached copy of catalog URLs
            HTTPClient.clearCatalogCache(getActivity().getCacheDir(),
                    getString(R.string.service_catalog_catalog_url),
                    getString(R.string.service_catalog_favorites_url)
            );

            // Update text fields with new status
            setCatalogText(catalogField);
            setJSONCacheText(JSONcacheField);

            catalogLoaded = false;
            seriesLoaded = false;
            favoritesLoaded = false;
            setCatalogButtonState();

            getActivity().startService(catalogIntent);

        });

        // JSON cache listener
        cacheButton.setOnClickListener(v -> {
            HTTPClient.clearCache(getActivity().getCacheDir());
            setJSONCacheText(JSONcacheField);
        });

        // Login/logout listener
        loginButton.setOnClickListener(v -> {

            SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE).edit();
            boolean isAuthenticated = prefs.getBoolean(AuthService.COMPLETED_AUTHENTICATION, false);

            if(isAuthenticated) {

                // Clear catalog
                ProgramList.getInstance().clear();

                // Cleare Resume Points
                ResumePointList.getInstance().clear();

                // Clear continue watching
                VideoContinueWatchingList.getInstance().clear();

                // Clear Watch Later
                VideoWatchLaterList.getInstance().clear();

                // Clear all caches
                HTTPClient.clearCache(getActivity().getCacheDir());

                // Unset all shared pref keys
                editor.putBoolean(AuthService.COMPLETED_AUTHENTICATION, false);
                editor.remove("firstName");
                editor.remove("lastName");
                editor.remove("X-VRT-Token");
                editor.remove("vrtlogin-at");
                editor.remove("vrtlogin-rt");
                editor.remove("vrtlogin-expiry");
                editor.remove(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS);
                editor.remove(VrtPlayerTokenService.VRTPLAYERTOKEN_ANONYMOUS_EXPIRY);
                editor.remove(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED);
                editor.remove(VrtPlayerTokenService.VRTPLAYERTOKEN_AUTHENTICATED_EXPIRY);
                editor.apply();

            } else {

                // Start loginActivity
                startActivity(new Intent(getActivity(), LoginActivity.class));

            }

            // Update text fields with new status
            setCatalogText(catalogField);
            setJSONCacheText(JSONcacheField);
            setLoginText(loggedinField);
            setLoginButtonState();
        });

    }

    private void setCatalogText(TextView field) {
        if(field == null) { return; }

        ProgramList programList = ProgramList.getInstance();
        field.setText(programList.getPrograms().size() + " programs, " +
                programList.getFavoritesCount() + " favorites, " +
                programList.getSeriesCount() + " series"
        );
    }

    private void setCatalogButtonState() {
        // Do not use setVisibility on the button as it makes it lose focus
        if(catalogLoaded && seriesLoaded && favoritesLoaded) {
            catalogButton.setText("Refresh");
            catalogButton.setBackground(getResources().getDrawable(R.drawable.button_default, null));
            catalogProgressBar.setVisibility(View.INVISIBLE);
        } else {
            catalogButton.setText("");
            catalogButton.setBackground(null);
            catalogProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void setDeveloperMode() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        boolean developerMode = prefs.getBoolean(SETTING_DEVELOPER_MODE, false);
        if(developerModeSwitch != null) {
            developerModeSwitch.setChecked(developerMode);
        }

        if(developerMode) {
            catalogContainer.setVisibility(View.VISIBLE);
            jsonCacheContainer.setVisibility(View.VISIBLE);
        } else {
            catalogContainer.setVisibility(View.GONE);
            jsonCacheContainer.setVisibility(View.GONE);
        }

    }


    private void setLoginButtonState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        boolean isAuthenticated = prefs.getBoolean(AuthService.COMPLETED_AUTHENTICATION, false);

        if (isAuthenticated) {
            loginButton.setText("Logout");
        } else {
            loginButton.setText("Login");
        }
    }

    private void setJSONCacheText(TextView field) {
        if(field == null) { return; }
        field.setText(HTTPClient.getCacheStatistics(getActivity().getCacheDir()));
    }

    private void setLoginText(TextView field) {
        if(field == null) { return; }
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        field.setText(prefs.getString("firstName", "None")
                + " "
                + prefs.getString("lastName","")
        );
    }

    // Prepare intents to repopulate Catalog + Series + Favorites
    private void prepareIntents() {

        catalogIntent = new Intent(getActivity(), CatalogService.class);
        catalogIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    setCatalogText(catalogField);
                    setJSONCacheText(JSONcacheField);
                    getActivity().startService(seriesIntent);
                    getActivity().startService(accessTokenIntent);
                }

                catalogLoaded = true;
                setCatalogButtonState();
            }
        });

        seriesIntent = new Intent(getActivity(), SeriesService.class);
        seriesIntent.putExtra(SeriesService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    setCatalogText(catalogField);
                    setJSONCacheText(JSONcacheField);
                }

                seriesLoaded = true;
                setCatalogButtonState();
            }
        });

        accessTokenIntent = new Intent(getActivity(), AccessTokenService.class);
        accessTokenIntent.putExtra(AccessTokenService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == Activity.RESULT_OK) {
                    xvrttoken = resultData.getString("X-VRT-Token", "");
                    getActivity().startService(favoritesIntent);
                } else {
                    // user not logged in
                    favoritesLoaded = true;
                    setCatalogButtonState();
                }
            }
        });

        favoritesIntent = new Intent(getActivity(), FavoriteService.class);
        favoritesIntent.putExtra("ACTION", FavoriteService.ACTION_GET);
        favoritesIntent.putExtra("X-VRT-Token", xvrttoken);
        favoritesIntent.putExtra(FavoriteService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);

                // show messages, if any
                if (resultData.getString("MSG", "").length() > 0) {
                    Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                }

                if (resultCode == Activity.RESULT_OK) {
                    setCatalogText(catalogField);
                    setJSONCacheText(JSONcacheField);
                }

                favoritesLoaded = true;
                setCatalogButtonState();
            }
        });

    }

}