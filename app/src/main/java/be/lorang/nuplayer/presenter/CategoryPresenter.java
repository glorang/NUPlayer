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

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Category;

/*
 * Card presenter for Categories in Category Fragment
 */

public class CategoryPresenter<T extends BaseCardView> extends BaseCardPresenter {

    public CategoryPresenter(Context context) {
        super(context, R.layout.card_category);
    }

    public void onBindViewHolder(Object item, BaseCardView cardView) {
        Category category = (Category)item;

        // set background image
        ImageView imageView = cardView.findViewById(R.id.main_image);
        if (category.getThumbnail() != null) {

            Glide.with(getContext())
                    .asBitmap()
                    .load(category.getThumbnail())
                    .into(imageView);
        }

        // set title
        TextView textViewTitle = cardView.findViewById(R.id.programTitle);
        textViewTitle.setText(Html.fromHtml(category.getTitle(), Html.FROM_HTML_MODE_COMPACT));
    }

}
