package io.bouckaert.quicksa1.shared.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.vendors.currentDialect

class WithQuery(
    override var set: FieldSet,
    where: Op<Boolean>?,
    private val withQuery: Expression<*>,
): Query(set, where) {
    override fun prepareSQL(builder: QueryBuilder): String {
        builder {
            append("WITH w(val) AS (")
            append("SELECT ")
            append(withQuery)
            append (") ")

            append("SELECT ")

            if (count) {
                append("COUNT(*)")
            } else {
                if (distinct) {
                    append("DISTINCT ")
                }
                set.realFields.appendTo { +it }
            }
            if (set.source != Table.Dual || currentDialect.supportsDualTableConcept) {
                append(" FROM ")
                set.source.describe(transaction, this)
                append(", ")
                append("w ")
            }

            where?.let {
                append(" WHERE ")
                +it
            }

            if (!count) {
                if (groupedByColumns.isNotEmpty()) {
                    append(" GROUP BY ")
                    groupedByColumns.appendTo {
                        +((it as? ExpressionAlias)?.aliasOnlyExpression() ?: it)
                    }
                }

                having?.let {
                    append(" HAVING ")
                    append(it)
                }

                if (orderByExpressions.isNotEmpty()) {
                    append(" ORDER BY ")
                    orderByExpressions.appendTo { (expression, sortOrder) ->
                        currentDialect.dataTypeProvider.precessOrderByClause(this, expression, sortOrder)
                    }
                }

                limit?.let {
                    append(" ")
                    append(currentDialect.functionProvider.queryLimit(it, offset, orderByExpressions.isNotEmpty()))
                }
            }
        }
        return builder.toString()
    }
}

inline fun FieldSet.selectWith(withQuery: Expression<*>, where: SqlExpressionBuilder.() -> Op<Boolean>): Query = selectWith(withQuery, SqlExpressionBuilder.where())

fun FieldSet.selectWith(withQuery: Expression<*>, where: Op<Boolean>): Query = WithQuery(this, where, withQuery)
