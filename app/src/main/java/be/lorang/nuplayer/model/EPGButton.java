package be.lorang.nuplayer.model;

import android.content.Context;

import be.lorang.nuplayer.model.EPGEntry;

/*
 * Simple class that extends Button to store extra data fields
 */

public class EPGButton extends android.widget.Button {

    private EPGEntry epgEntry;
    private int leftMargin;
    private int width;

    public EPGButton(Context context) {
        super(context);
    }

    public int getEPGLeftMargin() {
        return leftMargin;
    }

    public void setEPGLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getEPGWidth() {
        return width;
    }

    public void setEPGWidth(int width) {
        this.width = width;
    }
}
