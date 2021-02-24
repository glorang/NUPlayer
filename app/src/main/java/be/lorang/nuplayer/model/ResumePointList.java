package be.lorang.nuplayer.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a list of ResumePoints
 *
 * Whenever a Program is opened all Videos are matched against this list to set progress
 *
 */
public class ResumePointList {

    // Singleton instance
    private static ResumePointList instance = null;

    final static String TAG = "ResumePointList";
    private List<ResumePoint> resumePoints = new ArrayList<>();

    private ResumePointList() {}

    public static ResumePointList getInstance() {
        if (instance == null) {
            instance = new ResumePointList();
        }
        return instance;
    }

    public List<ResumePoint> getResumePoints() {
        return resumePoints;
    }

    public void clear() {
        resumePoints.clear();
    }

    public void add(ResumePoint resumePoint) {
        resumePoints.add(resumePoint);
    }

    public void remove(ResumePoint resumePoint) {
        resumePoints.remove(resumePoint);
    }

    public boolean setProgress(Video video, int position) {
        int progress = (int) (((double) position / video.getDuration()) * 100);

        for (ResumePoint resumePoint : resumePoints) {
            if(resumePoint.getUrl().equals(video.getURL())) {
                Log.d(TAG, "Setting progress for video " + video.getVideoId() + " to: " + progress);
                resumePoint.setPosition(position);
                resumePoint.setProgress(progress);
                return true;
            }
        }
        return false;
    }

}