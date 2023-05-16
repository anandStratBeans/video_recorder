package io.github.memfis19.annca.internal.manager.listener;


public interface CameraCloseListener<CameraId> {
    void onCameraClosed(CameraId closedCameraId);
}
