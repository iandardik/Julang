package julay.concurrency

import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal suspend fun awaitParticipantCount(
    chan: SyncChannel<*, *>,
    expected: Int,
    timeout: Duration = 5.seconds,
) {
    withTimeout(timeout) {
        while (chan.participantCountForTests() != expected) {
            yield()
        }
    }
}

internal suspend fun awaitExactlyOneActive(
    jobs: List<Job>,
    chan: SyncChannel<*, *>,
    timeout: Duration = 5.seconds,
) {
    withTimeout(timeout) {
        while (true) {
            val active = jobs.count { it.isActive }
            val waiting = chan.participantCountForTests()
            if (active == 1 && waiting == 1) break
            yield()
        }
    }
}
