### 新的需求TODO List：

1. 梳理Domain的处理逻辑，尤其是无效域名，黑名单域名，等





### 编程经验

个人理解只要该成员方法不需要与非static的成员打交道，就可以使用static。换句话说，只要该方法的执行与对象本身的状态没有关系，就可以使用static.

通常，我们在定义一个工具类时，一般把它的方法定义成static的，因为这种类只用作工具，只关注他的行为，不关注他的状态，所以不需要定义成员变量。使用这种工具类的方法时无需创建对象，既简单又节省资源。创建对象来调用反而麻烦且浪费资源，所以这种类被设计出来后就干脆不允许创建对象，因为其构造方法被设计成private权限了。比如我们用的Math和Arrays，还有Collections。这三个类时我们java中最常见的三个工具类。

如果一个类，在某个程序中可能只会有一个实例，不会有不同的实例，那么它的成员变量就可以设置为static，方便调用。直接通过类名称去调用，而不用通过对象去调用。
比如 burpExtender这个类，在一个插件中就只有一个实例对象，所以它其中的变量都设置成static后，方便调用，棒！这也是经过多次踩坑得出的经验啊~

正则表示表达式的运用

```
ArrayList<String> result = new ArrayList<String>();
//主要目的是找url        path: '/admin/menu',
String webpack_PATTERN = "([0-9a-z])*"; //TODO 正则表达不正确
Pattern pRegex = Pattern.compile(webpack_PATTERN);
String content = inputTextArea.getText();
Matcher matcher = pRegex.matcher(content);
while (matcher.find()) {//多次查找
	result.add(matcher.group());
}
```

