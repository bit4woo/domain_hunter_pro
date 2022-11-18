package test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 想要使用线程安全的Set，CopyOnWriteArraySet。
 * 但是当变量的类型没有写成CopyOnWriteArraySet而是Set，比如Set<String> subDomainSet = new CopyOnWriteArraySet<String>();
 * 
 * 如果这个类又提供了getter setter方法。那么setter方法就可以改变变量的类型。从而这个变量变得不再线程安全!!!需注意、
 * 
 * 这个时候有两种处理思路：
 * 1、不提供setter方法。通过getter后，进行clear，然后addAll。---这种方式更容易和旧代码兼容。
 * 2、将变量的类型限定为CopyOnWriteArraySet。
 * 3、将变量的类型限定为CopyOnWriteArraySet，并且提供的getter方法，返回类型都改为Set，也可以很好兼容旧代码。而且类型没有篡改风险。
 *
 */
public class ThreadSafeSet {
	public static void main(String[] args) {
		Set<String> subDomainSet = new CopyOnWriteArraySet<String>();
		System.out.println(subDomainSet.getClass());
		subDomainSet = new HashSet<String>();
		System.out.println(subDomainSet.getClass());
	}
}
