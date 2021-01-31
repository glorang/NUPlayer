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

package be.lorang.nuplayer.presenter;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Program;

/*
 * Card presenter for Favorites on Home Fragment
 */

public class FavoritesPresenter<T extends BaseCardView> extends BaseCardPresenter {

    private static final String TAG = "FavoritesPresenter";

    public FavoritesPresenter(Context context) {
        super(context, R.layout.card_favorite);
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Program program = (Program) item;

        // set background image
        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (program.getThumbnail() != null) {
            int width = (int) getContext().getResources()
                    .getDimension(R.dimen.program_width);
            int height = (int) getContext().getResources()
                    .getDimension(R.dimen.program_height);

            RequestOptions myOptions = new RequestOptions()
                    .override(width, height);
            Glide.with(getContext())
                    .asBitmap()
                    .load(program.getThumbnail())
                    .apply(myOptions)
                    .into(imageView);
        }

        // set brand image
        ImageView brandImageView = cardView.findViewById(R.id.brand_image);
        if (brandImageView != null && program.getBrand() != null) {
            int resourceID = getContext().getResources().getIdentifier("ic_" + program.getBrand(), "drawable", getContext().getPackageName());
            if (resourceID > 0) {
                brandImageView.setImageDrawable(getContext().getResources().getDrawable(resourceID, null));
            }
        }

        // set title
        TextView textViewTitle = cardView.findViewById(R.id.favoriteTitle);
        textViewTitle.setText(program.getTitle());
    }

}