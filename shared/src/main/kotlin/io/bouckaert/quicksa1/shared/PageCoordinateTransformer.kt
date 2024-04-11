package io.bouckaert.quicksa1.shared

import de.topobyte.jgs.transform.CoordinateTransformer

class PageCoordinateTransformer(
    private val pageWidth: Double,
    private val pageHeight: Double,
    private val pageBorder: PageBorder,
    private val boundsMinX: Double,
    private val boundsWidth: Double,
    private val boundsMinY: Double,
    private val boundsHeight: Double
): CoordinateTransformer {

    private val pageAspectRatio = pageWidth / pageHeight
    private val boundsAspectRatio = boundsWidth / boundsHeight
    private val xFactor = (pageWidth - pageBorder.left - pageBorder.right).let {
        if (boundsAspectRatio > pageAspectRatio) it else it * (boundsAspectRatio / pageAspectRatio)
    }
    private val yFactor = (pageHeight - pageBorder.top - pageBorder.bottom).let {
        if (boundsAspectRatio < pageAspectRatio) it else it * (pageAspectRatio / boundsAspectRatio)
    }
    private val xOffset = if (boundsAspectRatio < pageAspectRatio) {
        ((pageWidth - pageBorder.left - pageBorder.right) / 2) - (xFactor / 2)
    } else 0.0
    private val yOffset = if (boundsAspectRatio > pageAspectRatio) {
        ((pageHeight - pageBorder.top - pageBorder.bottom) / 2) - (yFactor / 2)
    } else 0.0

    override fun getX(x: Double): Double {
        val xAsPercentageOfBoundsWidth = (x - boundsMinX) / boundsWidth

        return (xAsPercentageOfBoundsWidth * xFactor) + pageBorder.left + xOffset
    }
    override fun getY(y: Double): Double {
        val yAsPercentageOfBoundsHeight = (y - boundsMinY) / boundsHeight

        return pageHeight - (yAsPercentageOfBoundsHeight * yFactor) - pageBorder.bottom - yOffset
    }

    data class PageBorder(
        val top: Double,
        val right: Double,
        val bottom: Double,
        val left: Double
    )
}
