package test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SetTest {
	public static void main(String[] args){
		SetRemove();
	}
	
	public static Boolean uselessExtension(String urlpath) {
		Set extendset = new HashSet();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		Iterator iter = extendset.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next());
			if(urlpath.endsWith(iter.next().toString())) {
				return true;
			}
		}
		return false;
	}

	public static void SetRemove() {
		Set extendset = new HashSet();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		extendset.remove("aaa");
	}
}
