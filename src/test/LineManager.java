package test;

import java.util.*;
import java.util.concurrent.ExecutorService;

import burp.LineConfig;
import burp.LineEntry;

public class LineManager{
    private final LineConfig config;
    private ArrayList<LineEntry> lineEntries;
    private ArrayList<LineEntryListener> lineEntryListeners;
    private int totalRequests = 0;
    
    LineManager(LineConfig config){
        this.config = config;
        lineEntries = new ArrayList<>();
        lineEntryListeners = new ArrayList<>();
    }

    private void addNewRequest(LineEntry lineEntry){
        //After handling request / response lineEntries generation.
        //Add to grepTable / modify existing entry.
        synchronized (lineEntries) {
            while(lineEntries.size() >= getMaximumEntries()){
                final LineEntry removed = lineEntries.remove(0);
                for (LineEntryListener listener : lineEntryListeners) {
                    listener.onRequestRemoved(0, removed);
                }
            }
            lineEntries.add(lineEntry);
            for (LineEntryListener listener : lineEntryListeners) {
                listener.onRequestAdded(lineEntry);
            }
            totalRequests++;
        }
    }

    public ArrayList<LineEntry> getLineEntries() {
        return lineEntries;
    }

    public void reset() {
        this.lineEntries.clear();
        this.totalRequests = 0;
    }

    public void addLogListener(LineEntryListener listener) {
        lineEntryListeners.add(listener);
    }
    
    public void removeLogListener(LineEntryListener listener) {
        lineEntryListeners.remove(listener);
    }
    
    public ArrayList<LineEntryListener> getLineEntryListeners() {
        return lineEntryListeners;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getMaximumEntries() {
        return config.getMaximumEntries();
    }
}
