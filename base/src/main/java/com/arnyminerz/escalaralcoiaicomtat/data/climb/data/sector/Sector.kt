package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.parse.fetchPinOrNetworkSync
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
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
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.mapboxsdk.geometry.LatLng
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeoutException

data class Sector constructor(
    override val objectId: String,
    override val displayName: String,
    override val timestamp: Date?,
    val sunTime: SunTime,
    val kidsApt: Boolean,
    val walkingTime: Int,
    val location: LatLng?,
    override val imageUrl: String
) : DataClass<Path, Zone>(
    objectId,
    displayName,
    timestamp,
    imageUrl,
    null,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    NAMESPACE,
    Path.NAMESPACE
) {
    /**
     * Creates a new sector from the data of a ParseObject.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210312
     * @param parseObject The object to get data from. It must be of class Sector
     * @see ParseObject
     */
    constructor(parseObject: ParseObject) : this(
        parseObject.objectId,
        parseObject.getString("displayName")!!.fixTildes(),
        parseObject.updatedAt,
        SunTime.find(parseObject.getInt("sunTime")),
        parseObject.getBoolean("kidsApt"),
        parseObject.getInt("walkingTime"),
        parseObject.getParseGeoPoint("location").toLatLng(),
        parseObject.getString("image")!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(objectId)
        parcel.writeString(displayName)
        parcel.writeString(timestamp?.let { TIMESTAMP_FORMAT.format(timestamp) })
        parcel.writeInt(sunTime.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            parcel.writeBoolean(kidsApt)
        else
            parcel.writeInt(if (kidsApt) 1 else 0)
        parcel.writeInt(walkingTime)
        parcel.writeParcelable(location, 0)
        parcel.writeString(imageUrl)
        parcel.writeList(innerChildren)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!, // objectId
        parcel.readString()!!, // Display Name
        parcel.readString().toTimestamp(), // Timestamp
        SunTime.find(parcel.readInt()), // Sun Time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) parcel.readBoolean() else parcel.readInt() == 1, // Kids Apt
        parcel.readInt(), // Walking Time
        parcel.readParcelable<LatLng?>(LatLng::class.java.classLoader),
        parcel.readString()!!, // Image Url
    ) {
        parcel.readList(innerChildren, Path::class.java.classLoader)
    }

    /**
     * Loads the Sector's children Paths
     * @author Arnau Mora
     * @since 20210323
     * @return The loaded Paths list
     * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
     * @throws TimeoutException If timeout passed before finishing the task
     * @see Path
     */
    @WorkerThread
    @Throws(NoInternetAccessException::class, TimeoutException::class)
    override fun loadChildren(): List<Path> {
        val key = namespace.toLowerCase(Locale.getDefault())
        Timber.d("Loading elements from \"$childrenNamespace\", where $key=$objectId")

        val parentQuery = ParseQuery.getQuery<ParseObject>(namespace)
        parentQuery.whereEqualTo("objectId", objectId)

        val query = ParseQuery.getQuery<ParseObject>(childrenNamespace)
        query.addAscendingOrder("sketchId")
        query.whereMatchesQuery(key, parentQuery)

        val loads = query.fetchPinOrNetworkSync(pin, true)
        Timber.d("Got ${loads.size} elements.")
        val result = arrayListOf<Path>()
        for (load in loads)
            result.add(Path(load))
        result.sortBy { it.sketchId }
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
     * @param context The context to call from
     * @param chart The [BarChart] to update
     */
    @UiThread
    fun loadChart(context: Context, chart: BarChart) {
        val chartHelper = BarChartHelper.fromPaths(context, children)
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

    companion object CREATOR : Parcelable.Creator<Sector> {
        override fun createFromParcel(parcel: Parcel): Sector = Sector(parcel)
        override fun newArray(size: Int): Array<Sector?> = arrayOfNulls(size)

        const val NAMESPACE = "Sector"
    }
}
