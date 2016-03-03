package de.mm.android.longitude.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater

import de.mm.android.longitude.R
import it.gmariotti.changelibs.library.view.ChangeLogRecyclerView

/**
 * Created by Max on 10.09.2015.
 */
class ChangeLogDialogFragment : DialogFragment() {

    fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val chgList = LayoutInflater.from(getActivity()).inflate(R.layout.changelog, null, false) as ChangeLogRecyclerView

        return AlertDialog.Builder(getActivity())
                .setTitle(R.string.changelog_title)
                .setView(chgList)
                .setPositiveButton(android.R.string.ok, { dialog, whichButton -> dialog.dismiss() })
                .create()
    }

}
