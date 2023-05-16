package io.github.memfis19.annca.internal.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.github.memfis19.annca.R;
import io.github.memfis19.annca.internal.utils.Utils;


public class CameraSwitchView extends AppCompatImageButton {

    public static final int CAMERA_TYPE_FRONT = 0;
    public static final int CAMERA_TYPE_REAR = 1;

    @IntDef({CAMERA_TYPE_FRONT, CAMERA_TYPE_REAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraType {
    }

    private OnCameraTypeChangeListener onCameraTypeChangeListener;

    public interface OnCameraTypeChangeListener {
        void onCameraTypeChanged(@CameraType int cameraType);
    }

    private Context context;
    private Drawable frontCameraDrawable;
    private Drawable rearCameraDrawable;
    private int padding = 5;

    private
    @CameraType
    int currentCameraType = CAMERA_TYPE_FRONT;

    public CameraSwitchView(Context context) {
        this(context, null);
    }

    public CameraSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initializeView();
    }

    public CameraSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    private void initializeView() {
        frontCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_front_white_24dp);
        frontCameraDrawable = DrawableCompat.wrap(frontCameraDrawable);
        DrawableCompat.setTintList(frontCameraDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        rearCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_rear_white_24dp);
        rearCameraDrawable = DrawableCompat.wrap(rearCameraDrawable);
        DrawableCompat.setTintList(rearCameraDrawable.mutate(), ContextCompat.getColorStateList(context, R.drawable.switch_camera_mode_selector));

        setBackgroundResource(R.drawable.circle_frame_background_dark);
        setOnClickListener(new CameraTypeClickListener());
        setIcons1();
        padding = Utils.convertDipToPixels(context, padding);
        setPadding(padding, padding, padding, padding);
    }

    //Make new method for from camera and call in init method of view
    private void setIcons1() {
        if (currentCameraType == CAMERA_TYPE_FRONT) {
            setImageDrawable(rearCameraDrawable);
        } else setImageDrawable(frontCameraDrawable);
    }


    private void setIcons() {
        if (currentCameraType == CAMERA_TYPE_REAR) {
            setImageDrawable(frontCameraDrawable);
        } else setImageDrawable(rearCameraDrawable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (Build.VERSION.SDK_INT > 10) {
            if (enabled) {
                setAlpha(1f);
            } else {
                setAlpha(0.5f);
            }
        }
    }

    public void setCameraType(@CameraType int cameraType) {
        this.currentCameraType = cameraType;
        setIcons();
    }

    public
    @CameraType
    int getCameraType() {
        return currentCameraType;
    }

    public void setOnCameraTypeChangeListener(OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
    }

    private class CameraTypeClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            if (currentCameraType == CAMERA_TYPE_REAR) {
                currentCameraType = CAMERA_TYPE_FRONT;
            } else currentCameraType = CAMERA_TYPE_REAR;

            setIcons();

            if (onCameraTypeChangeListener != null)
                onCameraTypeChangeListener.onCameraTypeChanged(currentCameraType);
        }
    }
}
