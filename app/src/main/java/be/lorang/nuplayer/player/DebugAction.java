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

package be.lorang.nuplayer.player;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.leanback.widget.PlaybackControlsRow;

import be.lorang.nuplayer.R;

/*
 * Custom PlaybackControlsRow.MultiAction with "Info" icon. Used to toggle debug overlay in VideoMediaPlayerGlue
 */

public class DebugAction extends PlaybackControlsRow.MultiAction {

    public static final int INDEX_OFF = 0;
    public static final int INDEX_ON = 1;

    static int getIconHighlightColor(Context context) {
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.playbackControlsIconHighlightColor,
                outValue, true)) {
            return outValue.data;
        }
        return context.getResources().getColor(R.color.lb_playback_icon_highlight_no_theme, null);
    }

    static Drawable coloredDrawable(Drawable drawable, int highlightColor) {
        Drawable drawableCopy = drawable.getConstantState().newDrawable().mutate();
        drawableCopy.setColorFilter(new PorterDuffColorFilter(highlightColor, PorterDuff.Mode.SRC_ATOP));
        return drawableCopy;
    }

    public DebugAction(Context context) {
        this(context, getIconHighlightColor(context));
    }

    public DebugAction(Context context, int highlightColor) {
        super(R.id.buttonDebugAction);

        Drawable uncoloredDrawable = context.getResources().getDrawable(R.drawable.ic_baseline_info, null);

        Drawable[] drawables = new Drawable[2];
        drawables[INDEX_OFF] = uncoloredDrawable;
        drawables[INDEX_ON] = coloredDrawable(uncoloredDrawable, highlightColor);

        setDrawables(drawables);
    }
}