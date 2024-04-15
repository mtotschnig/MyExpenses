package org.totschnig.myexpenses.util.log

import android.content.Context
import android.util.Log
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory
import timber.log.Timber
import java.nio.charset.StandardCharsets

//Credits: http://www.sureshjoshi.com/mobile/file-logging-in-android-with-timber/
class TagFilterFileLoggingTree(context: Context, private val tag: String) : Timber.DebugTree() {
    private val logger: Logger = LoggerFactory.getLogger(tag) as Logger

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return this.tag == tag
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.VERBOSE -> logger.trace(message)
            Log.DEBUG -> logger.debug(message)
            Log.INFO -> logger.info(message)
            Log.WARN -> logger.warn(message)
            Log.ERROR -> logger.error(message)
        }
    }

    init {
        val logDirectory = context.getExternalFilesDir(null).toString() + "/logs"
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        logger.level = Level.DEBUG
        logger.addAppender(AsyncAppender().apply {
            this.context = loggerContext
            addAppender(RollingFileAppender<ILoggingEvent>().apply outer@{
                this.context = loggerContext
                isAppend = true
                file = "${logDirectory}/$tag-latest.log"
                rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
                    this.context = loggerContext
                    fileNamePattern = "${logDirectory}/$tag.%d{yyyy-MM-dd}.%i.log"
                    maxHistory = 5
                    timeBasedFileNamingAndTriggeringPolicy =
                        SizeAndTimeBasedFNATP<ILoggingEvent>().apply {
                            this.context = loggerContext
                            setMaxFileSize(FileSize.valueOf("1MB"))
                        }
                    setParent(this@outer) // parent and context required!
                    start()
                }
                encoder = PatternLayoutEncoder().apply {
                    this.context = loggerContext
                    charset = StandardCharsets.UTF_8
                    pattern = "%date %level [%thread] %msg%n"
                    start()
                }
                start()
            })
            start()
        })
        // print any status messages (warnings, etc) encountered in logback config
        //StatusPrinter.print(loggerContext)
    }
}