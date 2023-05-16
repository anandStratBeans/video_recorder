package io.github.memfis19.annca.internal.ui.view;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.github.memfis19.annca.R;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.utils.DateTimeUtils;


public class CameraControlPanel extends RelativeLayout
        implements RecordButton.RecordButtonListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener {

    private Context context;

    private CameraSwitchView cameraSwitchView;
    private RecordButton recordButton;
    private MediaActionSwitchView mediaActionSwitchView;
    private FlashSwitchView flashSwitchView;
    private TextView recordDurationText;
    private TextView recordSizeText;
    private CameraSettingsView settingsButton;

    private RecordButton.RecordButtonListener recordButtonListener;
    private MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener;
    private CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener;
    private FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener;
    private SettingsClickListener settingsClickListener;

    public TimerTaskBase countDownTimer;
    public TimerTaskBase countDownTimerAssessment;
    private long maxVideoFileSize = 0;
    private String mediaFilePath;

    private int assessmentRemainingTimer = -1;

    public interface SettingsClickListener {
        void onSettingsClick();
    }

    private boolean hasFlash = false;

    private
    @MediaActionSwitchView.MediaActionState
    int mediaActionState;

    private int mediaAction;

    private FileObserver fileObserver;

    public boolean isClickRecordButton=false;
    int minimumSeconds=0;
    int minimumMinute=0;
    public CameraControlPanel(Context context) {
        this(context, null);
    }

    public CameraControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
//        hasFlash = false;

        LayoutInflater.from(context).inflate(R.layout.camera_control_panel_layout, this);
        setBackgroundColor(Color.TRANSPARENT);

        settingsButton = (CameraSettingsView) findViewById(R.id.settings_view);
        settingsButton.setVisibility(GONE);
        cameraSwitchView = (CameraSwitchView) findViewById(R.id.front_back_camera_switcher);
        cameraSwitchView.setVisibility(GONE);
        mediaActionSwitchView = (MediaActionSwitchView) findViewById(R.id.photo_video_camera_switcher);
        recordButton = (RecordButton) findViewById(R.id.record_button);
        flashSwitchView = (FlashSwitchView) findViewById(R.id.flash_switch_view);
        recordDurationText = (TextView) findViewById(R.id.record_duration_text);
        recordSizeText = (TextView) findViewById(R.id.record_size_mb_text);

        cameraSwitchView.setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        mediaActionSwitchView.setOnMediaActionStateChangeListener(this);

        setOnCameraTypeChangeListener(onCameraTypeChangeListener);
        setOnMediaActionStateChangeListener(onMediaActionStateChangeListener);
        setFlashModeSwitchListener(flashModeSwitchListener);
        setRecordButtonListener(recordButtonListener);
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (settingsClickListener != null) settingsClickListener.onSettingsClick();
            }
        });

        if (hasFlash)
            flashSwitchView.setVisibility(VISIBLE);
        else flashSwitchView.setVisibility(GONE);

        countDownTimer = new TimerTask(recordDurationText);

    }

    public void lockControls() {
        cameraSwitchView.setEnabled(false);
        recordButton.setEnabled(false);
        settingsButton.setEnabled(false);
        flashSwitchView.setEnabled(false);
    }

    public void unLockControls() {
        cameraSwitchView.setEnabled(true);
        recordButton.setEnabled(true);
        settingsButton.setEnabled(true);
        flashSwitchView.setEnabled(true);
    }

    public void setup(int mediaAction) {
        this.mediaAction = mediaAction;
        if (AnncaConfiguration.MEDIA_ACTION_VIDEO == mediaAction) {
            recordButton.setup(mediaAction, this);
            flashSwitchView.setVisibility(GONE);
        } else {
            recordButton.setup(AnncaConfiguration.MEDIA_ACTION_PHOTO, this);
        }

        if (AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
            mediaActionSwitchView.setVisibility(GONE);
        } else mediaActionSwitchView.setVisibility(VISIBLE);

    }

    public void setFlasMode(@FlashSwitchView.FlashMode int flashMode) {
        flashSwitchView.setFlashMode(flashMode);
    }

    public void setMediaFilePath(final File mediaFile) {
        this.mediaFilePath = mediaFile.toString();
    }

    public void setMaxVideoFileSize(long maxVideoFileSize) {
        this.maxVideoFileSize = maxVideoFileSize;
    }

    public void setMaxVideoDuration(int maxVideoDurationInMillis) {
        if (maxVideoDurationInMillis > 0)
            countDownTimer = new CountdownTask(recordDurationText, maxVideoDurationInMillis);
        else countDownTimer = new TimerTask(recordDurationText);
    }


    public void setAssessmentRemainingTimer(int assessmentRemainingTimer) {
        this.assessmentRemainingTimer = assessmentRemainingTimer;
        if (assessmentRemainingTimer > 0) {
            countDownTimerAssessment = new CountdownTaskAssessment(new TextView(getContext()), assessmentRemainingTimer);
            countDownTimerAssessment.start();
        }
    }

    Activity activity;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setMediaActionState(@MediaActionSwitchView.MediaActionState int actionState) {
        if (mediaActionState == actionState) return;
        if (MediaActionSwitchView.ACTION_PHOTO == actionState) {
            recordButton.setMediaAction(AnncaConfiguration.MEDIA_ACTION_PHOTO);
            if (hasFlash)
                flashSwitchView.setVisibility(VISIBLE);
        } else {
            recordButton.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
            flashSwitchView.setVisibility(GONE);
        }
        mediaActionState = actionState;
        mediaActionSwitchView.setMediaActionState(actionState);
    }

    public void setRecordButtonListener(RecordButton.RecordButtonListener recordButtonListener) {
        this.recordButtonListener = recordButtonListener;
    }

    public void rotateControls(int rotation) {
        if (Build.VERSION.SDK_INT > 10) {
            cameraSwitchView.setRotation(rotation);
            mediaActionSwitchView.setRotation(rotation);
            flashSwitchView.setRotation(rotation);
            recordDurationText.setRotation(rotation);
            recordSizeText.setRotation(rotation);
        }
    }

    public void setOnMediaActionStateChangeListener(MediaActionSwitchView.OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
    }

    public void setOnCameraTypeChangeListener(CameraSwitchView.OnCameraTypeChangeListener onCameraTypeChangeListener) {
        this.onCameraTypeChangeListener = onCameraTypeChangeListener;
        if (cameraSwitchView != null)
            cameraSwitchView.setOnCameraTypeChangeListener(this.onCameraTypeChangeListener);
    }

    public void setFlashModeSwitchListener(FlashSwitchView.FlashModeSwitchListener flashModeSwitchListener) {
        this.flashModeSwitchListener = flashModeSwitchListener;
        if (flashSwitchView != null)
            flashSwitchView.setFlashSwitchListener(this.flashModeSwitchListener);
    }

    public void setSettingsClickListener(SettingsClickListener settingsClickListener) {
        this.settingsClickListener = settingsClickListener;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        if (recordButtonListener != null)
            recordButtonListener.onTakePhotoButtonPressed();
    }

    public void onStartVideoRecord(final File mediaFile) {
        isClickRecordButton=false;
        setMediaFilePath(mediaFile);
        if (maxVideoFileSize > 0) {
            recordSizeText.setText("1Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
            recordSizeText.setVisibility(VISIBLE);
            try {
                fileObserver = new FileObserver(this.mediaFilePath) {
                    private long lastUpdateSize = 0;

                    @Override
                    public void onEvent(int event, String path) {
                        final long fileSize = mediaFile.length() / (1024 * 1024);
                        if ((fileSize - lastUpdateSize) >= 1) {
                            lastUpdateSize = fileSize;
                            recordSizeText.post(new Runnable() {
                                @Override
                                public void run() {
                                    recordSizeText.setText(fileSize + "Mb" + " / " + maxVideoFileSize / (1024 * 1024) + "Mb");
                                }
                            });
                        }
                    }
                };
                fileObserver.startWatching();
            } catch (Exception e) {
                Log.e("FileObserver", "setMediaFilePath: ", e);
            }
        }
        countDownTimer.start();
    }

    public void allowRecord(boolean isAllowed) {
        recordButton.setEnabled(isAllowed);
    }

    public void stopTimer() {
        countDownTimer.stop();
    }

    public void allowCameraSwitching(boolean isAllowed) {
        // Set visible in ternuray case
        cameraSwitchView.setVisibility(isAllowed ? GONE : GONE);
    }

    public void onStopVideoRecord() {
        if (isClickRecordButton) {
            if (fileObserver != null)
                fileObserver.stopWatching();
            countDownTimer.stop();

            recordSizeText.setVisibility(GONE);
            //Set visible for Camera front and back switch
            cameraSwitchView.setVisibility(View.GONE);
            settingsButton.setVisibility(GONE);

            if (AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED != mediaAction) {
                mediaActionSwitchView.setVisibility(GONE);
            } else mediaActionSwitchView.setVisibility(VISIBLE);
            recordButton.setRecordState(RecordButton.READY_FOR_RECORD_STATE);
            recordButton.setClickRecord(false);
        }
    }

    @Override
    public void onStartRecordingButtonPressed() {

        if (countDownTimerAssessment != null) {
            countDownTimerAssessment.stop();
        }
        cameraSwitchView.setVisibility(View.GONE);
        mediaActionSwitchView.setVisibility(GONE);
        settingsButton.setVisibility(GONE);
//        recordButton.setVisibility(INVISIBLE);
        if (recordButtonListener != null)
            recordButtonListener.onStartRecordingButtonPressed();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        if (isClickRecordButton) {
            onStopVideoRecord();
            if (recordButtonListener != null)
                recordButtonListener.onStopRecordingButtonPressed();
            isClickRecordButton = false;
        } else {
            String timeMsg = "";
            if (minimumMinute != -1 && minimumMinute != 0) {
                timeMsg = minimumMinute + " " + getResources().getString(R.string.minute);
            } else {
                timeMsg = minimumSeconds + " " + getResources().getString(R.string.seconds);
            }
            Toast.makeText(context, String.format(getResources().getString(R.string.errMsgMinTime), timeMsg), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        setMediaActionState(mediaActionState);
        if (onMediaActionStateChangeListener != null)
            onMediaActionStateChangeListener.onMediaActionChanged(this.mediaActionState);
    }

    abstract class TimerTaskBase {
        Handler handler = new Handler(Looper.getMainLooper());
        TextView timerView;
        boolean alive = false;
        long recordingTimeSeconds = 0;
        long recordingTimeMinutes = 0;

        long recordingTimeSecondsInc = 0;
        long recordingTimeMinutesInc = 0;

        TimerTaskBase(TextView timerView) {
            this.timerView = timerView;
        }

        abstract void stop();

        abstract void start();

        public abstract void stopTimer();
    }

    private class CountdownTask extends TimerTaskBase implements Runnable {
        int minTime = 0;
        String totalTime = "";
        private int maxDurationMilliseconds = 0;

        public CountdownTask(TextView timerView, int maxDurationMilliseconds) {
            super(timerView);
            this.maxDurationMilliseconds = maxDurationMilliseconds;
            long recSec = maxDurationMilliseconds / 1000;
            long totalSec = recSec % 60;
            long totalMin = (recSec / 60) % 60;
            totalTime = String.format("%02d:%02d", totalMin, totalSec);
            long seconds = 0;
            if (AnncaConfiguration.miliseondPssed != 0) {
                seconds = (AnncaConfiguration.miliseondPssed / 1000);
                long s = seconds % 60;
                long m = (seconds / 60) % 60;
                if (m != 0) {
                    minimumMinute = (int) m;
                    minimumSeconds = -1;
                } else {
                    minimumSeconds = (int) s;
                    minimumMinute = -1;
                }
                minTime = (int) seconds;
            }
        }

        @Override
        public void run() {

            recordingTimeSeconds--;

            int millis = (int) recordingTimeSeconds * 1000;

//            timerView.setText(
//                    String.format("%02d:%02d",
//                            TimeUnit.MILLISECONDS.toMinutes(millis),
//                            TimeUnit.MILLISECONDS.toSeconds(millis) -
//                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
//                    ));

//            if (recordingTimeSeconds < 10) {
//                timerView.setTextColor(Color.RED);
//            }

            recordingTimeSecondsInc++;
            if (recordingTimeSecondsInc == 60) {
                recordingTimeSecondsInc = 0;
                recordingTimeMinutesInc++;
            }
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutesInc, recordingTimeSecondsInc)
                            + "/" + totalTime);
            if (minimumSeconds != -1 && minimumSeconds == recordingTimeSecondsInc) {
                //recordButton.setVisibility(VISIBLE);
                isClickRecordButton = true;
                recordButton.setClickRecord(isClickRecordButton);
            } else if (minimumMinute != -1 && minimumMinute == recordingTimeMinutesInc) {
                //  recordButton.setVisibility(VISIBLE);
                isClickRecordButton = true;
                recordButton.setClickRecord(isClickRecordButton);
            }
            Log.e("rotate rec sec", String.valueOf(recordingTimeSeconds));
//            if (recordingTimeSeconds == 0) {
//                isClickRecordButton = true;
//                stop();
//                onStopVideoRecord();
//                onStopRecordingButtonPressed();
//            }
            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }

        @Override
        void start() {
            alive = true;
            recordingTimeSeconds = maxDurationMilliseconds / 1000;

            recordingTimeMinutesInc = 0;
            recordingTimeSecondsInc = 0;

            timerView.setTextColor(Color.WHITE);
//            timerView.setText(
//                    String.format("%02d:%02d",
//                            TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds),
//                            TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds) -
//                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds))
//                    ));

            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutesInc, recordingTimeSecondsInc)
                            + "/" + totalTime);
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        public void stopTimer() {

        }
    }

    private class TimerTask extends TimerTaskBase implements Runnable {
        int maxMin = 0;

        public TimerTask(TextView timerView) {
            super(timerView);
            Date date = new Date(AnncaConfiguration.miliseondPssed);
            // formula for conversion for
            // milliseconds to minutes.
            long minutes = (AnncaConfiguration.miliseondPssed / 1000) / 60;
            maxMin = (int) minutes;
            //Log.e("MaxMin", String.valueOf(maxMin));
        }

        @Override
        public void run() {
            recordingTimeSeconds++;

            if (recordingTimeSeconds == 60) {
                recordingTimeSeconds = 0;
                recordingTimeMinutes++;
            }

            if (recordingTimeMinutes >= maxMin) {
                stop();
                onStopVideoRecord();
                onStopRecordingButtonPressed();
            }
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            if (alive) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        public void start() {
            alive = true;
            recordingTimeMinutes = 0;
            recordingTimeSeconds = 0;
            timerView.setText(
                    String.format("%02d:%02d", recordingTimeMinutes, recordingTimeSeconds));
            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        public void stopTimer() {

        }

        public void stop() {
            timerView.setVisibility(View.INVISIBLE);
            alive = false;
        }
    }


    private class CountdownTaskAssessment extends TimerTaskBase implements Runnable {

        private int maxDurationMilliseconds = 0;

        public CountdownTaskAssessment(TextView timerView, int maxDurationMilliseconds) {
            super(timerView);
            this.maxDurationMilliseconds = maxDurationMilliseconds;
        }

        @Override
        public void run() {

            recordingTimeSeconds--;
            int millis = (int) recordingTimeSeconds * 1000;

//            timerView.setText(
//                    String.format("%02d:%02d",
//                            TimeUnit.MILLISECONDS.toMinutes(millis),
//                            TimeUnit.MILLISECONDS.toSeconds(millis) -
//                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
//                    ));

            if (recordingTimeSeconds == 0) {
                // timerView.setTextColor(Color.RED);
                this.stopTimer();
            }

            if (alive && recordingTimeSeconds > 0) handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        void stop() {
            alive = false;
        }

        @Override
        void start() {
            alive = true;
            recordingTimeSeconds = maxDurationMilliseconds / 1000;
            // timerView.setTextColor(Color.WHITE);
//            timerView.setText(
//                    String.format("%02d:%02d",
//                            TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds),
//                            TimeUnit.MILLISECONDS.toSeconds(maxDurationMilliseconds) -
//                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxDurationMilliseconds))
//                    ));
//            timerView.setVisibility(View.VISIBLE);
            handler.postDelayed(this, DateTimeUtils.SECOND);
        }

        @Override
        public void stopTimer() {
            activity.finish();
        }
    }

    public void setClickRecordButton(boolean clickRecordButton) {
        isClickRecordButton = clickRecordButton;
    }
}
