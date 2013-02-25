package org.popcorn.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class Test {
	static {
		StatusLogger test = StatusLogger.getLogger();
		test.setLevel(Level.ALL);
	}
	
	private static Logger logger = LogManager.getLogger(Test.class);

	public static void main(String[] args) {
		logger.debug("Sample debug message");
		logger.error("Sample error message");
//		try {
//			int a = 0/0;
//		} catch (Exception e) {
//			logger.error("error", e);
//		}
	}
}
