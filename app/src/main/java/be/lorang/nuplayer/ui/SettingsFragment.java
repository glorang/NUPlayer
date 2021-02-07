package be.lorang.nuplayer.ui;

/*
 * Settings fragment, used to logout, refresh catalog etc
 */

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.leanback.app.BrowseFragment;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.services.AccessTokenService;
import be.lorang.nuplayer.services.AuthService;
import be.lorang.nuplayer.services.CatalogService;
import be.lorang.nuplayer.services.FavoriteService;
import be.lorang.nuplayer.services.SeriesService;
import be.lorang.nuplayer.services.VrtPlayerTokenService;
import be.lorang.nuplayer.utils.HTTPClient;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {

    private BrowseFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseFragment.MainFragmentAdapter(this);
    private static final String TAG = "SettingsFragment";

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

    private Button catalogButton;
    private Button cacheButton;
    private Button loginButton;

    private ProgressBar catalogProgressBar;

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareIntents();
        getMainFragmentAdapter().getFragmentHost().showTitleView(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View settingsFragment = inflater.inflate(R.layout.fragment_settings, container, false);

        // get form text fields
        catalogField = settingsFragment.findViewById(R.id.valueSettingsCatalog);
        JSONcacheField = settingsFragment.findViewById(R.id.valueSettingsJSONCache);
        loggedinField = settingsFragment.findViewById(R.id.valueSettingsLoggedIn);

        // get form controls
        catalogButton = settingsFragment.findViewById(R.id.buttonSettingsCatalogRefresh);
        catalogProgressBar = settingsFragment.findViewById(R.id.progressBarSettingsCatalogRefresh);
        cacheButton = settingsFragment.findViewById(R.id.buttonSettingsJSONCache);
        loginButton = settingsFragment.findViewById(R.id.buttonSettingsLoggedIn);

        // set initial values
        setCatalogText(catalogField);
        setJSONCacheText(JSONcacheField);
        setLoginText(loggedinField);
        setLoginButtonState();

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

                // Remove catalog caches when logging out (removes user's Favorites)
                HTTPClient.clearCatalogCache(getActivity().getCacheDir(),
                        getString(R.string.service_catalog_catalog_url),
                        getString(R.string.service_catalog_favorites_url)
                );

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

                // We don't know when the login activity finishes nor its result
                // so we finish our current settings activity and return to the main screen
                getActivity().finish();
            }

            // Update text fields with new status
            setCatalogText(catalogField);
            setJSONCacheText(JSONcacheField);
            setLoginText(loggedinField);
            setLoginButtonState();
        });

        return settingsFragment;
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

        if(catalogLoaded && seriesLoaded && favoritesLoaded) {
            //catalogButton.setText("Refresh");
            catalogButton.setVisibility(View.VISIBLE);
            catalogProgressBar.setVisibility(View.INVISIBLE);
        } else {
            //catalogButton.setText("...");
            catalogButton.setVisibility(View.INVISIBLE);
            catalogProgressBar.setVisibility(View.VISIBLE);
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