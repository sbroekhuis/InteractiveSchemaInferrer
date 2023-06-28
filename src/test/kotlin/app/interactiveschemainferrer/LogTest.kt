package app.interactiveschemainferrer

import java.io.IOException
import java.util.logging.Logger
import kotlin.test.Test


class LogTest {
    private val logger: Logger = Logger.getLogger(LogTest::class.java.canonicalName)

    @Test
    @Throws(IOException::class)
    fun testLogs() {
        logger.finer("BEFORE")
        logger.fine("BEFORE")
        logger.info("BEFORE")
        logger.warning("BEFORE")
        logger.severe("BEFORE")
//        val `is` = LogTest::class.java.getResourceAsStream("/logging.properties")
//        LogManager.getLogManager().readConfiguration(`is`)
        logger.finer("AFTER")
        logger.fine("AFTER")
        logger.info("AFTER")
        logger.warning("AFTER")
        logger.severe("AFTER")
    }

}
