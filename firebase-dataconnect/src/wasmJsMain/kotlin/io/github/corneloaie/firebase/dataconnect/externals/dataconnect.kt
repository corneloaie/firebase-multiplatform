/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JsModule("firebase/data-connect")

package io.github.corneloaie.firebase.dataconnect.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

public external interface DataConnect : JsAny

public external interface QueryRef : JsAny

public external interface MutationRef : JsAny

/** The result of `executeQuery`/`executeMutation`: the data, and for a query `"CACHE"` or `"SERVER"`. */
public external interface OperationResult : JsAny {
    public val data: JsAny?
    public val source: String
}

/** [connectorConfig] is `{ location, connector, service }`; [settings] is `{ cacheSettings?: { cacheProvider, maxAgeSeconds? } }`. */
public external fun getDataConnect(app: FirebaseApp, connectorConfig: JsAny, settings: JsAny): DataConnect

public external fun connectDataConnectEmulator(dc: DataConnect, host: String, port: Int, sslEnabled: Boolean)

public external fun queryRef(dc: DataConnect, queryName: String, variables: JsAny?): QueryRef

public external fun mutationRef(dc: DataConnect, mutationName: String, variables: JsAny?): MutationRef

/** [options] is `{ fetchPolicy: "PREFER_CACHE" | "CACHE_ONLY" | "SERVER_ONLY" }`. */
public external fun executeQuery(queryRef: QueryRef, options: JsAny): Promise<OperationResult>

public external fun executeMutation(mutationRef: MutationRef): Promise<OperationResult>

public external fun makeMemoryCacheProvider(): JsAny

public external fun terminate(dataConnect: DataConnect): Promise<JsAny?>

public external fun setLogLevel(logLevel: String)
