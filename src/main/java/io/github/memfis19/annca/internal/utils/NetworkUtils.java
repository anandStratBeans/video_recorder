package io.github.memfis19.annca.internal.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;


public class NetworkUtils {
    /*
     * This checks whether the Internet connectivity is available.
     *
     * @return true if available
     */
    public static boolean isInternetAvailable(Activity activity) {

        /*Using application context reduces the chance of a memory leak.*/
        ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static void showToast(Activity activity,String message)
    {
        Toast.makeText(activity,message,Toast.LENGTH_LONG).show();
    }
}
