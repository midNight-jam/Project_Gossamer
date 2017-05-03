package gash.router.server;

import java.util.HashMap;
import java.util.Random;

public class ServerUtils {
	
	static HashMap<String, Long> timeOutMap = new HashMap<>();
/**
	public static long getCurrentUnixTimeStamp() {
		long unixTime = System.currentTimeMillis() / 1000L;
		return unixTime;
	}
	public static long getElectionTimeout() {
		Random random = new Random();
		long variableTimeout = random.nextInt((int) ((200000L - 1000L) + 1L));
		long currentTimeout = 30000L + (long) variableTimeout;
		//return currentTimeout;
		return 10000L;
	}
	int randomWithRange(int min, int max) {
		int range = (max - min) + 1;
		return (int) (Math.random() * range) + min;
	}
	public static long getFixedTimeout() {
		return 10000L;
	}
	
	
	**/
	
	public static long getCurrentUnixTimeStamp() {
		long unixTime = System.currentTimeMillis() / 1000L;
		return unixTime;

	}

	public static long getElectionTimeout() {
		Random random = new Random();
		long variableTimeout = random.nextInt((int) ((20000L - 1000L) + 1L));
		long currentTimeout = 30000L + (long) variableTimeout;
		return currentTimeout;
		//return 30000L;

	}

	int randomWithRange(int min, int max) {
		int range = (max - min) + 1;
		return (int) (Math.random() * range) + min;
	}

	public static long getFixedTimeout() {

		return 1000L;

	}
	
	public static long getFileReceiveTimeout(){
		
		return 100000L;
	}
	
	public static void setRequestTimeout(String fileName, long milliseconds){
		timeOutMap.put(fileName, milliseconds);
	}
	
	public static long getRequestTimeout(String fileName){
		return timeOutMap.get(fileName);
	}
	
}








