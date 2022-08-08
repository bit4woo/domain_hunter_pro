package toElastic;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import burp.BurpExtender;
import config.ConfigPanel;
import title.LineEntry;

//https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.13/java-rest-high-document-index.html

public class ElasticClient {
	private static RestHighLevelClient httpClient;
	private static ActionListener<IndexResponse> listener;
	private static String indexName = "domain_hunter_pro";
	private static String url = "";
	private static String username = "";
	private static String password = "";

	/**
	 * 创建客户端实例
	 * @return
	 */
	private static RestHighLevelClient getInstance(String inputurl,String inputusername,String inputpassword) {
		if (url.equals(inputurl) && username.equals(inputusername) && password.equals(inputpassword) && httpClient != null) {
			return httpClient;
		}
		try {
			url = inputurl;
			username = inputusername;
			password = inputpassword;
			URL elasticUrl = new URL(url);
			RestClientBuilder builder = RestClient.builder(new HttpHost(elasticUrl.getHost(), elasticUrl.getPort(), elasticUrl.getProtocol()));
			String authValue = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
			builder.setDefaultHeaders(new Header[]{new BasicHeader("Authorization", String.format("Basic %s", authValue))});
			
			httpClient = new RestHighLevelClient(builder);
			
			createIndices(indexName); //创建数据库
			createListener();//创建异步处理的监听器
			
			return httpClient;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 把我们先要写入elastic的数据转换为IndexRequest对象
	 * @param entry
	 * @return
	 */
	private static IndexRequest buildContent(LineEntry entry) {
		try {
			XContentBuilder contentBuilder = XContentFactory.jsonBuilder();
			contentBuilder.startObject();
			{
				contentBuilder.field("url", entry.getUrl());
				contentBuilder.timeField("request", new String(entry.getRequest(),"ISO-8859-1"));
				contentBuilder.field("response", new String(entry.getResponse(),"ISO-8859-1"));
			}
			contentBuilder.endObject();
			IndexRequest indexRequest = new IndexRequest(indexName)
					.source(contentBuilder);
			return indexRequest;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}  
	}
	/**
	 * 创建异步处理时的事件监听器，可以知道异步执行的结果
	 */
	private static void createListener() {
		listener = new ActionListener<IndexResponse>() {
			@Override
			public void onResponse(IndexResponse indexResponse) {
				/*
				String index = indexResponse.getIndex();
				String id = indexResponse.getId();
				if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {

				} else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {

				}
				ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
				if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

				}
				if (shardInfo.getFailed() > 0) {
					for (ReplicationResponse.ShardInfo.Failure failure :
						shardInfo.getFailures()) {
						String reason = failure.reason();
					}
				}*/
				System.out.println("write data to elastic successful");
			}

			@Override
			public void onFailure(Exception e) {
				System.out.println("write data to elastic failed");
			}
		};
	}
	
	/**
	 * 从图形界面自动获取Elastic的配置信息。
	 * @param entry
	 */
	public static void writeData(LineEntry entry) {
		String apiUrl = ConfigPanel.textFieldElasticURL.getText().trim();
		String userAndPass = ConfigPanel.textFieldElasticUserPass.getText();
		if (userAndPass.contains(":")) {
			String[] arr = userAndPass.split(":");
			writeData(apiUrl,arr[0],arr[1],entry);
		}else {
			BurpExtender.getStderr().println("Wrong username and password");
		}
	}
	
	/**
	 * 对外提供的数据写入函数
	 * @param entry
	 */
	public static void writeData(String inputurl,String inputusername,String inputpassword,LineEntry entry) {
		try {
			getInstance(inputurl,inputusername,inputpassword);
			IndexRequest indexRequest = buildContent(entry);
			//异步执行
			httpClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
			//同步执行
			/*
			IndexResponse indexResponse = httpClient.index(indexRequest, RequestOptions.DEFAULT);
			System.out.println(indexResponse);
			System.out.println(indexResponse.getId());
			System.out.println(indexResponse.getIndex());
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 创建索引，index类似于mysql中的database
	 * @throws IOException
	 */
	private static void createIndices(String indexName) throws IOException {
		GetIndexRequest request = new GetIndexRequest(indexName);

		boolean exists = httpClient.indices().exists(request, RequestOptions.DEFAULT);

		if(!exists) {
			CreateIndexRequest _request = new CreateIndexRequest(indexName);
			httpClient.indices().create(_request, RequestOptions.DEFAULT);
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		String url = "http://10.12.72.55:9200/";
		String username = "elastic";
		String password = "changeme";
	}
}
