package org.totschnig.myexpenses.util.log;

import android.content.Context;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;
import timber.log.Timber;

//Credits: http://www.sureshjoshi.com/mobile/file-logging-in-android-with-timber/
public class TagFilterFileLoggingTree extends Timber.DebugTree {
  private static Logger mLogger = LoggerFactory.getLogger(TagFilterFileLoggingTree.class);
  private final String tag;

  public TagFilterFileLoggingTree(Context context, String tag) {
    this.tag = tag;
    final String logDirectory = context.getExternalFilesDir(null) + "/logs";
    configureLogger(logDirectory);
  }

  private void configureLogger(String logDirectory) {
    // reset the default context (which may already have been initialized)
    // since we want to reconfigure it
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.reset();

    RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
    rollingFileAppender.setContext(loggerContext);
    rollingFileAppender.setAppend(true);
    rollingFileAppender.setFile(logDirectory + "/" + tag + "-latest.log");

    SizeAndTimeBasedFNATP<ILoggingEvent> fileNamingPolicy = new SizeAndTimeBasedFNATP<>();
    fileNamingPolicy.setContext(loggerContext);
    fileNamingPolicy.setMaxFileSize("1MB");

    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
    rollingPolicy.setContext(loggerContext);
    rollingPolicy.setFileNamePattern(logDirectory + "/" + tag + ".%d{yyyy-MM-dd}.%i.log");
    rollingPolicy.setMaxHistory(5);
    rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fileNamingPolicy);
    rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
    rollingPolicy.start();

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setCharset(Charset.forName("UTF-8"));
    encoder.setPattern("%date %level [%thread] %msg%n");
    encoder.start();

    rollingFileAppender.setRollingPolicy(rollingPolicy);
    rollingFileAppender.setEncoder(encoder);
    rollingFileAppender.start();

    // add the newly created appenders to the root logger;
    // qualify Logger to disambiguate from org.slf4j.Logger
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.DEBUG);
    root.addAppender(rollingFileAppender);

    // print any status messages (warnings, etc) encountered in logback config
    StatusPrinter.print(loggerContext);
  }

  @Override
  protected boolean isLoggable(String tag, int priority) {
    return tag.equals(this.tag);
  }

  @Override
  protected void log(int priority, String tag, String message, Throwable t) {
    switch (priority) {
      case Log.VERBOSE:
        mLogger.trace(message);
      case Log.DEBUG:
        mLogger.debug(message);
        break;
      case Log.INFO:
        mLogger.info(message);
        break;
      case Log.WARN:
        mLogger.warn(message);
        break;
      case Log.ERROR:
        mLogger.error(message);
        break;
    }
  }
}