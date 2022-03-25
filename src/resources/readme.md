遇到的问题：

单纯使用 com.google.common.net.InternetDomainName当中的方法来获取域名的公共后缀，还是有很多误报。因为公共后缀的列表实在是太大了，https://publicsuffix.org/list/public_suffix_list.dat 用的就是这个列表中的数据。



我们必须在误报和漏报直接取得平衡。目前常见的公共域名后缀（顶级域名）梳理如下。符合如下情况的自动添加，其他的还是需要使用者自行添加。



## 所有的顶级域名列表

A list of all valid top-level domains is maintained by the IANA and is updated from time to time.

https://data.iana.org/TLD/tlds-alpha-by-domain.txt

https://www.iana.org/domains/root/db

## 顶级域名分类

所有 op-level domains 还可以分为

### 1.Country code top-level domains 国家代码顶级域名

#### 1.1 Latin Character ccTLDs 拉丁字母国家顶级域名

https://en.wikipedia.org/wiki/Country_code_top-level_domain#Internationalized_ccTLDs

#### 1.2 Internationalized ccTLDs 国际化顶级域名

https://en.wikipedia.org/wiki/Country_code_top-level_domain#Internationalized_ccTLDs

#### 1.3 Generic ccTLDs 通用国家顶级域名

https://en.wikipedia.org/wiki/Country_code_top-level_domain#Generic_ccTLDs

这部分域名是不受限制的国家域名，不限制国家，就像一般的com、org一样的顶级域名。

比如 常见的有 `hourshopeekredit.co.id help.seatalk.in.th`等

### 2.Original top-level domains 原始顶级域名

https://en.wikipedia.org/wiki/List_of_Internet_top-level_domains#Original_top-level_domains



## 常用顶级域名列表

原始顶级域名列表

```
.com
.org
.net
.int
.edu
.gov
.mil
```

通用国家顶级域名（拉丁字母国家顶级域名 已经包含这一部分了！）

```
.ad
.as
.az
.bz
.cc
.cd
.co
.dj
.fm
.gg
.io
.la
.me
.ms
.nu
.sc
.tf
.tv
.ws
```

拉丁字母国家顶级域名

```
.ac
.ad
.ae
.af
.ag
.ai
.al
.am
.ao
.aq
.ar
.as
.at
.au
.aw
.ax
.az
.ba
.bb
.bd
.be
.bf
.bg
.bh
.bi
.bj
.bm
.bn
.bo
.bq
.br
.bs
.bt
.bw
.by
.bz
.ca
.cc
.cd
.cf
.cg
.ch
.ci
.ck
.cl
.cm
.cn
.co
.cr
.cu
.cv
.cw
.cx
.cy
.cz
.de
.dj
.dk
.dm
.do
.dz
.ec
.ee
.eg
.eh
.er
.es
.et
.eu
.fi
.fj
.fk
.fm
.fo
.fr
.ga
.gd
.ge
.gf
.gg
.gh
.gi
.gl
.gm
.gn
.gp
.gq
.gr
.gs
.gt
.gu
.gw
.gy
.hk
.hm
.hn
.hr
.ht
.hu
.id
.ie
.il
.im
.in
.io
.iq
.ir
.is
.it
.je
.jm
.jo
.jp
.ke
.kg
.kh
.ki
.km
.kn
.kp
.kr
.kw
.ky
.kz
.la
.lb
.lc
.li
.lk
.lr
.ls
.lt
.lu
.lv
.ly
.ma
.mc
.md
.me
.mg
.mh
.mk
.ml
.mm
.mn
.mo
.mp
.mq
.mr
.ms
.mt
.mu
.mv
.mw
.mx
.my
.mz
.na
.nc
.ne
.nf
.ng
.ni
.nl
.no
.np
.nr
.nu
.nz
.om
.pa
.pe
.pf
.pg
.ph
.pk
.pl
.pm
.pn
.pr
.ps
.pt
.pw
.py
.qa
.re
.ro
.rs
.ru
.rw
.sa
.sb
.sc
.sd
.se
.sg
.sh
.si
.sk
.sl
.sm
.sn
.so
.sr
.ss
.st
.su
.sv
.sx
.sy
.sz
.tc
.td
.tf
.tg
.th
.tj
.tk
.tl
.tm
.tn
.to
.tr
.tt
.tv
.tw
.tz
.ua
.ug
.uk
.us
.uy
.uz
.va
.vc
.ve
.vg
.vi
.vn
.vu
.wf
.ws
.ye
.yt
.za
.zm
.zw
```



其他常用域名后缀，以上不包含的！

```
co.id
co.th
in.th
com.my
com.mx
com.mm
com.br
```

