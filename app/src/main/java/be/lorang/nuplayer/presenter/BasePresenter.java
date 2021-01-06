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

import android.graphics.drawable.Drawable;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.TextView;

import be.lorang.nuplayer.R;

/*
 * Base presenter, used to set colors and size of the Catalog, Programs and Live TV "cards"
 *
 * Presenters should implement onBindViewHolder to cast the passed object to
 * the correct class (Program / Video) and set card image size
 *
 */

public abstract class BasePresenter extends Presenter {

    private int mSelectedBackgroundColor = -1;
    private int mSelectedForegroundColor = -1;
    private int mDefaultBackgroundColor = -1;
    private int mDefaultForegroundColor = -1;
    private Drawable mDefaultCardImage;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_black_tint_2);
        mDefaultForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        mSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_blue);
        mSelectedForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        mDefaultCardImage = parent.getResources().getDrawable(R.drawable.default_background, null);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int backgroundcolor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int foregroundcolor = selected ? mSelectedForegroundColor : mDefaultForegroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(backgroundcolor);
        view.findViewById(R.id.info_field).setBackgroundColor(backgroundcolor);

        // change foreground color
        ((TextView)view.findViewById(R.id.title_text)).setTextColor(foregroundcolor);
        ((TextView)view.findViewById(R.id.content_text)).setTextColor(foregroundcolor);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }

    public Drawable getDefaultCardImage() {
        return mDefaultCardImage;
    }

    @Override
    public abstract void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item);

}

