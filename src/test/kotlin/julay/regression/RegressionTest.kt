package julay.regression

import kotlin.test.Test
import java.io.File

class RegressionTest {
    @Test
    fun regressionCases() {
        val projectRoot = File(System.getProperty("user.dir"))
        RegressionRunner.runAll(projectRoot)
    }
}
