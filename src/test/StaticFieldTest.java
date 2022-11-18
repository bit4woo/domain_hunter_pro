package test;

/**
 * 结论：应当尽量避免使用static
 */
public class StaticFieldTest {
	
    public static void main(String[] args) {
    	test2();
    }
    
    public static void test1(){
    	Book book = new Book();
        Book book1 = new Book();
        
        book.name = "name";
        book.author = "author";
        System.out.println(book.toString());
        
        book1.name = "name1";
        book1.author = "author1";//通过book1修改author后，通过book再访问author，也是修改过后的。
        //即static虽然访问方便，但是多个实例之间只有一个copy！！！谨慎使用static！避免串对象
        // https://stackoverflow.com/questions/20319081/are-static-variable-in-a-class-duplicated-when-a-new-instance-of-the-class-is-cr
        
        
        System.out.println(book.toString());//这里对
        
        System.out.println(book1.toString());
    }
    
    public static void test2(){
    	pen aa = new pen("钢笔");
    	pen bbb  = new pen("铅笔");
    	
    	System.out.println(aa.instance);
    	System.out.println(bbb.instance);
    }
}

class Book {
	public String name = "";
	public static String author = "";
	
	public String toString(){
		return name+" "+ author;
	}
}


class pen {
	public String name = "";
	public static pen instance;
	
	pen(String name){
		this.name = name;
		instance = this;
	}
	
	public String toString(){
		return name;
	}
}