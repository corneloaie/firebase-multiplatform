/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.firestore.internal

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.PipelineResult
import com.google.firebase.firestore.pipeline.AddFieldsStage
import com.google.firebase.firestore.pipeline.AggregateFunction
import com.google.firebase.firestore.pipeline.AggregateStage
import com.google.firebase.firestore.pipeline.AliasedExpression
import com.google.firebase.firestore.pipeline.BooleanConstant
import com.google.firebase.firestore.pipeline.BooleanField
import com.google.firebase.firestore.pipeline.BooleanFunctionExpression
import com.google.firebase.firestore.pipeline.CollectionGroupSource
import com.google.firebase.firestore.pipeline.CollectionSource
import com.google.firebase.firestore.pipeline.Constant
import com.google.firebase.firestore.pipeline.DatabaseSource
import com.google.firebase.firestore.pipeline.DefineStage
import com.google.firebase.firestore.pipeline.DeleteStage
import com.google.firebase.firestore.pipeline.DistinctStage
import com.google.firebase.firestore.pipeline.DocumentsSource
import com.google.firebase.firestore.pipeline.Expression
import com.google.firebase.firestore.pipeline.Field
import com.google.firebase.firestore.pipeline.FindNearestStage
import com.google.firebase.firestore.pipeline.FunctionExpression
import com.google.firebase.firestore.pipeline.GenericArg
import com.google.firebase.firestore.pipeline.InsertStage
import com.google.firebase.firestore.pipeline.InternalOptions
import com.google.firebase.firestore.pipeline.LimitStage
import com.google.firebase.firestore.pipeline.LiteralsSource
import com.google.firebase.firestore.pipeline.OffsetStage
import com.google.firebase.firestore.pipeline.Ordering
import com.google.firebase.firestore.pipeline.PipelineValueExpression
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
import com.google.firebase.firestore.pipeline.UnnestStage
import com.google.firebase.firestore.pipeline.UpdateStage
import com.google.firebase.firestore.pipeline.UpsertStage
import com.google.firebase.firestore.pipeline.Variable
import com.google.firebase.firestore.pipeline.WhereStage
import com.google.firebase.firestore.task
import com.google.firebase.firestore.toCompat
import com.google.firebase.firestore.toCompatData
import com.google.firebase.firestore.toIos
import com.google.firebase.firestore.toNSData
import com.google.firebase.firestore.VectorValue
import platform.Foundation.NSNull
import platform.Foundation.NSNumber
import swiftPMImport.io.github.corneloaie.firebase.firestore.FIRFieldPath
import swiftPMImport.io.github.corneloaie.firebase.firestore.FIRVectorValue
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRAddFieldsStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRAggregateFunctionBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRAggregateStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRCollectionGroupSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRCollectionSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRConstantBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRDatabaseSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRDefineStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRDeleteStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRDistinctStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRDocumentsSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRExprBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRFieldBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRInsertStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRFindNearestStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRFunctionExprBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRLimitStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRLiteralsSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIROffsetStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIROrderingBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRPipelineBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRPipelineExprBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRPipelineResultBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRPipelineSnapshotBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRRawStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRRemoveFieldsStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRReplaceWithStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRSampleStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRSearchStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRSelectStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRSortStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRSubcollectionSourceStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRUnionStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRUnnestStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRUpdateStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRUpsertStageBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRVariableBridge
import swiftPMImport.io.github.corneloaie.firebase.firestore.__FIRWhereStageBridge

/*
 * The layer's pipelines on the iOS SDK's Objective-C pipeline bridge (FIRPipelineBridge.h, public in
 * FirebaseFirestoreInternal), which its Swift pipeline API is built on: functions and aggregates by their backend name,
 * each stage through its bridge class. A stage with options goes as a raw stage (name, arguments and options, as the
 * Android SDK sends them), as the stage bridges take no options; source stages have no raw form, so they only take the
 * forced index. The bridge executes with no options but atomic, so the other execute options are not supported either.
 */

internal actual fun executePipeline(firestore: FirebaseFirestore, pipeline: Pipeline, options: InternalOptions?): Task<Pipeline.Snapshot> {
    val unsupported = options?.entries?.keys.orEmpty() - "atomic"
    if (unsupported.isNotEmpty()) throw UnsupportedOperationException("The Firebase iOS SDK does not support execute options: $unsupported")
    val bridge = if (options?.entries?.get("atomic") == true) {
        __FIRPipelineBridge(pipeline.toIosStages(), firestore.ios, atomic = true)
    } else {
        __FIRPipelineBridge(pipeline.toIosStages(), firestore.ios)
    }
    return task { completion ->
        bridge.executeWithCompletion { snapshot, error -> completion(snapshot?.toSnapshot(), error) }
    }
}

private fun __FIRPipelineSnapshotBridge.toSnapshot(): Pipeline.Snapshot = Pipeline.Snapshot(
    execution_time.toCompat() as Timestamp,
    results.map { result ->
        (result as __FIRPipelineResultBridge).let {
            PipelineResult(
                it.reference?.let { reference -> DocumentReference(reference) },
                it.data().toCompatData() ?: emptyMap(),
                it.create_time?.toCompat() as Timestamp?,
                it.update_time?.toCompat() as Timestamp?,
            )
        }
    },
)

private fun Pipeline.toIosStages(): List<__FIRStageBridge> {
    val source = stages.first()
    val sources: List<__FIRStageBridge> = when (source) {
        // the query's filters, order, limits and cursors as stages, converted by the SDK
        is QuerySource -> __FIRPipelineBridge.createStageBridgesFromQuery(source.query.ios).map { it as __FIRStageBridge }
        else -> listOf(source.toIosSource(firestore))
    }
    return sources + stages.drop(1).map { it.toIosStage() }
}

private fun Stage<*>.toIosSource(firestore: FirebaseFirestore?): __FIRStageBridge {
    fun db() = checkNotNull(firestore) { "The $name stage needs a database" }.ios
    val forceIndex = options.entries["force_index"] as String?
    val unsupported = options.entries.keys - "force_index"
    if (unsupported.isNotEmpty()) throw UnsupportedOperationException("The Firebase iOS SDK does not support the options $unsupported of the $name stage")
    return when (this) {
        is CollectionSource -> __FIRCollectionSourceStageBridge(db().collectionWithPath(path), db(), forceIndex)
        is CollectionGroupSource -> __FIRCollectionGroupSourceStageBridge(collectionId, forceIndex)
        is DatabaseSource -> __FIRDatabaseSourceStageBridge()
        is DocumentsSource -> __FIRDocumentsSourceStageBridge(documents.map { db().documentWithPath(it) }, db())
        is SubcollectionSource -> __FIRSubcollectionSourceStageBridge(path)
        // each value as an expression: a constant, or evaluated (maps and lists holding expressions as map and array)
        is LiteralsSource -> __FIRLiteralsSourceStageBridge(data.map { document -> document.mapValues { (_, value) -> Expression.toExprOrConstant(value).toIosExpression() } })
        else -> throw IllegalArgumentException("A pipeline starts with a source stage, not $name")
    }
}

private fun Stage<*>.toIosStage(): __FIRStageBridge {
    // The stage bridges take no options: a stage with options is sent as a raw stage, with the Android SDK's arguments.
    if (!options.isEmpty() && this !is RawStage && this !is SearchStage && !hasTypedOptions()) {
        return __FIRRawStageBridge(name, rawArguments(), options.toIosOptions(name))
    }
    return when (this) {
        is WhereStage -> __FIRWhereStageBridge(condition.toIosExpression())
        is LimitStage -> __FIRLimitStageBridge(limit.toLong())
        is OffsetStage -> __FIROffsetStageBridge(offset.toLong())
        is AddFieldsStage -> __FIRAddFieldsStageBridge(selectables(fields))
        is RemoveFieldsStage -> __FIRRemoveFieldsStageBridge(fields.map { it.alias })
        is SelectStage -> __FIRSelectStageBridge(selectables(fields))
        is DistinctStage -> __FIRDistinctStageBridge(selectables(groups))
        is DefineStage -> __FIRDefineStageBridge(selectables(aliasedExpressions))
        is AggregateStage -> __FIRAggregateStageBridge(
            accumulators.mapValues { (_, function) -> function.toIosAggregate() },
            groups.mapValues { (_, expr) -> expr.toIosExpression() },
        )
        is FindNearestStage -> __FIRFindNearestStageBridge(
            (property as? Field ?: throw UnsupportedOperationException("The Firebase iOS SDK only searches the nearest neighbours of a field")).toIosField(),
            vectorValue(),
            distanceMeasure.value,
            (options.entries["limit"] as Long?)?.let { NSNumber(longLong = it) },
            (options.entries["distance_field"] as Field?)?.toIosField(),
        )
        is SearchStage -> __FIRSearchStageBridge(
            buildMap {
                put("query", query.toIosExpression())
                languageCode?.let { put("language_code", constant(it)) }
                retrievalDepth?.let { put("retrieval_depth", constant(it)) }
                offset?.let { put("offset", constant(it)) }
                limit?.let { put("limit", constant(it)) }
                putAll(options.toIosOptions(name))
            },
            addFields?.let { selectables(it) }.orEmpty(),
            emptyMap<Any?, Any?>(),
            sort?.map { it.toIosOrdering() }.orEmpty(),
        )
        is SortStage -> __FIRSortStageBridge(orders.map { it.toIosOrdering() })
        is ReplaceStage -> __FIRReplaceWithStageBridge(mapValue.toIosExpression())
        is SampleStage -> if (mode == SampleStage.Mode.DOCUMENTS) __FIRSampleStageBridge(size.toLong()) else __FIRSampleStageBridge(size.toDouble())
        is UnionStage -> __FIRUnionStageBridge(__FIRPipelineBridge(other.toIosStages(), checkNotNull(other.firestore) { "A union needs a pipeline with a database" }.ios))
        is UnnestStage -> __FIRUnnestStageBridge(selectable.expr.toIosExpression(), Expression.field(selectable.alias).toIosExpression(), (options.entries["index_field"] as Field?)?.toIosField())
        is RawStage -> __FIRRawStageBridge(name, arguments.map { it.toIosArgument() }, options.toIosOptions(name))
        is DeleteStage -> __FIRDeleteStageBridge()
        is UpdateStage -> __FIRUpdateStageBridge(selectables(fields))
        is InsertStage -> __FIRInsertStageBridge(collectionPath, documentIdExpression?.toIosExpression())
        is UpsertStage -> __FIRUpsertStageBridge(selectables(fields), collectionPath, documentIdExpression?.toIosExpression())
        else -> throw IllegalArgumentException("The $name stage starts a pipeline")
    }
}

/** Whether the bridge of this stage takes all its options as arguments of its own. */
private fun Stage<*>.hasTypedOptions(): Boolean = when (this) {
    is FindNearestStage -> options.entries.keys.all { it == "limit" || it == "distance_field" } &&
        options.entries["limit"].let { it == null || it is Long } && options.entries["distance_field"].let { it == null || it is Field }
    is UnnestStage -> options.entries.keys == setOf("index_field") && options.entries["index_field"] is Field
    else -> false
}

/** The vector of a nearest-neighbour search, which the bridge takes as a constant. */
private fun FindNearestStage.vectorValue(): FIRVectorValue = ((vector as? Constant)?.value as? VectorValue)?.toIos() as? FIRVectorValue
    ?: throw UnsupportedOperationException("The Firebase iOS SDK only searches the nearest neighbours of a constant vector")

/** The arguments of this stage as the Android SDK sends them, for a raw stage. */
private fun Stage<*>.rawArguments(): List<Any> = when (this) {
    is WhereStage -> listOf(condition.toIosExpression())
    is LimitStage -> listOf(constant(limit))
    is OffsetStage -> listOf(constant(offset))
    is AddFieldsStage -> listOf(selectables(fields))
    is RemoveFieldsStage -> fields.map { it.toIosExpression() }
    is SelectStage -> listOf(selectables(fields))
    is DistinctStage -> listOf(selectables(groups))
    is DefineStage -> listOf(selectables(aliasedExpressions))
    is AggregateStage -> listOf(accumulators.mapValues { (_, function) -> function.toIosAggregate() }, groups.mapValues { (_, expr) -> expr.toIosExpression() })
    is FindNearestStage -> listOf(property.toIosExpression(), vector.toIosExpression(), constant(distanceMeasure.value))
    is SortStage -> orders.map { it.toIosArgument() }
    is ReplaceStage -> listOf(mapValue.toIosExpression(), constant(mode.value))
    is SampleStage -> listOf(constant(size), constant(mode.value))
    is UnionStage -> listOf(__FIRPipelineExprBridge(other.toIosStages()))
    is UnnestStage -> listOf(selectable.expr.toIosExpression(), Expression.field(selectable.alias).toIosExpression())
    else -> throw IllegalArgumentException("The $name stage has no raw form")
}

/** The selectables by alias, as the bridge's dictionaries; an alias may appear once. */
private fun selectables(fields: Array<out Selectable>): Map<Any?, *> = Stage.associateWithoutDuplications(fields).mapValues { (_, expr) -> expr.toIosExpression() }

private fun constant(value: Any?): __FIRConstantBridge = __FIRConstantBridge(value?.toIos() ?: NSNull.`null`())

/** The options of a stage as the bridge's: expressions by name. */
private fun InternalOptions.toIosOptions(stage: String): Map<Any?, *> {
    val options = entries
    return buildMap<Any?, Any?> { options.forEach { (key, value) -> put(key, value.toIosOption(stage, key)) } }
}

private fun Any.toIosOption(stage: String, key: String): __FIRExprBridge = when (this) {
    is Field -> toIosField()
    is InternalOptions -> constant(entries.mapValues { (nestedKey, value) -> (value as? Field)?.let { throw UnsupportedOperationException("The Firebase iOS SDK cannot send the nested field reference option '$key.$nestedKey' of the $stage stage") } ?: value })
    else -> constant(this)
}

private fun Field.toIosField(): __FIRFieldBridge = __FIRFieldBridge(FIRFieldPath(segments))

private fun Expression.toIosExpression(): __FIRExprBridge = when (this) {
    is Constant -> when (value) {
        is ByteArray -> __FIRConstantBridge(value.toNSData())
        else -> constant(value)
    }
    is Field -> toIosField()
    is Variable -> __FIRVariableBridge(name)
    is FunctionExpression -> __FIRFunctionExprBridge(name, params.map { it.toIosExpression() }, options.takeUnless { it.isEmpty() }?.toIosOptions(name))
    is BooleanFunctionExpression -> expr.toIosExpression()
    is BooleanConstant -> constant.toIosExpression()
    is BooleanField -> field.toIosExpression()
    is AliasedExpression -> expr.toIosExpression()
    is PipelineValueExpression -> __FIRPipelineExprBridge(pipeline.toIosStages())
    else -> throw IllegalArgumentException("Unknown expression $this")
}

private fun Ordering.toIosOrdering(): __FIROrderingBridge = __FIROrderingBridge(expr.toIosExpression(), dir.name.lowercase())

/** An ordering as the Android SDK sends it in a stage's arguments: a map of its direction and expression. */
private fun Ordering.toIosArgument(): Map<Any?, *> = mapOf("direction" to constant(dir.name.lowercase()), "expression" to expr.toIosExpression())

private fun AggregateFunction.toIosAggregate(): __FIRAggregateFunctionBridge = __FIRAggregateFunctionBridge(name, params.map { it.toIosExpression() })

private fun GenericArg.toIosArgument(): Any = when (this) {
    is GenericArg.ExprArg -> expr.toIosExpression()
    is GenericArg.AggregateArg -> aggregate.toIosAggregate()
    is GenericArg.OrderingArg -> ordering.toIosArgument()
    is GenericArg.MapArg -> args.mapValues { (_, value) -> value.toIosArgument() }
    // the bridge takes an array as a constant, so only of constants
    is GenericArg.ListArg -> constant(
        args.map { arg ->
            val constant = (arg as? GenericArg.ExprArg)?.expr as? Constant
                ?: throw UnsupportedOperationException("The Firebase iOS SDK cannot send an array of expressions as a stage argument")
            constant.value
        },
    )
}
