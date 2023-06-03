package base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntArraySlice {
	public static void main(String args[]) {
		int[] input = {-2,1,2,3,6,7,8,9,19,20,21,22,55};
		//int[] input = {0,1,2,3,4,5,6,7,8,9};
		//int[] input = {0,2,5};
		//input = new int[]{0, 17};
		input = new int[]{0};
		List<int[]> results = slice(input);
		//System.out.println(Arrays.asList(results));
		for (int[] item:results) {
			//System.out.println(Arrays.asList(item));
			System.out.println(Arrays.toString(item));
		}
	}

	public static void printArray(Integer[] rows) {
		for (int row:rows) {
			System.out.println(row);
		}
	}

	/*
	 * 1.如果是
	 */
	public static List<int[]> slice(int[] rows){
		//https://stackoverflow.com/questions/880581/how-to-convert-int-to-integer-in-java
		//Integer[] ever = IntStream.of(rows).boxed().toArray(Integer[]::new);
		Integer[] tempRows = Arrays.stream(rows).boxed().toArray(Integer[]::new);//covert int[] to Integer[]
		//Arrays.sort(rows1); //升序排序
		Arrays.sort(tempRows,Collections.reverseOrder());//倒叙排序
		printArray(tempRows);
		rows = Arrays.stream(tempRows).mapToInt(Integer::intValue).toArray();//covert Integer[] to int[]

		List<int[]> result = new ArrayList<>();
		int beginIndex = 0;//连续队列中的第一个元素在rows中的index
		for (int i=0;i<rows.length;i++) {
			if (i-beginIndex == rows[beginIndex]-rows[i] ) {//下标的差和值的差相等，表示是连续的
				//continue;
			}else {
				int[] tmpArray = Arrays.copyOfRange(rows, beginIndex, i);
				result.add(tmpArray);
				beginIndex = i;
			}

			if (i==rows.length-1) {//结束
				//System.out.println("final: "+i+" "+ rows[i]);
				int[] tmpArray1 = Arrays.copyOfRange(rows, beginIndex, i + 1);
				result.add(tmpArray1);
			}
		}
		return result;
	}
}
