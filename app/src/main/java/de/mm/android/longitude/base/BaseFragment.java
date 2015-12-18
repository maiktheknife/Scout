package de.mm.android.longitude.base;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import de.mm.android.longitude.util.NetworkUtil;

/**
 * Base Fragment which provided Connectivity status value {@link #showMessage} Methods
 */
public abstract class BaseFragment extends Fragment {
    protected boolean isInetAvailable;

    @Override
    public void onStart() {
        super.onStart();
        isInetAvailable = NetworkUtil.isInternetAvailable(getActivity());
    }

    protected void showMessage(@StringRes int errorMessage) {
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    protected void showMessage(@NonNull String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

}
