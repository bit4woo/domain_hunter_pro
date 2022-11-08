package test;


/**
 * 
 * 是可以传递当前正在构建的对象的，通过this关键词。
 * 以前的null错误是因为ObjectPassAndStructorMethod.png中的问题，先调用this()再赋值导致的。
 *
 */
public class ObjectPassTest1 {
	public static void main(String[] args) {
		new grandFather();
	}
}

class grandFather{
	String name = "a";
	Father father;
	grandFather(){
		new Father(this);
	}
}

class Father{
	private grandFather obj;

	Father(grandFather obj){
		this.obj = obj;
		System.out.println(obj);
		System.out.println(this.obj);
		
		try {
			Thread.sleep(1000*6);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Son son = new Son(obj);
	}

	String name = "b";
}

class Son{
	private grandFather obj;

	Son(grandFather obj){
		this.obj = obj;
		System.out.println(obj);
		System.out.println(this.obj);
	}
}

