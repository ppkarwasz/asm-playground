package com.vlkan;

import org.apache.logging.log4j.Logger;

public class LogBuilderUtil {

    public static void debug(Logger logger, String message, StackTraceElement location) {
        logger.atDebug()
                .withLocation(location)
                .log(message);
    }
}
