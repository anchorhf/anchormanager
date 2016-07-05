package com.anke.util;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoggerUtil {

	private static LoggerUtil instance;

	private Logger logger;

	private LoggerUtil(Class<?> clz) {
		Properties props = new Properties();
		InputStream fis = this.getClass().getResourceAsStream("/log4j.properties");
		try {
			props.load(fis);
			fis.close();
			String logFile = this.getClass().getResource("/").getPath() + props.getProperty("log4j.appender.R.File");
			props.setProperty("log4j.appender.R.File", logFile);
			PropertyConfigurator.configure(props);
		} catch (Exception e) {
		}
		logger = Logger.getLogger(clz);
	}

	public Logger getLogger() {
		return logger;
	}

	public static Logger getInstance(Class<?> clz) {
		if (instance == null)
			instance = new LoggerUtil(clz);
		else
			instance.logger = Logger.getLogger(clz);
		return instance.logger;
	}

}
