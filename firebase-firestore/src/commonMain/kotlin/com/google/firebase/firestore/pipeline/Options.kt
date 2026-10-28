/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore.pipeline

import kotlin.jvm.JvmField

/**
 * The options of a stage, of a source or of an execution, by name, as the Android SDK sends them to the backend. A value
 * is a [String], [Boolean], [Long], [Double], a [Field] (a field reference), a [List] of strings, or nested [InternalOptions].
 */
internal class InternalOptions private constructor(internal val entries: Map<String, Any>) {
    fun with(key: String, value: Any): InternalOptions = InternalOptions(entries + (key to value))

    fun without(key: String): InternalOptions = InternalOptions(entries - key)

    fun adding(newOptions: InternalOptions): InternalOptions {
        val duplicate = newOptions.entries.keys.firstOrNull { it in entries }
        require(duplicate == null) { "Multiple entries with same key: $duplicate" }
        return InternalOptions(entries + newOptions.entries)
    }

    fun isEmpty(): Boolean = entries.isEmpty()

    override fun equals(other: Any?): Boolean = this === other || (other is InternalOptions && entries == other.entries)

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = entries.toString()

    companion object {
        val EMPTY: InternalOptions = InternalOptions(emptyMap())
    }
}

/**
 * The base of the option builders of the pipeline API (stage options, hints and [execute options][com.google.firebase.firestore.Pipeline.ExecuteOptions]):
 * immutable, each `with` returns a copy with the option set. Besides the typed options of each builder, [with] sets an
 * option by name, as the backend knows it.
 */
public abstract class AbstractOptions<T : AbstractOptions<T>> internal constructor(internal val options: InternalOptions) {
    internal abstract fun self(options: InternalOptions): T

    internal fun with(key: String, value: InternalOptions): T = self(options.with(key, value))

    /** A copy with the option [key] set to the array [values]. */
    protected fun with(key: String, vararg values: String): T = self(options.with(key, values.toList()))

    /** A copy with the option [key] set to the options of [subSection]. */
    protected fun with(key: String, subSection: AbstractOptions<*>): T = self(options.with(key, subSection.options))

    /** A copy with the options of [newOptions] added; they must not set an option already set. */
    protected fun adding(newOptions: AbstractOptions<*>): T = self(options.adding(newOptions.options))

    /** A copy with the option [key] set to [value]. */
    public fun with(key: String, value: String): T = self(options.with(key, value))

    /** A copy with the option [key] set to [value]. */
    public fun with(key: String, value: Boolean): T = self(options.with(key, value))

    /** A copy with the option [key] set to [value]. */
    public fun with(key: String, value: Long): T = self(options.with(key, value))

    /** A copy with the option [key] set to [value]. */
    public fun with(key: String, value: Double): T = self(options.with(key, value))

    /**
     * A copy with the option [key] set to a reference to the field [value]. The JS SDK sends a field reference only as a
     * nearest-neighbour search's distance field or an unnest's index field; on JS and wasmJs any other fails the execution.
     */
    public fun with(key: String, value: Field): T = self(options.with(key, value))

    /** A copy with the option [key] set to the options [value]. */
    public fun with(key: String, value: RawOptions): T = self(options.with(key, value.options))
}

/** Options by name, for an option of [AbstractOptions.with] that takes nested options; start from [DEFAULT]. */
public class RawOptions private constructor(options: InternalOptions) : AbstractOptions<RawOptions>(options) {
    override fun self(options: InternalOptions): RawOptions = RawOptions(options)

    public companion object {
        /** No options. */
        @JvmField
        public val DEFAULT: RawOptions = RawOptions(InternalOptions.EMPTY)
    }
}
