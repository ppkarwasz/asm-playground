package com.vlkan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppActual {

    private static final Logger logger = LogManager.getLogger();

    private static final StackTraceElement[] locations = {
            new StackTraceElement("lala", "bala", "cala", 123),
            new StackTraceElement("momo", "bomo", "como", 145)
    };

    public static void main(String[] args) {
        System.out.println("should log at line 12");
        logger.debug("should log at line 12");
        System.out.println("nothing to see here");
        System.out.println("should log at line 15");
        logger.debug("should log at line 15");
        f();
    }

    private static void f() {
        System.out.println("adding some indirection");
        System.out.println("should log at line 21");
        logger.debug("should log at line 21");
    }

}
