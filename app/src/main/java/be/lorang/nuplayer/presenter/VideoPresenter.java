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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Video;

/**
 * This will show list of episode as rows in a grid (max columns = 1) with a very wide length
 * so the description and other info fits in easily
 *
 * Hacked together from SideInfoCardPresenter and AbstractCardPresenter from Leanback showcase app
 */

public class VideoPresenter<T extends BaseCardView> extends Presenter {

    private static final String TAG = "VideoPresenter";
    private Context mContext;
    private int mSelectedBackgroundColor = -1;
    private int mSelectedForegroundColor = -1;
    private int mDefaultBackgroundColor = -1;
    private int mDefaultForegroundColor = -1;
    private Drawable mDefaultCardImage;

    public VideoPresenter(Context context) {
        mContext = context;
    }
    public Context getContext() {
        return mContext;
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {

        mDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_black_tint_2);
        mDefaultForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        mSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_blue);
        mSelectedForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        mDefaultCardImage = parent.getResources().getDrawable(R.drawable.default_background, null);

        BaseCardView cardView = onCreateView();
        return new ViewHolder(cardView);
    }

    protected BaseCardView onCreateView() {
        final BaseCardView cardView = new BaseCardView(getContext(), null, R.style.VideoCardStyle) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };
        cardView.setFocusable(true);
        cardView.addView(LayoutInflater.from(getContext()).inflate(R.layout.video_card, null));
        return cardView;
    }

    private void updateCardBackgroundColor(BaseCardView view, boolean selected) {
        int backgroundcolor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int foregroundcolor = selected ? mSelectedForegroundColor : mDefaultForegroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(backgroundcolor);
        view.findViewById(R.id.info).setBackgroundColor(backgroundcolor);

        // change foreground color
        ((TextView)view.findViewById(R.id.video_title)).setTextColor(foregroundcolor);
        ((TextView)view.findViewById(R.id.primary_text)).setTextColor(foregroundcolor);
        ((TextView)view.findViewById(R.id.secondary_text)).setTextColor(foregroundcolor);

    }

    @Override
    public final void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;
        onBindViewHolder(video, (T) viewHolder.view);
    }

    @Override
    public final void onUnbindViewHolder(ViewHolder viewHolder) {
        onUnbindViewHolder((T) viewHolder.view);
    }

    public void onUnbindViewHolder(T cardView) {
        // Nothing to clean up. Override if necessary.
    }


    public void onBindViewHolder(Video video, BaseCardView cardView) {

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
            int resourceID = getContext().getResources().getIdentifier("ic_" + video.getBrand(), "drawable", getContext().getPackageName());
            if (resourceID > 0) {
                brandImageView.setImageDrawable(getContext().getResources().getDrawable(resourceID, null));
            }
        }

        // set title
        TextView videoTitle = cardView.findViewById(R.id.video_title);
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
                video.getDuration() + " min";

        TextView primaryText = cardView.findViewById(R.id.primary_text);
        primaryText.setText(primaryTextValue);

        // set video description
        TextView secondaryText = cardView.findViewById(R.id.secondary_text);
        secondaryText.setText(video.getDescription());

    }

}