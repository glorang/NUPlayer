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
    @SerializedName("subTitle") private String mSubTitle = "";
    @SerializedName("seasonName") private String mSeasonName = "";
    @SerializedName("seasonTitle") private String mSeasonTitle = "";
    @SerializedName("episodeNumber") private int mEpisodeNumber = 0;
    @SerializedName("duration") private int mDuration = 0;
    @SerializedName("thumbnail") private String mThumbnail = "";
    @SerializedName("videoId") private String mVideoId = "";
    @SerializedName("pubId") private String mPubId = "";
    @SerializedName("brand") private String mBrand = "";
    @SerializedName("programName") private String mProgramName = "";
    @SerializedName("programTitle") private String mProgramTitle = "";
    @SerializedName("assetPath") private String mAssetPath = "";
    @SerializedName("url") private String mUrl = "";
    @SerializedName("whatsonId") private String mWhatsonId = "";
    @SerializedName("programWhatsonId") private String mProgramWhatsonId = "";
    @SerializedName("allowedRegion") private String mAllowedRegion = "";
    @SerializedName("onTime") private String mOnTime = "";
    @SerializedName("offTime") private String mOffTime = "";
    @SerializedName("imageServer") private String mImageServer = "";
    @SerializedName("streamType") private String mStreamType = "";
    @SerializedName("progressPct") private int mProgressPct = 0;
    @SerializedName("currentPosition") private int mCurrentPosition = 0;
    @SerializedName("validImageSizes") private static String[] mValidImageSizes = {"w160hx", "w320hx", "w640hx", "w1280hx", "w1600hx", "w1920hx", "VV_4x3_120", "VV_4x3_240", "VV_4x3_480"};

    public Video() { }

    public Video(String title,
                 String subTitle,
                 String seasonName,
                 String seasonTitle,
                 int episodeNumber,
                 int duration,
                 String thumbnail,
                 String videoId,
                 String pubId,
                 String brand,
                 String programName,
                 String programTitle,
                 String assetPath,
                 String url,
                 String whatsonId,
                 String programWhatsonId,
                 String allowedRegion,
                 String onTime,
                 String offTime,
                 String imageServer,
                 String streamType) {
        mTitle = title;
        mSubTitle = subTitle;
        mSeasonName = seasonName;
        mSeasonTitle = seasonTitle;
        mEpisodeNumber = episodeNumber;
        mDuration = duration;
        mThumbnail = thumbnail;
        mVideoId = videoId;
        mPubId = pubId;
        mBrand = brand;
        mProgramName = programName;
        mProgramTitle = programTitle;
        mAssetPath = assetPath;
        mUrl = url;
        mWhatsonId = whatsonId;
        mProgramWhatsonId = programWhatsonId;
        mAllowedRegion = allowedRegion;
        mOnTime = onTime;
        mOffTime = offTime;
        mImageServer = imageServer;
        mStreamType = streamType;
    }

    public String getTitle() {
        return mTitle;
    }
    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSubTitle() {
        return mSubTitle;
    }
    public void setSubTitle(String subTitle) {
        mSubTitle = subTitle;
    }

    public String getSeasonName() { return mSeasonName; }
    public void setSeasonName(String seasonName) { mSeasonName = seasonName; }

    public String getSeasonTitle() { return mSeasonTitle; }
    public void setSeasonTitle(String seasonTitle) { mSeasonTitle = seasonTitle; }

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

        // Live TV screenshots are not on images.vrt.be, return thumbnail immediately
        if(mThumbnail.startsWith("https://www.vrt.be/vrtnu-static/screenshots")) {
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

    public String getBrand() { return mBrand; }
    public void setBrand(String brand) { mBrand = brand; }

    public String getProgramName() { return mProgramName; }
    public void setProgramName(String programName) { mProgramName = programName; }

    public String getProgramTitle() { return mProgramTitle; }
    public void setmProgramTitle(String programTitle) { mProgramTitle = programTitle; }

    public String getAssetPath() { return mAssetPath; }
    public void setAssetPath(String assetPath) { mAssetPath = assetPath; }

    public String getURL() { return mUrl; }
    public void setURL(String url) { mUrl = url; }

    public String getWhatsonId() { return mWhatsonId; }
    public void setWhatsonId(String whatsonId) { mWhatsonId = whatsonId; }

    public String getProgramWhatsonId() { return mProgramWhatsonId; }
    public void setProgramWhatsonId(String programWhatsonId) { mProgramWhatsonId = programWhatsonId; }

    public String getAllowedRegion() { return mAllowedRegion; }
    public void setAllowedRegion(String allowedRegion) { mAllowedRegion = allowedRegion; }

    public String getOnTime() { return mOnTime; }
    public void setOnTime(String onTime) { mOnTime = onTime; }

    public String getOffTime() { return mOffTime; }
    public void setOffTime(String offTime) { mOffTime = offTime; }

    public String getStreamType() { return mStreamType; }
    public void setStreamType(String streamType) { mStreamType = streamType; }

    public int getProgressPct() { return mProgressPct; }
    public void setProgressPct(int progress) { mProgressPct = progress; }

    public int getCurrentPosition() { return mCurrentPosition; }
    public void setCurrentPosition(int position) { mCurrentPosition = position; }
}
