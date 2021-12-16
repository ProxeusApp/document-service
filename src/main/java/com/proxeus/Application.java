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
		}
		return config;
	}
}
