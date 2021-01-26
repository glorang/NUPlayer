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

package be.lorang.nuplayer.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

/**
 * Describe a Video, multiple Videos are stored in the VideoList forming a Program
 */
public class Video {

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("seasonName") private String mSeasonName = "";
    @SerializedName("episodeNumber") private int mEpisodeNumber = 0;
    @SerializedName("duration") private int mDuration = 0;
    @SerializedName("thumbnail") private String mThumbnail = "";
    @SerializedName("videoId") private String mVideoId = "";
    @SerializedName("pubId") private String mPubId = "";
    @SerializedName("formattedBroadcastDate") private String mFormattedBroadcastDate = "";
    @SerializedName("formattedBroadcastShortDate") private String mFormattedBroadcastShortDate = "";
    @SerializedName("brand") private String mBrand = "";
    @SerializedName("program") private String mProgram = "";
    @SerializedName("assetPath") private String mAssetPath = "";
    @SerializedName("imageServer") private String mImageServer = "";
    @SerializedName("streamType") private String mStreamType = "";
    @SerializedName("progressPct") private int mProgressPct = 0;
    @SerializedName("validImageSizes") private static String[] mValidImageSizes = {"w160hx", "w320hx", "w640hx", "w1280hx", "w1600hx", "w1920hx", "VV_4x3_120", "VV_4x3_240", "VV_4x3_480"};

    public Video(String title,
                 String description,
                 String seasonName,
                 int episodeNumber,
                 int duration,
                 String thumbnail,
                 String videoId,
                 String pubId,
                 String formattedBroadcastDate,
                 String formattedBroadcastShortDate,
                 String brand,
                 String program,
                 String assetPath,
                 String imageServer,
                 String streamType) {
        mTitle = title;
        mDescription = description;
        mSeasonName = seasonName;
        mEpisodeNumber = episodeNumber;
        mDuration = duration;
        mThumbnail = thumbnail;
        mVideoId = videoId;
        mPubId = pubId;
        mFormattedBroadcastDate = formattedBroadcastDate;
        mFormattedBroadcastShortDate = formattedBroadcastShortDate;
        mBrand = brand;
        mProgram = program;
        mAssetPath = assetPath;
        mImageServer = imageServer;
        mStreamType = streamType;

    }

    public String getTitle() {
        return mTitle;
    }
    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }
    public void setDescription(String description) {
        mDescription = description;
    }

    public String getSeasonName() { return mSeasonName; }
    public void setSeasonName(String seasonName) { mSeasonName = seasonName; }

    public int getEpisodeNumber() { return mEpisodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { mEpisodeNumber = episodeNumber; }

    public int getDuration() {
        return mDuration;
    }
    public void setDuration(int duration) {
        mDuration = duration;
    }

    // by default we return the "wide" 320px thumbnail
    public String getThumbnail() {
        return getThumbnail("w320hx");
    }

    public String getThumbnail(String size) {

        if(Arrays.stream(mValidImageSizes).noneMatch(size::equals)) {
            Log.d("Program", size + " is an invalid thumbnail size");
            return "";
        }

        // FIXME: hack for live tv screenshots
        if(mThumbnail.startsWith("https://vrtnu-api.vrt.be")) {
            return mThumbnail;
        } else if(mThumbnail.length() > 0){
            return mImageServer + "/" + size + "/" + mThumbnail;
        } else {
            return "";
        }
    }

    public void setThumbnail(String thumbnail) { mThumbnail  = thumbnail; }

    public String getVideoId() { return mVideoId; }
    public void setVideoId(String videoId) { mVideoId = videoId; }

    public String getPubId() { return mPubId; }
    public void setPubId(String pubId) { mPubId = pubId; }

    public String getFormattedBroadcastDate() { return mFormattedBroadcastDate; }
    public void setFormattedBroadcastDate(String formattedBroadcastDate) { mFormattedBroadcastDate = formattedBroadcastDate; }

    public String getFormattedBroadcastShortDate() { return mFormattedBroadcastShortDate; }
    public void setFormattedBroadcastShortDate(String formattedBroadcastShortDate) { mFormattedBroadcastShortDate = formattedBroadcastShortDate; }

    public String getBrand() { return mBrand; }
    public void setBrand(String brand) { mBrand = brand; }

    public String getProgram() { return mProgram; }
    public void setProgram(String program) { mProgram = program; }

    public String getAssetPath() { return mAssetPath; }
    public void setAssetPath(String assetPath) { mAssetPath = assetPath; }

    public String getStreamType() { return mStreamType; }
    public void setStreamType(String streamType) { mStreamType = streamType; }

    public int getProgressPct() { return mProgressPct; }
    public void setProgressPct(int progress) { mProgressPct = progress; }

}
