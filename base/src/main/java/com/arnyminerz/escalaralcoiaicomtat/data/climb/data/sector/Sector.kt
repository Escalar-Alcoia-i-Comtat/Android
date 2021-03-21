package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.parse.fetchPinOrNetworkSync
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.Path
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.TIMESTAMP_FORMAT
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.toTimestamp
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

    @WorkerThread
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

    fun kidsAptChip(context: Context, chip: Chip) {
        visibility(chip, kidsApt)
        if (kidsApt)
            chip.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.sector_info_dialog_kids_title)
                    .setMessage(R.string.sector_info_dialog_kids_msg)
                    .setPositiveButton(R.string.action_close) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
    }

    fun walkingTimeView(context: Context, view: View) {
        if (view is TextView)
            view.text = String.format(view.text.toString(), walkingTime.toString())
        if (location != null) {
            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "geo:0,0?q=${location.latitude},${location.longitude}($displayName)".replace(" ", "+")
                )
            ).setPackage("com.google.android.apps.maps")
            view.setOnClickListener {
                context.startActivity(mapIntent)
            }
        } else {
            view.isClickable = false
            Timber.e("Sector doesn't have any location stored")
        }
    }

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
