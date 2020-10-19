package org.totschnig.myexpenses.util.log;

import android.content.Context;
import android.util.Log;

import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import androidx.annotation.NonNull;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import timber.log.Timber;

//Credits: http://www.sureshjoshi.com/mobile/file-logging-in-android-with-timber/
public class TagFilterFileLoggingTree extends Timber.DebugTree {
  private final Logger logger;
  private final String tag;

  public TagFilterFileLoggingTree(Context context, String tag) {
    this.tag = tag;
    this.logger = (Logger) LoggerFactory.getLogger(tag);
    final String logDirectory = context.getExternalFilesDir(null) + "/logs";
    configureLogger(logDirectory);
  }

  private void configureLogger(String logDirectory) {

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
    rollingFileAppender.setContext(loggerContext);
    rollingFileAppender.setAppend(true);
    rollingFileAppender.setFile(logDirectory + "/" + tag + "-latest.log");

    SizeAndTimeBasedFNATP<ILoggingEvent> fileNamingPolicy = new SizeAndTimeBasedFNATP<>();
    fileNamingPolicy.setContext(loggerContext);
    fileNamingPolicy.setMaxFileSize(FileSize.valueOf("1MB"));

    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
    rollingPolicy.setContext(loggerContext);
    rollingPolicy.setFileNamePattern(logDirectory + "/" + tag + ".%d{yyyy-MM-dd}.%i.log");
    rollingPolicy.setMaxHistory(5);
    rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fileNamingPolicy);
    rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
    rollingPolicy.start();

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    //noinspection CharsetObjectCanBeUsed
    encoder.setCharset(Charset.forName("UTF-8"));
    encoder.setPattern("%date %level [%thread] %msg%n");
    encoder.start();

    rollingFileAppender.setRollingPolicy(rollingPolicy);
    rollingFileAppender.setEncoder(encoder);
    rollingFileAppender.start();

    AsyncAppender asyncAppender = new AsyncAppender();
    asyncAppender.setContext(loggerContext);
    asyncAppender.addAppender(rollingFileAppender);
    asyncAppender.start();
    
    logger.setLevel(Level.DEBUG);
    logger.addAppender(asyncAppender);

    // print any status messages (warnings, etc) encountered in logback config
    StatusPrinter.print(loggerContext);
  }

  @Override
  protected boolean isLoggable(String tag, int priority) {
    return this.tag.equals(tag);
  }

  @Override
  protected void log(int priority, String tag, @NonNull String message, Throwable t) {
    switch (priority) {
      case Log.VERBOSE:
        logger.trace(message);
        break;
      case Log.DEBUG:
        logger.debug(message);
        break;
      case Log.INFO:
        logger.info(message);
        break;
      case Log.WARN:
        logger.warn(message);
        break;
      case Log.ERROR:
        logger.error(message);
        break;
    }
  }
}