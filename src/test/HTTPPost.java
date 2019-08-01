package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class HTTPPost {
	public static String httpPostRequest(String url, String param)
			throws IOException {
		URL u;
		HttpURLConnection con = null;
		OutputStreamWriter osw;
		StringBuffer buffer = new StringBuffer();

		u = new URL(url);
		con = (HttpURLConnection) u.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestProperty("Content-Type",
				"application/json");

		osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
		osw.write(param);
		osw.flush();
		osw.close();

		BufferedReader br = new BufferedReader(new InputStreamReader(con
				.getInputStream(), "UTF-8"));
		String temp;
		while ((temp = br.readLine()) != null) {
			buffer.append(temp);
			buffer.append("\n");
		}

		return buffer.toString();
	}
	
	public static void main(String args[]) {
		try {
			httpPostRequest("http://dyfk9vxc9wug4nynx6hr24pjya40sp.burpcollaborator.net","");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public static String Map2String(Map input) {
//		return new Gson().toJson(input);
//	}
	
	
}
