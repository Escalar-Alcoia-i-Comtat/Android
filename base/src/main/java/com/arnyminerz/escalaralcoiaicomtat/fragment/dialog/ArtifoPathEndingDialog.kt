package com.arnyminerz.escalaralcoiaicomtat.fragment.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.EndingType
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.databinding.DialogArtifoEndingBinding
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ArtifoEndingPitchAdapter
import timber.log.Timber

class ArtifoPathEndingDialog(
    context: Context,
    private val endings: List<EndingType>,
    private val pitches: List<Pitch>
) : Dialog(context, R.style.ThemeOverlay_App_AlertDialog) {

    private lateinit var binding: DialogArtifoEndingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogArtifoEndingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        Timber.v("Endings: $endings")
        Timber.v("Pitches: $pitches")

        binding.artifoTitleTextView.text = context.getString(endings.first().displayName)

        binding.artifoEndingTypeImageView.setImageResource(endings.first().getImage())

        val listEndings = endings.subList(1, endings.size)
        Timber.v("  List will show ${listEndings.size} endings.")
        Timber.v("  List will show ${pitches.size} pitches.")
        binding.artifoDialogRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.artifoDialogRecyclerView.adapter =
            ArtifoEndingPitchAdapter(context, listEndings, pitches)
    }
}
