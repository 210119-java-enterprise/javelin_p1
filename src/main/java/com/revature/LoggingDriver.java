package com.revature;

// Import log4j classes.
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;




public class LoggingDriver {
    private static final Logger logger = LogManager.getLogger(LoggingDriver.class);

    public static void main(String[] args) {
        logger.trace("Starting application");
        if (!doIt()) {
            logger.error("Didn't do it");
        }
        logger.trace("Exiting application");
    }

    private static boolean doIt() {
        logger.traceEntry();
        logger.error("Did it again!");
        return logger.traceExit(false);
    }
}
