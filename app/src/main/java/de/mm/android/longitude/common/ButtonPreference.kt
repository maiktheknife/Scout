package de.mm.android.longitude.common

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View

import de.mm.android.longitude.R

/**
 * Created by Max on 12.04.2015.
 */
class ButtonPreference : Preference {

    private var listener: OnPreferenceClickListener? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        widgetLayoutResource = R.layout.preference
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        view.findViewById(R.id.preference_button).setOnClickListener { v -> listener?.onPreferenceClick(this) }
    }

    fun setOnButtonClickListener(listener: OnPreferenceClickListener) {
        this.listener = listener
    }

}

