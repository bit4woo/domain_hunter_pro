package test;

/**
 * 如何尽量避免使用static变量？
 * 在burp插件的编写过程中，以前总是习惯使用static变量，因为这样可以通过class.method或class.field的方法访问变量，认为很方便。
 * 但是其存在弊端，容易在多对象时发生问题。所有应当尽量避免使用static变量。
 * 那么如何在当前对象中，调用其他对象的属性呢？答案就是通过构造函数传递对象！！！
 * 
 *
 */
public class HowToVoidUseStatic {

}
