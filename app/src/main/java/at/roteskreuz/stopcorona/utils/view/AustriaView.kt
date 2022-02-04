package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.entities.statistics.Bundesland
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.utils.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import java.lang.Math.PI
import java.lang.Math.tan

class AustriaView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var state: Bundesland = Bundesland.Oesterreich
    private var statistics: CovidStatistics? = null
    private var statisticIncidenceStates = mutableListOf<StatisticIncidenceState>()
    private var featureCollectionDistricts: FeatureCollection? = null
    private var featureCollectionStates: FeatureCollection? = null

    private val strokePaint = Paint().apply {
        isDither = true
        color = ContextCompat.getColor(context, R.color.brownGrey)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f
    }

    private val fillPaint = Paint().apply {
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f
    }
    private var fullPath = Path()
    private val radius = 6378137.0 /* in meters on the equator */

    private fun featureCollection(): FeatureCollection? {
        return if (state == Bundesland.Oesterreich) {
            featureCollectionStates
        } else {
            featureCollectionDistricts
        }
    }

    init {
        val geoJsonDistricts =
            resources.openRawResource(R.raw.statistics_bezirke_95_geo).bufferedReader()
                .use { it.readText() }
        featureCollectionDistricts = FeatureCollection.fromJson(geoJsonDistricts)

        val geoJsonStates =
            resources.openRawResource(R.raw.statistics_laender_95_geo).bufferedReader()
                .use { it.readText() }
        featureCollectionStates = FeatureCollection.fromJson(geoJsonStates)
    }

    private fun updateView() {
        if (statistics != null) {
            statisticIncidenceStates.clear()
            fullPath = Path()
            featureCollection()?.features()?.forEach { oneFeature ->
                val iso = oneFeature.properties()?.get("iso")?.asLong
                var incidenceValue: Double? = null

                if (state == Bundesland.Oesterreich) {
                    val covidFaelleTimeline =
                        statistics?.covidFaelleTimeline?.filter { it.bundeslandID == iso }
                    if (covidFaelleTimeline == null || covidFaelleTimeline.isEmpty()) {
                        return@forEach
                    }
                    incidenceValue = covidFaelleTimeline.last().siebenTageInzidenzFaelle.roundTo(1)

                } else {
                    val gkzRange = statistics?.covidFaelleGKZ?.filter {
                        it.state((context as FragmentActivity).supportFragmentManager) == state
                    }?.map { it.gkz }

                    if (gkzRange?.contains(iso) != true) {
                        return@forEach
                    }

                    val currentGkz = statistics?.covidFaelleGKZ?.filter { it.gkz == iso }
                    if (currentGkz == null || currentGkz.isEmpty()) {
                        return@forEach
                    }
                    incidenceValue = currentGkz.first().incidenceValue().roundTo(1)
                }

                statisticIncidenceStates.add(
                    StatisticIncidenceState(
                        iso = iso?.toInt(),
                        path = featureToPath(oneFeature),
                        color = incidenceValue.incidenceColor()
                    )
                )
            }

            statisticIncidenceStates.sortBy { it.iso }
            scaleAndTranslateFullPath()
        }
    }

    private fun scaleAndTranslateFullPath() {

        val rectFScale = RectF()
        fullPath.computeBounds(rectFScale, true)

        val scaleMatrix = Matrix()
        val scaleVertical = width / rectFScale.width()
        val scaleHorizontal = height / rectFScale.height()
        val scale = kotlin.math.min(scaleVertical, scaleHorizontal)

        scaleMatrix.setScale(scale, scale)
        fullPath.transform(scaleMatrix)



        val rectFTranslate = RectF()
        fullPath.computeBounds(rectFTranslate, true)
        val translX = width.toFloat() / 2 - rectFTranslate.centerX()
        val translY = height.toFloat() / 2 - rectFTranslate.centerY()

        val translateMatrix = Matrix()
        translateMatrix.setTranslate(translX, translY)
        fullPath.transform(translateMatrix)

        statisticIncidenceStates.forEach { state ->
            state.path.transform(scaleMatrix)
            state.path.transform(translateMatrix)
        }

    }

    private fun featureToPath(feature: Feature): Path {
        val path = Path()
        if (feature.geometry() is MultiPolygon) {
            val multiPolygon = feature.geometry() as MultiPolygon
            multiPolygon.polygons().forEach { polygon ->
                polygon.coordinates().map { points ->
                    path.addPath(pointListToPath(points))
                }
            }
        }

        val scaleMatrix = Matrix()
        // Flip the path horizontally
        scaleMatrix.setScale(1f, -1f)
        path.transform(scaleMatrix)

        fullPath.addPath(path)
        return path
    }

    private fun pointListToPath(points: List<Point>): Path {
        val path = Path()
        points.forEach { point ->
            val x = lon2x(point.longitude()).toFloat()
            val y = lat2y(point.latitude()).toFloat()
            if (point == points.first()) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        return path
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        statisticIncidenceStates.forEach {
            fillPaint.color = ContextCompat.getColor(context, it.color)
            canvas?.drawPath(it.path, fillPaint)
            canvas?.drawPath(it.path, strokePaint)
        }
    }

    // https://wiki.openstreetmap.org/wiki/Mercator#Java

    /* These functions take their angle parameter in degrees and return a length in meters */
    private fun lat2y(aLat: Double): Double {
        return Math.log(
            tan(
                PI / 4 + Math.toRadians(
                    aLat
                ) / 2
            )
        ) * radius
    }

    private fun lon2x(aLong: Double): Double {
        return Math.toRadians(aLong) * radius
    }

    fun setState(state: String) {
        this.state = Bundesland.fromValue(state)
        updateView()
    }

    fun setStatistics(statistics: CovidStatistics?) {
        this.statistics = statistics
    }
}

data class StatisticIncidenceState(
    val iso: Int?,
    val path: Path,
    val color: Int
)