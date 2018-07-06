package test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class test {
	public static void main(String[] args){
		try {
			URL url = new URL("http://www.runoob.com/index.html?language=cn#j2se");
			String urlpath = url.getPath();
			System.out.println(uselessExtension(urlpath));
			System.out.println("xxx");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}
