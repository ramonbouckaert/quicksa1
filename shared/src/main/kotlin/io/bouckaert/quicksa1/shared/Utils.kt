package io.bouckaert.quicksa1.shared

import kotlinx.coroutines.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import java.awt.Rectangle
import kotlin.math.roundToInt

fun <K, V> Map<out K?, V?>.filterNotNull(): Map<K, V> = this.mapNotNull {
    it.key?.let { key ->
        it.value?.let { value ->
            key to value
        }
    }
}.toMap()

suspend fun <T, R> Iterable<T>.processInParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    processBlock: suspend (v: T) -> R,
): List<R> = coroutineScope { // or supervisorScope
    map {
        async(dispatcher) { processBlock(it) }
    }.awaitAll()
}
suspend fun <K, V, R> Map<out K, V>.processInParallel(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    processBlock: suspend (Map.Entry<K, V>) -> R,
): List<R> = coroutineScope { // or supervisorScope
    map {
        async(dispatcher) { processBlock(it) }
    }.awaitAll()
}

fun com.lowagie.text.Rectangle.toAwt() = Rectangle(
    0,
    0,
    this.width.roundToInt(),
    this.height.roundToInt()
)

fun Rectangle.toLowagie() = com.lowagie.text.Rectangle(
    this.x.toFloat(),
    this.y.toFloat(),
    this.width.toFloat(),
    this.height.toFloat()
)

fun Rectangle.scale(scale: Int) = Rectangle(
    this.x,
    this.y,
    this.width*scale,
    this.height*scale
)

fun Geometry.safeUnion(other: Geometry, geometryFactory: GeometryFactory): Geometry {
    if (this !is GeometryCollection && other !is GeometryCollection) {
        return this.union(other)
    } else if (other !is GeometryCollection) {
        val unionedThis = this.union()
        if (unionedThis is GeometryCollection) return other else return unionedThis.union(other)
    } else if (this !is GeometryCollection) {
        val unionedOther = other.union()
        if (unionedOther is GeometryCollection) return this else return this.union(unionedOther)
    } else {
        var geomArray: Array<Geometry> = emptyArray()
        for (i in 0 until this.numGeometries) {
            geomArray += this.getGeometryN(i)
        }
        for (i in 0 until other.numGeometries) {
            geomArray += other.getGeometryN(i)
        }
        return GeometryCollection(geomArray, geometryFactory)
    }
}