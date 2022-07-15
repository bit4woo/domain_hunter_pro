数据持久化框架的选择(mybatis hibernet JdbcTemplate)
https://blog.csdn.net/qq_42282196/article/details/100550626
https://z.itpub.net/article/detail/97A2C412902983D0F6F13F67DEB03FF1

jOOQ系列教程
https://jooq.diamondfsd.com/learn/section-1-how-to-start.html#jOOQ-%E7%AE%80%E4%BB%8B

DbUtils
https://commons.apache.org/proper/commons-dbutils/
https://commons.apache.org/proper/commons-dbutils/examples.html


JdbcTemplate
https://www.javatpoint.com/spring-JdbcTemplate-tutorial
https://www.developersoapbox.com/java-connect-to-sqlite-using-spring-boot/
https://stackoverflow.com/questions/41230234/using-datasource-to-connect-to-sqlite-with-xerial-sqlite-jdbc-driver
https://www.journaldev.com/17053/spring-jdbctemplate-example
使用方法看起来很简单，只需要执行SQL语句，不需要自己去处理异常，关闭数据库链接等。
见JdbcTemplateTest，测试成功，就用它了


关于数据库设计：
目前是一次性将所有数据加载到内存中的，之前的搜索查询，其实是从内存对象中进行的。当数据量大了之后，会发生内存溢出。
这也是想要重构的主要原因。

要改变这个现状，搜索查询就需要直接查询数据库，那么为了提高查询效率，是不是应该将对象的各个属性分别存储为数据库的不同字段。