package burp;

/*
 * @(#)IMessageEditor.java
 *
 * Copyright PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Free Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */
import java.awt.Component;

/**
 * This interface is used to provide extensions with an instance of Burp's HTTP
 * message editor, for the extension to use in its own UI. Extensions should
 * call
 * <code>IBurpExtenderCallbacks.createMessageEditor()</code> to obtain an
 * instance of this interface.
 */
public interface IMessageEditor
{
    /**
     * This method returns the UI component of the editor, for extensions to add
     * to their own UI.
     *
     * @return The UI component of the editor.
     */
    Component getComponent();

    /**
     * This method is used to display an HTTP message in the editor.
     *
     * @param message The HTTP message to be displayed.
     * @param isRequest Flags whether the message is an HTTP request or
     * response.
     */
    void setMessage(byte[] message, boolean isRequest);

    /**
     * This method is used to retrieve the currently displayed message, which
     * may have been modified by the user.
     *
     * @return The currently displayed HTTP message.
     */
    byte[] getMessage();

    /**
     * This method is used to determine whether the current message has been
     * modified by the user.
     *
     * @return An indication of whether the current message has been modified by
     * the user since it was first displayed.
     */
    boolean isMessageModified();

    /**
     * This method returns the data that is currently selected by the user.
     *
     * @return The data that is currently selected by the user, or
     * <code>null</code> if no selection is made.
     */
    byte[] getSelectedData();
}
