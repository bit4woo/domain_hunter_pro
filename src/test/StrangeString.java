package test;

import java.net.URL;

public class StrangeString {
	public static void main(String args[]) throws Exception {
		String a = "http://res-admin.sf-exprêss.com";
		String b = "http://res-admin.sf-express.com";
		if (a==b) {
			System.out.println("same");
		}else {
			System.out.println("diff");
		}
		
		URL aa= new URL(a);
		URL bb= new URL(b);
		if (aa.getHost()==bb.getHost()) {
			System.out.println("same");
		}else {
			System.out.println("diff");
		}
	}
}
