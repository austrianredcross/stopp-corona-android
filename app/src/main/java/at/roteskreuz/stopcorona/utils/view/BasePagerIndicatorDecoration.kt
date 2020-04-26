/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 David Medenjak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package at.roteskreuz.stopcorona.utils.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.utils.color
import at.roteskreuz.stopcorona.skeleton.core.utils.dip
import at.roteskreuz.stopcorona.skeleton.core.utils.dipif

/**
 * Base pager indicator for recycler view.
 * It is inspired by https://stackoverflow.com/a/46084182.
 */
abstract class BasePagerIndicatorDecoration(
    private val paddingBottom: Int,
    /**
     * Indicator stroke width.
     */
    indicatorStrokeWidth: Float,

    /**
     * Indicator width.
     */
    private val indicatorItemLength: Int,
    /**
     * Padding between indicators.
     */
    private val indicatorItemPadding: Int,

    /**
     * Some more natural animation interpolation
     */
    protected val interpolator: Interpolator
) : RecyclerView.ItemDecoration() {

    protected val paint = Paint()

    init {
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = indicatorStrokeWidth
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val itemCount = parent.adapter.safeMap(defaultValue = 0) { adapter ->
            adapter.itemCount
        }

        // center horizontally, calculate width and subtract half from center
        val totalLength = indicatorItemLength * itemCount
        val paddingBetweenItems = Math.max(0, itemCount - 1) * indicatorItemPadding
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f
        val indicatorPosY = getIndicatorPosY(parent)

        drawInactiveIndicators(c, indicatorStartX, indicatorPosY, itemCount)

        // find active page (which should be highlighted)
        (parent.layoutManager as LinearLayoutManager?).safeRun { layoutManager ->

            val activePosition = layoutManager.findFirstVisibleItemPosition()
            if (activePosition == RecyclerView.NO_POSITION) {
                return
            }

            // find offset of active page (if the user is scrolling)
            layoutManager.findViewByPosition(activePosition).safeRun { activeChild ->
                val left = activeChild.left
                val width = activeChild.width

                // on swipe the active item will be positioned from [-width, 0]
                // interpolate offset for smooth animation
                val progress = interpolator.getInterpolation(left * -1 / width.toFloat())

                drawHighlights(c, indicatorStartX, indicatorPosY, activePosition, progress, itemCount)
            }
        }
    }

    protected open fun getIndicatorPosY(parent: RecyclerView): Float {
        return parent.height.toFloat() - paddingBottom
    }

    /**
     * Draw the inactive state of the indicators.
     */
    abstract fun drawInactiveIndicators(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        itemCount: Int
    )

    /**
     * Draw the active state of the indicators.
     */
    abstract fun drawHighlights(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        highlightPosition: Int,
        progress: Float,
        itemCount: Int
    )
}

/**
 * Pager indicators shaped as lines.
 */
class LinePagerIndicatorDecoration(
    private val context: Context,
    private val paddingBottom: Int = 0,
    private val paddingTop: Int = 0,
    private val gravity: Int = Gravity.BOTTOM,

    @ColorInt
    private val colorActive: Int = context.color(R.color.gallery_indicator_active),

    @ColorInt
    private val colorInactive: Int = context.color(R.color.gallery_indicator_inactive),
    /**
     * Indicator stroke width.
     */
    indicatorStrokeWidth: Float = context.dipif(2),

    /**
     * Indicator width.
     */
    private val indicatorItemLength: Int = context.dip(16),
    /**
     * Padding between indicators.
     */
    private val indicatorItemPadding: Int = context.dip(4),
    // More natural interpolation.
    interpolator: Interpolator = AccelerateDecelerateInterpolator()
) : BasePagerIndicatorDecoration(
    paddingBottom,
    indicatorStrokeWidth,
    indicatorItemLength,
    indicatorItemPadding,
    interpolator
) {

    init {
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = indicatorStrokeWidth
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val itemCount = parent.adapter.safeMap(defaultValue = 0) { adapter ->
            adapter.itemCount
        }

        // center horizontally, calculate width and subtract half from center
        val totalLength = indicatorItemLength * itemCount
        val paddingBetweenItems = Math.max(0, itemCount - 1) * indicatorItemPadding
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f
        val indicatorPosY = getIndicatorPosY(parent)

        drawInactiveIndicators(c, indicatorStartX, indicatorPosY, itemCount)

        // find active page (which should be highlighted)
        (parent.layoutManager as LinearLayoutManager?).safeRun { layoutManager ->

            val activePosition = layoutManager.findFirstVisibleItemPosition()
            if (activePosition == RecyclerView.NO_POSITION) {
                return
            }

            // find offset of active page (if the user is scrolling)
            layoutManager.findViewByPosition(activePosition).safeRun { activeChild ->
                val left = activeChild.left
                val width = activeChild.width

                // on swipe the active item will be positioned from [-width, 0]
                // interpolate offset for smooth animation
                val progress = interpolator.getInterpolation(left * -1 / width.toFloat())

                drawHighlights(c, indicatorStartX, indicatorPosY, activePosition, progress, itemCount)
            }
        }
    }

    override fun getIndicatorPosY(parent: RecyclerView): Float {
        return when (gravity) {
            Gravity.TOP -> paddingTop.toFloat()
            else -> parent.height.toFloat() - paddingBottom
        }
    }

    override fun drawInactiveIndicators(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        itemCount: Int
    ) {
        paint.color = colorInactive

        // width of item indicator including padding
        val itemWidth = indicatorItemLength + indicatorItemPadding

        var start = indicatorStartX
        for (i in 0 until itemCount) {
            // draw the line for every item
            c.drawLine(start, indicatorPosY, start + indicatorItemLength, indicatorPosY, paint)
            start += itemWidth
        }
    }

    override fun drawHighlights(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        highlightPosition: Int,
        progress: Float, itemCount: Int
    ) {
        paint.color = colorActive

        // width of item indicator including padding
        val itemWidth = indicatorItemLength + indicatorItemPadding

        if (progress == 0f) {
            // no swipe, draw a normal indicator
            val highlightStart = indicatorStartX + itemWidth * highlightPosition
            c.drawLine(highlightStart, indicatorPosY,
                highlightStart + indicatorItemLength, indicatorPosY, paint)
        } else {
            var highlightStart = indicatorStartX + itemWidth * highlightPosition
            // calculate partial highlight
            val partialLength = indicatorItemLength * progress

            // draw the cut off highlight
            c.drawLine(highlightStart + partialLength, indicatorPosY,
                highlightStart + indicatorItemLength, indicatorPosY, paint)

            // draw the highlight overlapping to the next item as well
            if (highlightPosition < itemCount - 1) {
                highlightStart += itemWidth
                c.drawLine(highlightStart, indicatorPosY,
                    highlightStart + partialLength, indicatorPosY, paint)
            }
        }
    }
}

/**
 * Pager indicators shaped as circles.
 */
class CirclePagerIndicatorDecoration(
    private val context: Context,
    paddingBottom: Int,
    @ColorInt
    private val colorActive: Int = -0x1,
    @ColorInt
    private val colorInactive: Int = 0x66FFFFFF,
    indicatorStrokeWidth: Float = context.dipif(2),
    private val indicatorItemDiameter: Int = context.dip(16),
    private val indicatorItemPadding: Int = context.dip(4),
    // More natural interpolation.
    interpolator: Interpolator = AccelerateDecelerateInterpolator()
) : BasePagerIndicatorDecoration(
    paddingBottom,
    indicatorStrokeWidth,
    indicatorItemDiameter,
    indicatorItemPadding,
    interpolator
) {

    override fun drawInactiveIndicators(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        itemCount: Int
    ) {
        paint.color = colorInactive

        // width of item indicator including padding
        val itemWidth = indicatorItemDiameter + indicatorItemPadding

        var start = indicatorStartX
        for (i in 0 until itemCount) {
            // draw the circle for every item
            c.drawCircle(
                start + indicatorItemDiameter / 2,
                indicatorPosY,
                indicatorItemDiameter.toFloat() / 2,
                paint
            )
            start += itemWidth
        }
    }

    override fun drawHighlights(
        c: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        highlightPosition: Int,
        progress: Float,
        itemCount: Int
    ) {
        paint.color = colorActive

        // width of item indicator including padding
        val itemWidth = indicatorItemDiameter + indicatorItemPadding

        if (progress < 0.8f) {
            val highlightStart = indicatorStartX + itemWidth * highlightPosition
            c.drawCircle(
                highlightStart + indicatorItemDiameter / 2,
                indicatorPosY,
                indicatorItemDiameter.toFloat() / 2,
                paint
            )
        } else if (progress >= 0.8f) {
            var highlightStart = indicatorStartX + itemWidth * highlightPosition
            highlightStart += itemWidth
            c.drawCircle(
                highlightStart + indicatorItemDiameter / 2,
                indicatorPosY,
                indicatorItemDiameter.toFloat() / 2,
                paint
            )
        }
    }
}


