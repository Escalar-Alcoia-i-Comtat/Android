package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_completed_paths.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalUnsignedTypes
class CompletedPathsDialog(
    private val user: UserData,
    private val networkState: ConnectivityProvider.NetworkState
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = with(inflater.inflate(R.layout.dialog_completed_paths, container)) {
        visibility(completedPaths_progressBar, true)
        visibility(completedPaths_recyclerView, false)

        Timber.d("Getting paths...")
        GlobalScope.launch {
            val completedPathsFlow = user.completedPaths(networkState)
            val completedPaths = completedPathsFlow.toCollection(arrayListOf())
            val c = requireContext()

            Timber.d("Got ${completedPaths.size} paths.")

            runOnUiThread {
                visibility(completedPaths_progressBar, false)
                visibility(completedPaths_recyclerView, true)

                completedPaths_recyclerView.layoutManager =
                    LinearLayoutManager(c)
                completedPaths_recyclerView.adapter =
                    CompletedPathBigAdapter(c, completedPaths)

                val verticalDecoration = DividerItemDecoration(
                    completedPaths_recyclerView.context,
                    DividerItemDecoration.HORIZONTAL
                )
                val verticalDivider = getDrawable(R.drawable.vertical_divider)!!
                verticalDecoration.setDrawable(verticalDivider)
                completedPaths_recyclerView.addItemDecoration(verticalDecoration)
            }
        }

        return@with this
    }
}