package com.proxeus;

import com.proxeus.util.Json;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server config
 */
public class Config {
	public String protocol = "http";
	public String host = "localhost";
	public Integer port = 2115;
	public String tmpFolder = "docsCache";
	public String timeout = "10s";

	//workers
	public int min = 8;
	public int max = 100;
	public int timeoutMillis = 100000;

	private static Map<String, Object> config;

	public Config(){}

	public static Config create(Map<String, Object> params){
		config = params;
		Config conf = root(Config.class);
		System.setProperty("document.template.cache", conf.tmpFolder);
		conf.timeoutMillis = (int) parseDurationToMillis(conf.timeout);
		return conf;
	}

	private final static Pattern durReg = Pattern.compile("\\s*(\\d+)\\s*(\\w+)\\s*");
	public static long parseDurationToMillis(String duration) {
		if(duration == null || duration.isEmpty()){
			return 0;
		}
		duration = duration.trim();
		Matcher m = durReg.matcher(duration);
		if(m.find()){
			int dur = Integer.valueOf(m.group(1));
			String unit = m.group(2).trim();
			if(unit.matches("ms|millisecond|milliseconds")){
				return TimeUnit.MILLISECONDS.toMillis(dur);
			}else if(unit.matches("s|second|seconds")){
				return TimeUnit.SECONDS.toMillis(dur);
			}else if(unit.matches("m|minute|minutes")){
				return TimeUnit.MINUTES.toMillis(dur);
			}else if(unit.matches("h|hour|hours")){
				return TimeUnit.HOURS.toMillis(dur);
			}else if(unit.matches("d|day|days")){
				return TimeUnit.DAYS.toMillis(dur);
			}
		}
		return 0;
	}

	public String getProtocol() {
		return protocol;
	}
	public String getHost() {
		return host;
	}
	public Integer getPort() {
		return port;
	}
	public String getTmpFolder(){
		return tmpFolder;
	}

	public String toString(){
		return Json.toJson(config);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getConfigOf(String module){
		try{
			return (Map<String, Object>)config.get(module);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T fromJson(byte[] json, Class<T> clazz){
		try {

		}  catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static  <T> T by(Class<T> clazz){
		T o = map(config, clazz);
		if(o == null){
			try{
				o = clazz.newInstance();
			}catch (Exception e){
				//no default constructor
			}
		}
		return o;
	}

	public static  <T> T root(Class<T> clazz){
		T o = Json.fromJson(Json.toJson(config), clazz);
		if(o == null){
			try{
				o = clazz.newInstance();
			}catch (Exception e){
				//no default constructor
			}
		}
		return o;
	}

	private static <T> T map(Map data, Class<T> clazz){
		if(data == null){
			return null;
		}
		String name = clazz.getSimpleName().toLowerCase();
		for (Object key : data.keySet()) {
			Object o = data.get(key);
			if(key.toString().toLowerCase().equals(name)){
				return Json.fromJson(Json.toJson(o), clazz);
			}
			if(o instanceof Map){
				T oo = map((Map)o, clazz);
				if(oo!=null){
					return oo;
				}
			}
		}
		return null;
	}
}