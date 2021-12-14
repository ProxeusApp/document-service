package com.proxeus;

import com.proxeus.util.Json;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.*;
import spark.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Application is the config and log init helper.
 */
public class Application {
	public static Config config;

	@SuppressWarnings("unchecked")
	public static Config init() throws Exception{
		if(config ==null){
			File file = new File("./conf.json");
			if(!file.exists()){
				FileUtils.copyToFile(Application.class.getClassLoader().getResourceAsStream("conf.json"), file);
			}
			config = Config.create(Json.fromJson(IOUtils.toString(new FileInputStream(file)), Map.class));
			try{
				// TODO: https://logging.apache.org/log4j/2.x/manual/customconfig.html#ConfigurationBuilder
				// initLog4j(Config.by(LogConfig.class));
			}catch (Exception e){
                e.printStackTrace();
			}
		}
		return config;
	}

	/*
	private static void initLog4j(LogConfig config){
		ConsoleAppender ca = new ConsoleAppender(); //create appender
		ca.setLayout(new PatternLayout(config.pattern));
		ca.setThreshold(Level.toLevel(config.level_console));
		ca.activateOptions();

		RollingFileAppender fa = new RollingFileAppender();
		fa.setName("FileLogger");
		fa.setFile((config.filePath));
		fa.setLayout(new PatternLayout(config.pattern));
		fa.setThreshold(Level.toLevel(config.level));
		fa.setAppend(true);
		fa.setMaxBackupIndex(config.maxBackupIndex);
		fa.setMaxFileSize(config.maxFileSize);
		fa.activateOptions();

		Logger.getRootLogger().getLoggerRepository().resetConfiguration();

		new DefaultOutAndErrToLog4jRedirector();
		Logger.getRootLogger().addAppender(fa);
		Logger.getRootLogger().addAppender(ca);
	}
	*/


}
