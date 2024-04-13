package base;

import java.util.ArrayList;
import java.util.List;

public class Stack {
    //实现栈的List
    private List<String> stack;

    public Stack() {
        stack = new ArrayList<>(10);//初始容量为10
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

    /**
     * push时需要删除旧的记录
     * @param t
     */
    public void push(String t) {
        stack.remove(t);//不存在也没关系
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

    public List<String> getItemList(){
    	return new ArrayList<>(stack);
    }

    public static void main(String[] args) {
        Stack stack = new Stack();
        stack.hashCode();
        System.out.println(stack.peek());
        System.out.println(stack.isEmpty());
        stack.push("java");
        stack.push("is");
        stack.push("beautiful");
        stack.push("language");

        System.out.println(stack.hashCode());
        System.out.println(stack.pop());
        stack.push("language");
        System.out.println(stack.hashCode());
        System.out.println(stack.isEmpty());
        System.out.println(stack.peek());
    }
}