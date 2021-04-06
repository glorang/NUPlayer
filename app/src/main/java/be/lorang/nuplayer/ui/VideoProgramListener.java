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

package be.lorang.nuplayer.ui;

import android.app.Activity;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.Video;

/*
 * Helper class to implement OnItemViewClickedListener when a Video or Program is selected
 */
public class VideoProgramListener extends VideoProgramBaseListener implements OnItemViewClickedListener {

    private static String TAG = "VideoProgramListener";

    public VideoProgramListener(Activity activity) {
        super(activity);
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof Video) {

            Video video = (Video) item;
            startVideoIntent(video);

        }else if(item instanceof Program) {

            Program program = (Program) item;
            startProgramIntent(program);
        }

    }


}
