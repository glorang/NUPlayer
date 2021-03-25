package be.lorang.nuplayer.model;

/*
 * This class represents an entry in the EPG
 */

import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;

public class EPGEntry {

    @SerializedName("channelID") private String channelID;
    @SerializedName("title") private String title;
    @SerializedName("description") private String description;
    @SerializedName("thumbnail") private String thumbnail;
    @SerializedName("imageServer") private String imageServer;
    @SerializedName("startTime") private String startTime;
    @SerializedName("endTime") private String endTime;

    public EPGEntry(String channelID, String title, String description, String image, String imageServer, String startTime, String endTime) {
        this.channelID = channelID;
        this.title = title;
        this.description = description;
        this.thumbnail = image;
        this.imageServer = imageServer;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // by default we return the "wide" 320px thumbnail
    public String getThumbnail() {
        return getThumbnail("w320hx");
    }

    public String getThumbnail(String size) {

        if(thumbnail.length() > 0){
            return imageServer + "/" + size + "/" + thumbnail;
        } else {
            return "";
        }
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getImageServer() {
        return imageServer;
    }

    public void setImageServer(String imageServer) {
        this.imageServer = imageServer;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
