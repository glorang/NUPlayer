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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;

/*
 * Card presenter for Live TV on Home Fragment
 *
 */
public class LiveTVPresenter<T extends BaseCardView> extends BaseCardPresenter {

    public LiveTVPresenter(Context context) {
        super(context, R.layout.card_livetv);
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Video video = (Video) item;

        // set background image
        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (video.getThumbnail() != null) {
            int width = (int) getContext().getResources()
                    .getDimension(R.dimen.livetv_width);
            int height = (int) getContext().getResources()
                    .getDimension(R.dimen.livetv_height);

            // Don't cache live tv screenshots
            RequestOptions options = new RequestOptions();
            options.signature(new ObjectKey(String.valueOf(System.currentTimeMillis())));
            options.override(width, height);

            Glide.with(getContext())
                    .asBitmap()
                    .load(video.getThumbnail())
                    .apply(options)
                    .into(imageView);
        }

        // set brand image
        ImageView brandImageView = cardView.findViewById(R.id.brand_image);
        if (brandImageView != null && video.getBrand() != null) {
            int resourceID = getContext().getResources().getIdentifier(
                    "ic_" + video.getBrand().replaceAll("-",""),
                    "drawable", getContext().getPackageName());
            if (resourceID > 0) {
                brandImageView.setImageResource(resourceID);
                brandImageView.setVisibility(View.VISIBLE);
            } else {
                brandImageView.setVisibility(View.GONE);
            }
        }

        // set title
        TextView textViewTitle = cardView.findViewById(R.id.liveTVChannel);
        textViewTitle.setText(video.getTitle());

        // set description
        TextView textViewDescription = cardView.findViewById(R.id.liveTVDescription);
        textViewDescription.setText(Html.fromHtml(video.getDescription(), Html.FROM_HTML_MODE_COMPACT));

    }

}
