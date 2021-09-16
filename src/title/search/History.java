package title.search;

import java.util.Vector;

import com.google.gson.Gson;

public class History {
	private Vector<String> historyVector;
	private int currentPosition = -1;//没有内容时的指针位置
	private int size;
    private static History singleton = new History(10);

    public static History getInstance() {
        return singleton;
    }
    
    //default constructor for fastjson
    History(){
    	
    }
	
	private History(int size){
		this.size = size;
		historyVector = new Vector<String>(this.size);
	}
	
	public static History fromJson(String input) {
		return new Gson().fromJson(input, History.class);
	}
	
	public static void setInstance(History input) {
		if (input == null || input.historyVector == null) {
			singleton = new History(10);
		}else {
			singleton = input;
		}
	}
	
	public String toJson() {
		return new Gson().toJson(this);
	}

	public void addRecord(String record){
		if (historyVector.contains(record)){//去除历史重复
			historyVector.remove(record);
		}
		if (historyVector.size() == this.size) {//保存固定大小
			historyVector.remove(0);
		}
		historyVector.add(record);
		currentPosition = historyVector.size()-1;
	}

	public String moveUP(){
		if (currentPosition ==-1){//没有元素
			return null;
		}else if (currentPosition == 0){//实现循环，移动指针到末尾
			currentPosition = historyVector.size()-1;
		}else {
			currentPosition--;
		}
		return historyVector.get(currentPosition);
	}

	public String moveDown(){
		if (historyVector.size() == 0){
			return null;
		}else {
			currentPosition++;
			currentPosition = currentPosition % (historyVector.size());
			return historyVector.get(currentPosition);
		}
	}
	
	public static void main(String[] args) {
		History test = new History(10);
		test.addRecord("1111");
		String json = test.toJson();
		System.out.println(test.toJson());
		System.out.println(History.fromJson(json));
	}
}
