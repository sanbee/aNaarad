/*
 * Copyright (C) 2026 aNaarad Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should obtain a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package naarad.client.tabhost

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//
// An interruptible sleep
//
// Why iSleep.sleep() Consumes 0% CPU
//
// 1. Kernel-Level Thread Parking (TIMED_WAITING): Under the hood,
//    latch.await(millisecond, TimeUnit.MILLISECONDS) uses native Linux
//    kernel thread synchronization (sys_futex). When sleep() is called,
//    the OS kernel immediately moves the thread out of the CPU runqueue
//    into a TIMED_WAITING state.
//
// 2. Zero Polling / Zero CPU Cycles: The thread does not spin, poll, or
//    execute any CPU instructions while waiting. It consumes 0 clock
//    cycles and 0% CPU.
//
// 3. Instant Kernel Signal: The thread remains completely idle until
//    either:
//
//    The OS timer interrupt fires when millisecond expires.
//
//    abort() calls latch.countDown(), which sends an immediate kernel
//    signal to wake the thread instantly.
//
class iSleep {
    @Volatile
    private var latch = CountDownLatch(1) // Start a latch for a single worker thread

    @Volatile
    var isAborted: Boolean = false
        private set

    /**
     * Sleeps for the specified [millisecond] duration unless [abort] is called.
     *
     * @param millisecond Duration to sleep in milliseconds.
     * @return `true` if slept the full duration normally, or `false` if interrupted by [abort].
     */
    fun sleep(millisecond: Long): Boolean {
        if (isAborted || millisecond <= 0) return false
        
        // Wait for countdown latch or until timeout expires
        val timedOut = !latch.await(millisecond, TimeUnit.MILLISECONDS)
        return timedOut && !isAborted
    }

    /**
     * Aborts any in-flight [sleep] call immediately, causing it to wake up instantly.
     */
    fun abort() {
        isAborted = true
        latch.countDown() // Instantly unblocks any thread waiting in sleep()
    }

    /**
     * Resets the [iSleep] instance so it can be reused for subsequent sleep cycles.
     */
    fun reset() {
        isAborted = false
        latch = CountDownLatch(1)
    }
}
