/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.concurrent

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * A mutually exclusive advisory file lock implementation.
 *
 * The lock is cooperative and only protects the critical region from other [FileLock] users in
 * other process with the same `filename`. Do not use this class on its own, instead use
 * [ExclusiveLock] which guarantees in-process locking too.
 *
 * @param filename The path to the file to protect. Note that an actual lock is not grab on the file
 * itself but on a temporary file create with the same path but ending with `.lck`.
 */
internal actual class FileLock actual constructor(filename: String) {
    private val lockFilename = "$filename.lck"
    private var lockChannel: FileChannel? = null

    actual fun lock() {
        if (lockChannel != null) {
            return
        }
        try {
            val lockFile = File(lockFilename)
            lockFile.parentFile?.mkdirs()
            lockChannel = FileOutputStream(lockFile).channel
            lockChannel?.lock()
        } catch (ex: Throwable) {
            lockChannel?.close()
            lockChannel = null
            throw IllegalStateException("Unable to lock file: '$lockFilename'.", ex)
        }
    }

    actual fun unlock() {
        val channel = lockChannel ?: return
        try {
            channel.close()
        } finally {
            lockChannel = null
        }
    }
}