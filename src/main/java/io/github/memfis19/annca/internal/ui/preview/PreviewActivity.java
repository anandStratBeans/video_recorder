package io.github.memfis19.annca.internal.ui.preview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import java.io.File;

import io.github.memfis19.annca.R;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.ui.BaseAnncaActivity;
import io.github.memfis19.annca.internal.ui.view.AspectFrameLayout;
import io.github.memfis19.annca.internal.utils.AnncaImageLoader;
import io.github.memfis19.annca.internal.utils.NetworkUtils;
import io.github.memfis19.annca.internal.utils.PromptDialog;
import io.github.memfis19.annca.internal.utils.Utils;
import io.github.memfis19.annca.tooltip.ToolTip;
import io.github.memfis19.annca.tooltip.ToolTipCoordinatesFinder;
import io.github.memfis19.annca.tooltip.ToolTipsManager;


public class PreviewActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PreviewActivity";
    public static final String PLEASE_CHECK_THE_INTERNET_CONNECTIVITY = "Internet Connection not available. Please Check Your Internet Connectivity for upload video";
    private final static String MEDIA_ACTION_ARG = "media_action_arg";
    public final static String FILE_PATH_ARG = "file_path_arg";
    public final static String HIDE_RETAKE_BUTTON = "hide_retake_button";
    public final static String RESPONSE_CODE_ARG = "response_code_arg";
    private final static String VIDEO_POSITION_ARG = "current_video_position";
    private final static String VIDEO_IS_PLAYED_ARG = "is_played";
    private final static String MIME_TYPE_VIDEO = "video";
    private final static String MIME_TYPE_IMAGE = "image";

    private int mediaAction;
    private String previewFilePath;
    private boolean isHideRetake = false;

    private SurfaceView surfaceView;
    private FrameLayout photoPreviewContainer;
    private ImageView imagePreview;
    private ViewGroup buttonPanel;
    private AspectFrameLayout videoPreviewContainer;
    private View cropMediaAction;
    private TextView ratioChanger;

    private MediaController mediaController;
    private MediaPlayer mediaPlayer;

    private int currentPlaybackPosition = 0;
    private boolean isVideoPlaying = true;

    private int currentRatioIndex = 0;
    private float[] ratios;
    private String[] ratioLabels;

    PromptDialog promptDialog;

    //Internet stripe things
    private TextView tvInternetStrip;
    private View confirmMediaResult;
    View reTakeMedia;
    private RelativeLayout preview_activity_container;
    public static int timerRunnable = 2000;
    int flagShowView = 0;
    ToolTipsManager toolTipsManager;
    ToolTip toolTip;
    boolean isPauseMediaPlayer = false;
    private SurfaceHolder.Callback surfaceCallbacks = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            showVideoPreview(holder);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    int seekTo = 0;
    private MediaController.MediaPlayerControl MediaPlayerControlImpl = new MediaController.MediaPlayerControl() {
        @Override
        public void start() {
            mediaPlayer.start();
        }

        @Override
        public void pause() {
            mediaPlayer.pause();
        }

        @Override
        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            return mediaPlayer.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            mediaPlayer.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return mediaPlayer.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return 0;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return mediaPlayer.getAudioSessionId();
        }
    };

    public static Intent newIntent(Context context,
                                   @AnncaConfiguration.MediaAction int mediaAction,
                                   String filePath) {

        return new Intent(context, PreviewActivity.class)
                .putExtra(MEDIA_ACTION_ARG, mediaAction)
                .putExtra(FILE_PATH_ARG, filePath);
    }

    public static Intent newIntent(Context context,
                                   @AnncaConfiguration.MediaAction int mediaAction,
                                   String filePath,
                                   boolean isHideRetake) {

        return new Intent(context, PreviewActivity.class)
                .putExtra(MEDIA_ACTION_ARG, mediaAction)
                .putExtra(FILE_PATH_ARG, filePath)
                .putExtra(HIDE_RETAKE_BUTTON, isHideRetake);
    }


    PromptDialog.OnDialogButtonClick dialogButtonClick = new PromptDialog.OnDialogButtonClick() {
        @Override
        public void setPositiveButtonClick(int clickFlag) {

            if (clickFlag == PromptDialog.CLICK_FLAG_BACK) {
                Intent resultIntent = new Intent();
                // deleteMediaFile();
                resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CANCEL);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            } else {
                Intent resultIntent = new Intent();
                int resCode = RESULT_OK;
                if (clickFlag == PromptDialog.CLICK_FLAG_CANCEL || clickFlag == PromptDialog.CLICK_FLAG_BACK1) {
                    // deleteMediaFile();
                    resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CANCEL);
                    resCode = RESULT_CANCELED;
                    setResultCode(resCode, resultIntent);
                } else if (clickFlag == PromptDialog.CLICK_FLAG_CANCEL_PREVIEW || clickFlag == PromptDialog.CLICK_FLAG_BACK_PREVIEW) {
                    resCode = RESULT_OK;
                    saveVideoAndMovePreviousScreen(resultIntent, resCode);

                } else if (clickFlag == PromptDialog.CLICK_FLAG_REPEAT) {
                    deleteMediaFile();
                    resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_RETAKE);
                    resCode = RESULT_OK;
                    setResultCode(resCode, resultIntent);
                } else if (clickFlag == PromptDialog.CLICK_FLAG_SUCCESS) {
                    resCode = RESULT_OK;
                    saveVideoAndMovePreviousScreen(resultIntent, resCode);
                }

            }
        }

        @Override
        public void setNegativeButtonClick(int clickFlag) {
            if (clickFlag == PromptDialog.CLICK_FLAG_CANCEL_PREVIEW || clickFlag == PromptDialog.CLICK_FLAG_BACK_PREVIEW) {
                Intent resultIntent = new Intent();
                //deleteMediaFile();
                resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CANCEL);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        }
    };

    private void setResultCode(int resCode, Intent resultIntent) {
        setResult(resCode, resultIntent);
        finish();
    }

    public void saveVideoAndMovePreviousScreen(Intent resultIntent, int resCode) {
        if (!NetworkUtils.isInternetAvailable(PreviewActivity.this)) {
            NetworkUtils.showToast(PreviewActivity.this, PLEASE_CHECK_THE_INTERNET_CONNECTIVITY);
            return;
        }
        resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CONFIRM)
                .putExtra(FILE_PATH_ARG, previewFilePath);
        setResultCode(resCode, resultIntent);
    }

    private void showPromptDialog(int clickFlag) {
        promptDialog.setClickFlag(clickFlag);
        switch (clickFlag) {
            case PromptDialog.CLICK_FLAG_BACK_PREVIEW:
            case PromptDialog.CLICK_FLAG_CANCEL_PREVIEW:
                promptDialog.showProgressDialogCancelBack(isHideRetake);
                break;
            default:
                promptDialog.showProgressDialog();
                break;
        }

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        promptDialog = new PromptDialog(PreviewActivity.this, dialogButtonClick);
        String originalRatioLabel = getString(R.string.preview_controls_original_ratio_label);
        ratioLabels = new String[]{originalRatioLabel, "1:1", "4:3", "16:9"};
        ratios = new float[]{0f, 1f, 4f / 3f, 16f / 9f};

        surfaceView = (SurfaceView) findViewById(R.id.video_preview);
        surfaceView.setRotation(0);
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mediaController == null) return false;
                if (mediaController.isShowing()) {
                    mediaController.hide();
                    showButtonPanel(true);
                } else {
                    showButtonPanel(true);
                    mediaController.show();
                }
                hideToolTipView();
                return false;
            }
        });

        videoPreviewContainer = (AspectFrameLayout) findViewById(R.id.previewAspectFrameLayout);
        photoPreviewContainer = (FrameLayout) findViewById(R.id.photo_preview_container);
        buttonPanel = (ViewGroup) findViewById(R.id.preview_control_panel);
        confirmMediaResult = findViewById(R.id.confirm_media_result);
        preview_activity_container = findViewById(R.id.preview_activity_container);
        reTakeMedia = findViewById(R.id.re_take_media);
        View cancelMediaAction = findViewById(R.id.cancel_media_action);
        tvInternetStrip = (TextView) findViewById(R.id.tvInternetStrip);
        cropMediaAction = findViewById(R.id.crop_image);
        ratioChanger = (TextView) findViewById(R.id.ratio_image);
        ratioChanger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRatioIndex = (currentRatioIndex + 1) % ratios.length;
                ratioChanger.setText(ratioLabels[currentRatioIndex]);
            }
        });

        cropMediaAction.setVisibility(View.GONE);
        ratioChanger.setVisibility(View.GONE);

        if (cropMediaAction != null)
            cropMediaAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });

        if (confirmMediaResult != null) {
            confirmMediaResult.setOnClickListener(this);
            ToolTipCoordinatesFinder.isMargin = true;
            ToolTipCoordinatesFinder.setMarginView(0, 0, 10, 0);
            toolTipsManager = new ToolTipsManager(tipListener);
            showToolTip();
        }


        if (reTakeMedia != null)
            reTakeMedia.setOnClickListener(this);

        if (cancelMediaAction != null)
            cancelMediaAction.setOnClickListener(this);

        Bundle args = getIntent().getExtras();

        mediaAction = args.getInt(MEDIA_ACTION_ARG);
        previewFilePath = args.getString(FILE_PATH_ARG);
        isHideRetake = args.getBoolean(HIDE_RETAKE_BUTTON);

        if (mediaAction == AnncaConfiguration.MEDIA_ACTION_VIDEO) {
            displayVideo(savedInstanceState);
            hideRetakeButton(reTakeMedia);
        } else if (mediaAction == AnncaConfiguration.MEDIA_ACTION_PHOTO) {
            displayImage();
        } else {
            String mimeType = Utils.getMimeType(previewFilePath);
            if (mimeType.contains(MIME_TYPE_VIDEO)) {
                displayVideo(savedInstanceState);
                hideRetakeButton(reTakeMedia);
            } else if (mimeType.contains(MIME_TYPE_IMAGE)) {
                displayImage();
            } else finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveVideoParams(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaController != null) {
            mediaController.hide();
            mediaController = null;
        }
    }

    private void displayImage() {
        videoPreviewContainer.setVisibility(View.GONE);
        surfaceView.setVisibility(View.GONE);

        showImagePreview();
        ratioChanger.setText(ratioLabels[currentRatioIndex]);
    }

    private void showImagePreview() {
        imagePreview = new ImageView(this);
        AnncaImageLoader.Builder builder = new AnncaImageLoader.Builder(this);
        builder.load(previewFilePath).build().into(imagePreview);
        photoPreviewContainer.removeAllViews();
        photoPreviewContainer.addView(imagePreview);
    }

    private void displayVideo(Bundle savedInstanceState) {
        cropMediaAction.setVisibility(View.GONE);
        ratioChanger.setVisibility(View.GONE);
        if (savedInstanceState != null) {
            loadVideoParams(savedInstanceState);
        }
        photoPreviewContainer.setVisibility(View.GONE);
        surfaceView.getHolder().addCallback(surfaceCallbacks);

    }

    private void hideRetakeButton(View retakeMedia) {
        if (isHideRetake) {
            retakeMedia.setVisibility(View.INVISIBLE);
            retakeMedia.setClickable(false);
            retakeMedia.setEnabled(false);
        } else {
            retakeMedia.setVisibility(View.VISIBLE);
            retakeMedia.setClickable(true);
            retakeMedia.setEnabled(true);
        }
    }

    private void showVideoPreview(SurfaceHolder holder) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(previewFilePath);
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                        mediaController = new MediaController(PreviewActivity.this);
                        mediaController.setAnchorView(surfaceView);
                        mediaController.setMediaPlayer(MediaPlayerControlImpl);
                        int videoWidth = mp.getVideoWidth();
                        int videoHeight = mp.getVideoHeight();
                        videoPreviewContainer.setAspectRatio((double) videoWidth / videoHeight);
                        mediaPlayer.start();
                        mediaPlayer.seekTo(currentPlaybackPosition);
                        if (!isVideoPlaying)
                            mediaPlayer.pause();

                    isPauseMediaPlayer=false;
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    finish();
                    return true;
                }
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error media player playing video.");
            finish();
        }
    }

    private void saveVideoParams(Bundle outState) {
        if (mediaPlayer != null) {
            outState.putInt(VIDEO_POSITION_ARG, mediaPlayer.getCurrentPosition());
            outState.putBoolean(VIDEO_IS_PLAYED_ARG, mediaPlayer.isPlaying());
        }
    }

    private void loadVideoParams(Bundle savedInstanceState) {
        currentPlaybackPosition = savedInstanceState.getInt(VIDEO_POSITION_ARG, 0);
        isVideoPlaying = savedInstanceState.getBoolean(VIDEO_IS_PLAYED_ARG, true);
    }

    private void showButtonPanel(boolean show) {
        if (show) {
            buttonPanel.setVisibility(View.VISIBLE);
        } else {
            buttonPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        Intent resultIntent = new Intent();
        if (view.getId() == R.id.confirm_media_result) {
            hideToolTipView();
            showPromptDialog(PromptDialog.CLICK_FLAG_SUCCESS);
            //  resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CONFIRM).putExtra(FILE_PATH_ARG, previewFilePath);
        } else if (view.getId() == R.id.re_take_media) {
            showPromptDialog(PromptDialog.CLICK_FLAG_REPEAT);
            //   deleteMediaFile();
            //    resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_RETAKE);
        } else if (view.getId() == R.id.cancel_media_action) {
            showPromptDialog(PromptDialog.CLICK_FLAG_CANCEL_PREVIEW);
            //deleteMediaFile();
            // resultIntent.putExtra(RESPONSE_CODE_ARG, BaseAnncaActivity.ACTION_CANCEL);
        }
        //  setResult(RESULT_OK, resultIntent);
        //  finish();
    }


    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        //deleteMediaFile();
        showPromptDialog(PromptDialog.CLICK_FLAG_BACK_PREVIEW);

    }

    private boolean deleteMediaFile() {
        File mediaFile = new File(previewFilePath);
        return mediaFile.delete();
    }

    public static boolean isResultConfirm(@NonNull Intent resultIntent) {
        return BaseAnncaActivity.ACTION_CONFIRM == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1);
    }

    public static String getMediaFilePatch(@NonNull Intent resultIntent) {
        return resultIntent.getStringExtra(FILE_PATH_ARG);
    }

    public static boolean isResultRetake(@NonNull Intent resultIntent) {
        return BaseAnncaActivity.ACTION_RETAKE == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1);
    }

    public static boolean isResultCancel(@NonNull Intent resultIntent) {
        return BaseAnncaActivity.ACTION_CANCEL == resultIntent.getIntExtra(RESPONSE_CODE_ARG, -1);
    }


    /**
     * Creates and returns intent filter for the BroadcastReceiver
     *
     * @return filter for actions
     */
    private IntentFilter GetIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        return intentFilter;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check for internet
            if ((NetworkUtils.isInternetAvailable(PreviewActivity.this))) {
                Log.e("Internet", "availabel");
                if (flagShowView > 0) {
                    showStripeView(getResources().getString(R.string.online),
                            getResources().getColor(R.color.color_green));
                    removeCallBack();
                    runnableCallForStripeView();
                    flagShowView = 0;
                }

                return;
            }
            if (flagShowView == 0) {
                removeCallBack();
                showStripeView(getResources().getString(R.string.offline)
                        , getResources().getColor(R.color.color_red));
                flagShowView = 1;
            }

            Log.e("Internet", "Internet connection could not be established. Please try later.");

        }
    };

    private void showStripeView(String msg, int backgroundColor) {
        if (tvInternetStrip != null) {
            tvInternetStrip.setVisibility(View.VISIBLE);
            tvInternetStrip.setText(msg);
            tvInternetStrip.setBackgroundColor(backgroundColor);
        }
    }

    private void runnableCallForStripeView() {
        if (tvInternetStrip != null) {
            tvInternetStrip.removeCallbacks(runnableStripeView);
            tvInternetStrip.postDelayed(runnableStripeView, timerRunnable);
        }
    }

    private void removeCallBack() {
        if (tvInternetStrip != null) {
            tvInternetStrip.removeCallbacks(runnableStripeView);
        }
    }

    Runnable runnableStripeView = new Runnable() {

        @Override
        public void run() {
            tvInternetStrip.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, GetIntentFilter());
        if (mediaPlayer != null) {
            Log.e("seekTo_1", String.valueOf(seekTo));
//            mediaPlayer.start();
//            mediaPlayer.seekTo(currentPlaybackPosition);
//            mediaPlayer=null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPauseMediaPlayer = true;
            mediaController = null;
            seekTo = mediaPlayer.getCurrentPosition();
            currentPlaybackPosition = mediaPlayer.getCurrentPosition();
        }
        Log.e("seekTo_0", String.valueOf(seekTo));
        unregisterReceiver(mReceiver);
        tvInternetStrip.removeCallbacks(runnableStripeView);
    }

    public void showToolTip() {
        confirmMediaResult.postDelayed(new Runnable() {
            @Override
            public void run() {
                int position = ToolTip.POSITION_ABOVE;
                // define alignment
                int align = ToolTip.ALIGN_RIGHT;
                // create method
                displayToolTip(position, align);
                hideToolTip();

            }
        }, 100);
    }

    private void hideToolTip() {
        confirmMediaResult.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideToolTipView();
            }
        }, 5000);
    }


    private void displayToolTip(int position, int align) {
        // get message from edit text
        String sMessage = getResources().getString(R.string.msgSaveRecording);
        // set tooltip on text view
        toolTipsManager.findAndDismiss(confirmMediaResult);
        // check condition
        if (!sMessage.isEmpty()) {
            // when message is not equal to empty
            // create tooltip
            ToolTip.Builder builder = new ToolTip.Builder(this, confirmMediaResult
                    , preview_activity_container, sMessage, position);
            builder.withArrow(true);
            // set align
            builder.setAlign(align);
            // set background color
            builder.setBackgroundColor(getResources().getColor(R.color.color_green));
            // show tooltip
            toolTip = builder.build();
            toolTipsManager.show(toolTip);

        }
    }

    private void hideToolTipView() {
        if (toolTipsManager != null && toolTipsManager.isVisible(toolTip.getAnchorView())) {
            toolTipsManager.dismissAll();
        }
    }

    ToolTipsManager.TipListener tipListener = new ToolTipsManager.TipListener() {
        @Override
        public void onTipDismissed(View view, int anchorViewId, boolean byUser) {

        }
    };
}
