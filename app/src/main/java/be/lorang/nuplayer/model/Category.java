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

/*
 * Class that represents a Category
 */

import android.util.Log;
import com.google.gson.annotations.SerializedName;

public class Category {

    @SerializedName("name") private String name = "";
    @SerializedName("title") private String title = "";
    @SerializedName("thumbnail") private String thumbnail = "";
    @SerializedName("imageServer") private String imageServer = "";

    public Category(String name, String title, String thumbnail, String imageServer) {
        this.name = name;
        this.title = title;
        this.thumbnail = thumbnail;
        this.imageServer = imageServer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
