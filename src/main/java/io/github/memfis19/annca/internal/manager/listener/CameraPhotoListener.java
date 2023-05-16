package io.github.memfis19.annca.internal.manager.listener;

import java.io.File;


public interface CameraPhotoListener {
    void onPhotoTaken(File photoFile);

    void onPhotoTakeError();
}
