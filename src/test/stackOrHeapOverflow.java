package test;
/**
 * 
 * 
 * java.lang.RuntimeException: java.lang.OutOfMemoryError: Java heap space
	at burp.gze.T(Unknown Source)
	at burp.m9a.makeHttpRequest(Unknown Source)
	at burp.fc8.makeHttpRequest(Unknown Source)
	at burp.mj3.makeHttpRequest(Unknown Source)
	at burp.mj3.makeHttpRequest(Unknown Source)
	at title.TempLineEntry.doRequest(TempLineEntry.java:235)
	at title.TempLineEntry.doGetTitle(TempLineEntry.java:162)
	at title.TempLineEntry.getFinalLineEntry(TempLineEntry.java:56)
	at thread.Producer.run(Producer.java:74)
java.lang.RuntimeException: java.lang.OutOfMemoryError: Java heap space
	at burp.gze.T(Unknown Source)
	at burp.m9a.makeHttpRequest(Unknown Source)
	at burp.fc8.makeHttpRequest(Unknown Source)
	at burp.mj3.makeHttpRequest(Unknown Source)
	at burp.mj3.makeHttpRequest(Unknown Source)
	at title.TempLineEntry.doRequest(TempLineEntry.java:235)
	at title.TempLineEntry.doGetTitle(TempLineEntry.java:162)
	at title.TempLineEntry.getFinalLineEntry(TempLineEntry.java:56)
	at thread.Producer.run(Producer.java:74)
 *
 */
public class stackOrHeapOverflow {

}
