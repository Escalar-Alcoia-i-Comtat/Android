package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.RequiredSafesData
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.EquipmentAdapterType
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import kotlinx.android.synthetic.main.dialog_path_equipment.*

@ExperimentalUnsignedTypes
class PathEquipmentDialog(context: Context, private val fixedSafesData: FixedSafesData, private val requiredSafesData: RequiredSafesData): Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_path_equipment)

        pathEquipment_recyclerView.layoutManager = LinearLayoutManager(context)
        pathEquipment_recyclerView.adapter = EquipmentAdapter(context, fixedSafesData, EquipmentAdapterType.FIXED)

        pathRequiredEquipment_recyclerView.layoutManager = LinearLayoutManager(context)
        pathRequiredEquipment_recyclerView.adapter = EquipmentAdapter(context, requiredSafesData, EquipmentAdapterType.REQUIRED)
        visibility(pathRequiredTitle_textView, requiredSafesData.any())
    }
}