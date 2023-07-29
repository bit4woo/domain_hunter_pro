package utils;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class DomainToURLs {
	String host;
	int port;

	/**
	 * 构造函数，输入解析
	 * @param inputHost 如果想要指定自定义端口，传入类似baidu.com:8888的形式即可
	 * @return
	 */
	public DomainToURLs(String inputHost){
        try {
            inputHost = inputHost.trim();

            if (inputHost.contains(":")) {//处理带有端口号的域名
                String tmpport = inputHost.substring(inputHost.indexOf(":") + 1);
                port = Integer.parseInt(tmpport);
                String tmphost = inputHost.substring(0, inputHost.indexOf(":"));
                if (IPAddressUtils.isValidIP(tmphost) || DomainNameUtils.isValidDomain(tmphost)) {
                	this.host = tmphost;
                }
            }else {
            	if (IPAddressUtils.isValidIP(inputHost) || DomainNameUtils.isValidDomain(inputHost)) {
	                host = inputHost;
	                port = -1;
            	}
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    //将域名或IP拼接成URL
    public Set<URL> getUrls(){
    	Set<URL> result = new HashSet<URL>();
    	if (host == null) return result;
        try{
        	result.add(new URL(String.format("http://%s:%s/",host,80)));
        	result.add(new URL(String.format("https://%s:%s/",host,443)));

            if (port == -1 || port == 80 || port == 443){
                //Nothing to do;
            }else{
            	result.add(new URL(String.format("http://%s:%s/",host,port)));
                
            	result.add(new URL(String.format("https://%s:%s/",host,port)));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
