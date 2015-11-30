package de.mm.android.longitude.model;

import com.google.example.games.basegameutils.GameHelperUtils;

/**
 * Created by Max on 10.04.2015.
 */
public class SignInFailureReason {
    public static final int NO_ACTIVITY_RESULT_CODE = -100;

    private int mServiceErrorCode = 0;
    private int mActivityResultCode = NO_ACTIVITY_RESULT_CODE;

    public SignInFailureReason(int serviceErrorCode, int activityResultCode) {
        mServiceErrorCode = serviceErrorCode;
        mActivityResultCode = activityResultCode;
    }

    public SignInFailureReason(int serviceErrorCode) {
        this(serviceErrorCode, NO_ACTIVITY_RESULT_CODE);
    }

    public int getServiceErrorCode() {
        return mServiceErrorCode;
    }

    public int getActivityResultCode() {
        return mActivityResultCode;
    }

    public String getServiceErrorText(){
        return GameHelperUtils.errorCodeToString(mServiceErrorCode);
    }

    public String geActivityResultText(){
        return GameHelperUtils.activityResponseCodeToString(mActivityResultCode);
    }

    @Override
    public String toString() {
        return "SignInFailureReason(serviceErrorCode:"
                + GameHelperUtils.errorCodeToString(mServiceErrorCode)
                + ((mActivityResultCode == NO_ACTIVITY_RESULT_CODE) ? ")"
                : (",activityResultCode:"
                + GameHelperUtils.activityResponseCodeToString(mActivityResultCode) + ")"));
    }
}
