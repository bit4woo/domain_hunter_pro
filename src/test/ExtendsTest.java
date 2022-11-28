package test;

public class ExtendsTest {

}

class Animal { 
    private String name;  
    private int id;
    private String xxx;
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
    public Mouse(String myName, int myid) { 
        super(myName, myid);
    } 
}
