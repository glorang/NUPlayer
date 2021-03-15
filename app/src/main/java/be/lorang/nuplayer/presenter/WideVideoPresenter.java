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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;
import be.lorang.nuplayer.ui.VideoLongPressListener;
import be.lorang.nuplayer.utils.Utils;

/*
 *
 * Card presenter for Videos in a Program, it is used in a grid view with only 1 column
 * The cards are made very wide in the layout's XML
 *
 */

public class WideVideoPresenter<T extends BaseCardView> extends BaseCardPresenter {

    private static final String TAG = "WideVideoPresenter";
    public enum CardType { LATEST, PROGRAM };
    private CardType cardType;

    public WideVideoPresenter(Context context, CardType cardType) {
        super(context, R.layout.card_video_wide);
        this.cardType = cardType;
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Video video = (Video) item;

        // Set long press listener on card
        cardView.setOnLongClickListener(new VideoLongPressListener(getContext(), video));

        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (video.getThumbnail() != null) {
            int width = (int) getContext().getResources()
                    .getDimension(R.dimen.program_width);
            int height = (int) getContext().getResources()
                    .getDimension(R.dimen.program_height);

            RequestOptions myOptions = new RequestOptions()
                    .override(width, height);
            Glide.with(getContext())
                    .asBitmap()
                    .load(video.getThumbnail())
                    .apply(myOptions)
                    .into(imageView);
        }

        String title = video.getTitle();

        // Overwrite title in Latest Fragment with Program name instead of Video name
        if(cardType.equals(CardType.LATEST)) {
            title = video.getProgram();
        }

        TextView titleTextView = cardView.findViewById(R.id.videoWideTitle);
        titleTextView.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT));

        TextView primaryTextView = cardView.findViewById(R.id.videoWidePrimaryText);
        primaryTextView.setText(Html.fromHtml(video.getDescription(), Html.FROM_HTML_MODE_COMPACT));

        TextView secondaryTextView = cardView.findViewById(R.id.videoWideSecondaryText);
        StringBuilder stringBuilder = new StringBuilder();

        if(video.getSeasonTitle().length() > 0) {
            stringBuilder.append(getContext().getString(R.string.season) + " " +
                    video.getSeasonTitle() + " - ");
        }

        if(video.getEpisodeNumber() >= 0) {
            stringBuilder.append(getContext().getString(R.string.episode) + " " +
                    video.getEpisodeNumber() + System.lineSeparator());
        }

        if(video.getDuration() <= 60) {
            stringBuilder.append(getContext().getString(R.string.runtime) + ": " +
                    getContext().getString(R.string.runtime_one_minute) +
                    System.lineSeparator());
        }else if(video.getDuration() > 60) {
            stringBuilder.append(getContext().getString(R.string.runtime) + ": " +
                    (video.getDuration() / 60) + " " +
                    getContext().getString(R.string.runtime_minutes) +
                    System.lineSeparator());
        }

        if(video.getFormattedBroadcastDate().length() > 0) {
            stringBuilder.append(getContext().getString(R.string.airdate) + " : " +
                    video.getFormattedBroadcastDate() +
                    System.lineSeparator());
        }

        // set brand image in "Latest" fragment
        ImageView brandImageView = cardView.findViewById(R.id.brand_image);
        if (brandImageView != null) {
            int resourceID = getContext().getResources().getIdentifier(
                    "ic_" + video.getBrand().replaceAll("-",""),
                    "drawable", getContext().getPackageName());
            if (resourceID > 0 && cardType.equals(CardType.LATEST)) {
                brandImageView.setImageResource(resourceID);
                brandImageView.setVisibility(View.VISIBLE);
            } else {
                brandImageView.setVisibility(View.GONE);
            }
        }

        // set progress
        ProgressBar progressBar = cardView.findViewById(R.id.progressBarVideoWide);
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();
        if(video.getProgressPct() > 0) {
            Log.d(TAG, "Setting progress for video " + video.getTitle() + " to = " + video.getProgressPct());
            progressBar.setProgress(video.getProgressPct());
            params.height = 8;

        } else {
            params.height = 0;
        }
        progressBar.setLayoutParams(params);
        secondaryTextView.setText(stringBuilder.toString());
    }

}