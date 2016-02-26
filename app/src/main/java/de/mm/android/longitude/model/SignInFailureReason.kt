package de.mm.android.longitude.model

import com.google.example.games.basegameutils.GameHelperUtils

/**
 * Created by Max on 10.04.2015.
 */
class SignInFailureReason (val serviceErrorCode: Int, val activityResultCode: Int = SignInFailureReason.NO_ACTIVITY_RESULT_CODE) {

    fun getServiceErrorText(): String {
        return GameHelperUtils.errorCodeToString(serviceErrorCode)
    }

    fun getActivityResultText(): String {
        return GameHelperUtils.activityResponseCodeToString(activityResultCode)
    }

    override fun toString(): String {
        return "SignInFailureReason(serviceErrorCode:" + GameHelperUtils.errorCodeToString(serviceErrorCode) +
                ", activityResultCode:" + GameHelperUtils.activityResponseCodeToString(activityResultCode) + ")"
    }

    companion object {
        val NO_ACTIVITY_RESULT_CODE = -100
    }
}
