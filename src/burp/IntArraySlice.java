package burp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntArraySlice {
	public static void main(String args[]) {
		int[] input = {-2,1,2,3,6,7,8,9,19,20,21,22,55};
		//int[] input = {0,1,2,3,4,5,6,7,8,9};
		//int[] input = {0,2,5};
		List<int[]> results = slice(input);
		//System.out.println(Arrays.asList(results));
		for (int[] item:results) {
			//System.out.println(Arrays.asList(item));
			System.out.println(Arrays.toString(item));
		}
	}
	
	/*
	 * 1.如果是
	 */
	public static List<int[]> slice(int[] rows){
		Arrays.sort(rows); //升序排序
		List<int[]> result = new ArrayList<>();
		int beginIndex = 0;//连续队列中的第一个元素在rows中的index
		for (int i=0;i<rows.length;i++) {
			//System.out.println("checking: "+i+" "+ rows[i]);
			if (i-beginIndex == rows[i]-rows[beginIndex] ) {//下标的差和值的差相等，表示是连续的
				if (i==rows.length-1) {//结束
					//System.out.println("final: "+i+" "+ rows[i]);
					int[] tmpArray = Arrays.copyOfRange(rows, beginIndex, i+1);
					result.add(tmpArray);
				}else {
					continue;
				}
			}else {
				int[] tmpArray = Arrays.copyOfRange(rows, beginIndex, i);
				result.add(tmpArray);
				beginIndex = i;
				if (i==rows.length-1) {//结束
					//System.out.println("final: "+i+" "+ rows[i]);
					int[] tmpArray1 = Arrays.copyOfRange(rows, beginIndex, i+1);
					result.add(tmpArray1);
				}
				//System.out.println();
			}
		}
		return result;
	}
}
