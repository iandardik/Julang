package julay.regression

object RegressionTimeouts {
    const val CASE_MS = 30_000L

    fun remainingMs(deadlineMs: Long): Long =
        (deadlineMs - System.currentTimeMillis()).coerceAtLeast(0)

    fun requireRemaining(caseId: String, deadlineMs: Long) {
        check(remainingMs(deadlineMs) > 0) {
            "Regression case \"$caseId\" exceeded the ${CASE_MS}ms case timeout"
        }
    }

    fun capMs(requestedMs: Long, deadlineMs: Long): Long =
        minOf(requestedMs, remainingMs(deadlineMs)).coerceAtLeast(1)
}
