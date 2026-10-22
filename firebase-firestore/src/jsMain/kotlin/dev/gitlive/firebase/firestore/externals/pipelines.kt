@file:JsModule("firebase/firestore/pipelines")
@file:JsNonModule

package dev.gitlive.firebase.firestore.externals.pipelines

import dev.gitlive.firebase.firestore.externals.DocumentReference
import dev.gitlive.firebase.firestore.externals.FieldPath
import dev.gitlive.firebase.firestore.externals.Query
import dev.gitlive.firebase.firestore.externals.Timestamp
import kotlin.js.Json
import kotlin.js.Promise

/*
 * The part of the JS SDK's pipeline API (`firebase/firestore/pipelines`, which also adds `pipeline()` to `Firestore`)
 * that the translation of the layer's pipelines uses: functions by their backend name, and the stages with their
 * options objects (each with `rawOptions`, the options by backend name).
 */

public external fun execute(options: Json): Promise<PipelineSnapshot>

public external fun field(path: FieldPath): Expression

public external fun constant(value: Any?): Expression

public external fun variable(name: String): Expression

public external fun subcollection(options: Json): Pipeline

public external interface Expression {
    public fun asBoolean(): Expression

    @JsName("as")
    public fun alias(name: String): Expression

    public fun ascending(): Ordering

    public fun descending(): Ordering
}

/** An [Expression] (use `unsafeCast`). */
public external class FunctionExpression(name: String, params: Array<Expression>) {
    public val params: Array<Expression>
}

public external class AggregateFunction(name: String, params: Array<Expression>) {
    @JsName("as")
    public fun alias(name: String): AliasedAggregate
}

public external interface AliasedAggregate

public external interface Ordering

public external interface PipelineSource {
    public fun collection(options: Json): Pipeline
    public fun collectionGroup(options: Json): Pipeline
    public fun database(options: Json): Pipeline
    public fun documents(options: Json): Pipeline
    public fun literals(options: Json): Pipeline
    public fun createFrom(query: Query): Pipeline
}

public external interface Pipeline {
    public fun addFields(options: Json): Pipeline
    public fun removeFields(options: Json): Pipeline
    public fun select(options: Json): Pipeline
    public fun where(options: Json): Pipeline
    public fun offset(options: Json): Pipeline
    public fun limit(options: Json): Pipeline
    public fun distinct(options: Json): Pipeline
    public fun aggregate(options: Json): Pipeline
    public fun findNearest(options: Json): Pipeline
    public fun search(options: Json): Pipeline
    public fun sort(options: Json): Pipeline
    public fun replaceWith(options: Json): Pipeline
    public fun sample(options: Json): Pipeline
    public fun union(options: Json): Pipeline
    public fun unnest(options: Json): Pipeline
    public fun define(options: Json): Pipeline
    public fun delete(options: Json): Pipeline
    public fun update(options: Json): Pipeline
    public fun insert(options: Json): Pipeline
    public fun upsert(options: Json): Pipeline
    public fun rawStage(name: String, params: Array<Any?>, options: Json): Pipeline
    public fun toArrayExpression(): FunctionExpression
}

public external interface PipelineSnapshot {
    public val results: Array<PipelineResult>
    public val executionTime: Timestamp?
}

public external interface PipelineResult {
    public val ref: DocumentReference?
    public val createTime: Timestamp?
    public val updateTime: Timestamp?
    public fun data(): Any?
}
