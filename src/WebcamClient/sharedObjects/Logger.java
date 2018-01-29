package sharedObjects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	public Logger() {
		
	}
	
	public synchronized void logLn(String txt) {
		String s = "[" + LocalDateTime.now().format(formatter) + "] " + txt + "\n";
		System.out.print(s);
	}
	
	public synchronized void logException(Exception e) {
		String trace = "[" + LocalDateTime.now().format(formatter) + "] Exception: " + e + "\n";
		for (StackTraceElement e1 : e.getStackTrace()) {
			trace += "\tat " + e1.toString() + "\n";
		}
		System.out.print(trace);
	}
	
	public synchronized void logError(Error e) {
		String trace = "[" + LocalDateTime.now().format(formatter) + "] Error: " + e + "\n";
		for (StackTraceElement e1 : e.getStackTrace()) {
			trace += "\tat " + e1.toString() + "\n";
		}
		System.out.print(trace);
	}
	
	public synchronized void logThrowable(Throwable e) {
		String trace = "[" + LocalDateTime.now().format(formatter) + "] Throwable: " + e + "\n";
		for (StackTraceElement e1 : e.getStackTrace()) {
			trace += "\tat " + e1.toString() + "\n";
		}
		System.out.print(trace);
	}
}
