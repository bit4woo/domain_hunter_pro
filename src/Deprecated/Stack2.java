package Deprecated;

import java.util.ArrayList;
import java.util.List;

//泛型会导致fastjson反序列化出错！！！！
public class Stack2<T> {
    //实现栈的List
    private List<Object> stack;

    Stack2() {
        stack = new ArrayList<Object>(10);//初始容量为10
    }

    //判断是否为空
    public boolean isEmpty() {
        return stack.size() == 0;
    }

    //返回栈顶元素
    public T peek() {
        T t = null;
        if (stack.size() > 0)
            t = (T) stack.get(stack.size() - 1);
        return t;
    }

    public void push(T t) {
        if (stack.contains(t)) {
            stack.remove(t);
        }
        stack.add(t);
    }

    //出栈
    public T pop() {
        T t = peek();
        if (stack.size() > 0) {
            stack.remove(stack.size()-1);
        }
        return t;
    }

    public boolean contains(T t) {
        return stack.contains(t);
    }


    public static void main(String[] args) {
        Stack2<String> stack = new Stack2<>();
        System.out.println(stack.peek());
        System.out.println(stack.isEmpty());
        stack.push("java");
        stack.push("is");
        stack.push("beautiful");
        stack.push("language");
        System.out.println(stack.pop());
        System.out.println(stack.isEmpty());
        System.out.println(stack.peek());
    }
}