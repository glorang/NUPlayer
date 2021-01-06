/*
 * Copyright 2021 Geert Lorang
 * Copyright 2016 The Android Open Source Project
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

package be.lorang.nuplayer.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    private static final String TAG = "Utils";

    private HttpsURLConnection urlConnection ;
    private BufferedReader reader;
    private OutputStream writer;
    private CookieManager cookieManager;
    private JSONObject returnObject;
    private int responseCode;
    private String responseMessage;

    public interface MediaDimensions {
        double MEDIA_HEIGHT = 0.95;
        double MEDIA_WIDTH = 0.95;
        double MEDIA_TOP_MARGIN = 0.025;
        double MEDIA_RIGHT_MARGIN = 0.025;
        double MEDIA_BOTTOM_MARGIN = 0.025;
        double MEDIA_LEFT_MARGIN = 0.025;
    }

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    /**
     * Returns the screen/display size.
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // You can get the height & width like such:
        // int width = size.x;
        // int height = size.y;
        return size;
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static float convertPixelsToDp(Context ctx, float px){
        return px / ((float) ctx.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Example for handling resizing content for overscan.  Typically you won't need to resize when
     * using the Leanback support library.
     */
    public void overScan(Activity activity, VideoView videoView) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int w = (int) (metrics.widthPixels * MediaDimensions.MEDIA_WIDTH);
        int h = (int) (metrics.heightPixels * MediaDimensions.MEDIA_HEIGHT);
        int marginLeft = (int) (metrics.widthPixels * MediaDimensions.MEDIA_LEFT_MARGIN);
        int marginTop = (int) (metrics.heightPixels * MediaDimensions.MEDIA_TOP_MARGIN);
        int marginRight = (int) (metrics.widthPixels * MediaDimensions.MEDIA_RIGHT_MARGIN);
        int marginBottom = (int) (metrics.heightPixels * MediaDimensions.MEDIA_BOTTOM_MARGIN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        videoView.setLayoutParams(lp);
    }

    public static long getDuration(String videoUrl) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mmr.setDataSource(videoUrl, new HashMap<>());
        } else {
            mmr.setDataSource(videoUrl);
        }
        return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
    }

    /**
     * Will read the content from a given {@link InputStream} and return it as a {@link String}.
     *
     * @param inputStream The {@link InputStream} which should be read.
     * @return Returns <code>null</code> if the the {@link InputStream} could not be read. Else
     * returns the content of the {@link InputStream} as {@link String}.
     */
    public static String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }
    }

    public static Uri getResourceUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID));
    }

    /* https://stackoverflow.com/a/35250539/5414711 */
    /**
     * @param bitmap The source bitmap.
     * @param opacity a value between 0 (completely transparent) and 255 (completely
     * opaque).
     * @return The opacity-adjusted bitmap.  If the source bitmap is mutable it will be
     * adjusted and returned, otherwise a new bitmap is created.
     */
    public static Bitmap adjustOpacity(Bitmap bitmap, int opacity)
    {
        Bitmap mutableBitmap = bitmap.isMutable()
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }

    // Check if vrtPlayerToken is expired
    public static boolean isVrtPlayerTokenExpired(String expirationDate) {
        if(expirationDate.length() == 0) { return true; }

        //2020-12-22T18:14:39.827Z
        Instant parsedDate = LocalDateTime.parse(expirationDate,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        ).atZone(ZoneId.of("UTC")).toInstant();

        if(Instant.now().compareTo(parsedDate) > 0) {
            return true;
        } else {
            return false;
        }
    }


}
