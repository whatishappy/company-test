import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 */

/*无参数post请求*/
public class demo3 {
    public static void main(String[] args) throws IOException, URISyntaxException {
        // 1. 使用 URIBuilder 构建带中文参数的 URI
        URI uri = new URIBuilder("https://mall.gdaee.com.cn/page/searchProject")
                .setParameter("keyWord", "棠下")   // URIBuilder 会自动编码
                .setParameter("type", "all")
                .build();
        /*创建httpclient对象*/
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /*创建http GET请求*/
        HttpPost httpPost = new HttpPost(uri);

        /*设置请求头*/
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");

        /*执行httppost请求*/
        CloseableHttpResponse response = null;
        try {
             response = httpClient.execute(httpPost);

             /*获取请求体中内容*/
            String context = EntityUtils.toString(response.getEntity(), "UTF-8");
            /*将请求体内容写入本地文件中*/
            FileUtils.writeStringToFile(new File("E:\\result\\demo(post).html"),context);
            System.out.println("内容长度为：" + context.length());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            /*最终将资源释放*/
            if (response != null) {
                response.close();
            }
            /*关闭client*/
            httpClient.close();
        }

    }

}
