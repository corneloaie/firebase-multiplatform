/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore.internal

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.PipelineResult
import com.google.firebase.firestore.objectKeys
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
import com.google.firebase.firestore.pipeline.DistinctStage
import com.google.firebase.firestore.pipeline.DocumentsSource
import com.google.firebase.firestore.pipeline.Expression
import com.google.firebase.firestore.pipeline.Field
import com.google.firebase.firestore.pipeline.FindNearestStage
import com.google.firebase.firestore.pipeline.FunctionExpression
import com.google.firebase.firestore.pipeline.GenericArg
import com.google.firebase.firestore.pipeline.InternalOptions
import com.google.firebase.firestore.pipeline.LimitStage
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
import com.google.firebase.firestore.pipeline.Variable
import com.google.firebase.firestore.pipeline.WhereStage
import com.google.firebase.firestore.task
import com.google.firebase.firestore.toCompat
import com.google.firebase.firestore.toJs
import com.google.firebase.firestore.VectorValue
import kotlin.js.Json
import kotlin.js.json
import dev.gitlive.firebase.firestore.externals.FieldPath as JsFieldPath
import dev.gitlive.firebase.firestore.externals.pipelines.AggregateFunction as JsAggregateFunction
import dev.gitlive.firebase.firestore.externals.pipelines.Expression as JsExpression
import dev.gitlive.firebase.firestore.externals.pipelines.FunctionExpression as JsFunctionExpression
import dev.gitlive.firebase.firestore.externals.pipelines.Ordering as JsOrdering
import dev.gitlive.firebase.firestore.externals.pipelines.Pipeline as JsPipeline
import dev.gitlive.firebase.firestore.externals.pipelines.PipelineSnapshot as JsPipelineSnapshot
import dev.gitlive.firebase.firestore.externals.pipelines.PipelineSource as JsPipelineSource
import dev.gitlive.firebase.firestore.externals.pipelines.constant as jsConstant
import dev.gitlive.firebase.firestore.externals.pipelines.execute as jsExecute
import dev.gitlive.firebase.firestore.externals.pipelines.field as jsField
import dev.gitlive.firebase.firestore.externals.pipelines.subcollection as jsSubcollection
import dev.gitlive.firebase.firestore.externals.pipelines.variable as jsVariable

/*
 * The layer's pipelines on the JS SDK's (`firebase/firestore/pipelines`): functions and aggregates by their backend
 * name, the stages through the JS SDK's methods, and the options of each stage, by backend name, as its `rawOptions`,
 * so that the request is the one the Android SDK sends. Two differences remain: the JS SDK sends an integral double
 * option as an integer, and it cannot send a field reference as an option, except the distance field of a nearest-
 * neighbour search and the index field of an unnest, which have typed options; any other fails the execution.
 *
 * Mirrored in wasmJsMain (Pipelines.wasmJs.kt).
 */

internal actual fun executePipeline(firestore: FirebaseFirestore, pipeline: Pipeline, options: InternalOptions?): Task<Pipeline.Snapshot> {
    val request = json("pipeline" to pipeline.toJsPipeline())
    if (options != null && !options.isEmpty()) request["rawOptions"] = options.toJsRawOptions("execute")
    return task { jsExecute(request).then { it.toSnapshot() } }
}

private fun JsPipelineSnapshot.toSnapshot(): Pipeline.Snapshot = Pipeline.Snapshot(
    executionTime?.toCompat() as Timestamp? ?: Timestamp.now(),
    results.map { result ->
        @Suppress("UNCHECKED_CAST")
        PipelineResult(
            result.ref?.let { DocumentReference(it) },
            result.data().toCompat() as Map<String, Any?>? ?: emptyMap(),
            result.createTime?.toCompat() as Timestamp?,
            result.updateTime?.toCompat() as Timestamp?,
        )
    },
)

private fun Pipeline.toJsPipeline(): JsPipeline = stages.drop(1).fold(stages.first().toJsSource(firestore)) { js, stage -> stage.appendTo(js) }

private fun Stage<*>.toJsSource(firestore: FirebaseFirestore?): JsPipeline {
    fun source(): JsPipelineSource = checkNotNull(firestore) { "The $name stage needs a database" }.js.asDynamic().pipeline().unsafeCast<JsPipelineSource>()
    return when (this) {
        is CollectionSource -> source().collection(stageOptions("collection" to path))
        is CollectionGroupSource -> source().collectionGroup(stageOptions("collectionId" to collectionId))
        is DatabaseSource -> source().database(stageOptions())
        is DocumentsSource -> source().documents(stageOptions("docs" to documents.toTypedArray()))
        is QuerySource -> source().createFrom(query.js)
        is SubcollectionSource -> jsSubcollection(stageOptions("path" to path))
        else -> throw IllegalArgumentException("A pipeline starts with a source stage, not $name")
    }
}

private fun Stage<*>.appendTo(js: JsPipeline): JsPipeline = when (this) {
    is WhereStage -> js.where(stageOptions("condition" to condition.toJsExpression()))
    is LimitStage -> js.limit(stageOptions("limit" to limit))
    is OffsetStage -> js.offset(stageOptions("offset" to offset))
    is AddFieldsStage -> js.addFields(stageOptions("fields" to selectables(fields)))
    is RemoveFieldsStage -> js.removeFields(stageOptions("fields" to fields.map { it.toJsExpression() }.toTypedArray()))
    is SelectStage -> js.select(stageOptions("selections" to selectables(fields)))
    is DistinctStage -> js.distinct(stageOptions("groups" to selectables(groups)))
    is DefineStage -> js.define(stageOptions("variables" to selectables(aliasedExpressions)))
    is AggregateStage -> js.aggregate(
        stageOptions(
            "accumulators" to accumulators.map { (alias, function) -> function.toJsAggregate().alias(alias) }.toTypedArray(),
            "groups" to groups.map { (alias, expr) -> selectable(alias, expr) }.toTypedArray(),
        ),
    )
    is FindNearestStage -> {
        val vectorValue = (vector as? Constant)?.value as? VectorValue
        if (vectorValue != null && property is Field) {
            js.findNearest(stageOptions("field" to property.toJsExpression(), "vectorValue" to vectorValue.toJs(), "distanceMeasure" to distanceMeasure.value))
        } else {
            js.rawStage(name, arrayOf(property.toJsExpression(), vector.toJsExpression(), jsConstant(distanceMeasure.value)), options.toJsRawOptions(name))
        }
    }
    is SearchStage -> js.search(
        stageOptions(
            "query" to query.toJsExpression(),
            "languageCode" to languageCode,
            "retrievalDepth" to retrievalDepth?.toDouble(),
            "sort" to sort?.map { it.toJsOrdering() }?.toTypedArray(),
            "offset" to offset?.toDouble(),
            "limit" to limit?.toDouble(),
            "addFields" to addFields?.let { selectables(it) },
        ),
    )
    is SortStage -> js.sort(stageOptions("orderings" to orders.map { it.toJsOrdering() }.toTypedArray()))
    is ReplaceStage -> js.replaceWith(stageOptions("map" to mapValue.toJsExpression()))
    is SampleStage -> js.sample(stageOptions(if (mode == SampleStage.Mode.DOCUMENTS) "documents" to size.toDouble() else "percentage" to size.toDouble()))
    is UnionStage -> js.union(stageOptions("other" to other.toJsPipeline()))
    is UnnestStage -> js.unnest(stageOptions("selectable" to selectable(selectable.alias, selectable.expr)))
    is RawStage -> js.rawStage(name, arguments.map { it.toJsArgument() }.toTypedArray(), options.toJsRawOptions(name))
    else -> throw IllegalArgumentException("The $name stage starts a pipeline")
}

/** The selectables by alias, as JS selectables; an alias may appear once. */
private fun selectables(fields: Array<out Selectable>): Array<JsExpression> = Stage.associateWithoutDuplications(fields).map { (alias, expr) -> selectable(alias, expr) }.toTypedArray()

private fun selectable(alias: String, expr: Expression): JsExpression = if (expr is Field && expr.alias == alias) expr.toJsExpression() else expr.toJsExpression().alias(alias)

/** The field reference options with a typed option in the JS SDK, by stage and option name. */
private val fieldOptions = mapOf(("find_nearest" to "distance_field") to "distanceField", ("unnest" to "index_field") to "indexField")

/** The options object of a JS stage method: the [typed] arguments, and the options of the stage as its `rawOptions`. */
private fun Stage<*>.stageOptions(vararg typed: Pair<String, Any?>): Json {
    val result = json(*typed.filter { it.second != null }.toTypedArray())
    val raw = json()
    for ((key, value) in options.entries) {
        if (value is Field) {
            val typedKey = fieldOptions[name to key] ?: throw UnsupportedOperationException("The Firebase JS SDK cannot send the field reference option '$key' of the $name stage")
            result[typedKey] = value.alias
        } else {
            raw[key] = value.toJsOption(name, key)
        }
    }
    if (objectKeys(raw).isNotEmpty()) result["rawOptions"] = raw
    return result
}

private fun InternalOptions.toJsRawOptions(stage: String): Json = json(*entries.map { (key, value) -> key to value.toJsOption(stage, key) }.toTypedArray())

private fun Any.toJsOption(stage: String, key: String): Any? = when (this) {
    is Field -> throw UnsupportedOperationException("The Firebase JS SDK cannot send the field reference option '$key' of the $stage stage")
    is InternalOptions -> toJsRawOptions(stage)
    is List<*> -> map { it?.toJsOption(stage, key) }.toTypedArray()
    else -> toJs()
}

private fun Expression.toJsExpression(): JsExpression = when (this) {
    is Constant -> when (value) {
        // Kotlin/JS numbers other than Long are JS numbers, so an integral double is sent as an integer, as the JS SDK
        // sends it; wasmJs, whose doubles are distinct, sends them as doubles.
        is ByteArray -> jsConstant(Blob.fromBytes(value).toJs())
        else -> jsConstant(value.toJs())
    }
    is Field -> jsField(JsFieldPath(*segments.toTypedArray()))
    is Variable -> jsVariable(name)
    is FunctionExpression -> JsFunctionExpression(name, params.map { it.toJsExpression() }.toTypedArray()).unsafeCast<JsExpression>()
    is BooleanFunctionExpression -> expr.toJsExpression().asBoolean()
    is BooleanConstant -> constant.toJsExpression().asBoolean()
    is BooleanField -> field.toJsExpression().asBoolean()
    is AliasedExpression -> expr.toJsExpression().alias(alias)
    // The JS SDK's pipeline value expression is not exported: take it from the subquery function it builds.
    is PipelineValueExpression -> pipeline.toJsPipeline().toArrayExpression().params[0]
    else -> throw IllegalArgumentException("Unknown expression $this")
}

private fun Ordering.toJsOrdering(): JsOrdering = if (dir == Ordering.Direction.ASCENDING) expr.toJsExpression().ascending() else expr.toJsExpression().descending()

private fun AggregateFunction.toJsAggregate(): JsAggregateFunction = JsAggregateFunction(name, params.map { it.toJsExpression() }.toTypedArray())

private fun GenericArg.toJsArgument(): Any? = when (this) {
    is GenericArg.ExprArg -> expr.toJsExpression()
    is GenericArg.AggregateArg -> aggregate.toJsAggregate()
    is GenericArg.OrderingArg -> json("direction" to ordering.dir.name.lowercase(), "expression" to ordering.expr.toJsExpression())
    is GenericArg.MapArg -> json(*args.map { (key, value) -> key to value.toJsArgument() }.toTypedArray())
    // the JS SDK takes an array as a constant, so only of constants
    is GenericArg.ListArg -> jsConstant(
        args.map { arg ->
            val constant = (arg as? GenericArg.ExprArg)?.expr as? Constant
                ?: throw UnsupportedOperationException("The Firebase JS SDK cannot send an array of expressions as a stage argument")
            constant.value.toJs()
        }.toTypedArray(),
    )
}
