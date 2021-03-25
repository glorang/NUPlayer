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
 * Class that will hold EPG data for a single day
 */

import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class EPGList {

    @SerializedName("date") private ZonedDateTime date;
    @SerializedName("epgData") private List<EPGEntry> epgData = new ArrayList<>();

    public EPGList(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public List<EPGEntry> getEpgData() {
        return epgData;
    }

    public void addEPGEntry(EPGEntry epgEntry) {
        epgData.add(epgEntry);
    }
}
