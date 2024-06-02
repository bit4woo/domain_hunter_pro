package base;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Stack {
    //实现栈的List
    private List<String> stack;
    public static final int sizeOfStack = 10;
    public Stack() {
        stack = new ArrayList<>(sizeOfStack);//初始容量为10
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
        if (stack.size() >= sizeOfStack-1){
            stack.remove(0);
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
    
    public boolean remove(String item) {
    	return stack.remove(item);
    }

    public boolean contains(String t) {
        return stack.contains(t);
    }

    public List<String> getItemList(){
    	return new ArrayList<>(stack);
    }

    @Override
    public int hashCode() {
        // 创建一个 StringBuilder 来存储连接后的字符串
        StringBuilder stringBuilder = new StringBuilder();

        // 将 List 中的所有字符串连接成一个大字符串
        for (String str : stack) {
            stringBuilder.append(str);
        }

        // 使用连接后的大字符串的 hashCode 作为 hashCode 方法的返回值
        return Objects.hash(stringBuilder.toString());
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