package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogPathEquipmentBinding
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapterType
import com.arnyminerz.escalaralcoiaicomtat.view.visibility

@ExperimentalUnsignedTypes
class PathEquipmentDialog(context: Context, private val fixedSafesData: FixedSafesData, private val requiredSafesData: RequiredSafesData): Dialog(context) {
    private lateinit var binding: DialogPathEquipmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPathEquipmentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.pathEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pathEquipmentRecyclerView.adapter = EquipmentAdapter(context, fixedSafesData, EquipmentAdapterType.FIXED)

        binding.pathRequiredEquipmentRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.pathRequiredEquipmentRecyclerView.adapter = EquipmentAdapter(context, requiredSafesData, EquipmentAdapterType.REQUIRED)
        visibility(binding.pathRequiredTitleTextView, requiredSafesData.any())
    }
}