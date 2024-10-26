package InternetSearch.Client;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.bit4woo.utilbox.utils.UrlUtils;

import InternetSearch.HttpClientOfBurp;
import InternetSearch.SearchEngine;
import InternetSearch.SearchResultEntry;
import burp.BurpExtender;

/**
 * 什么时候使用Interface，什么时候使用abstract class?
 * <p>
 * 当所有函数都需要自行实现时，使用Interface
 * 当部分函数的逻辑是确定的，有共享的函数、属性时，使用抽象类。
 */
public abstract class BaseClient {

    protected PrintWriter stdout;
    protected PrintWriter stderr;
    private String url;
    private byte[] raw;
    private String resp_body;

    BaseClient() {
        try {
            stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
            stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
        } catch (Exception e) {
            stdout = new PrintWriter(System.out, true);
            stderr = new PrintWriter(System.out, true);
        }
    }

    public abstract String getEngineName();

    /**
     * 返回body 的list
     *
     * @param searchContent
     * @return
     */
    public List<String> Search(String searchContent, String searchType) {
        List<String> resp_bodies = new ArrayList<>();
        int page = 1;
        while (true) {
            try {
                String searchDork = buildSearchDork(searchContent, searchType);
                this.url = buildSearchUrl(searchDork, page);
                this.raw = buildRawData(searchDork, page);
                if (UrlUtils.isVaildUrl(url)) {
                    String body = HttpClientOfBurp.doRequest(new URL(url), raw);
                    this.resp_body = body;
                    if (body.length() <= 0) {
                        break;
                    } else {
                        resp_bodies.add(body);
                        if (hasNextPage(body, page)) {
                            page++;
                            continue;
                        } else {
                            break;
                        }
                    }
                } else {
                    stderr.println(this.getClass() + " invalid URL to search: " + url);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace(stderr);
                break;
            }
        }
        return resp_bodies;
    }

    @Deprecated
    public void printDebugInfoxx(String content, String tips) {
        content = content + "";//可以将null转化为字符串
        tips = tips + "";

        if (content.length() > 200) {
            content = content.substring(0, 200);
        }
        stderr.println("==========" + tips + "==========");
        stderr.println(content);
        stderr.println("==========" + tips + "==========");
    }

    public void printDebugInfo() {
        String content;

        if (StringUtils.length(resp_body) > 200) {
            //可以处理是null的情况
            content = resp_body.substring(0, 200);
        } else {
            content = resp_body;
        }

        String reqRaw;
        if (this.raw == null) {
            reqRaw = this.raw + "";
        } else {
            reqRaw = new String(this.raw);
        }
        
        stderr.println();
        stderr.println("====================>>");
        stderr.println("URL:");
        stderr.println(this.url);
        stderr.println("Request:");
        stderr.println(reqRaw);
        stderr.println("Response Body:");
        stderr.println(content);
        stderr.println("<<====================");
        stderr.println();
    }

    public List<SearchResultEntry> SearchToGetEntry(String searchContent, String searchType) {
        List<SearchResultEntry> result = new ArrayList<>();
        try {
            List<String> resp_bodies = Search(searchContent, searchType);
            for (String body : resp_bodies) {
                List<SearchResultEntry> tmp_result = parseResp(body);
                if (tmp_result != null && tmp_result.size() > 0) {
                    result.addAll(tmp_result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(stderr);
        }
        return result;
    }

    protected abstract List<SearchResultEntry> parseResp(String respbody);

    protected abstract boolean hasNextPage(String respbody, int currentPage);

    protected String buildSearchDork(String searchContent, String searchType) {
        return SearchEngine.buildSearchDork(searchContent, getEngineName(), searchType);
    }

    protected abstract String buildSearchUrl(String searchDork, int page);

    protected abstract byte[] buildRawData(String searchDork, int page);
}
