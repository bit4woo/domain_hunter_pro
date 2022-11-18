domain panel中各个TextArea的MVC逻辑。数据存储在DomainManager对象中。

数据加载过程：
DB ---daoQuery---> DomainManager---show()-->UI
当用户编辑文本框：
UI--listener--->DomainManager
UI--listener--->DB
当后台逻辑直接编辑DomainManager时，比如流量分析获取到了内容： 
DomainManager--->DB
DomainManager--show()---->UI----listener--->DomainManager
                          UI----listener--->DB
                          
                          
无论是用户的手动输入编辑文本框内容，还是调用setText方法，都将触发DocumentListener!(见JTextAreaListenerTest.java)
所以show()函数的设计都应该关闭监听器的开关。
                 
Title Panel中JTable的数据处理逻辑：


