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

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import be.lorang.nuplayer.R;

/*
 *  Helper class to make a horizontal menu, used for both main menu as sub menu
 */

public class HorizontalMenuFragment extends Fragment {

    private static final String TAG = "HorizontalMenuFragment";
    private Button selectedMenuButton = null;
    private Fragment selectedFragment = null;
    private Fragment loadedFragment = null;
    private LinearLayout contentContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_horizontal_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup onFocusChangeListener so we always return to last selected button
        // Extending class should call setSelectedMenuButton
        FrameLayout subMenuNavigationOverlay = view.findViewById(R.id.menuNavigationOverlay);
        if(subMenuNavigationOverlay != null) {
            subMenuNavigationOverlay.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus) {
                    //Log.d(TAG, "View " + view + " has focus");
                    if (selectedMenuButton != null) {
                        selectedMenuButton.requestFocus();
                    }
                }
            });
        }
    }

    public void setSelectedMenuButton(Button selectedMenuButton) {
        this.selectedMenuButton = selectedMenuButton;
    }

    public Button getSelectedMenuButton() {
        return selectedMenuButton;
    }

    public Fragment getSelectedFragment() {
        return selectedFragment;
    }

    public void setSelectedFragment(Fragment selectedFragment) {
        this.selectedFragment = selectedFragment;
    }

    public Fragment getLoadedFragment() {
        return loadedFragment;
    }

    public void setLoadedFragment(Fragment loadedFragment) {
        this.loadedFragment = loadedFragment;
    }

    public void loadFragment(int container) {
        if(getSelectedFragment() != null && getSelectedFragment() != getLoadedFragment()) {
            //Log.d(TAG, "Loading new fragment  " + selectedFragment);
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentInnerTransaction = fragmentManager.beginTransaction();
            fragmentInnerTransaction.replace(container, getSelectedFragment());
            fragmentInnerTransaction.commit();
            setLoadedFragment(getSelectedFragment());
        }
    }

    public Button createMenuButton(String title) {
        Button button = new Button(getActivity());
        button.setText(title);
        button.setAllCaps(true);
        button.setBackground(getResources().getDrawable(R.drawable.button_border_bottom, null));
        button.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        return button;
    }

    public Button createMenuButton(String title, int drawable, int width) {
        Button button = createMenuButton(title);
        button.setWidth(width);
        button.setGravity(Gravity.CENTER);
        button.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        button.setCompoundDrawablePadding(0);
        return button;
    }

    public void setContentContainer(LinearLayout contentContainer) {
        this.contentContainer = contentContainer;
    }

    public void showProgressBar() {
        if(getView() != null) {
            ProgressBar loadingProgressBar = getView().findViewById(R.id.loadingProgressBar);

            if(loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            if(contentContainer != null) {
                contentContainer.setVisibility(View.GONE);
            }
        }
    }

    public void hideProgressBar() {
        if(getView() != null) {
            ProgressBar loadingProgressBar = getView().findViewById(R.id.loadingProgressBar);

            if(loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }

            if(contentContainer != null) {
                contentContainer.setVisibility(View.VISIBLE);
            }

        }
    }

}
