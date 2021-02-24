package be.lorang.nuplayer.model;

import com.google.gson.annotations.SerializedName;

/*
 * Describes a ResumePoint
 */
public class ResumePoint {

    @SerializedName("created") private long created;
    @SerializedName("updated") private long updated;
    @SerializedName("url") private String url;
    @SerializedName("watchLater") private boolean watchLater;
    @SerializedName("position") private double position;
    @SerializedName("total") private double total;
    @SerializedName("progress") private double progress;

    public ResumePoint(long created, long updated, String url, double position, double total, double progress) {
        this.created = created;
        this.updated = updated;
        this.url = url;
        this.position = position;
        this.total = total;
        this.progress = progress;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isWatchLater() {
        return watchLater;
    }

    public void setWatchLater(boolean watchLater) {
        this.watchLater = watchLater;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}
