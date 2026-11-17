/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore.pipeline

import com.google.firebase.firestore.FieldPath
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/*
 * The window functions of the pipeline API, mirroring the Android SDK's WindowBound.kt, WindowSpec.kt and
 * window_functions.kt: values computed over a window of the documents around each document, added to it by the
 * `add_window_fields` stage (Pipeline.addWindowFields). Each class keeps the internal model the Android SDK sends to the
 * backend, which the platform translations read. Window functions are in preview until the backend feature launches.
 */

/**
 * A symbolic bound of a window frame, as opposed to a numeric offset: [CURRENT] or [UNBOUNDED]. Beta in the Android SDK.
 *
 * In a `documents` frame [CURRENT] is the current document's position; in a `range` frame it also includes the documents
 * whose sort values tie with the current document's.
 */
public class WindowBound private constructor(internal val value: String) {
    override fun equals(other: Any?): Boolean = this === other || (other is WindowBound && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "WindowBound($value)"

    public companion object {
        /** The current document, in a `range` frame together with the documents whose sort values tie with it. */
        @JvmField
        public val CURRENT: WindowBound = WindowBound("current")

        /** No bound in this direction. */
        @JvmField
        public val UNBOUNDED: WindowBound = WindowBound("unbounded")
    }
}

/**
 * The window a window function is evaluated over: how the documents are partitioned and sorted, and the frame of
 * documents around each one, by position ([documents]) or by sort value ([range]). Immutable: each function returns a
 * copy. A window has at most one frame, so [documents] and [range] replace the frame set before. Beta in the Android SDK.
 */
public class WindowSpec internal constructor(
    internal val partition: List<Expression>,
    internal val sort: List<Ordering>,
    internal val documentsFrame: Frame?,
    internal val rangeFrame: Frame?,
) {
    /** A frame's bounds, offsets or [WindowBound]s as constants, and for a `range` frame its optional unit. */
    internal data class Frame(val preceding: Expression, val following: Expression, val unit: Expression? = null)

    /** A single window over all the documents, unsorted and without a frame. */
    public constructor() : this(emptyList(), emptyList(), null, null)

    /** A copy partitioning the documents by [expression] and [additionalExpressions] (expressions, field names or [FieldPath]s). */
    public fun partition(expression: Expression, vararg additionalExpressions: Any): WindowSpec = WindowSpec(resolveGroups(arrayOf(expression, *additionalExpressions)), sort, documentsFrame, rangeFrame)

    /** A copy partitioning the documents by the field [fieldName] and [additionalExpressions] (expressions, field names or [FieldPath]s). */
    public fun partition(fieldName: String, vararg additionalExpressions: Any): WindowSpec = WindowSpec(resolveGroups(arrayOf(fieldName, *additionalExpressions)), sort, documentsFrame, rangeFrame)

    /** A copy sorting the documents of each partition by [order] and [additionalOrders]. */
    public fun sort(order: Ordering, vararg additionalOrders: Ordering): WindowSpec = WindowSpec(partition, listOf(order, *additionalOrders), documentsFrame, rangeFrame)

    /** A copy sorting the documents of each partition by [orders]. */
    public fun sort(orders: List<Ordering>): WindowSpec = WindowSpec(partition, orders, documentsFrame, rangeFrame)

    private fun withDocumentsFrame(preceding: Any, following: Any): WindowSpec = WindowSpec(partition, sort, Frame(toBoundaryExpression(preceding), toBoundaryExpression(following)), null)

    private fun withRangeFrame(preceding: Any, following: Any, unit: Any?): WindowSpec = WindowSpec(
        partition,
        sort,
        null,
        Frame(toBoundaryExpression(preceding), toBoundaryExpression(following), unit?.let(Expression::toExprOrConstant)),
    )

    /** A copy whose frame is the documents from [preceding] positions before each document to [following] positions after it. */
    public fun documents(preceding: Int, following: Int): WindowSpec = withDocumentsFrame(preceding, following)

    /** A copy whose frame is the documents between the bounds [preceding] and [following]. */
    public fun documents(preceding: WindowBound, following: WindowBound): WindowSpec = withDocumentsFrame(preceding, following)

    /** A copy whose frame is the documents between the positions [preceding] and [following] evaluate to. */
    public fun documents(preceding: Expression, following: Expression): WindowSpec = withDocumentsFrame(preceding, following)

    /** A copy whose frame is the documents between [preceding] and [following]: offsets, [WindowBound]s or expressions. */
    public fun documents(preceding: Any, following: Any): WindowSpec = withDocumentsFrame(preceding, following)

    /** A copy whose frame is the documents whose sort value is from [preceding] below each document's to [following] above it. */
    public fun range(preceding: Int, following: Int): WindowSpec = withRangeFrame(preceding, following, null)

    /** A copy whose frame is the documents whose sort value is from [preceding] [unit]s before each document's to [following] after it. */
    public fun range(preceding: Int, following: Int, unit: String): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is from [preceding] [unit]s before each document's to [following] after it. */
    public fun range(preceding: Int, following: Int, unit: Expression): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between the bounds [preceding] and [following]. */
    public fun range(preceding: WindowBound, following: WindowBound): WindowSpec = withRangeFrame(preceding, following, null)

    /** A copy whose frame is the documents whose sort value is between the bounds [preceding] and [following], in [unit]s. */
    public fun range(preceding: WindowBound, following: WindowBound, unit: String): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between the bounds [preceding] and [following], in [unit]s. */
    public fun range(preceding: WindowBound, following: WindowBound, unit: Expression): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is from [preceding] below each document's to [following] above it. */
    public fun range(preceding: Double, following: Double): WindowSpec = withRangeFrame(preceding, following, null)

    /** A copy whose frame is the documents whose sort value is from [preceding] [unit]s before each document's to [following] after it. */
    public fun range(preceding: Double, following: Double, unit: String): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is from [preceding] [unit]s before each document's to [following] after it. */
    public fun range(preceding: Double, following: Double, unit: Expression): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between [preceding] and [following]: offsets, [WindowBound]s or expressions. */
    public fun range(preceding: Any, following: Any): WindowSpec = withRangeFrame(preceding, following, null)

    /** A copy whose frame is the documents whose sort value is between [preceding] and [following], in [unit]s. */
    public fun range(preceding: Any, following: Any, unit: String): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between [preceding] and [following], in [unit]s. */
    public fun range(preceding: Any, following: Any, unit: Expression): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between the offsets [preceding] and [following] evaluate to. */
    public fun range(preceding: Expression, following: Expression): WindowSpec = withRangeFrame(preceding, following, null)

    /** A copy whose frame is the documents whose sort value is between the offsets [preceding] and [following] evaluate to, in [unit]s. */
    public fun range(preceding: Expression, following: Expression, unit: String): WindowSpec = withRangeFrame(preceding, following, unit)

    /** A copy whose frame is the documents whose sort value is between the offsets [preceding] and [following] evaluate to, in [unit]s. */
    public fun range(preceding: Expression, following: Expression, unit: Expression): WindowSpec = withRangeFrame(preceding, following, unit)

    override fun equals(other: Any?): Boolean = this === other ||
        (other is WindowSpec && partition == other.partition && sort == other.sort && documentsFrame == other.documentsFrame && rangeFrame == other.rangeFrame)

    override fun hashCode(): Int = listOf(partition, sort, documentsFrame, rangeFrame).hashCode()
}

/** The partition expressions: expressions as they are, field names and [FieldPath]s as fields. */
private fun resolveGroups(groups: Array<out Any>): List<Expression> = groups.map {
    when (it) {
        is String -> Expression.field(it)
        is FieldPath -> Expression.field(it)
        is Expression -> it
        else -> throw IllegalArgumentException("Invalid partition group type: $it")
    }
}

/** A frame bound as an expression: a [WindowBound] as its backend name, any other value as a constant. */
private fun toBoundaryExpression(boundary: Any): Expression = when (boundary) {
    is WindowBound -> Expression.constant(boundary.value)
    else -> Expression.toExprOrConstant(boundary)
}

/**
 * A function evaluated over a window of the documents around each document: [rank], or an aggregate function with
 * [AggregateFunction.over]. Name its result with [alias]. Beta in the Android SDK.
 */
public class WindowFunction private constructor(
    internal val name: String,
    internal val params: Array<out Expression>,
    internal val options: InternalOptions,
    internal val window: WindowSpec?,
) {
    /** This window function under the alias [alias]: its name in the output documents. Beta in the Android SDK. */
    public fun alias(alias: String): AliasedWindowFunction = AliasedWindowFunction(alias, this)

    /**
     * This function evaluated over the frame of [window], instead of the window of the enclosing `addWindowFields`
     * stage. Only the frame ([WindowSpec.documents] or [WindowSpec.range]) is supported here: the partition and the sort
     * belong to the stage's window. Beta in the Android SDK.
     */
    public fun over(window: WindowSpec): WindowFunction = WindowFunction(name, params, options, window)

    override fun equals(other: Any?): Boolean = this === other ||
        (other is WindowFunction && name == other.name && params.contentEquals(other.params) && options == other.options && window == other.window)

    override fun hashCode(): Int = listOf(name, params.contentHashCode(), options, window).hashCode()

    public companion object {
        /**
         * The rank of each document in its partition, by the window's sort, which must have exactly one ordering: documents
         * with equal sort values get the same rank, and the ranks after them are skipped. Beta in the Android SDK.
         */
        @JvmStatic
        public fun rank(): WindowFunction = WindowFunction("rank", emptyArray<Expression>(), InternalOptions.EMPTY, null)

        /** [aggregate] as a window function, with its name, arguments and options, over the frame of [window] if any. */
        internal fun fromAggregate(aggregate: AggregateFunction, window: WindowSpec?): WindowFunction = WindowFunction(aggregate.name, aggregate.params, aggregate.options, window)
    }
}

/**
 * A window function with an alias, the name of its value in the output documents; created with [WindowFunction.alias].
 * Beta in the Android SDK.
 */
public class AliasedWindowFunction internal constructor(internal val alias: String, internal val expr: WindowFunction) {
    override fun equals(other: Any?): Boolean = this === other || (other is AliasedWindowFunction && alias == other.alias && expr == other.expr)

    override fun hashCode(): Int = 31 * alias.hashCode() + expr.hashCode()

    internal companion object {
        /** [o] as a window field: itself, or an [AliasedAggregate] as its aggregate over the stage's window. */
        fun toAliasedWindowFunction(o: Any): AliasedWindowFunction = when (o) {
            is AliasedWindowFunction -> o
            is AliasedAggregate -> AliasedWindowFunction(o.alias, WindowFunction.fromAggregate(o.expr, null))
            else -> throw IllegalArgumentException("Unsupported window field type: $o")
        }
    }
}
