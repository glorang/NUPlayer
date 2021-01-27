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

/*
 * Base Card presenter, code shared between all Card Presenters, used to set colors etc.
 *
 * Hacked together from SideInfoCardPresenter and AbstractCardPresenter from Leanback showcase app
 */

public abstract class BaseCardPresenter<T extends BaseCardView> extends Presenter {

    private static final String TAG = "BasePresenter";
    private Context context;
    private int cardLayout;
    private int selectedBackgroundColor = -1;
    private int selectedForegroundColor = -1;
    private int defaultBackgroundColor = -1;
    private int defaultForegroundColor = -1;
    private Drawable defaultCardImage;

    public BaseCardPresenter(Context context, int cardLayout) {
        this.context = context;
        this.cardLayout = cardLayout;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {

        defaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_black_tint_1);
        defaultForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        selectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_blue);
        selectedForegroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.vrtnu_white);
        defaultCardImage = parent.getResources().getDrawable(R.drawable.default_background, null);

        BaseCardView cardView = onCreateView();
        return new ViewHolder(cardView);
    }

    protected BaseCardView onCreateView() {
        final BaseCardView cardView = new BaseCardView(getContext(), null, R.style.BaseCardStyle) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };
        cardView.setFocusable(true);
        cardView.addView(LayoutInflater.from(getContext()).inflate(cardLayout, null));
        return cardView;
    }

    private void updateCardBackgroundColor(BaseCardView view, boolean selected) {
        int backgroundColor = selected ? selectedBackgroundColor : defaultBackgroundColor;
        view.setBackgroundColor(backgroundColor);
        view.findViewById(R.id.info).setBackgroundColor(backgroundColor);
    }

    @Override
    public final void onBindViewHolder(ViewHolder viewHolder, Object item) {
        onBindViewHolder(item, (T) viewHolder.view);
    }

    @Override
    public final void onUnbindViewHolder(ViewHolder viewHolder) {
        onUnbindViewHolder((T) viewHolder.view);
    }

    public void onUnbindViewHolder(T cardView) {
        // Nothing to clean up. Override if necessary.
    }

    public abstract void onBindViewHolder(Object item, BaseCardView cardView);
}