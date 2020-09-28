package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Pitch
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.EndingType
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ArtifoEndingPitchAdapter
import kotlinx.android.synthetic.main.dialog_artifo_ending.*
import timber.log.Timber

class ArtifoPathEndingDialog(context: Context, private val endings: ArrayList<EndingType>, private val pitches: ArrayList<Pitch>) :
    Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_artifo_ending)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        Timber.v("Endings: $endings")
        Timber.v("Pitches: $pitches")

        artifoTitle_textView.text = context.getString(endings.first().displayName)

        artifoEndingType_imageView.setImageResource(endings.first().getImage())

        val listEndings = endings.subList(1, endings.size)
        Timber.v("  List will show ${listEndings.size} endings.")
        Timber.v("  List will show ${pitches.size} pitches.")
        artifoDialog_recyclerView.layoutManager = LinearLayoutManager(context)
        artifoDialog_recyclerView.adapter = ArtifoEndingPitchAdapter(context, listEndings, pitches)
    }
}