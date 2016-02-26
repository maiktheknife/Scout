package de.mm.android.longitude.base

import android.support.annotation.NonNull
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.widget.Toast

import de.mm.android.longitude.util.NetworkUtil

/**
 * Base Fragment which provided Connectivity status value [.showMessage] Methods
 */
abstract class BaseFragment : Fragment() {
    protected var isInetAvailable: Boolean = false

    override fun onStart() {
        super.onStart()
        isInetAvailable = NetworkUtil.isInternetAvailable(activity)
    }

    protected fun showMessage(@StringRes errorMessage: Int) {
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
    }

    protected fun showMessage(@NonNull message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

}
