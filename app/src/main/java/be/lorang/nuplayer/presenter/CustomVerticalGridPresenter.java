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
 * Class to implement either top padding or an offset
 */

public class CustomVerticalGridPresenter extends VerticalGridPresenter {

    private static final String TAG = "CustomVerticalGridPresenter";
    VerticalGridView gridView;

    private int offset = 0;
    private int paddingTop = 0;

    public CustomVerticalGridPresenter(int zoom, boolean val){
        super(zoom, val);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    @Override
    protected void initializeGridViewHolder(ViewHolder vh) {
        super.initializeGridViewHolder(vh);
        gridView = vh.getGridView();

        if(offset > 0) {
            gridView.setWindowAlignment(BaseGridView.WINDOW_ALIGN_HIGH_EDGE);
            gridView.setWindowAlignmentOffset(offset);
        }
        int top = paddingTop > 0 ? paddingTop : gridView.getPaddingTop();
        int bottom = gridView.getPaddingBottom();
        int right = gridView.getPaddingRight();
        int left = gridView.getPaddingLeft();
        gridView.setPadding(left,top,right,bottom);
    }
}