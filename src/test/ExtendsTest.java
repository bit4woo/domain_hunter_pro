package test;

public class ExtendsTest {
	public static void main(String[] args) {
		Mouse mouse = new Mouse("mike",1,"xxx");
		System.out.println(mouse.getName());
		System.out.println(((Animal)mouse).getName());
	}
}

class Animal { 
    private String name="default";  
    private int id;
    
    
    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public Animal(String myName, int myid) { 
        name = myName; 
        id = myid;
    } 
    public void eat(){ 
        System.out.println(name+"正在吃"); 
    }
    public void sleep(){
        System.out.println(name+"正在睡");
    }
    public void introduction() { 
        System.out.println("大家好！我是"         + id + "号" + name + "."); 
    } 
}

class Mouse extends Animal { 
    private String xxx;
    private String name;
    
    
	public String getXxx() {
		return xxx;
	}

	public void setXxx(String xxx) {
		this.xxx = xxx;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	

	public Mouse(String myName, int myid,String xxx) { 
        super(myName, myid);
        this.xxx=xxx;
        this.name= myName;
    } 
}
