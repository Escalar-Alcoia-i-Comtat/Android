package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.TextView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.enum.SunTime
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.*
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.view.BarChartHelper
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.google.android.libraries.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import timber.log.Timber
import java.util.*

@ExperimentalUnsignedTypes
data class Sector constructor(
    override val id: Int,
    override val version: Int,
    override val displayName: String,
    override val timestamp: Date?,
    val sunTime: SunTime,
    val kidsApt: Boolean,
    val walkingTime: Int,
    val location: LatLng?,
    override val imageUrl: String,
    override val parentId: Int
) : DataClass<Path, Zone>(
    id,
    version,
    displayName,
    timestamp,
    imageUrl,
    R.drawable.ic_wide_placeholder,
    R.drawable.ic_wide_placeholder,
    parentId,
    NAMESPACE
) {

    constructor(json: JSONObject) : this(
        json.getInt("id"), // id
        json.getInt("version", 0), // version
        json.getString("display_name"), // displayName
        json.getTimestampSafe("timestamp"), // timestamp
        SunTime.find(json.getInt("sun_time")), // sunTime
        json.getBooleanFromString("kids_apt"), // kidsApt
        json.getInt("walking_time"), // walkingTime
        json.getLatLngSafe("location"),
        json.getString("image"),
        json.getInt("climbing_zone")
    ) {
        val sector = fromDB(json)
        this.children.addAll(sector.children)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(version)
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
        parcel.writeInt(parentId)
        parcel.writeList(children)
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(), // Id
        parcel.readInt(), // Version
        parcel.readString()!!, // Display Name
        parcel.readString().toTimestamp(), // Timestamp
        SunTime.find(parcel.readInt()), // Sun Time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) parcel.readBoolean() else parcel.readInt() == 1, // Kids Apt
        parcel.readInt(), // Walking Time
        parcel.readParcelable<LatLng?>(LatLng::class.java.classLoader),
        parcel.readString()!!, // Image Url
        parcel.readInt() // Parent Id
    ) {
        parcel.readList(children, Path::class.java.classLoader)
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
                    "geo:0,0?q=${location.latitude},${location.longitude}(${
                        displayName.replace(" ", "+")
                    })"
                )
            ).setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context.packageManager) != null)
                view.setOnClickListener {
                    context.startActivity(mapIntent)
                }
            else {
                view.setOnClickListener {
                    context.toast(R.string.toast_error_gmaps_not_installed)
                }
                Timber.e("Could not resolve activity")
            }
        } else {
            view.isClickable = false
            Timber.e("Sector doesn't have any location stored")
        }
    }

    fun loadChart(context: Context, chart: BarChart) {
        val chartHelper = BarChartHelper.fromPaths(context, children)
        with(chart) {
            data = chartHelper.data
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
        }

        val xAxis = chart.xAxis
        xAxis.granularity = 1f
        xAxis.valueFormatter = chartHelper.xFormatter
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        chartHelper.removeStyles(chart.axisLeft)
        chartHelper.removeStyles(chart.axisRight)

        val legend = chart.legend
        legend.isEnabled = false

        with(chart) {
            setDrawGridBackground(false)
            setDrawBorders(false)
            description = with(Description()) {
                text = ""
                this
            }

            invalidate()
        }
    }

    companion object CREATOR : Parcelable.Creator<Sector> {
        override fun createFromParcel(parcel: Parcel): Sector = Sector(parcel)
        override fun newArray(size: Int): Array<Sector?> = arrayOfNulls(size)

        fun fromDB(json: JSONObject): Sector {
            val sector = Sector(
                json.getInt("id"),
                json.getInt("version", 0),
                json.getString("display_name"),
                json.getTimestampSafe("timestamp"),
                SunTime.find(json.getInt("sun_time")),
                json.getBooleanFromString("kids_apt"),
                json.getInt("walking_time"),
                json.getLatLngSafe("location"),
                json.getString("image"),
                json.getInt("climbing_zone")
            )
            if (json.has("paths")) {
                val paths = json.getJSONArray("paths")
                Timber.v("Sector has paths, adding them. Count: ${paths.length()}")
                for (p in 0 until paths.length()) {
                    val path = paths.getJSONObject(p)
                    sector.children.add(Path.fromDB(path))
                }
            }
            return sector
        }

        fun fromId(id: Int): Sector {
            val json = jsonFromUrl("$EXTENDED_API_URL/sector/$id")

            return fromDB(json)
        }

        const val NAMESPACE = "sector"
    }
}