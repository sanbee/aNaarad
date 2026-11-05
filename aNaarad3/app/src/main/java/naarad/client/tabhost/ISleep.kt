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
