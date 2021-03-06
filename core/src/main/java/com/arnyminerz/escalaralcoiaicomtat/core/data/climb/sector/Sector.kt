package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.UIMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.utils.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toTimestamp
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toUri
import com.arnyminerz.escalaralcoiaicomtat.core.view.BarChartHelper
import com.arnyminerz.escalaralcoiaicomtat.core.view.getAttribute
import com.arnyminerz.escalaralcoiaicomtat.core.view.visibility
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Sector constructor(
    objectId: String,
    displayName: String,
    timestamp: Date,
    val sunTime: SunTime,
    val kidsApt: Boolean,
    val walkingTime: Long,
    val location: LatLng?,
    val weight: String,
    imageUrl: String,
    documentPath: String,
    webUrl: String?
) : DataClass<Path, Zone>(
    displayName,
    timestamp,
    imageUrl,
    null,
    UIMetadata(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
    ),
    DataClassMetadata(
        objectId,
        NAMESPACE,
        documentPath,
        webUrl
    )
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
        data.getString("displayName")!!,
        data.getDate("created")!!,
        SunTime.find(data.getLong("sunTime")!!.toInt()),
        data.getBoolean("kidsApt") ?: false,
        data.getLong("walkingTime")!!,
        data.getGeoPoint("location")?.toLatLng(),
        data.getString("weight")!!,
        data.getString("image")!!,
        documentPath = data.reference.path,
        data.getString("webURL")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeInt(sunTime.value)
        parcel.writeInt(if (kidsApt) 1 else 0)
        parcel.writeLong(walkingTime)
        parcel.writeParcelable(location, 0)
        parcel.writeString(weight)
        parcel.writeString(imageReferenceUrl)
        parcel.writeString(metadata.documentPath)
        parcel.writeList(innerChildren)
        parcel.writeString(metadata.webURL)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!, // objectId
        parcel.readString()!!, // Display Name
        parcel.readString().toTimestamp()!!, // Timestamp
        SunTime.find(parcel.readInt()), // Sun Time
        parcel.readInt() == 1, // Kids Apt
        parcel.readLong(), // Walking Time
        parcel.readParcelable<LatLng?>(LatLng::class.java.classLoader),
        parcel.readString()!!,
        parcel.readString()!!, // Image Url
        parcel.readString()!!, // Pointer
        parcel.readString(), // Web URL
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
    override suspend fun loadChildren(firestore: FirebaseFirestore): List<Path> {
        val paths = arrayListOf<Path>()
        Timber.v("Loading Sector's children.")

        Timber.d("Fetching...")
        val ref = firestore
            .document(metadata.documentPath)
            .collection("Paths")
            .orderBy("sketchId")
        val childTask = ref.get()
        try {
            Timber.v("Awaiting results...")
            val snapshot = suspendCoroutine<QuerySnapshot> { cont ->
                childTask
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            Timber.v("Got children result")
            val pathsDocs = snapshot.documents
            Timber.d("Got ${pathsDocs.size} elements. Processing result")
            for (l in pathsDocs.indices) {
                val pathData = pathsDocs[l]
                Timber.d("Processing sector #$l")
                val path = Path(pathData)
                paths.add(path)
            }
            Timber.d("Finished loading zones")
        } catch (e: Exception) {
            Timber.w(e, "Could not get.")
            e.let { throw it }
        }
        return paths
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
     * @param paths A collection of [Path]s.
     */
    @UiThread
    fun loadChart(activity: Activity, chart: BarChart, paths: Collection<Path>) {
        val chartHelper = BarChartHelper.fromPaths(activity, paths)
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

            val valueTextColor = getAttribute(context, com.google.android.material.R.attr.colorOnPrimary)
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

    companion object CREATOR : Parcelable.Creator<Sector> {
        override fun createFromParcel(parcel: Parcel): Sector = Sector(parcel)
        override fun newArray(size: Int): Array<Sector?> = arrayOfNulls(size)

        const val NAMESPACE = "Sector"
    }
}
