package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogPathEquipmentBinding
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapterType

class PathEquipmentDialog(
    context: Context,
    private val fixedSafesData: FixedSafesData,
    private val requiredSafesData: RequiredSafesData
) : Dialog(context, R.style.ThemeOverlay_App_AlertDialog) {
    private lateinit var binding: DialogPathEquipmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPathEquipmentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.pathEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pathEquipmentRecyclerView.adapter =
            EquipmentAdapter(context, fixedSafesData, EquipmentAdapterType.FIXED)

        binding.pathRequiredEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pathRequiredEquipmentRecyclerView.adapter =
            EquipmentAdapter(context, requiredSafesData, EquipmentAdapterType.REQUIRED)
        visibility(binding.pathRequiredTitleTextView, requiredSafesData.any())
    }
}
