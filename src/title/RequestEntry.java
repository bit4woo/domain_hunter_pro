package title;

import burp.*;
import java.net.URL;

public class RequestEntry
{
    final int tool;
    final IHttpRequestResponsePersisted requestResponse;
    final URL url;

    RequestEntry(int tool, IHttpRequestResponsePersisted requestResponse, URL url)
    {
        this.tool = tool;
        this.requestResponse = requestResponse;
        this.url = url;
    }
}