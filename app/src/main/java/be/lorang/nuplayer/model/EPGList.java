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
