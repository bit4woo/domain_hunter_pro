package title.search;

import java.util.Vector;

import com.google.gson.Gson;

public class History {
	private Vector<String> historyVector;
	private int currentPosition = -1;//没有内容时的指针位置
	private static int size = 10;
	private static boolean allowLoop = true;
	private static History singleton = null;

	public static History getInstance(int size,boolean allowLoop) {
		if (singleton == null) {
			singleton = new History(size,allowLoop);
		}
		return singleton;
	}

	public static History getInstance() {
		if (singleton == null) {
			singleton = new History(size,allowLoop);
		}
		return singleton;
	}

	//default constructor for fastjson
	History(){

	}

	public History(int size,boolean allowLoop){
		History.size = size;
		History.allowLoop = allowLoop;
		historyVector = new Vector<String>(History.size);
	}

	public static History fromJson(String input) {
		return new Gson().fromJson(input, History.class);
	}

	public String toJson() {
		return new Gson().toJson(this);
	}

	public void addRecord(String record){
		if (historyVector.contains(record)){//去除历史重复
			historyVector.remove(record);
		}
		if (historyVector.size() == History.size) {//保存固定大小
			historyVector.remove(0);
		}
		historyVector.add(record);
		currentPosition = historyVector.size()-1;
	}

	public boolean contains(String record) {
		return historyVector.contains(record);
	}

	public String moveUP(){
		if (currentPosition ==-1){//没有元素
			return null;
		}else if (currentPosition == 0){//实现循环，移动指针到末尾
			if (allowLoop) {
				currentPosition = historyVector.size()-1;
			}else {
				//就返回0,无需改动
			}
		}else {
			currentPosition = currentPosition-1;
		}
		return historyVector.get(currentPosition);
	}

	public String moveDown(){
		if (historyVector.size() == 0){
			return null;
		}else {
			if (allowLoop) {
				currentPosition = currentPosition+1;
				currentPosition = currentPosition % (historyVector.size());
			}else {
				if (currentPosition == historyVector.size()-1) {
					//do nothing
				}else {
					currentPosition = currentPosition+1;
				}
			}
			return historyVector.get(currentPosition);
		}
	}

	public static void test() {
		History test = new History(10,true);
		test.addRecord("1111");
		test.addRecord("2222");
		test.addRecord("3333");
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println();
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
	}

	public static void test1() {
		History test = new History(10,false);
		test.addRecord("1111");
		test.addRecord("2222");
		test.addRecord("3333");
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println(test.moveUP());
		System.out.println();
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
		System.out.println(test.moveDown());
	}


	public static void main(String[] args) {
		test1();
	}
}
