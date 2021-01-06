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

import android.content.res.Resources;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Program;

/*
    4:3 format presenter for Series on home fragment
 */

public class SeriesPresenter extends BasePresenter {

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Program program = (Program)item;

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(program.getTitle());
        //cardView.setContentText(program.getBrand());

        String image = null;
        if(program.getAltImage() != null && program.getAltImage().length() > 0) {
            image = program.getAltImage();
        }  else if(program.getThumbnail() != null && program.getThumbnail().length() > 0) {
            image = program.getThumbnail();
        }

        if (image != null) {

            // Set card size from dimension resources.
            Resources res = cardView.getResources();
            int width = res.getDimensionPixelSize(R.dimen.series_width);
            int height = res.getDimensionPixelSize(R.dimen.series_height);
            cardView.setMainImageDimensions(width, height);

            Glide.with(cardView.getContext())
                    .load(image)
                    .apply(RequestOptions.errorOf(getDefaultCardImage()))
                    .into(cardView.getMainImageView());
        }

    }

}
