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
 * Main Catalog Fragment to inflate channel checkboxes + VerticalGridSupportFragment
 */


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.ProgramList;

public class CatalogMainFragment extends Fragment {

    private static final String TAG = "CatalogMainFragment";
    private List<String> selectedBrands = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog_main, container, false);
    }

    public void notifyDataReady() {
        if(getFragmentManager() != null) {
            for (Fragment fragment : getFragmentManager().getFragments()) {
                if (fragment instanceof OnDemandFragment) {
                    ((OnDemandFragment) fragment).hideProgressBar();
                }
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ProgramList programList = ProgramList.getInstance();
        LinearLayout checkBoxView = view.findViewById(R.id.brandCheckBoxes);

        if(checkBoxView != null) {
            // Add list of all brands (channels)
            for (String brand : programList.getBrands()) {
                FrameLayout brandCheckbox = createBrandCheckbox(brand);
                if (brandCheckbox == null) {
                    continue;
                }
                checkBoxView.addView(brandCheckbox);
            }
        }

    }

    private FrameLayout createBrandCheckbox(String brand) {

        // Create FrameLayout
        FrameLayout frameLayout = new FrameLayout(getContext());
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                150, ViewGroup.LayoutParams.MATCH_PARENT));
        frameLayout.setPadding(5, 5, 5, 5);

        ImageButton imageButton = new ImageButton(getContext());

        int resourceID = getContext().getResources().getIdentifier(
                "ic_" + brand.replaceAll("-", ""),
                "drawable", getContext().getPackageName());

        // Only add Brands for which we have a logo
        if(resourceID <= 0) {
            return null;
        }

        imageButton.setBackground(getResources().getDrawable(R.drawable.button_border_bottom, null));
        imageButton.setScaleType(ImageView.ScaleType.CENTER);
        imageButton.setAdjustViewBounds(true);
        imageButton.setImageResource(resourceID);

        // slight abuse but whatever. How hard is it again to pass a simple String? DONOTWANT
        imageButton.setContentDescription(brand);

        // focus Catalog submenu button on up
        imageButton.setNextFocusUpId(R.id.buttonSubOnDemandCatalog);

        imageButton.setOnClickListener(view -> {
            String selectedBrand = imageButton.getContentDescription().toString();

            if(selectedBrands.contains(selectedBrand)) {
                selectedBrands.remove(selectedBrand);
                imageButton.setBackground(getResources().getDrawable(R.drawable.button_border_bottom, null));
            } else {
                selectedBrands.add(selectedBrand);
                imageButton.setBackground(getResources().getDrawable(R.drawable.button_border_bottom_selected, null));
            }

            notifyChildFragment();
        });

        frameLayout.addView(imageButton);

        return frameLayout;
    }

    public void notifyChildFragment() {

        for(Fragment fragment : getChildFragmentManager().getFragments()) {
            if(fragment instanceof CatalogFragment) {
                ((CatalogFragment) fragment).setSelectedBrands(selectedBrands);
            }
        }

    }


}