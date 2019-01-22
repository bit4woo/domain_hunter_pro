package test;

import burp.LineEntry;

/**
 * Created by corey on 21/08/17.
 */
public interface LineEntryListener {
    void onRequestAdded(LineEntry logEntry);
    void onRequestRemoved(int index, final LineEntry logEntry);
}
