@file:JsModule("firebase/firestore/pipelines")

package io.github.corneloaie.firebase.firestore.externals.pipelines

import io.github.corneloaie.firebase.firestore.externals.DocumentReference
import io.github.corneloaie.firebase.firestore.externals.FieldPath
import io.github.corneloaie.firebase.firestore.externals.Query
import io.github.corneloaie.firebase.firestore.externals.Timestamp
import kotlin.js.Promise

/*
 * The part of the JS SDK's pipeline API (`firebase/firestore/pipelines`, which also adds `pipeline()` to `Firestore`)
 * that the translation of the layer's pipelines uses: functions by their backend name, and the stages with their
 * options objects (each with `rawOptions`, the options by backend name).
 */

public external fun execute(options: JsAny): Promise<PipelineSnapshot>

public external fun field(path: FieldPath): Expression

public external fun constant(value: JsAny?): Expression

public external fun constant(value: Double, options: JsAny): Expression

public external fun variable(name: String): Expression

public external fun subcollection(options: JsAny): Pipeline

public external interface Expression : JsAny {
    public fun asBoolean(): Expression

    @JsName("as")
    public fun alias(name: String): Expression

    public fun ascending(): Ordering

    public fun descending(): Ordering
}

/** An [Expression] (use `unsafeCast`). */
public external class FunctionExpression(name: String, params: JsArray<Expression>) : JsAny {
    public val params: JsArray<Expression>
}

public external class AggregateFunction(name: String, params: JsArray<Expression>) : JsAny {
    @JsName("as")
    public fun alias(name: String): AliasedAggregate
}

public external interface AliasedAggregate : JsAny

public external interface Ordering : JsAny

public external interface PipelineSource : JsAny {
    public fun collection(options: JsAny): Pipeline
    public fun collectionGroup(options: JsAny): Pipeline
    public fun database(options: JsAny): Pipeline
    public fun documents(options: JsAny): Pipeline
    public fun literals(options: JsAny): Pipeline
    public fun createFrom(query: Query): Pipeline
}

public external interface Pipeline : JsAny {
    public fun addFields(options: JsAny): Pipeline
    public fun removeFields(options: JsAny): Pipeline
    public fun select(options: JsAny): Pipeline
    public fun where(options: JsAny): Pipeline
    public fun offset(options: JsAny): Pipeline
    public fun limit(options: JsAny): Pipeline
    public fun distinct(options: JsAny): Pipeline
    public fun aggregate(options: JsAny): Pipeline
    public fun findNearest(options: JsAny): Pipeline
    public fun search(options: JsAny): Pipeline
    public fun sort(options: JsAny): Pipeline
    public fun replaceWith(options: JsAny): Pipeline
    public fun sample(options: JsAny): Pipeline
    public fun union(options: JsAny): Pipeline
    public fun unnest(options: JsAny): Pipeline
    public fun define(options: JsAny): Pipeline
    public fun delete(options: JsAny): Pipeline
    public fun update(options: JsAny): Pipeline
    public fun insert(options: JsAny): Pipeline
    public fun upsert(options: JsAny): Pipeline
    public fun rawStage(name: String, params: JsArray<JsAny?>, options: JsAny): Pipeline
    public fun toArrayExpression(): FunctionExpression
}

public external interface PipelineSnapshot : JsAny {
    public val results: JsArray<PipelineResult>
    public val executionTime: Timestamp?
}

public external interface PipelineResult : JsAny {
    public val ref: DocumentReference?
    public val createTime: Timestamp?
    public val updateTime: Timestamp?
    public fun data(): JsAny?
}
