package io.bouckaert.quicksa1.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import java.awt.Rectangle
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

fun <K, V> Map<out K?, V?>.filterNotNull(): Map<K, V> = this.mapNotNull {
    it.key?.let { key ->
        it.value?.let { value ->
            key to value
        }
    }
}.toMap()

suspend fun <T, R> Iterable<T>.processInParallel(
    dispatcher: CoroutineContext = Dispatchers.IO,
    processBlock: suspend (v: T) -> R,
): List<R> = coroutineScope { // or supervisorScope
    map {
        async(dispatcher) { processBlock(it) }
    }.awaitAll()
}
suspend fun <K, V, R> Map<out K, V>.processInParallel(
    dispatcher: CoroutineContext = Dispatchers.IO,
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

fun Geometry.safeWithin(other: Geometry): Boolean {
    val thisUnion = this.union()
    val otherUnion = other.union()
    if (this !is GeometryCollection && otherUnion !is GeometryCollection) {
        return thisUnion.within(otherUnion)
    } else if (otherUnion !is GeometryCollection) {
        for (i in 0 until thisUnion.numGeometries) {
            if (thisUnion.getGeometryN(i).within(otherUnion)) return true
        }
    } else if (thisUnion !is GeometryCollection) {
        for (i in 0 until otherUnion.numGeometries) {
            if (thisUnion.within(otherUnion.getGeometryN(i))) return true
        }
    } else {
        for (i in 0 until thisUnion.numGeometries) {
            for (j in 0 until otherUnion.numGeometries) {
                if (thisUnion.getGeometryN(i).within(otherUnion.getGeometryN(j))) return true
            }
        }
    }
    return false
}

fun Geometry.safeIntersects(other: Geometry): Boolean {
    if (this !is GeometryCollection && other !is GeometryCollection) {
        return this.intersects(other)
    } else if (other !is GeometryCollection) {
        for (i in 0 until this.numGeometries) {
            if (this.getGeometryN(i).intersects(other)) return true
        }
    } else if (this !is GeometryCollection) {
        for (i in 0 until other.numGeometries) {
            if (this.intersects(other.getGeometryN(i))) return true
        }
    } else {
        for (i in 0 until this.numGeometries) {
            for (j in 0 until other.numGeometries) {
                if (this.getGeometryN(i).intersects(other.getGeometryN(j))) return true
            }
        }
    }
    return false
}
