package de.mm.android.longitude.fragment

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by Max on 09.08.2015.
 */
class IntroFragment : Fragment() {

    private var layoutResId: Int = 0

    fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID)
    }

    fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        return inflater.inflate(layoutResId, container, false)
    }

    companion object {
        private val ARG_LAYOUT_RES_ID = "layoutResId"

        fun newInstance(@LayoutRes layoutResId: Int): IntroFragment {
            val sampleSlide = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES_ID, layoutResId)
            sampleSlide.setArguments(args)
            return sampleSlide
        }
    }

}