package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.SunTime
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.UIMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
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
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * Creates a new [Sector] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
class Sector internal constructor(
    override val objectId: String,
    override val displayName: String,
    override val timestampMillis: Long,
    @SunTime val sunTime: Int,
    val kidsApt: Boolean,
    val walkingTime: Long,
    val location: LatLng?,
    val weight: String,
    override val imageReferenceUrl: String,
    override val documentPath: String,
    val webUrl: String?,
    val parentZoneId: String,
) : DataClass<Path, Zone>(
    displayName,
    timestampMillis,
    imageReferenceUrl,
    null,
    UIMetadata(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
    ),
    DataClassMetadata(
        objectId,
        NAMESPACE,
        Zone.NAMESPACE,
        Path.NAMESPACE,
        documentPath,
        webUrl,
        parentZoneId
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
        data.getDate("created")!!.time,
        data.getLong("sunTime")!!.toInt(),
        data.getBoolean("kidsApt") ?: false,
        data.getLong("walkingTime")!!,
        data.getGeoPoint("location")?.toLatLng(),
        data.getString("weight")!!,
        data.getString("image")!!,
        documentPath = data.reference.path,
        data.getString("webURL"),
        data.reference.parent.parent!!.id
    )

    @IgnoredOnParcel
    override val imageQuality: Int = 100

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

    companion object {
        const val NAMESPACE = "Sector"
    }
}
