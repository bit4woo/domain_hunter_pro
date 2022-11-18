package test;

public class ObjectPassTest {
	public static void main(String[] args) {
		new b(new a());
	}
}


class a{
	String name = "a";
}

class b{
	private Object obj;

	b(Object obj){
		this.obj = obj;
		System.out.println(obj);
		System.out.println(this.obj);

		new c(this.obj);
		new d(this.obj).run();
	}

	String name = "b";
}

class c{
	private Object obj;
	c(Object obj){
		this.obj = obj;
		System.out.println(obj);
		System.out.println(this.obj);
	}
	String name = "c";
}

class d extends Thread{
	private Object obj;
	d(Object obj){
		this.obj = obj;
		System.out.println(obj);
		System.out.println(this.obj);
	}
	String name = "c";

	@Override
	public void run() {
		System.out.println(this.obj);
	}
}