package io.github.memfis19.annca.internal.manager.listener;

import java.io.File;

import io.github.memfis19.annca.internal.utils.Size;


public interface CameraVideoListener {
    void onVideoRecordStarted(Size videoSize);

    void onVideoRecordStopped(File videoFile);

    void onVideoRecordError();
}
