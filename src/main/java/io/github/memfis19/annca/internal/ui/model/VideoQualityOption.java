package io.github.memfis19.annca.internal.ui.model;

import android.media.CamcorderProfile;

import java.util.concurrent.TimeUnit;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;


public class VideoQualityOption implements CharSequence {

    private String title;

    @AnncaConfiguration.MediaQuality
    private int mediaQuality;

    public VideoQualityOption(@AnncaConfiguration.MediaQuality int mediaQuality, CamcorderProfile camcorderProfile, double videoDuration) {
        this.mediaQuality = mediaQuality;

        long minutes = TimeUnit.SECONDS.toMinutes((long) videoDuration);
        long seconds = ((long) videoDuration) - minutes * 60;

        if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_AUTO) {
            title = "Auto " + ", (" + (minutes > 10 ? minutes : ("0" + minutes)) + ":" + (seconds > 10 ? seconds : ("0" + seconds)) + " min)";
        } else {
            title = String.valueOf(camcorderProfile.videoFrameWidth)
                    + " x " + String.valueOf(camcorderProfile.videoFrameHeight)
                    + (videoDuration <= 0 ? "" : ", (" + (minutes > 10 ? minutes : ("0" + minutes)) + ":" + (seconds > 10 ? seconds : ("0" + seconds)) + " min)");
        }
    }

    @AnncaConfiguration.MediaQuality
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int length() {
        return title.length();
    }

    @Override
    public char charAt(int index) {
        return title.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return title.subSequence(start, end);
    }

    @Override
    public String toString() {
        return title;
    }
}