package Deprecated;

import java.util.ArrayList;
import java.util.List;

public class Stack {
    //实现栈的List
    private List<String> stack;

    Stack() {
        stack = new ArrayList<String>(10);//初始容量为10
    }

    //判断是否为空
    public boolean isEmpty() {
        return stack.size() == 0;
    }

    //返回栈顶元素
    public String peek() {
    	String t = null;
        if (stack.size() > 0)
            t = stack.get(stack.size() - 1);
        return t;
    }

    public void push(String t) {
        if (stack.contains(t)) {
            stack.remove(t);
        }
        stack.add(t);
    }

    //出栈
    public String pop() {
    	String t = peek();
        if (stack.size() > 0) {
            stack.remove(stack.size()-1);
        }
        return t;
    }

    public boolean contains(String t) {
        return stack.contains(t);
    }


    public static void main(String[] args) {
        Stack stack = new Stack();
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