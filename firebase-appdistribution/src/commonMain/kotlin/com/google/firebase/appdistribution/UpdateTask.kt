/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import com.google.android.gms.tasks.Task

/**
 * An update of the app, as the Android SDK's `UpdateTask`: a [Task] that also reports the update's progress and state
 * changes to its [OnProgressListener]s. The `Task` members are inherited; instances come from [FirebaseAppDistribution].
 */
public expect abstract class UpdateTask() : Task<Nothing?> {
    /**
     * Adds a listener that is called with the progress while the update runs, and returns this task. On Apple platforms
     * the listener is never called: iOS downloads and installs a release itself, after the tester confirms.
     */
    public abstract fun addOnProgressListener(listener: OnProgressListener): UpdateTask
}

/** Receives the progress of an [UpdateTask]; the Android SDK's `OnProgressListener`. */
public fun interface OnProgressListener {
    /** Called periodically with the progress and state changes of the update. */
    public fun onProgressUpdate(updateState: UpdateProgress)
}

/** The progress, or final state, of an [UpdateTask]; the Android SDK's `UpdateProgress`. Common code cannot implement it. */
public interface UpdateProgress {
    /** The bytes of the APK downloaded so far, or -1 when updating to an AAB or when there is no new release. */
    public val apkBytesDownloaded: Long

    /** The size of the APK in bytes, or -1 when updating to an AAB or when there is no new release. */
    public val apkFileTotalBytes: Long

    /** The state of the update. */
    public val updateStatus: UpdateStatus
}

/** The state of an update, in an [UpdateProgress]; the Android SDK's `UpdateStatus`, in its order. */
public enum class UpdateStatus {
    /** The update is queued but not started. */
    PENDING,

    /** The new release is downloading. */
    DOWNLOADING,

    /** The new release has been downloaded. */
    DOWNLOADED,

    /** The new release failed to download. */
    DOWNLOAD_FAILED,

    /** The installation of the new release was canceled. */
    INSTALL_CANCELED,

    /** The installation of the new release failed. */
    INSTALL_FAILED,

    /** The tester was sent to the Play app to download an [BinaryType.AAB]. */
    REDIRECTED_TO_PLAY,

    /** The tester has the latest release they have access to. */
    NEW_RELEASE_NOT_AVAILABLE,

    /** The check for a new release failed. */
    NEW_RELEASE_CHECK_FAILED,

    /** The tester canceled the update. */
    UPDATE_CANCELED,
}

/**
 * How much the notification of [FirebaseAppDistribution.showFeedbackNotification] interrupts the tester; the Android
 * SDK's `InterruptionLevel`, which maps onto a notification channel importance from Android 8 and onto a notification
 * priority below it.
 */
public enum class InterruptionLevel {
    /** The minimum interruption level. */
    MIN,

    /** A low interruption level. */
    LOW,

    /** The default interruption level. */
    DEFAULT,

    /** A high interruption level. */
    HIGH,

    /** The maximum interruption level (a high channel importance from Android 8). */
    MAX,
}
