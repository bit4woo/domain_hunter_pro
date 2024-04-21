package test;

import java.util.UUID;

/**
 * 结论：应当尽量避免使用static
 */
public class StaticFieldTest {
	
    public static void main(String[] args) {
    	test1();
    }
    
    public static void test1(){
    	Book book1 = new Book("语文");
		book1.author = new Author("张三");


        Book book2 = new Book("数学");
		book2.author = new Author("李四");


        System.out.println(book1.toString());
        
		//通过book1修改author后，通过book再访问author，也是修改过后的。
        //即static虽然访问方便，但是多个实例之间只有一个copy！！！谨慎使用static！避免串对象
        // https://stackoverflow.com/questions/20319081/are-static-variable-in-a-class-duplicated-when-a-new-instance-of-the-class-is-cr

        
        System.out.println(book2.toString());
    }
}

class Book {
	public String name = "";
	public static Author author;

	public static final String instanceID = UUID.randomUUID().toString();
	public Book(String name) {
		this.name = name;
	}

	public String toString(){
		return name+" "+ author.toString()+ " "+ instanceID;
	}
}


class Author {
	public String name = "";

	public Author(String name) {
		this.name = name;
	}
}