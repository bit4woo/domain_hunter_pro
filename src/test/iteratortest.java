package test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class iteratortest {
	public static void main(String[] args) {
	    
		ArrayList<List<?>> xx = new ArrayList<List<?>>();
		List a = new ArrayList();
		a.add(1);
		a.add("a");
		List b = new ArrayList();
		b.add(2);
		b.add("b");
		
		xx.add(a);
		xx.add(b);

		Iterator<List<?>> item = xx.iterator();//java.lang.NullPointerException. why???
		if(item.hasNext()) {
			System.out.println(item.next());
		}
	}


}
