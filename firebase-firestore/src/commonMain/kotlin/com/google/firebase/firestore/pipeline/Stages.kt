/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore.pipeline

import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.VectorValue
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/*
 * The stages of a pipeline, mirroring the Android SDK's: each has the backend name of the stage, its arguments (internal
 * properties, read by the platform translation) and its options. Only some are public; the others are created by the
 * methods of Pipeline and PipelineSource.
 */

/** A stage of a [Pipeline]: immutable, [withOption] returns a copy with an option of the stage set. */
public sealed class Stage<T : Stage<T>>(internal val name: String, internal val options: InternalOptions) {
    internal abstract fun self(options: InternalOptions): T

    /** A copy of this stage with the option [key] set to [value]. */
    public fun withOption(key: String, value: String): T = self(options.with(key, value))

    /** A copy of this stage with the option [key] set to [value]. */
    public fun withOption(key: String, value: Boolean): T = self(options.with(key, value))

    /** A copy of this stage with the option [key] set to [value]. */
    public fun withOption(key: String, value: Long): T = self(options.with(key, value))

    /** A copy of this stage with the option [key] set to [value]. */
    public fun withOption(key: String, value: Double): T = self(options.with(key, value))

    /**
     * A copy of this stage with the option [key] set to a reference to the field [value]. The JS SDK cannot send a field
     * reference as an option: on JS and wasmJs, executing the pipeline fails with [UnsupportedOperationException].
     */
    public fun withOption(key: String, value: Field): T = self(options.with(key, value))

    public companion object {
        /** The selectables by alias; an alias may appear once, which the Android SDK checks when it builds the request. */
        internal fun associateWithoutDuplications(fields: Array<out Selectable>): Map<String, Expression> = fields.fold(LinkedHashMap()) { results, selectable ->
            require(selectable.alias !in results) { "Duplicate alias: '${selectable.alias}'" }
            results[selectable.alias] = selectable.expr
            results
        }
    }
}

/** A stage by its backend name and arguments, for stages this SDK has no method for; created with [ofName]. */
public class RawStage private constructor(
    name: String,
    internal val arguments: List<GenericArg>,
    options: InternalOptions = InternalOptions.EMPTY,
) : Stage<RawStage>(name, options) {
    override fun self(options: InternalOptions): RawStage = RawStage(name, arguments, options)

    /**
     * A copy of this stage with [arguments]: expressions, constants, [AggregateFunction]s, [Ordering]s, and lists and
     * maps (with string keys) of these.
     */
    public fun withArguments(vararg arguments: Any): RawStage = RawStage(name, arguments.map(GenericArg::from), options)

    public companion object {
        /** A stage named [name], without arguments. */
        @JvmStatic
        public fun ofName(name: String): RawStage = RawStage(name, emptyList(), InternalOptions.EMPTY)
    }
}

/** An argument of a [RawStage]. */
internal sealed class GenericArg {
    // Explicitly internal, as nested classes of an internal class are public on the JVM (see utils/HeaderStubs.kt).
    internal data class AggregateArg(val aggregate: AggregateFunction) : GenericArg()
    internal data class ExprArg(val expr: Expression) : GenericArg()
    internal data class OrderingArg(val ordering: Ordering) : GenericArg()
    internal data class MapArg(val args: Map<String, GenericArg>) : GenericArg()
    internal data class ListArg(val args: List<GenericArg>) : GenericArg()

    companion object {
        fun from(arg: Any?): GenericArg = when (arg) {
            is AggregateFunction -> AggregateArg(arg)
            is Ordering -> OrderingArg(arg)
            is Map<*, *> -> MapArg(arg.entries.associate { (key, value) -> key as String to from(value) })
            is List<*> -> ListArg(arg.map(::from))
            else -> ExprArg(Expression.toExprOrConstant(arg))
        }
    }
}

/** The documents of every collection of the database. */
internal class DatabaseSource(options: InternalOptions = InternalOptions.EMPTY) : Stage<DatabaseSource>("database", options) {
    override fun self(options: InternalOptions) = DatabaseSource(options)

    override fun equals(other: Any?): Boolean = this === other || other is DatabaseSource && options == other.options

    override fun hashCode(): Int = options.hashCode()
}

/** The documents of a collection, the source of [com.google.firebase.firestore.PipelineSource.collection]. */
public class CollectionSource internal constructor(internal val path: String, options: InternalOptions) : Stage<CollectionSource>("collection", options) {
    internal constructor(path: String, options: CollectionSourceOptions) : this(path, options.options)

    override fun self(options: InternalOptions): CollectionSource = CollectionSource(path, options)

    override fun equals(other: Any?): Boolean = this === other || other is CollectionSource && path == other.path && options == other.options

    override fun hashCode(): Int = 31 * path.hashCode() + options.hashCode()
}

/**
 * The options of a collection source. On Apple platforms only the forced index ([CollectionHints.withForceIndex]) is
 * supported; with any other option, executing the pipeline fails with [UnsupportedOperationException].
 */
public class CollectionSourceOptions internal constructor(options: InternalOptions) : AbstractOptions<CollectionSourceOptions>(options) {
    /** No options. */
    public constructor() : this(InternalOptions.EMPTY)

    /** A copy with the index [hints] added. */
    public fun withHints(hints: CollectionHints): CollectionSourceOptions = adding(hints)

    override fun self(options: InternalOptions): CollectionSourceOptions = CollectionSourceOptions(options)
}

/** Index hints of a collection or collection group source; on Apple platforms only [withForceIndex] is supported. */
public class CollectionHints internal constructor(options: InternalOptions) : AbstractOptions<CollectionHints>(options) {
    /** No hints. */
    public constructor() : this(InternalOptions.EMPTY)

    override fun self(options: InternalOptions): CollectionHints = CollectionHints(options)

    /** A copy that makes the backend use the index [value]. */
    public fun withForceIndex(value: String): CollectionHints = with("force_index", value)

    /** A copy that makes the backend ignore the index fields [values]. */
    public fun withIgnoreIndexFields(vararg values: String): CollectionHints = with("ignore_index_fields", *values)
}

/** The documents of every collection with an id, the source of [com.google.firebase.firestore.PipelineSource.collectionGroup]. */
public class CollectionGroupSource internal constructor(
    /** The id of the collections. */
    public val collectionId: String,
    options: InternalOptions,
) : Stage<CollectionGroupSource>("collection_group", options) {
    internal constructor(collectionId: String, options: CollectionGroupOptions) : this(collectionId, options.options)

    override fun self(options: InternalOptions): CollectionGroupSource = CollectionGroupSource(collectionId, options)

    override fun equals(other: Any?): Boolean = this === other || other is CollectionGroupSource && collectionId == other.collectionId && options == other.options

    override fun hashCode(): Int = 31 * collectionId.hashCode() + options.hashCode()
}

/** The options of a collection group source; on Apple platforms only the forced index is supported (see [CollectionSourceOptions]). */
public class CollectionGroupOptions internal constructor(options: InternalOptions) : AbstractOptions<CollectionGroupOptions>(options) {
    /** No options. */
    public constructor() : this(InternalOptions.EMPTY)

    override fun self(options: InternalOptions): CollectionGroupOptions = CollectionGroupOptions(options)

    /** A copy with the index [hints] added. */
    public fun withHints(hints: CollectionHints): CollectionGroupOptions = adding(hints)
}

/** Some documents by path. */
internal class DocumentsSource(internal val documents: List<String>, options: InternalOptions = InternalOptions.EMPTY) : Stage<DocumentsSource>("documents", options) {
    override fun self(options: InternalOptions) = DocumentsSource(documents, options)

    override fun equals(other: Any?): Boolean = this === other || other is DocumentsSource && documents == other.documents && options == other.options

    override fun hashCode(): Int = 31 * documents.hashCode() + options.hashCode()
}

/** The results of a query, the source of [com.google.firebase.firestore.PipelineSource.createFrom]: the platform SDK converts it. */
internal class QuerySource(internal val query: Query, options: InternalOptions = InternalOptions.EMPTY) : Stage<QuerySource>("query", options) {
    override fun self(options: InternalOptions) = QuerySource(query, options)
}

/** The documents of a subcollection of each input document, the source of a subquery; created with [of]. */
public class SubcollectionSource private constructor(internal val path: String, options: InternalOptions = InternalOptions.EMPTY) : Stage<SubcollectionSource>("subcollection", options) {
    override fun self(options: InternalOptions): SubcollectionSource = SubcollectionSource(path, options)

    public companion object {
        /** The subcollection at [path], relative to each input document. */
        @JvmStatic
        public fun of(path: String): SubcollectionSource = SubcollectionSource(path)
    }
}

private fun requireNoReservedAlias(fields: Array<out Selectable>, message: (String) -> String) {
    for (field in fields) {
        val alias = field.alias
        require(alias != Field.DOCUMENT_ID.alias) { message(Field.DOCUMENT_ID.alias) }
        require(alias != Field.CREATE_TIME.alias) { message(Field.CREATE_TIME.alias) }
        require(alias != Field.UPDATE_TIME.alias) { message(Field.UPDATE_TIME.alias) }
    }
}

internal class AddFieldsStage(internal val fields: Array<out Selectable>, options: InternalOptions = InternalOptions.EMPTY) : Stage<AddFieldsStage>("add_fields", options) {
    init {
        requireNoReservedAlias(fields) { "Alias $it is reserved" }
    }

    override fun self(options: InternalOptions) = AddFieldsStage(fields, options)

    override fun equals(other: Any?): Boolean = this === other || other is AddFieldsStage && fields.contentEquals(other.fields) && options == other.options

    override fun hashCode(): Int = 31 * fields.contentHashCode() + options.hashCode()
}

/** An aggregation of the input documents, optionally by groups; created with [withAccumulators]. */
public class AggregateStage private constructor(
    internal val accumulators: Map<String, AggregateFunction>,
    internal val groups: Map<String, Expression>,
    options: InternalOptions = InternalOptions.EMPTY,
) : Stage<AggregateStage>("aggregate", options) {
    private constructor(accumulators: Map<String, AggregateFunction>) : this(accumulators, emptyMap())

    override fun self(options: InternalOptions): AggregateStage = AggregateStage(accumulators, groups, options)

    /** A copy of this stage grouping by the field [groupField] and [additionalGroups] (selectables or field names). */
    public fun withGroups(groupField: String, vararg additionalGroups: Any): AggregateStage = withGroups(Expression.field(groupField), *additionalGroups)

    /** A copy of this stage grouping by [group] and [additionalGroups] (selectables or field names). */
    public fun withGroups(group: Selectable, vararg additionalGroups: Any): AggregateStage {
        val groups = additionalGroups.map(Selectable::toSelectable).fold(mapOf(group.alias to group.expr)) { acc, next ->
            require(next.alias !in acc) { "Duplicate alias: '${next.alias}'" }
            acc + (next.alias to next.expr)
        }
        return AggregateStage(accumulators, groups, options)
    }

    internal fun withOptions(options: AggregateOptions) = AggregateStage(accumulators, groups, options.options)

    override fun equals(other: Any?): Boolean = this === other || other is AggregateStage && accumulators == other.accumulators && groups == other.groups && options == other.options

    override fun hashCode(): Int = 31 * (31 * accumulators.hashCode() + groups.hashCode()) + options.hashCode()

    public companion object {
        /** An aggregation computing [accumulator] and [additionalAccumulators] over all the input documents. */
        @JvmStatic
        public fun withAccumulators(accumulator: AliasedAggregate, vararg additionalAccumulators: AliasedAggregate): AggregateStage {
            val accumulators = additionalAccumulators.fold(mapOf(accumulator.alias to accumulator.expr)) { acc, next ->
                require(next.alias !in acc) { "Duplicate alias: '${next.alias}'" }
                acc + (next.alias to next.expr)
            }
            return AggregateStage(accumulators)
        }
    }
}

/** Hints of an aggregation. */
public class AggregateHints internal constructor(options: InternalOptions) : AbstractOptions<AggregateHints>(options) {
    /** No hints. */
    public constructor() : this(InternalOptions.EMPTY)

    override fun self(options: InternalOptions): AggregateHints = AggregateHints(options)

    /** A copy that makes the backend stream the aggregation. */
    public fun withForceStreamableEnabled(): AggregateHints = with("force_streamable", true)
}

/** The options of an aggregation. */
public class AggregateOptions internal constructor(options: InternalOptions) : AbstractOptions<AggregateOptions>(options) {
    /** No options. */
    public constructor() : this(InternalOptions.EMPTY)

    override fun self(options: InternalOptions): AggregateOptions = AggregateOptions(options)

    /** A copy with the [hints] added. */
    public fun withHints(hints: AggregateHints): AggregateOptions = adding(hints)
}

internal class WhereStage(internal val condition: Expression, options: InternalOptions = InternalOptions.EMPTY) : Stage<WhereStage>("where", options) {
    override fun self(options: InternalOptions) = WhereStage(condition, options)

    override fun equals(other: Any?): Boolean = this === other || other is WhereStage && condition == other.condition && options == other.options

    override fun hashCode(): Int = 31 * condition.hashCode() + options.hashCode()
}

/** A nearest-neighbour search of a vector field. */
public class FindNearestStage internal constructor(
    internal val property: Expression,
    internal val vector: Expression,
    internal val distanceMeasure: DistanceMeasure,
    options: InternalOptions = InternalOptions.EMPTY,
) : Stage<FindNearestStage>("find_nearest", options) {
    private constructor(property: Expression, vector: Expression, distanceMeasure: DistanceMeasure, options: FindNearestOptions) : this(property, vector, distanceMeasure, options.options)

    override fun self(options: InternalOptions): FindNearestStage = FindNearestStage(property, vector, distanceMeasure, options)

    override fun equals(other: Any?): Boolean = this === other || other is FindNearestStage && property == other.property && vector == other.vector && distanceMeasure == other.distanceMeasure && options == other.options

    override fun hashCode(): Int = 31 * (31 * (31 * property.hashCode() + vector.hashCode()) + distanceMeasure.hashCode()) + options.hashCode()

    public companion object {
        internal fun of(vectorField: Field, vectorValue: VectorValue, distanceMeasure: DistanceMeasure, options: FindNearestOptions = FindNearestOptions()) = FindNearestStage(vectorField, Expression.constant(vectorValue), distanceMeasure, options)

        internal fun of(vectorField: Field, vectorValue: DoubleArray, distanceMeasure: DistanceMeasure, options: FindNearestOptions = FindNearestOptions()) = FindNearestStage(vectorField, Expression.vector(vectorValue), distanceMeasure, options)

        internal fun of(vectorField: String, vectorValue: VectorValue, distanceMeasure: DistanceMeasure, options: FindNearestOptions = FindNearestOptions()) = FindNearestStage(Expression.field(vectorField), Expression.constant(vectorValue), distanceMeasure, options)

        internal fun of(vectorField: String, vectorValue: DoubleArray, distanceMeasure: DistanceMeasure, options: FindNearestOptions = FindNearestOptions()) = FindNearestStage(Expression.field(vectorField), Expression.vector(vectorValue), distanceMeasure, options)

        internal fun of(vectorField: String, vectorValue: Expression, distanceMeasure: DistanceMeasure, options: FindNearestOptions = FindNearestOptions()) = FindNearestStage(Expression.field(vectorField), vectorValue, distanceMeasure, options)
    }

    /** How [FindNearestStage] measures the distance between vectors. */
    public class DistanceMeasure private constructor(internal val value: String) {
        public companion object {
            /** The Euclidean distance. */
            @JvmField
            public val EUCLIDEAN: DistanceMeasure = DistanceMeasure("euclidean")

            /** The cosine distance. */
            @JvmField
            public val COSINE: DistanceMeasure = DistanceMeasure("cosine")

            /** The dot product. */
            @JvmField
            public val DOT_PRODUCT: DistanceMeasure = DistanceMeasure("dot_product")
        }
    }
}

/** The options of a nearest-neighbour search. */
public class FindNearestOptions private constructor(options: InternalOptions) : AbstractOptions<FindNearestOptions>(options) {
    /** No options. */
    public constructor() : this(InternalOptions.EMPTY)

    override fun self(options: InternalOptions): FindNearestOptions = FindNearestOptions(options)

    /** A copy returning at most [limit] documents. */
    public fun withLimit(limit: Long): FindNearestOptions = with("limit", limit)

    /** A copy writing the distance of each result to the field [distanceField]. */
    public fun withDistanceField(distanceField: Field): FindNearestOptions = with("distance_field", distanceField)

    /** A copy writing the distance of each result to the field [distanceField]. */
    public fun withDistanceField(distanceField: String?): FindNearestOptions? = withDistanceField(Expression.field(distanceField!!))
}

/** A full-text or geo search; created with [withQuery]. Beta in the Android SDK. */
public class SearchStage internal constructor(
    internal val query: BooleanExpression,
    internal val languageCode: String? = null,
    internal val retrievalDepth: Long? = null,
    internal val sort: Array<Ordering>? = null,
    internal val offset: Long? = null,
    internal val limit: Long? = null,
    internal val addFields: Array<Selectable>? = null,
    options: InternalOptions = InternalOptions.EMPTY,
) : Stage<SearchStage>("search", options) {
    override fun self(options: InternalOptions): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, addFields, options)

    /** A copy of this stage adding [field] and [additionalFields] to the results. */
    public fun withAddFields(field: Selectable, vararg additionalFields: Selectable): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, arrayOf(field, *additionalFields), options)

    /** A copy of this stage sorting the results by [order] and [additionalOrderings]. */
    public fun withSort(order: Ordering, vararg additionalOrderings: Ordering): SearchStage = SearchStage(query, languageCode, retrievalDepth, arrayOf(order, *additionalOrderings), offset, limit, addFields, options)

    /** A copy of this stage returning at most [limit] results. */
    public fun withLimit(limit: Long): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, addFields, options)

    /** A copy of this stage retrieving at most [retrievalDepth] documents before sorting. */
    public fun withRetrievalDepth(retrievalDepth: Long): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, addFields, options)

    /** A copy of this stage skipping the first [offset] results. */
    public fun withOffset(offset: Long): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, addFields, options)

    /** A copy of this stage analysing the text in the language [languageCode]. */
    public fun withLanguageCode(languageCode: String): SearchStage = SearchStage(query, languageCode, retrievalDepth, sort, offset, limit, addFields, options)

    override fun equals(other: Any?): Boolean = this === other ||
        other is SearchStage && query == other.query && languageCode == other.languageCode && retrievalDepth == other.retrievalDepth &&
        sort.contentEquals(other.sort) && offset == other.offset && limit == other.limit && addFields.contentEquals(other.addFields) && options == other.options

    override fun hashCode(): Int = listOf(query, languageCode, retrievalDepth, sort?.contentHashCode(), offset, limit, addFields?.contentHashCode(), options).hashCode()

    public companion object {
        /** A search for the documents matching [query]. */
        @JvmStatic
        public fun withQuery(query: BooleanExpression): SearchStage = SearchStage(query)

        /** A search for the documents matching the search query [rquery]. */
        @JvmStatic
        public fun withQuery(rquery: String): SearchStage = withQuery(Expression.documentMatches(rquery))
    }
}

internal class LimitStage(internal val limit: Int, options: InternalOptions = InternalOptions.EMPTY) : Stage<LimitStage>("limit", options) {
    override fun self(options: InternalOptions) = LimitStage(limit, options)

    override fun equals(other: Any?): Boolean = this === other || other is LimitStage && limit == other.limit && options == other.options

    override fun hashCode(): Int = 31 * limit + options.hashCode()
}

internal class OffsetStage(internal val offset: Int, options: InternalOptions = InternalOptions.EMPTY) : Stage<OffsetStage>("offset", options) {
    override fun self(options: InternalOptions) = OffsetStage(offset, options)

    override fun equals(other: Any?): Boolean = this === other || other is OffsetStage && offset == other.offset && options == other.options

    override fun hashCode(): Int = 31 * offset + options.hashCode()
}

internal class SelectStage private constructor(internal val fields: Array<out Selectable>, options: InternalOptions) : Stage<SelectStage>("select", options) {
    override fun self(options: InternalOptions) = SelectStage(fields, options)

    override fun equals(other: Any?): Boolean = this === other || other is SelectStage && fields.contentEquals(other.fields) && options == other.options

    override fun hashCode(): Int = 31 * fields.contentHashCode() + options.hashCode()

    companion object {
        fun of(selection: Selectable, vararg additionalSelections: Any): SelectStage = SelectStage(arrayOf(selection, *additionalSelections.map(Selectable::toSelectable).toTypedArray()), InternalOptions.EMPTY)

        fun of(fieldName: String, vararg additionalSelections: Any): SelectStage = of(Expression.field(fieldName), *additionalSelections)
    }
}

internal class SortStage(internal val orders: Array<out Ordering>, options: InternalOptions = InternalOptions.EMPTY) : Stage<SortStage>("sort", options) {
    override fun self(options: InternalOptions) = SortStage(orders, options)

    override fun equals(other: Any?): Boolean = this === other || other is SortStage && orders.contentEquals(other.orders) && options == other.options

    override fun hashCode(): Int = 31 * orders.contentHashCode() + options.hashCode()
}

internal class DistinctStage(internal val groups: Array<out Selectable>, options: InternalOptions = InternalOptions.EMPTY) : Stage<DistinctStage>("distinct", options) {
    override fun self(options: InternalOptions) = DistinctStage(groups, options)

    override fun equals(other: Any?): Boolean = this === other || other is DistinctStage && groups.contentEquals(other.groups) && options == other.options

    override fun hashCode(): Int = 31 * groups.contentHashCode() + options.hashCode()
}

internal class RemoveFieldsStage(internal val fields: Array<out Field>, options: InternalOptions = InternalOptions.EMPTY) : Stage<RemoveFieldsStage>("remove_fields", options) {
    init {
        requireNoReservedAlias(fields) { "Alias $it is required" }
    }

    override fun self(options: InternalOptions) = RemoveFieldsStage(fields, options)

    override fun equals(other: Any?): Boolean = this === other || other is RemoveFieldsStage && fields.contentEquals(other.fields) && options == other.options

    override fun hashCode(): Int = 31 * fields.contentHashCode() + options.hashCode()
}

internal class ReplaceStage(internal val mapValue: Expression, internal val mode: Mode, options: InternalOptions = InternalOptions.EMPTY) : Stage<ReplaceStage>("replace_with", options) {
    internal class Mode private constructor(internal val value: String) {
        companion object {
            val FULL_REPLACE = Mode("full_replace")
            val MERGE_PREFER_NEXT = Mode("merge_prefer_nest")
            val MERGE_PREFER_PARENT = Mode("merge_prefer_parent")
        }
    }

    override fun self(options: InternalOptions) = ReplaceStage(mapValue, mode, options)

    override fun equals(other: Any?): Boolean = this === other || other is ReplaceStage && mapValue == other.mapValue && mode == other.mode && options == other.options

    override fun hashCode(): Int = 31 * (31 * mapValue.hashCode() + mode.hashCode()) + options.hashCode()
}

/** A random sample of the input documents; created with [withDocLimit] or [withPercentage]. */
public class SampleStage private constructor(
    internal val size: Number,
    internal val mode: Mode,
    options: InternalOptions = InternalOptions.EMPTY,
) : Stage<SampleStage>("sample", options) {
    override fun self(options: InternalOptions): SampleStage = SampleStage(size, mode, options)

    override fun equals(other: Any?): Boolean = this === other || other is SampleStage && size == other.size && mode == other.mode && options == other.options

    override fun hashCode(): Int = 31 * (31 * size.hashCode() + mode.hashCode()) + options.hashCode()

    /** Whether a [SampleStage] samples a number of documents or a percentage. */
    public class Mode private constructor(internal val value: String) {
        public companion object {
            /** A number of documents. */
            public val DOCUMENTS: Mode = Mode("documents")

            /** A percentage of the documents. */
            public val PERCENT: Mode = Mode("percent")
        }
    }

    public companion object {
        /** A sample of [percentage] percent of the input documents. */
        @JvmStatic
        public fun withPercentage(percentage: Double): SampleStage = SampleStage(percentage, Mode.PERCENT)

        /** A sample of at most [results] input documents. */
        @JvmStatic
        public fun withDocLimit(results: Int): SampleStage = SampleStage(results, Mode.DOCUMENTS)
    }
}

internal class UnionStage(internal val other: Pipeline, options: InternalOptions = InternalOptions.EMPTY) : Stage<UnionStage>("union", options) {
    override fun self(options: InternalOptions) = UnionStage(other, options)

    override fun equals(other: Any?): Boolean = this === other || other is UnionStage && this.other == other.other && options == other.options

    override fun hashCode(): Int = 31 * other.hashCode() + options.hashCode()
}

/** One output document per element of an array; created with [withField]. */
public class UnnestStage internal constructor(internal val selectable: Selectable, options: InternalOptions = InternalOptions.EMPTY) : Stage<UnnestStage>("unnest", options) {
    override fun self(options: InternalOptions): UnnestStage = UnnestStage(selectable, options)

    /** A copy of this stage writing the index of each element to the field [indexField]. */
    public fun withIndexField(indexField: String): UnnestStage = withOption("index_field", indexField)

    override fun equals(other: Any?): Boolean = this === other || other is UnnestStage && selectable == other.selectable && options == other.options

    override fun hashCode(): Int = 31 * selectable.hashCode() + options.hashCode()

    public companion object {
        /** One document per element of the array [arrayWithAlias], the element under its alias. */
        @JvmStatic
        public fun withField(arrayWithAlias: Selectable): UnnestStage = UnnestStage(arrayWithAlias)

        /** One document per element of the array field [arrayField], the element under [alias]. */
        @JvmStatic
        public fun withField(arrayField: String, alias: String): UnnestStage = UnnestStage(Expression.field(arrayField).alias(alias))
    }
}

/** The options of [Pipeline.unnest]. */
public class UnnestOptions private constructor(options: InternalOptions) : AbstractOptions<UnnestOptions>(options) {
    /** No options. */
    public constructor() : this(InternalOptions.EMPTY)

    /** A copy writing the index of each element to the field [indexField]. */
    public fun withIndexField(indexField: String): UnnestOptions = with("index_field", Expression.field(indexField))

    override fun self(options: InternalOptions): UnnestOptions = UnnestOptions(options)
}

internal class DefineStage(internal val aliasedExpressions: Array<out AliasedExpression>, options: InternalOptions = InternalOptions.EMPTY) : Stage<DefineStage>("let", options) {
    override fun self(options: InternalOptions) = DefineStage(aliasedExpressions, options)

    override fun equals(other: Any?): Boolean = this === other || other is DefineStage && aliasedExpressions.contentEquals(other.aliasedExpressions) && options == other.options

    override fun hashCode(): Int = 31 * aliasedExpressions.contentHashCode() + options.hashCode()
}
