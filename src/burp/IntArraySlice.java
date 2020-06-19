package burp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntArraySlice {
	public static void main(String args[]) {
		int[] input = {-2,1,2,3,6,7,8,9,19,20,21,22,55};
		List<int[]> results = slice(input);
		for (int[] item:results) {
			//System.out.println(Arrays.asList(item));
			System.out.println(Arrays.toString(item));
		}
	}
	
	public static List<int[]> slice(int[] rows){
		Arrays.sort(rows); //升序排序
		List<int[]> result = new ArrayList<>();
		int beginIndex = 0;//连续队列中的第一个元素在rows中的index
		for (int i=0;i<rows.length;i++) {
			int Currentitem = rows[i];
			System.out.println("checking: "+i+" "+Currentitem);
			
			if (i-beginIndex == rows[i]-rows[beginIndex] ) {//下标的差和值的差相等，表示是连续的
				continue;
			}else {
				int[] tmpArray = Arrays.copyOfRange(rows, beginIndex, i);
				result.add(tmpArray);
				beginIndex = i;
				System.out.println();
			}
			
			if (i==rows.length-1) {//结束
				System.out.println("final: "+i+" "+Currentitem);
				int[] tmpArray = Arrays.copyOfRange(rows, beginIndex, i+1);
				result.add(tmpArray);
			}
		}
		return result;
	}
}
