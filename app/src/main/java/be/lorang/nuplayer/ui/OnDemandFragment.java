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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import be.lorang.nuplayer.R;


/*
 * Fragment that shows all "on-demand" fragments with submenu
 */

public class OnDemandFragment extends HorizontalMenuFragment {

    private static final String TAG = "OnDemandFragment";
    private static final String[] menuItems = {"Latest", "Series", "Categories", "Catalog"};

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout menuNavigationContainer = view.findViewById(R.id.menuNavigationContainer);
        LinearLayout menuContentContainer = view.findViewById(R.id.menuContentContainer);

        for(int i=0;i<menuItems.length;i++) {

            Button button = createMenuButton(menuItems[i]);
            button.setNextFocusUpId(R.id.buttonTopNavOnDemand);

            button.setOnFocusChangeListener((v, hasFocus) -> {

                if(hasFocus) {

                    setSelectedMenuButton(button);

                    // Fragment options
                    String buttonText = button.getText().toString();
                    switch(buttonText) {

                        case "Latest":
                            if(!(getSelectedFragment() instanceof LatestFragment)) {
                                setSelectedFragment(new LatestFragment());
                            }
                            break;
                        case "Series":
                            if(!(getSelectedFragment() instanceof SeriesFragment)) {
                                setSelectedFragment(new SeriesFragment());
                            }
                            break;
                        case "Categories":
                            if(!(getSelectedFragment() instanceof CategoryCategoriesFragment)) {
                                setSelectedFragment(new CategoryCategoriesFragment());
                            }
                            break;
                        case "Catalog":
                            if(!(getSelectedFragment() instanceof CatalogMainFragment)) {
                                setSelectedFragment(new CatalogMainFragment());
                            }
                            break;
                        default:
                            setSelectedFragment(null);
                            break;
                    }

                    if(menuContentContainer != null) {
                        loadFragment(R.id.menuContentContainer);
                    }

                }
            });

            // Set button ids
            switch(menuItems[i]) {
                case "Latest":
                    button.setId(R.id.buttonSubOnDemandLatest);
                    break;
                case "Series":
                    button.setId(R.id.buttonSubOnDemandSeries);
                    break;
                case "Categories":
                    button.setId(R.id.buttonSubOnDemandCategories);
                    break;
                case "Catalog":
                    button.setId(R.id.buttonSubOnDemandCatalog);
                    break;
            }

            if(menuNavigationContainer != null) {
                menuNavigationContainer.addView(button);
            }
        }

        // add default Fragment
        if(menuContentContainer != null) {
            setSelectedFragment(new LatestFragment());
            loadFragment(R.id.menuContentContainer);
        }


    }

}
