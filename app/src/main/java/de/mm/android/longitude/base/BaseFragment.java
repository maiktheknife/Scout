package de.mm.android.longitude.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import de.mm.android.longitude.util.NetworkUtil;

/**
 * Base Fragment which provided ConnectivityChange Handling and {@link #showMessage} Methods
 */
public abstract class BaseFragment extends Fragment {
    private class LocalReceiver extends BroadcastReceiver {
        public final String TAG = LocalReceiver.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            switch (action) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    isInetAvailable = NetworkUtil.isInternetAvailable(context);
                    break;
                default:
                    Log.w(TAG, "Unknown Action: " + action);
                    break;
            }
        }
    }

    protected boolean isInetAvailable;
    private LocalReceiver localReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localReceiver = new LocalReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();
        setLocalWifiReceiverEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        setLocalWifiReceiverEnabled(false);
    }

    private void setLocalWifiReceiverEnabled(boolean isEnabled) {
        if (isEnabled) {
            getActivity().registerReceiver(localReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } else {
            getActivity().unregisterReceiver(localReceiver);
        }
    }

    protected void showMessage(@StringRes int errorMessage){
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    protected void showMessage(@NonNull String message){
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

}
