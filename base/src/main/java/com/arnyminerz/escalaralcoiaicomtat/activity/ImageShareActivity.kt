package com.arnyminerz.escalaralcoiaicomtat.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.getSerializable
import com.arnyminerz.escalaralcoiaicomtat.generic.hasExtras
import com.arnyminerz.escalaralcoiaicomtat.generic.isNull
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.CompletedPathBigAdapter
import com.arnyminerz.escalaralcoiaicomtat.social.generateSocialImage
import kotlinx.android.synthetic.main.activity_image_share.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*

const val SHARE_PATH_ID = "share_path_id"
const val SHARE_ATTEMPTS = "share_attempts"
const val SHARE_HANGS = "share_hangs"
const val SHARE_DATE = "share_date"

@ExperimentalUnsignedTypes
class ImageShareActivity : AppCompatActivity() {
    private fun updateImage() {
        runOnUiThread {
            image_share_imageView.setImageBitmap(
                generateSocialImage(
                    this,
                    path,
                    completedPath,
                    image
                )
            )
        }
    }

    private lateinit var image: Bitmap
    private lateinit var path: Path
    private lateinit var completedPath: CompletedPathBigAdapter.CompletedPathInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_image_share)

        if (intent.isNull()) return onBackPressed()
        if (intent.extras.isNull()) return onBackPressed()
        if (!intent.hasExtras(
                SHARE_PATH_ID,
                SHARE_DATE,
                SHARE_ATTEMPTS,
                SHARE_HANGS
            )
        ) return onBackPressed()

        val extras = intent.extras!!
        completedPath = CompletedPathBigAdapter.CompletedPathInfo(
            extras.getSerializable<Date>(SHARE_DATE),
            extras.getInt(SHARE_ATTEMPTS),
            extras.getInt(SHARE_HANGS)
        )

        GlobalScope.launch {
            path = Path.fromId(extras.getInt(SHARE_PATH_ID))
            val sector = Sector.fromId(path.sectorId)
            val zone = Zone.fromId(sector.parentId)

            val sectorImageUrl = URL(zone.image)

            image = BitmapFactory.decodeStream(sectorImageUrl.openStream())
            updateImage()
        }
    }
}