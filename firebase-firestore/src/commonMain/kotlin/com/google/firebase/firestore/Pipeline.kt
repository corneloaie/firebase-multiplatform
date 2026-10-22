/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.pipeline.AbstractOptions
import com.google.firebase.firestore.pipeline.AddFieldsStage
import com.google.firebase.firestore.pipeline.AggregateFunction
import com.google.firebase.firestore.pipeline.AggregateOptions
import com.google.firebase.firestore.pipeline.AggregateStage
import com.google.firebase.firestore.pipeline.AliasedAggregate
import com.google.firebase.firestore.pipeline.AliasedExpression
import com.google.firebase.firestore.pipeline.BooleanExpression
import com.google.firebase.firestore.pipeline.CollectionGroupOptions
import com.google.firebase.firestore.pipeline.CollectionGroupSource
import com.google.firebase.firestore.pipeline.CollectionSource
import com.google.firebase.firestore.pipeline.CollectionSourceOptions
import com.google.firebase.firestore.pipeline.DatabaseSource
import com.google.firebase.firestore.pipeline.DefineStage
import com.google.firebase.firestore.pipeline.DeleteStage
import com.google.firebase.firestore.pipeline.DistinctStage
import com.google.firebase.firestore.pipeline.DocumentsSource
import com.google.firebase.firestore.pipeline.Expression
import com.google.firebase.firestore.pipeline.Field
import com.google.firebase.firestore.pipeline.FindNearestOptions
import com.google.firebase.firestore.pipeline.FindNearestStage
import com.google.firebase.firestore.pipeline.FunctionExpression
import com.google.firebase.firestore.pipeline.InsertStage
import com.google.firebase.firestore.pipeline.InternalOptions
import com.google.firebase.firestore.pipeline.LimitStage
import com.google.firebase.firestore.pipeline.LiteralsSource
import com.google.firebase.firestore.pipeline.OffsetStage
import com.google.firebase.firestore.pipeline.Ordering
import com.google.firebase.firestore.pipeline.QuerySource
import com.google.firebase.firestore.pipeline.RawStage
import com.google.firebase.firestore.pipeline.RemoveFieldsStage
import com.google.firebase.firestore.pipeline.ReplaceStage
import com.google.firebase.firestore.pipeline.SampleStage
import com.google.firebase.firestore.pipeline.SearchStage
import com.google.firebase.firestore.pipeline.Selectable
import com.google.firebase.firestore.pipeline.SelectStage
import com.google.firebase.firestore.pipeline.SortStage
import com.google.firebase.firestore.pipeline.Stage
import com.google.firebase.firestore.pipeline.SubcollectionSource
import com.google.firebase.firestore.pipeline.UnionStage
import com.google.firebase.firestore.pipeline.UnnestOptions
import com.google.firebase.firestore.pipeline.UnnestStage
import com.google.firebase.firestore.pipeline.UpdateStage
import com.google.firebase.firestore.pipeline.UpsertStage
import com.google.firebase.firestore.pipeline.WhereStage
import dev.gitlive.firebase.firestore.internal.executePipeline
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/*
 * The pipeline API (Firestore Pipelines, a Firestore Enterprise edition feature), mirroring the Firebase Android SDK's
 * Pipeline, PipelineSource and PipelineResult. Plain common code, like com.google.firebase.firestore.pipeline (see
 * Expressions.kt there): the Android SDK's own classes run on Android and the JVM; elsewhere `execute` hands the stages to
 * the platform SDK.
 */

/**
 * A pipeline: a source (from [FirebaseFirestore.pipeline]) followed by stages that filter, transform and aggregate the
 * documents, run by [execute]. Immutable: each stage method returns a new pipeline with the stage appended.
 *
 * On Apple platforms, [execute] with [ExecuteOptions] that set options other than [ExecuteOptions.withAtomic], and source
 * stages with options other than the forced index (`CollectionHints.withForceIndex`), fail: the iOS SDK's pipeline bridge
 * has no way to pass them.
 */
public class Pipeline internal constructor(
    internal val firestore: FirebaseFirestore?,
    internal val stages: List<Stage<*>>,
) {
    internal constructor(firestore: FirebaseFirestore?, stage: Stage<*>) : this(firestore, listOf(stage))

    private fun append(stage: Stage<*>): Pipeline = Pipeline(firestore, stages + stage)

    /**
     * The options of an execution; besides the typed ones, [with] sets an option by its backend name. The iOS SDK takes
     * only [withAtomic]: on Apple platforms, executing with any other fails with [UnsupportedOperationException].
     */
    public class ExecuteOptions private constructor(options: InternalOptions) : AbstractOptions<ExecuteOptions>(options) {
        /** No options. */
        public constructor() : this(InternalOptions.EMPTY)

        override fun self(options: InternalOptions): ExecuteOptions = ExecuteOptions(options)

        /** How the execution uses indexes. */
        public class IndexMode private constructor(internal val value: String) {
            public companion object {
                /** The indexes the backend recommends. */
                @JvmField
                public val RECOMMENDED: IndexMode = IndexMode("recommended")
            }
        }

        /** A copy with the index mode [indexMode]. */
        public fun withIndexMode(indexMode: IndexMode): ExecuteOptions = with("index_mode", indexMode.value)

        /** A copy that runs the pipeline's writes atomically, all or none, if [atomic]. */
        public fun withAtomic(atomic: Boolean): ExecuteOptions = with("atomic", atomic)
    }

    /** The results of an execution, in order, and when the backend computed them. */
    public class Snapshot internal constructor(executionTime: Timestamp, results: List<PipelineResult>) : Iterable<PipelineResult> {
        /** When the backend computed the results. */
        public val executionTime: Timestamp = executionTime

        /** The results, in order. */
        public val results: List<PipelineResult> = results

        override fun iterator(): Iterator<PipelineResult> = results.iterator()

        override fun toString(): String = "Snapshot{executionTime=$executionTime, results=$results}"

        override fun equals(other: Any?): Boolean = this === other || other is Snapshot && results == other.results

        override fun hashCode(): Int = results.hashCode()
    }

    /** Runs the pipeline and completes with its results. */
    public fun execute(): Task<Snapshot> = execute(null)

    /** Runs the pipeline with [options] and completes with its results; on Apple platforms options are not supported (see [ExecuteOptions]). */
    public fun execute(options: ExecuteOptions): Task<Snapshot> = execute(options.options)

    private fun execute(options: InternalOptions?): Task<Snapshot> {
        val firestore = checkNotNull(firestore) {
            "This pipeline was created without a database (e.g., as a subcollection pipeline) and cannot be executed directly. It can only be used as part of another pipeline."
        }
        return executePipeline(firestore, this, options)
    }

    /** Appends [rawStage], a stage by its backend name. */
    public fun rawStage(rawStage: RawStage): Pipeline = append(rawStage)

    /** Adds [field] and [additionalFields] to the documents. */
    public fun addFields(field: Selectable, vararg additionalFields: Selectable): Pipeline = append(AddFieldsStage(arrayOf(field, *additionalFields)))

    /** Removes [field] and [additionalFields] from the documents. */
    public fun removeFields(field: Field, vararg additionalFields: Field): Pipeline = append(RemoveFieldsStage(arrayOf(field, *additionalFields)))

    /** Removes the fields [field] and [additionalFields] from the documents. */
    public fun removeFields(field: String, vararg additionalFields: String): Pipeline = append(RemoveFieldsStage(arrayOf(Expression.field(field), *additionalFields.map { Expression.field(it) }.toTypedArray())))

    /** Replaces the documents with [selection] and [additionalSelections] (selectables or field names). */
    public fun select(selection: Selectable, vararg additionalSelections: Any): Pipeline = append(SelectStage.of(selection, *additionalSelections))

    /** Replaces the documents with the field [fieldName] and [additionalSelections] (selectables or field names). */
    public fun select(fieldName: String, vararg additionalSelections: Any): Pipeline = append(SelectStage.of(fieldName, *additionalSelections))

    /** Sorts the documents by [order], then by [additionalOrders]. */
    public fun sort(order: Ordering, vararg additionalOrders: Ordering): Pipeline = append(SortStage(arrayOf(order, *additionalOrders)))

    /** Keeps the documents for which [condition] is true. */
    public fun where(condition: BooleanExpression): Pipeline = append(WhereStage(condition))

    /** Skips the first [offset] documents. */
    public fun offset(offset: Int): Pipeline = append(OffsetStage(offset))

    /** Keeps the first [limit] documents. */
    public fun limit(limit: Int): Pipeline = append(LimitStage(limit))

    /** Keeps one document per distinct value of [group] and [additionalGroups] (selectables or field names). */
    public fun distinct(group: Selectable, vararg additionalGroups: Any): Pipeline = append(DistinctStage(arrayOf(group, *additionalGroups.map(Selectable::toSelectable).toTypedArray())))

    /** Keeps one document per distinct value of the field [groupField] and [additionalGroups] (selectables or field names). */
    public fun distinct(groupField: String, vararg additionalGroups: Any): Pipeline = append(DistinctStage(arrayOf(Expression.field(groupField), *additionalGroups.map(Selectable::toSelectable).toTypedArray())))

    /** Replaces the documents with one document holding [accumulator] and [additionalAccumulators]. */
    public fun aggregate(accumulator: AliasedAggregate, vararg additionalAccumulators: AliasedAggregate): Pipeline = append(AggregateStage.withAccumulators(accumulator, *additionalAccumulators))

    /** Appends the aggregation [aggregateStage]. */
    public fun aggregate(aggregateStage: AggregateStage): Pipeline = aggregate(aggregateStage, AggregateOptions())

    /** Appends the aggregation [aggregateStage] with [options]. */
    public fun aggregate(aggregateStage: AggregateStage, options: AggregateOptions): Pipeline = append(aggregateStage.withOptions(options))

    /** Keeps the documents whose vector field [vectorField] is nearest to [vectorValue] by [distanceMeasure]. */
    public fun findNearest(vectorField: String, vectorValue: DoubleArray, distanceMeasure: FindNearestStage.DistanceMeasure): Pipeline = append(FindNearestStage.of(vectorField, vectorValue, distanceMeasure))

    /** Keeps the documents whose vector field [vectorField] is nearest to [vectorValue] by [distanceMeasure]. */
    public fun findNearest(vectorField: Field, vectorValue: DoubleArray, distanceMeasure: FindNearestStage.DistanceMeasure): Pipeline = append(FindNearestStage.of(vectorField, vectorValue, distanceMeasure))

    /** Keeps the documents whose vector field [vectorField] is nearest to [vectorValue] by [distanceMeasure], with [options]. */
    public fun findNearest(vectorField: String, vectorValue: Expression, distanceMeasure: FindNearestStage.DistanceMeasure, options: FindNearestOptions): Pipeline = append(FindNearestStage.of(vectorField, vectorValue, distanceMeasure, options))

    /** Replaces each document with the map in its field [field]. */
    public fun replaceWith(field: String): Pipeline = replaceWith(Expression.field(field))

    /** Replaces each document with the map [mapValue]. */
    public fun replaceWith(mapValue: Expression): Pipeline = append(ReplaceStage(mapValue, ReplaceStage.Mode.FULL_REPLACE))

    /** Keeps a random sample of at most [documents] documents. */
    public fun sample(documents: Int): Pipeline = append(SampleStage.withDocLimit(documents))

    /** Keeps the random sample [sample]. */
    public fun sample(sample: SampleStage): Pipeline = append(sample)

    /** Adds the documents of [other]. */
    public fun union(other: Pipeline): Pipeline = append(UnionStage(other))

    /** One document per element of the array field [arrayField], the element under [alias]. */
    public fun unnest(arrayField: String, alias: String): Pipeline = unnest(Expression.field(arrayField).alias(alias))

    /** One document per element of the array [arrayWithAlias], the element under its alias. */
    public fun unnest(arrayWithAlias: Selectable): Pipeline = unnest(arrayWithAlias, UnnestOptions())

    /** One document per element of the array [arrayWithAlias], the element under its alias, with [options]. */
    public fun unnest(arrayWithAlias: Selectable, options: UnnestOptions): Pipeline = append(UnnestStage(arrayWithAlias, options.options))

    /** Appends [unnestStage]. */
    public fun unnest(unnestStage: UnnestStage): Pipeline = append(unnestStage)

    /** Binds [aliasedExpression] and [additionalExpressions] to variables, by their aliases, for the following stages. */
    public fun define(aliasedExpression: AliasedExpression, vararg additionalExpressions: AliasedExpression): Pipeline = append(DefineStage(arrayOf(aliasedExpression, *additionalExpressions)))

    /** This pipeline as a subquery evaluating to the array of its results. */
    public fun toArrayExpression(): Expression = FunctionExpression("array", Expression.toExprOrConstant(this))

    /** This pipeline as a subquery evaluating to its single result. */
    public fun toScalarExpression(): Expression = FunctionExpression("scalar", Expression.toExprOrConstant(this))

    /** Appends the search [searchStage]. Beta in the Android SDK. */
    public fun search(searchStage: SearchStage): Pipeline = append(searchStage)

    /** Deletes each document of the previous stages from the database. */
    public fun delete(): Pipeline = append(DeleteStage())

    /**
     * Writes each document of the previous stages back to its path, with [fields] set under their aliases; the stored
     * document is replaced, so fields earlier stages removed are removed from it too. Creates no documents.
     */
    public fun update(vararg fields: Selectable): Pipeline = append(UpdateStage(fields))

    /** Inserts each document of the previous stages at the path in its `__name__` field; fails if the document exists. */
    public fun insert(): Pipeline = append(InsertStage(null, null))

    /** Inserts each document of the previous stages into the collection at [collectionPath], with its own id or a new one. */
    public fun insert(collectionPath: String): Pipeline = append(InsertStage(collectionPath, null))

    /** Inserts each document of the previous stages into [collection], with its own id or a new one. */
    public fun insert(collection: CollectionReference): Pipeline = append(InsertStage(collectionPathOf(collection), null))

    /** Inserts each document of the previous stages into its own collection, with the id [documentIdExpression] evaluates to. */
    public fun insert(documentIdExpression: Expression): Pipeline = append(InsertStage(null, documentIdExpression))

    /** Inserts each document of the previous stages into the collection at [collectionPath], with the id [documentIdExpression] evaluates to. */
    public fun insert(collectionPath: String, documentIdExpression: Expression): Pipeline = append(InsertStage(collectionPath, documentIdExpression))

    /** Inserts each document of the previous stages into [collection], with the id [documentIdExpression] evaluates to. */
    public fun insert(collection: CollectionReference, documentIdExpression: Expression): Pipeline = append(InsertStage(collectionPathOf(collection), documentIdExpression))

    /**
     * Writes each document of the previous stages, with [additionalFields] added, to the path in its `__name__` field,
     * creating or replacing the document (stored fields are not merged).
     */
    public fun upsert(vararg additionalFields: Selectable): Pipeline = append(UpsertStage(additionalFields, null, null))

    /** Writes each document of the previous stages to the collection at [collectionPath], with its own id or a new one, creating or replacing the document. */
    public fun upsert(collectionPath: String): Pipeline = append(UpsertStage(emptyArray(), collectionPath, null))

    /** Writes each document of the previous stages to [collection], with its own id or a new one, creating or replacing the document. */
    public fun upsert(collection: CollectionReference): Pipeline = append(UpsertStage(emptyArray(), collectionPathOf(collection), null))

    /** Writes each document of the previous stages to the collection at [collectionPath], with the id [documentIdExpression] evaluates to, creating or replacing the document. */
    public fun upsert(collectionPath: String, documentIdExpression: Expression): Pipeline = append(UpsertStage(emptyArray(), collectionPath, documentIdExpression))

    /** Writes each document of the previous stages to [collection], with the id [documentIdExpression] evaluates to, creating or replacing the document. */
    public fun upsert(collection: CollectionReference, documentIdExpression: Expression): Pipeline = append(UpsertStage(emptyArray(), collectionPathOf(collection), documentIdExpression))

    private fun collectionPathOf(collection: CollectionReference): String {
        if (firestore != null) {
            require(isSameFirestore(collection.firestore, firestore)) {
                "Invalid CollectionReference. The Firestore instance of the CollectionReference must match the Firestore instance of the Pipeline."
            }
        }
        return collection.path
    }
}

/** The sources of the pipelines of a [FirebaseFirestore], from [FirebaseFirestore.pipeline]. */
public class PipelineSource internal constructor(private val firestore: FirebaseFirestore) {
    /** A pipeline over the results of [query], with its filters, order, limits and cursors as stages. */
    public fun createFrom(query: Query): Pipeline {
        require(isSameFirestore(query.firestore, firestore)) { "Provided query is from a different Firestore instance." }
        return Pipeline(firestore, QuerySource(query))
    }

    /** A pipeline computing the aggregations of [aggregateQuery], by their aliases. */
    public fun createFrom(aggregateQuery: AggregateQuery): Pipeline {
        val aggregateFields = aggregateQuery.aggregateFields.map { it.toPipeline() }
        return createFrom(aggregateQuery.query).aggregate(aggregateFields.first(), *aggregateFields.drop(1).toTypedArray())
    }

    /** A pipeline over the documents of the collection at [path]. */
    public fun collection(path: String): Pipeline = collection(firestore.collection(path))

    /** A pipeline over the documents of the collection [ref]. */
    public fun collection(ref: CollectionReference): Pipeline = collection(ref, CollectionSourceOptions())

    /** A pipeline over the documents of the collection [ref], with [options]. */
    public fun collection(ref: CollectionReference, options: CollectionSourceOptions): Pipeline {
        require(isSameFirestore(ref.firestore, firestore)) {
            "Invalid CollectionReference. The Firestore instance of the CollectionReference must match the Firestore instance of the PipelineSource."
        }
        return Pipeline(firestore, CollectionSource(ref.path, options))
    }

    /** A pipeline over the documents of every collection with the id [collectionId]. */
    public fun collectionGroup(collectionId: String): Pipeline = collectionGroup(collectionId, CollectionGroupOptions())

    /** A pipeline over the documents of every collection with the id [collectionId], with [options]. */
    public fun collectionGroup(collectionId: String, options: CollectionGroupOptions): Pipeline = Pipeline(firestore, CollectionGroupSource(collectionId, options))

    /** A pipeline over every document of the database. */
    public fun database(): Pipeline = Pipeline(firestore, DatabaseSource())

    /**
     * A pipeline over the literal documents [data], one per map: values are constants or expressions, also nested in maps
     * and lists (FieldValue sentinels are not supported). To give a document a path, set its `__name__` to a
     * [DocumentReference].
     */
    public fun literals(vararg data: Map<String, Any?>): Pipeline = Pipeline(firestore, LiteralsSource(data.toList()))

    /** A pipeline over the documents at the paths [documents]. */
    public fun documents(vararg documents: String): Pipeline = documents(*documents.map { firestore.document(it) }.toTypedArray())

    /** A pipeline over [documents]. */
    public fun documents(vararg documents: DocumentReference): Pipeline {
        for (document in documents) {
            require(isSameFirestore(document.firestore, firestore)) { "Provided document reference is from a different Firestore instance." }
        }
        return Pipeline(firestore, DocumentsSource(documents.map { it.path }))
    }

    /** An aggregation of a query as a pipeline aggregate, under the alias the query's results use. */
    private fun AggregateField.toPipeline(): AliasedAggregate = when (this) {
        is AggregateField.CountAggregateField -> AggregateFunction.countAll().alias(alias)
        is AggregateField.SumAggregateField -> Expression.field(fieldPath).sum().alias(alias)
        is AggregateField.AverageAggregateField -> Expression.field(fieldPath).average().alias(alias)
        else -> throw IllegalArgumentException("Unknown aggregation: $operator")
    }

    public companion object {
        /** A pipeline over the subcollection at [path] of each input document, for use as a subquery. */
        @JvmStatic
        public fun subcollection(path: String): Pipeline = Pipeline(null, SubcollectionSource.of(path))

        /** A pipeline over the subcollection [source] of each input document, for use as a subquery. */
        @JvmStatic
        public fun subcollection(source: SubcollectionSource): Pipeline = Pipeline(null, source)
    }
}

/** A result of a [Pipeline]: a document of the last stage, with its fields and, for a stored document, its reference. */
public class PipelineResult internal constructor(
    ref: DocumentReference?,
    private val fields: Map<String, Any?>,
    createTime: Timestamp?,
    updateTime: Timestamp?,
) {
    /** When the document was created, for a stored document. */
    public val createTime: Timestamp? = createTime

    /** When the document was last updated, for a stored document. */
    public val updateTime: Timestamp? = updateTime

    /** The reference of the document, for a stored document. */
    public val ref: DocumentReference? = ref

    /** The id of the document, for a stored document. */
    public fun getId(): String? = ref?.id

    /** The fields of the result. */
    public fun getData(): Map<String, Any?> = fields.toMap()

    /** The value at the `.`-separated [field], or null. */
    public fun get(field: String): Any? = get(FieldPath.fromDotSeparatedPath(field))

    /** The value at [fieldPath], or null. */
    public fun get(fieldPath: FieldPath): Any? {
        val segments = fieldPathSegments(fieldPath)
        if (segments.isEmpty()) return fields
        var value: Any? = fields
        for (segment in segments) {
            value = (value as? Map<*, *>)?.takeIf { it.containsKey(segment) }?.get(segment) ?: return null
        }
        return value
    }

    override fun toString(): String = "PipelineResult{ref=$ref, updateTime=$updateTime}, data=${getData()}"

    override fun equals(other: Any?): Boolean = this === other || other is PipelineResult && ref == other.ref && fields == other.fields

    override fun hashCode(): Int = 31 * (ref?.hashCode() ?: 0) + fields.hashCode()
}
