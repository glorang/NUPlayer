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

import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.VerticalGridView;

/*
 * Class to implement top padding (340) in Program fragment
 */

public class CustomVerticalGridPresenter extends VerticalGridPresenter {

    VerticalGridView gridView;

    public CustomVerticalGridPresenter(int zoom, boolean val){
        super(zoom, val);
    }

    @Override
    protected void initializeGridViewHolder(ViewHolder vh) {
        super.initializeGridViewHolder(vh);
        gridView = vh.getGridView();
        gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_HIGH_EDGE);
        gridView.setWindowAlignmentOffset(340);
        int top = gridView.getPaddingTop();
        int bottom = gridView.getPaddingBottom();
        int right = gridView.getPaddingRight();
        int left = gridView.getPaddingLeft();
        gridView.setPadding(left,top,right,bottom);
    }
}