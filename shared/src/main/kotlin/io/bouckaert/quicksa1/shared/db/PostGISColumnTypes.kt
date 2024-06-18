package io.bouckaert.quicksa1.db

import net.postgis.jdbc.PGbox2d
import net.postgis.jdbc.PGgeometry
import net.postgis.jdbc.geometry.MultiPolygon
import net.postgis.jdbc.geometry.Polygon
import org.jetbrains.exposed.sql.*

fun Table.multiPolygon(name: String, srid: Int = 3857): Column<MultiPolygon> =
    registerColumn(name, MultiPolygonColumnType(srid))

infix fun Expression<*>.within(op: Expression<*>): Op<Boolean> = WithinOp(listOf(this, op))

private class MultiPolygonColumnType(val srid: Int = 3857) : ColumnType() {
    override fun sqlType() = "GEOMETRY(MultiPolygon, $srid)"
    override fun valueFromDB(value: Any): Any = if (value is PGgeometry) value.geometry else value
    override fun notNullValueToDB(value: Any): Any {
        if (value is MultiPolygon) {
            if (value.srid == MultiPolygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }

    override fun valueToDB(value: Any?): Any? {
        if (value is MultiPolygon) {
            if (value.srid == MultiPolygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }
}

class PolygonColumnType(val srid: Int = 3857) : ColumnType() {
    override fun sqlType() = "GEOMETRY(Polygon, $srid)"
    override fun valueFromDB(value: Any): Any = if (value is PGgeometry) value.geometry else value
    override fun notNullValueToDB(value: Any): Any {
        if (value is Polygon) {
            if (value.srid == Polygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }

    override fun valueToDB(value: Any?): Any? {
        if (value is Polygon) {
            if (value.srid == Polygon.UNKNOWN_SRID) value.srid = srid
            return PGgeometry(value)
        }
        return value
    }
}

private class WithinOp(val expressions: List<Expression<*>>) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        expressions.appendTo(this, separator = " && ") {
            if (it is ComplexExpression) {
                append("(", it, ")")
            } else {
                append(it)
            }
        }
    }
}

class ST_MakeEnvelope(box: PGbox2d): CustomFunction<Boolean>(
    "ST_MakeEnvelope",
    PolygonColumnType(),
    LiteralOp(LongColumnType(), box.llb.x),
    LiteralOp(LongColumnType(), box.llb.y),
    LiteralOp(LongColumnType(), box.urt.x),
    LiteralOp(LongColumnType(), box.urt.y),
    LiteralOp(IntegerColumnType(), 3857)
)
