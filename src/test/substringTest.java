package test;

public class substringTest {
	public static void main(String args[]) {
		String host = "123.58.49.32:8089";
		host = host.substring(0,host.indexOf(":"));
		String port = host.substring(host.indexOf(":"),host.length()-1);
		if (port.length()>0) {
			System.out.print(Integer.parseInt(port));
		}
	}
}
