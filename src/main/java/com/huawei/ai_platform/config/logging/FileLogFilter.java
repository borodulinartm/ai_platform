package com.huawei.ai_platform.config.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * File logging rules:
 * - accept all INFO/WARN/ERROR logs
 * - accept DEBUG logs only for AI repo package
 * - deny all other events
 */
public class FileLogFilter extends Filter<ILoggingEvent> {
    private static final String DEBUG_PACKAGE = "com.huawei.ai_platform.rss.infrastructure.ai.repo";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.DENY;
        }

        if (event.getLevel().isGreaterOrEqual(Level.INFO)) {
            return FilterReply.ACCEPT;
        }

        if (event.getLevel() == Level.DEBUG && event.getLoggerName() != null
                && event.getLoggerName().startsWith(DEBUG_PACKAGE)) {
            return FilterReply.ACCEPT;
        }

        return FilterReply.DENY;
    }
}
