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
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Program;

/*
    Series Card presenter in 4:3 format for Series on home fragment
 */

public class SeriesPresenter<T extends BaseCardView> extends BaseCardPresenter {

    public SeriesPresenter(Context context) {
        super(context, R.layout.card_serie);
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Program program = (Program)item;

        String image = null;
        if(program.getAltImage() != null && program.getAltImage().length() > 0) {
            image = program.getAltImage();
        }  else if(program.getThumbnail() != null && program.getThumbnail().length() > 0) {
            image = program.getThumbnail();
        }

        // set background image
        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (image != null) {
            int width = (int) getContext().getResources()
                    .getDimension(R.dimen.series_width);
            int height = (int) getContext().getResources()
                    .getDimension(R.dimen.series_height);

            RequestOptions myOptions = new RequestOptions()
                    .override(width, height);
            Glide.with(getContext())
                    .asBitmap()
                    .load(image)
                    .apply(myOptions)
                    .into(imageView);
        }

        // set title
        TextView textViewTitle = cardView.findViewById(R.id.serieTitle);
        textViewTitle.setText(program.getTitle());

    }

}
