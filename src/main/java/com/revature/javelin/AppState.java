package com.revature.javelin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class AppState {
    protected static final Logger logger = LogManager.getLogger(AppState.class);

    
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