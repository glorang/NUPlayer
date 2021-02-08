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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;

/*
 * Card presenter for Videos - will set Program Name, episode, run time, description
 * brand logo and progress bar (if set in Video object)
 *
 */

public class VideoPresenter<T extends BaseCardView> extends BaseCardPresenter {

    private static final String TAG = "VideoPresenter";

    public VideoPresenter(Context context) {
        super(context, R.layout.card_video);
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Video video = (Video) item;

        // set background image
        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (video.getThumbnail() != null) {
            int width = (int) getContext().getResources()
                    .getDimension(R.dimen.video_width);
            int height = (int) getContext().getResources()
                    .getDimension(R.dimen.video_height);

            RequestOptions myOptions = new RequestOptions()
                    .override(width, height);
            Glide.with(getContext())
                    .asBitmap()
                    .load(video.getThumbnail())
                    .apply(myOptions)
                    .into(imageView);
        }

        // set brand image
        ImageView brandImageView = cardView.findViewById(R.id.brand_image);
        if (brandImageView != null) {
            int resourceID = getContext().getResources().getIdentifier(
                    "ic_" + video.getBrand().replaceAll("-",""),
                    "drawable", getContext().getPackageName());
            if (resourceID > 0) {
                brandImageView.setImageResource(resourceID);
            }
        }

        // set title
        TextView videoTitle = cardView.findViewById(R.id.videoTitle);
        videoTitle.setText(video.getProgram());

        // set progress (if set)
        ProgressBar progressBar = cardView.findViewById(R.id.progressBar);
        if(video.getProgressPct() > 0) {
            progressBar.setProgress(video.getProgressPct());
        } else {
            progressBar.setAlpha(0);
        }

        // set video info
        String primaryTextValue = video.getFormattedBroadcastShortDate() +
                " - " +
                "episode " + video.getEpisodeNumber() +
                " - " +
                (video.getDuration() / 60) + " min";

        TextView primaryText = cardView.findViewById(R.id.videoPrimaryText);
        primaryText.setText(primaryTextValue);

        // set video description
        TextView secondaryText = cardView.findViewById(R.id.videoSecondaryText);
        secondaryText.setText(Html.fromHtml(video.getDescription(), Html.FROM_HTML_MODE_COMPACT));

    }

}