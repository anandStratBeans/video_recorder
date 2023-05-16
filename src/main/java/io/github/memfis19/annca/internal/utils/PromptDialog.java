package io.github.memfis19.annca.internal.utils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.github.memfis19.annca.R;

public class PromptDialog {
    public static final int CLICK_FLAG_BACK = 1;
    public static final int CLICK_FLAG_REPEAT = 2;
    public static final int CLICK_FLAG_SUCCESS = 3;
    public static final int CLICK_FLAG_CANCEL = 4;
    public static final int CLICK_FLAG_CANCEL_PREVIEW = 7;
    public static final int CLICK_FLAG_BACK_PREVIEW = 8;
    public static final int CLICK_FLAG_BACK1 = 5;
    public static final int CLICK_FLAG_RECORD_VIDEO = 6;

    public long timeLeft = 0;
    public int min = 0;
    public int max = 0;
    public int netWorkType = -1;


    public interface OnDialogButtonClick {
        void setPositiveButtonClick(int clickFlag);

        void setNegativeButtonClick(int clickFlag);
    }

    AlertDialog.Builder dialogBuilder;
    Activity activity;
    OnDialogButtonClick dialogButtonClick;
    int clickFlag = 0;
    AlertDialog dialog;
    boolean isShowCancelButton = true;

    public PromptDialog(Activity activity, OnDialogButtonClick dialogButtonClick) {
        this.activity = activity;
        dialogBuilder = new AlertDialog.Builder(activity);
        this.dialogButtonClick = dialogButtonClick;
    }

    public void setClickFlag(int clickFlag) {
        this.clickFlag = clickFlag;
    }

    public void showProgressDialog() {
        dialogBuilder.setTitle(getTitle());
        dialogBuilder.setMessage(getMessage());
        String okButton = activity.getResources().getString(R.string.ok_label);
        String cancelButton = activity.getResources().getString(R.string.cancel_label);
        dialogBuilder.setPositiveButton(okButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialogButtonClick.setPositiveButtonClick(clickFlag);
            }
        });


        dialogBuilder.setNegativeButton(cancelButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialogButtonClick.setNegativeButtonClick(clickFlag);
            }
        });

        dialog = dialogBuilder.create();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    public void showProgressDialogCancelBack(boolean isHideCancelButton) {
        dialogBuilder.setTitle(getTitle());
        dialogBuilder.setMessage(getMessage());
        String okButton = activity.getResources().getString(R.string.saveCloseLbl);
        String cancelButton = activity.getResources().getString(R.string.dontSaveLbl);
        dialogBuilder.setPositiveButton(okButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialogButtonClick.setPositiveButtonClick(clickFlag);
            }
        });

        if (!isHideCancelButton) {
            dialogBuilder.setNegativeButton(cancelButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    dialogButtonClick.setNegativeButtonClick(clickFlag);
                }
            });
        }

        dialog = dialogBuilder.create();

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    public void showVideoMessageDialog() {
        dialogBuilder.setTitle(getTitle());
        dialogBuilder.setMessage(messageCaptureVideoDialog());
        String okButton = activity.getResources().getString(R.string.ok_label);
        String cancelButton = activity.getResources().getString(R.string.cancel_label);
        dialogBuilder.setPositiveButton(okButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialogButtonClick.setPositiveButtonClick(clickFlag);
            }
        });
        dialog = dialogBuilder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private String getTitle() {
        switch (clickFlag) {
            case CLICK_FLAG_BACK:
            case CLICK_FLAG_BACK1:
            case CLICK_FLAG_BACK_PREVIEW:
                return activity.getResources().getString(R.string.back_label);
            case CLICK_FLAG_CANCEL:
            case CLICK_FLAG_CANCEL_PREVIEW:
                return activity.getResources().getString(R.string.cancel_label);
            case CLICK_FLAG_REPEAT:
                return activity.getResources().getString(R.string.repeat_label);
            case CLICK_FLAG_SUCCESS:
                return activity.getResources().getString(R.string.success_label);
            case CLICK_FLAG_RECORD_VIDEO:
                return activity.getResources().getString(R.string.recordVideo);
            default:
                return activity.getResources().getString(R.string.cancel_label);
        }
    }



    private String getMessage() {
        switch (clickFlag) {
            case CLICK_FLAG_BACK:
                return activity.getResources().getString(R.string.backMessage);
            case CLICK_FLAG_BACK1:
                return activity.getResources().getString(R.string.backMessage1);
            case CLICK_FLAG_CANCEL:
                return activity.getResources().getString(R.string.cancelMessage);
            case CLICK_FLAG_REPEAT:
                return activity.getResources().getString(R.string.repeatMessage);
            case CLICK_FLAG_SUCCESS:
                return activity.getResources().getString(R.string.successMessage);
            case CLICK_FLAG_RECORD_VIDEO:
                String msg = messageVideoDialog().toString();
                return msg;
            case CLICK_FLAG_BACK_PREVIEW:
            case CLICK_FLAG_CANCEL_PREVIEW:
                return activity.getResources().getString(R.string.backCancleMsg);
            default:
                return activity.getResources().getString(R.string.cancelMessage);
        }
    }

    public void setMessageForNetWorkChange(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog.isShowing())
                {
                    dialog.dismiss();
                    showVideoMessageDialog();
                }
            }
        });

    }

    public SpannableString messageCaptureVideoDialog() {
        if (timeLeft < max) {
            return messageVideoDialogTimeLeft();
        } else {
            return messageVideoDialog();
        }
    }



    public SpannableString messageVideoDialog() {
        String msgPart1 = activity.getResources().getString(R.string.videoDialogMessage);
        String msgPart2 = activity.getResources().getString(R.string.videoDialogMessageEnd);
        String minTime = convertSecondsToMmSs(min);
        String maxTime = convertSecondsToMmSs(max);
        String minMessage = String.format(activity.getResources().getString(R.string.messageMin), minTime);
        String maxMessage = String.format(activity.getResources().getString(R.string.messageMax), maxTime);
        String lblAnd = activity.getResources().getString(R.string.lblAnd);
        String lblClickContinue = activity.getResources().getString(R.string.clickToContinue);
        String networkString = activity.getResources().getString(R.string.messageNetworkType);
        SpannableString msg;

        if (netWorkType == ConnectivityManager.TYPE_MOBILE) {
            msg = new SpannableString(msgPart1 + " " + minMessage + " " + lblAnd
                    + " " + maxMessage + " " + msgPart2 + lblClickContinue + networkString);
        } else {
            msg = new SpannableString(msgPart1 + " " + minMessage + " " + lblAnd
                    + " " + maxMessage + " " + msgPart2 + lblClickContinue);
        }
        msg.setSpan(new StyleSpan(Typeface.BOLD), msgPart1.length() + 1, msgPart1.length() + 1 + minMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        msg.setSpan(new StyleSpan(Typeface.BOLD), msgPart1.length() + 1 + minMessage.length() + 1 + lblAnd.length() + 1, msgPart1.length() + 1 + minMessage.length() + 1 + lblAnd.length() + 1 + maxMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (netWorkType == ConnectivityManager.TYPE_MOBILE) {
            int start = msgPart1.length() + 1 + minMessage.length() + 1 + lblAnd.length() + 1 + maxMessage.length() + msgPart2.length() + lblClickContinue.length() + 1;
            int end = msgPart1.length() + 1 + minMessage.length() + 1 + lblAnd.length() + 1 + maxMessage.length() + msgPart2.length() + lblClickContinue.length() + 1 + networkString.length();
            msg.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            msg.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return msg;
    }


    public SpannableString messageVideoDialogTimeLeft() {
        String msgPart1 = activity.getResources().getString(R.string.videoDialogMessage);
        String msgPart2 = activity.getResources().getString(R.string.videoDialogMessageEnd);
        String minTime = convertSecondsToMmSs(timeLeft);
        String maxTime = convertSecondsToMmSs(max);
        String minMessage = String.format(activity.getResources().getString(R.string.messageMin), minTime);
        String lblClickContinue = activity.getResources().getString(R.string.clickToContinue);
        String networkString = activity.getResources().getString(R.string.messageNetworkType);
        SpannableString msg;

        if (netWorkType == ConnectivityManager.TYPE_MOBILE) {
            msg = new SpannableString(msgPart1 + " " + minMessage + " " + msgPart2 + lblClickContinue + networkString);
        } else {
            msg = new SpannableString(msgPart1 + " " + minMessage + " " + msgPart2 + lblClickContinue);
        }
        msg.setSpan(new StyleSpan(Typeface.BOLD), msgPart1.length() + 1, msgPart1.length() + 1 + minMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (netWorkType == ConnectivityManager.TYPE_MOBILE) {
            int start = msgPart1.length() + 1 + minMessage.length() + msgPart2.length() + lblClickContinue.length() + 1;
            int end = msgPart1.length() + 1 + minMessage.length() + msgPart2.length() + lblClickContinue.length() + 1 + networkString.length();
            msg.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            msg.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return msg;
    }

    public static String convertSecondsToHMmSs(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;

        return String.format("%02d:%02d", m, s);
    }

    public static String convertSecondsMM(long seconds) {
        long m = (seconds / 60) % 60;
        return String.format("%02d", m);
    }

    public void setMin(int min) {
        this.min = min;
    }

    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }

    public void setMinMax(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static String convertSecondsToMmSs(long seconds) {
        if (seconds == 0) {
            return "0 Seconds";
        }
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        if (m != 0) {
            return String.format("%d", m) + " Minutes";
        } else {
            return String.format("%d", s) + " Seconds";
        }
    }

    public void setNetworkType(int netWorkType) {
        this.netWorkType = netWorkType;
    }


    public boolean isShowingDialog(){
        return  dialog.isShowing();
    }
}
