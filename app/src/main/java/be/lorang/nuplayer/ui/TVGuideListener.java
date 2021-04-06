package be.lorang.nuplayer.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import be.lorang.nuplayer.model.ChannelList;
import be.lorang.nuplayer.model.EPGEntry;
import be.lorang.nuplayer.model.Program;
import be.lorang.nuplayer.model.ProgramList;
import be.lorang.nuplayer.model.Video;

/*
 * Helper class to implement View.OnClickListener for EPG entries
 */
public class TVGuideListener extends VideoProgramBaseListener implements View.OnClickListener {

    private static String TAG = "TVGuideListener";
    private Activity activity;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private EPGEntry epgEntry;


    public TVGuideListener(Activity activity, ZonedDateTime startTime, ZonedDateTime endTime, EPGEntry epgEntry) {
        super(activity);
        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;
        this.epgEntry = epgEntry;
    }

    public void onClick(View v) {

        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
        // start Live TV
        if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
            Video video = ChannelList.getInstance().getLiveChannel(epgEntry.getChannelID());
            if (video != null) {
                startVideoIntent(video);
            }
        } else {
            // start Program display
            Program program = ProgramList.getInstance().getProgram(epgEntry.getTitle());
            if (program != null) {
                startProgramIntent(program);
            } else {
                if(activity != null) {
                    Toast.makeText(activity, "Program not found in catalog", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}