package com.android.skip.data

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Compatibility tombstone for periodic jobs persisted before updates were removed. */
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
