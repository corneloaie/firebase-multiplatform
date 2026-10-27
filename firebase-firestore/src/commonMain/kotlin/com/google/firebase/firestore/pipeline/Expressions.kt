/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore.pipeline

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.fieldPathSegments
import com.google.firebase.firestore.isIntegerValue
import kotlin.jvm.JvmStatic

/*
 * The expression tree of the pipeline API, mirroring com.google.firebase.firestore.pipeline from the Firebase Android
 * SDK. Like the rest of this package it is plain common code: on Android and the JVM it is compiled against, verified to
 * match the SDK's classes and stripped (see buildSrc utils/HeaderStubs.kt), so the SDK's own classes run there; on the
 * other platforms it is the implementation, a tree of functions by their backend name that `Pipeline.execute` hands to
 * the platform SDK (io.github.corneloaie.firebase.firestore.internal.executePipeline).
 *
 * The public functions of Expression and AggregateFunction are generated (Expression.kt, AggregateFunction.kt).
 */

/** An expression that names a field of the output documents of a stage: a [Field], or an expression with an [alias][Expression.alias]. */
public abstract class Selectable : Expression() {
    internal abstract val alias: String
    internal abstract val expr: Expression

    internal companion object {
        fun toSelectable(o: Any): Selectable = when (o) {
            is Selectable -> o
            is String -> Expression.field(o)
            is FieldPath -> Expression.field(o)
            else -> throw IllegalArgumentException("Unknown Selectable type: $o")
        }
    }
}

/** An expression with an alias, the name of its value in the output documents; created with [Expression.alias]. */
public class AliasedExpression internal constructor(override val alias: String, override val expr: Expression) : Selectable() {
    override fun equals(other: Any?): Boolean = this === other || other is AliasedExpression && alias == other.alias && expr == other.expr

    override fun hashCode(): Int = 31 * alias.hashCode() + expr.hashCode()
}

/**
 * A reference to a field of the input documents, created with [Expression.field]; as a [Selectable] its alias is the
 * field path.
 */
public class Field internal constructor(internal val segments: List<String>) : Selectable() {
    override val alias: String = canonicalString(segments)
    override val expr: Expression get() = this

    /** The distance in meters between the location in this field and [location]; for the search stage only. */
    public fun geoDistance(location: GeoPoint): Expression = Expression.geoDistance(this, location)

    override fun equals(other: Any?): Boolean = this === other || other is Field && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()

    public companion object {
        // Not `internal const`: a constant compiles to a public static field, which the SDK's class does not have.
        private const val DOCUMENT_ID_NAME: String = "__name__"
        private const val CREATE_TIME_NAME: String = "__create_time__"
        private const val UPDATE_TIME_NAME: String = "__update_time__"
        internal val DOCUMENT_ID: Field = Field(listOf(DOCUMENT_ID_NAME))
        internal val CREATE_TIME: Field = Field(listOf(CREATE_TIME_NAME))
        internal val UPDATE_TIME: Field = Field(listOf(UPDATE_TIME_NAME))

        /** The field at [path], `.`-separated, as the Android SDK's `FieldPath.fromDotSeparatedPath` splits it. */
        internal fun fromDotSeparatedPath(path: String): Field = when (path) {
            DOCUMENT_ID_NAME -> DOCUMENT_ID
            CREATE_TIME_NAME -> CREATE_TIME
            UPDATE_TIME_NAME -> UPDATE_TIME
            else -> {
                require(path.none { it in "~*/[]" }) { "Invalid field path ($path). Paths must not contain '~', '*', '/', '[', or ']'" }
                val segments = path.split('.')
                require(path.isNotEmpty() && segments.none { it.isEmpty() }) {
                    "Invalid field path ($path). Paths must not be empty, begin with '.', end with '.', or contain '..'"
                }
                Field(segments)
            }
        }

        internal fun of(fieldPath: FieldPath): Field = Field(fieldPathSegments(fieldPath))

        /** The canonical string of a field path: segments joined with `.`, backquoted unless they are plain identifiers. */
        private fun canonicalString(segments: List<String>): String = segments.joinToString(".") { segment ->
            if (segment.isNotEmpty() && (segment[0].isLetter() || segment[0] == '_') && segment.all { it.isLetterOrDigit() || it == '_' } && segment.all { it.code < 128 }) {
                segment
            } else {
                "`" + segment.replace("\\", "\\\\").replace("`", "\\`") + "`"
            }
        }
    }
}

/** A function of the pipeline backend, by its name and arguments. */
public open class FunctionExpression internal constructor(
    internal val name: String,
    internal val params: Array<out Expression>,
    internal val options: InternalOptions = InternalOptions.EMPTY,
) : Expression() {
    internal constructor(name: String, params: List<Expression>, options: InternalOptions = InternalOptions.EMPTY) : this(name, params.toTypedArray(), options)
    internal constructor(name: String) : this(name, emptyArray<Expression>())
    internal constructor(name: String, param: Expression) : this(name, arrayOf(param))
    internal constructor(name: String, param: Expression, vararg params: Any) : this(name, arrayOf(param, *Expression.toArrayOfExprOrConstant(params)))
    internal constructor(name: String, param1: Expression, param2: Expression) : this(name, arrayOf(param1, param2))
    internal constructor(name: String, param1: Expression, param2: Expression, vararg params: Any) : this(name, arrayOf(param1, param2, *Expression.toArrayOfExprOrConstant(params)))
    internal constructor(name: String, fieldName: String) : this(name, arrayOf(Expression.field(fieldName)))
    internal constructor(name: String, fieldName: String, vararg params: Any) : this(name, arrayOf(Expression.field(fieldName), *Expression.toArrayOfExprOrConstant(params)))

    override fun equals(other: Any?): Boolean = this === other || other is FunctionExpression && name == other.name && params.contentEquals(other.params) && options == other.options

    override fun hashCode(): Int = 31 * (31 * name.hashCode() + params.contentHashCode()) + options.hashCode()
}

/** An expression that evaluates to a boolean: a condition of the where stage, of [conditional] and of [countIf]. */
public abstract class BooleanExpression : Expression() {
    /** An aggregate counting the documents for which this condition is true. */
    public fun countIf(): AggregateFunction = AggregateFunction.countIf(this)

    /** [thenExpr] where this condition is true, otherwise [elseExpr]. */
    public fun conditional(thenExpr: Expression, elseExpr: Expression): Expression = Expression.conditional(this, thenExpr, elseExpr)

    /** [thenValue] where this condition is true, otherwise [elseValue]. */
    public fun conditional(thenValue: Any, elseValue: Any): Expression = Expression.conditional(this, thenValue, elseValue)

    /** The negation of this condition. */
    public fun not(): BooleanExpression = Expression.not(this)

    internal fun ifError(catchExpr: BooleanExpression): BooleanExpression = Expression.ifError(this, catchExpr)

    public companion object {
        /** A condition evaluating the backend function [name] with the arguments [expr]. */
        @JvmStatic
        public fun rawFunction(name: String, vararg expr: Expression): BooleanExpression = BooleanFunctionExpression(name, expr)
    }
}

/** The order of a sort: an expression and a [direction][dir]; created with [Expression.ascending] and [Expression.descending]. */
public class Ordering internal constructor(
    /** The sorted expression. */
    public val expr: Expression,
    /** The direction of the sort. */
    public val dir: Direction,
) {
    override fun equals(other: Any?): Boolean = this === other || other is Ordering && expr == other.expr && dir == other.dir

    override fun hashCode(): Int = 31 * expr.hashCode() + dir.hashCode()

    public companion object {
        /** Sorts by [expr] in ascending order. */
        @JvmStatic
        public fun ascending(expr: Expression): Ordering = Ordering(expr, Direction.ASCENDING)

        /** Sorts by the field [fieldName] in ascending order. */
        @JvmStatic
        public fun ascending(fieldName: String): Ordering = Ordering(Expression.field(fieldName), Direction.ASCENDING)

        /** Sorts by [expr] in descending order. */
        @JvmStatic
        public fun descending(expr: Expression): Ordering = Ordering(expr, Direction.DESCENDING)

        /** Sorts by the field [fieldName] in descending order. */
        @JvmStatic
        public fun descending(fieldName: String): Ordering = Ordering(Expression.field(fieldName), Direction.DESCENDING)
    }

    /** The direction of an [Ordering]. */
    public enum class Direction {
        ASCENDING,
        DESCENDING,
    }
}

/** An aggregate with an alias, the name of its value in the output documents; created with [AggregateFunction.alias]. */
public class AliasedAggregate internal constructor(internal val alias: String, internal val expr: AggregateFunction)

/** A constant: null, or a value the Android SDK can encode (see [Expression.Companion.toExprOrConstant]). */
internal class Constant(internal val value: Any?) : Expression() {
    override fun equals(other: Any?): Boolean = this === other || other is Constant && constantEquals(value, other.value)

    override fun hashCode(): Int = when (value) {
        is ByteArray -> value.contentHashCode()
        else -> value.hashCode()
    }

    override fun toString(): String = when (value) {
        is String -> "\"$value\""
        is ByteArray -> value.contentToString()
        else -> value.toString()
    }

    private companion object {
        // Compared as the Android SDK compares the encoded values: an integer and a double constant differ.
        fun constantEquals(a: Any?, b: Any?): Boolean = when {
            a is ByteArray && b is ByteArray -> a.contentEquals(b)
            a is Number && b is Number -> if (isIntegerValue(a)) isIntegerValue(b) && a.toLong() == b.toLong() else !isIntegerValue(b) && a.toDouble() == b.toDouble()
            else -> a == b
        }
    }
}

/** A function returning a boolean, as a condition. */
internal class BooleanFunctionExpression internal constructor(internal val expr: Expression) : BooleanExpression() {
    internal constructor(name: String, params: Array<out Expression>) : this(FunctionExpression(name, params))
    internal constructor(name: String, param: Expression) : this(name, arrayOf(param))
    internal constructor(name: String, param1: Expression, param2: Any) : this(name, arrayOf(param1, Expression.toExprOrConstant(param2)))
    internal constructor(name: String, param: Expression, vararg params: Any) : this(name, arrayOf(param, *Expression.toArrayOfExprOrConstant(params)))
    internal constructor(name: String, param1: Expression, param2: Expression) : this(name, arrayOf(param1, param2))
    internal constructor(name: String, fieldName: String) : this(name, arrayOf(Expression.field(fieldName)))
    internal constructor(name: String, fieldName: String, vararg params: Any) : this(name, arrayOf(Expression.field(fieldName), *Expression.toArrayOfExprOrConstant(params)))

    override fun equals(other: Any?): Boolean = this === other || other is BooleanFunctionExpression && expr == other.expr || other is FunctionExpression && expr == other

    override fun hashCode(): Int = expr.hashCode()

    override fun toString(): String = expr.toString()
}

/** A boolean constant, as a condition. */
internal class BooleanConstant(internal val constant: Constant) : BooleanExpression() {
    override fun equals(other: Any?): Boolean = this === other || other is BooleanConstant && constant == other.constant || other is Constant && constant == other

    override fun hashCode(): Int = constant.hashCode()

    override fun toString(): String = constant.toString()
}

/** A field holding a boolean, as a condition. */
internal class BooleanField(internal val field: Field) : BooleanExpression() {
    override fun equals(other: Any?): Boolean = this === other || other is BooleanField && field == other.field || other is Field && field == other

    override fun hashCode(): Int = field.hashCode()

    override fun toString(): String = field.toString()
}

/** A variable bound by the define stage, or the element (or index) alias of `arrayFilter` and `arrayTransform`. */
internal class Variable(internal val name: String) : Expression() {
    override fun equals(other: Any?): Boolean = this === other || other is Variable && name == other.name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "var($name)"
}

/** A pipeline used as a value: the argument of the `array` and `scalar` subquery functions. */
internal class PipelineValueExpression(internal val pipeline: Pipeline) : Expression() {
    override fun toString(): String = "Pipeline(...)"
}
