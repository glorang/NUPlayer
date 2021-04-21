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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
  * Describe a Program, multiple programs are stored in the ProgramList forming the Catalog
  * Each program (can) contain(s) multiple Videos
  */
public class Program {

    private final static String TAG = "Program";

    @SerializedName("title") private String mTitle = "";
    @SerializedName("description") private String mDescription = "";
    @SerializedName("programName") private String mProgramName = "";
    @SerializedName("programType") private String mProgramType = "";
    @SerializedName("programUrl") private String mProgramUrl = "";
    @SerializedName("thumbnail") private String mThumbnail = "";
    @SerializedName("altImage") private String mAltImage = "";
    @SerializedName("brand") private String mBrand = "";
    @SerializedName("imageServer") private String mImageServer = "";
    @SerializedName("isFavorite") private boolean mIsFavorite = false;
    @SerializedName("isSerie") private boolean mIsSerie = false;
    @SerializedName("categories") private List<String> mCategories = new ArrayList<>();

    @SerializedName("validImageSizes") private static String[] mValidImageSizes = {"w160hx", "w320hx", "w640hx", "w1280hx", "w1600hx", "w1920hx", "VV_4x3_120", "VV_4x3_240", "VV_4x3_480"};

    public Program(String title,
                   String description,
                   String programName,
                   String programType,
                   String programUrl,
                   String thumbnail,
                   String altImage,
                   String brand,
                   String imageServer,
                   boolean isFavorite,
                   boolean isSerie) {
        mTitle = title;
        mDescription = description;
        mProgramName = programName;
        mProgramType = programType;
        mProgramUrl = programUrl;
        mThumbnail = thumbnail;
        mAltImage = altImage;
        mBrand = brand;
        mImageServer = imageServer;
        mIsFavorite = isFavorite;
        mIsSerie = isSerie;
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

    public String getProgramName() {
        return mProgramName;
    }
    public void setProgramName(String programName) {
        mProgramName = programName;
    }

    public String getProgramType() {
        return mProgramType;
    }
    public void setProgramType(String programType) {
        mProgramType = programType;
    }

    public String getProgramUrl() {
        return mProgramUrl;
    }
    public void setProgramUrl(String programUrl) {
        mProgramUrl = programUrl;
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

        if(mThumbnail.length() > 0){
            return mImageServer + "/" + size + "/" + mThumbnail;
        } else {
            return "";
        }
    }

    public void setThumbnail(String thumbnail) { mThumbnail  = thumbnail; }

    public String getAltImage() {
        return getAltImage("w320hx");
    }

    public String getAltImage(String size) {

        if(Arrays.stream(mValidImageSizes).noneMatch(size::equals)) {
            Log.d("Program", size + " is an invalid thumbnail size");
            return "";
        }

        if(mAltImage.length() > 0){
            return mImageServer + "/" + size + "/" + mAltImage;
        } else {
            return "";
        }
    }

    public void setAltImage(String altImage) { mAltImage  = altImage; }

    public String getBrand() {
        return mBrand;
    }
    public void setBrand(String brand) { mBrand = brand; }

    public boolean isFavorite() {
        return mIsFavorite;
    }
    public void setIsFavorite(boolean isFavorite) { mIsFavorite = isFavorite; }

    public boolean isSerie() {
        return mIsSerie;
    }
    public void setIsSerie(boolean isSerie) { mIsSerie = isSerie; }

    public void addCategory(String category) {
        if(!mCategories.contains(category)) {
            mCategories.add(category);
        }
    }

    public List<String> getCategories() { return mCategories; }

}
