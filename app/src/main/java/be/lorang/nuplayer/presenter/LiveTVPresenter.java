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
import com.bumptech.glide.signature.ObjectKey;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;

public class LiveTVPresenter extends BasePresenter {

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Video video = (Video) item;

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(video.getTitle());
        cardView.setContentText(video.getDescription());

        if (video.getThumbnail() != null) {

            // Set card size from dimension resources.
            Resources res = cardView.getResources();
            int width = res.getDimensionPixelSize(R.dimen.livetv_width);
            int height = res.getDimensionPixelSize(R.dimen.livetv_height);
            cardView.setMainImageDimensions(width, height);

            // Don't cache live tv screenshots
            RequestOptions options = new RequestOptions();
            options.signature(new ObjectKey(String.valueOf(System.currentTimeMillis())));

            Glide.with(cardView.getContext())
                    .load(video.getThumbnail())
                    .apply(options)
                    .apply(RequestOptions.errorOf(getDefaultCardImage()))
                    .into(cardView.getMainImageView());
        }
    }

}
