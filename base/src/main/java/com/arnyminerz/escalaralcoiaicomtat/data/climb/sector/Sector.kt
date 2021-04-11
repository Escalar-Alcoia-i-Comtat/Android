package com.arnyminerz.escalaralcoiaicomtat.data.climb.sector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toUri
import com.arnyminerz.escalaralcoiaicomtat.generic.fixTildes
import com.arnyminerz.escalaralcoiaicomtat.view.BarChartHelper
import com.arnyminerz.escalaralcoiaicomtat.view.getAttribute
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.mapboxsdk.geometry.LatLng
import timber.log.Timber
import java.util.Date

class Sector constructor(
    objectId: String,
    displayName: String,
    timestamp: Date?,
    val sunTime: SunTime,
    val kidsApt: Boolean,
    val walkingTime: Int,
    val location: LatLng?,
    imageUrl: String,
    documentPath: String,
) : DataClass<Path, Zone>(
    objectId,
    displayName,
    timestamp,
    imageUrl,
    null,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    NAMESPACE,
    documentPath
) {
    /**
     * Creates a new [Sector] from the data of a [DocumentSnapshot].
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getString("displayName")!!.fixTildes(),
        data.getDate("timestamp"),
        SunTime.find(data.get("sunTime", Int::class.java)!!),
        data.getBoolean("kidsApt") ?: false,
        data.get("walkingTime", Int::class.java)!!,
        data.getGeoPoint("location")?.toLatLng(),
        data.getString("image")!!.fixTildes(),
        documentPath = data.reference.path
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeInt(sunTime.value)
        parcel.writeInt(if (kidsApt) 1 else 0)
        parcel.writeInt(walkingTime)
        parcel.writeParcelable(location, 0)
        parcel.writeString(imageUrl)
        parcel.writeString(documentPath)
        parcel.writeList(innerChildren)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!, // objectId
        parcel.readString()!!, // Display Name
        parcel.readString().toTimestamp(), // Timestamp
        SunTime.find(parcel.readInt()), // Sun Time
        parcel.readInt() == 1, // Kids Apt
        parcel.readInt(), // Walking Time
        parcel.readParcelable<LatLng?>(LatLng::class.java.classLoader),
        parcel.readString()!!, // Image Url
        parcel.readString()!!, // Pointer
    ) {
        parcel.readList(innerChildren, Path::class.java.classLoader)
    }

    /**
     * Loads the Sector's children Paths
     * @author Arnau Mora
     * @since 20210411
     * @return The loaded Paths list
     * @see Path
     */
    @WorkerThread
    override fun loadChildren(): List<Path> {
        val result = arrayListOf<Path>()

        Timber.d("Getting Firestore Instance...")
        val firebaseDatabase = Firebase.firestore

        Timber.d("Fetching...")
        val ref = firebaseDatabase
            .document(documentPath)
            .collection("Paths")
        val childTask = ref.get()
        Tasks.await(childTask)
        val snapshot = childTask.result
        val e = childTask.exception
        if (!childTask.isSuccessful || snapshot == null) {
            Timber.w(e, "Could not get.")
            e?.let { throw it }
        } else {
            val paths = snapshot.documents
            Timber.d("Got ${paths.size} elements.")
            for (l in paths.indices)
                result.add(Path(paths[l]))
        }
        return result
    }

    override fun describeContents(): Int = 0

    /**
     * Sets the content for the chip as a kids apt chip.
     * @author Arnau Mora
     * @since 20210323
     * @param context The context to call from
     * @param chip The chip to update
     * @see kidsApt
     */
    @UiThread
    fun kidsAptChip(context: Context, chip: Chip) {
        visibility(chip, kidsApt)
        if (kidsApt)
            chip.setOnClickListener {
                MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle(R.string.sector_info_dialog_kids_title)
                    .setMessage(R.string.sector_info_dialog_kids_msg)
                    .setPositiveButton(R.string.action_close) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
    }

    /**
     * Updates the walking time [TextView]. Sets the text, and if [location] is not null, sets its
     * click action to open it into the maps app.
     * @author Arnau Mora
     * @since 20210323
     * @param context The context to call from
     * @param textView The walking time TextView
     * @see location
     * @see LatLng.toUri
     */
    @UiThread
    fun walkingTimeView(context: Context, textView: TextView) {
        textView.text = String.format(textView.text.toString(), walkingTime.toString())
        if (location != null) {
            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                location.toUri()
            )
            textView.setOnClickListener {
                context.startActivity(mapIntent)
            }
        } else {
            textView.isClickable = false
            Timber.w("Sector doesn't have any location stored")
        }
    }

    /**
     * Loads the Sector's chart. Sets all the styles and loads the data from the instance.
     * @author Arnau Mora
     * @since 20210323
     * @param activity The [Activity] to call from
     * @param chart The [BarChart] to update
     */
    @WorkerThread
    fun loadChart(activity: Activity, chart: BarChart) {
        val chartHelper = BarChartHelper.fromPaths(activity, getChildren())
        activity.runOnUiThread {
            with(chart) {
                data = chartHelper.barData
                setFitBars(false)
                setNoDataText(context.getString(R.string.error_chart_no_data))
                setDrawGridBackground(false)
                setDrawBorders(false)
                setDrawBarShadow(false)
                setDrawMarkers(false)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                isHighlightPerTapEnabled = false
                isHighlightPerDragEnabled = false
                legend.isEnabled = false

                description = with(Description()) {
                    text = ""
                    this
                }

                val valueTextColor = getAttribute(context, R.attr.text_dark)
                xAxis.apply {
                    granularity = 1f
                    valueFormatter = chartHelper.xFormatter
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = valueTextColor
                }
                chartHelper.barData.setValueTextColor(valueTextColor)

                chartHelper.removeStyles(axisLeft)
                chartHelper.removeStyles(axisRight)

                invalidate()
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Sector> {
        override fun createFromParcel(parcel: Parcel): Sector = Sector(parcel)
        override fun newArray(size: Int): Array<Sector?> = arrayOfNulls(size)

        const val NAMESPACE = "Sector"
    }
}
