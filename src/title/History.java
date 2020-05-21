package title;

import java.util.Vector;

public class History {
	public Vector<String> historyVector;
	int currentPosition = -1;//没有内容时的指针位置
	int size;
	public History(int size){
		this.size = size;
		historyVector = new Vector<String>(this.size);
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
}
