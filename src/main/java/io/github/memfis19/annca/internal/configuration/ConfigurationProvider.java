package io.github.memfis19.annca.internal.configuration;

import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;


public interface ConfigurationProvider {

    int getRequestCode();

    @AnncaConfiguration.MediaAction
    int getMediaAction();

    @AnncaConfiguration.MediaQuality
    int getMediaQuality();

    int getVideoDuration();

    int getAssessmentRemainingTimer();

    long getVideoFileSize();

    @AnncaConfiguration.SensorPosition
    int getSensorPosition();

    int getDegrees();

    int getMinimumVideoDuration();

    @AnncaConfiguration.FlashMode
    int getFlashMode();

    @CameraSwitchView.CameraType
    int getCameraFace();

    String getFilePath();

    @AnncaConfiguration.MediaResultBehaviour
    int getMediaResultBehaviour();
}
