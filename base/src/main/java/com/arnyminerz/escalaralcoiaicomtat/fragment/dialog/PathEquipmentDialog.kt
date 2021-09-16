package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.view.hide
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogPathEquipmentBinding
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapterType
import timber.log.Timber

/**
 * A dialog used to show the user the equipment that a [Path] has, and the equipment that is
 * required to climb it.
 * @author Arnau Mora
 * @since 20210916
 * @param context The [Context] to initialize the dialog from.
 * @param fixedSafesData The [FixedSafesData] that contains all the data to display as installed in
 * the [Path].
 * @param requiredSafesData The [RequiredSafesData] that contains all the data to display as
 * required to be brought by the user.
 */
class PathEquipmentDialog(
    context: Context,
    private val fixedSafesData: FixedSafesData,
    private val requiredSafesData: RequiredSafesData
) : Dialog(context, R.style.ThemeOverlay_App_AlertDialog) {
    private lateinit var binding: DialogPathEquipmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("Inflating dialog...")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPathEquipmentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.v("Instantiating fixed equipment adapter...")
        val fixedAdapter = EquipmentAdapter(context, fixedSafesData, EquipmentAdapterType.FIXED)
        Timber.v("Instantiating required equipment adapter...")
        val requiredAdapter =
            EquipmentAdapter(context, requiredSafesData, EquipmentAdapterType.REQUIRED)

        Timber.v("Updating path equipment recycler view...")
        binding.pathEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pathEquipmentRecyclerView.adapter = fixedAdapter

        if (requiredSafesData.any()) {
            Timber.v("Updating path required equipment recycler view.")
            binding.pathRequiredEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.pathRequiredEquipmentRecyclerView.adapter = requiredAdapter
        } else {
            Timber.v("Hiding required data recycler view since it's empty.")
            binding.pathRequiredTitleTextView.hide()
            binding.pathRequiredEquipmentRecyclerView.hide()
        }
    }
}
