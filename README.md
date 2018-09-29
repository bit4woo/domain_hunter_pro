**author**

[bit4](https://github.com/bit4woo)

**domain_hunter**

A Burp Suite extender that search *<u>**sub domains,similar domains and related domains**</u>* from sitemap. Some times similar domain and related domains give you surprise^_^. that's why I care about it.

**usage**

1. download this burp extender from [here](https://github.com/bit4woo/domain_hunter/releases).
2. add it to burp suite. you will see a new tab named “Domain Hunter”, if  no error encountered. 
3. visit your target website(or App) with burp proxy enabled, ensure burp recorded http or https traffic of your target.
4. you can just switch to the "domain hunter" tab, input the domain that you want to search and click "search" button.
5. or you can  run "spider all" first to try to find more subdomains and similiar domains. 

**screenshot**

![screenshot](doc/domain-hunter-v0.3.png)

![domain-hunter-v1.1](doc/domain-hunter-v1.1.png)

**change log**

2017-07-28: Add a function to crawl all known subdomains; fix some bug.

2018-07-06: Add the ability to get related domains by get SANs object of certification.  

2018-08-03: Use thread to speed up get related-domains.

2018-09-18: Optimize some steps to reduce memory usage.

2018-09-19: Update getSANs() method to void get domains of CDN provider.

2018-09-20: Update logic of getting possible https URLs that may contain related-domains

2018-09-21: Update logic of "includeInScope" and "sendToSpider" to reduces UI action time

2018-09-29: Add Upload function to support  upload result to your site or system

**xmind of domain collection**

![xmind](doc/xmind.png)

**Burp插件微信交流群**：

![wechat_group](doc/wechat_group.jpg)