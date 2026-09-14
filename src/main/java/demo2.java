import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 */

/*带参数get请求*/
public class demo2 {
    public static void main(String[] args) throws IOException, URISyntaxException {
        // 1. 使用 URIBuilder 构建带中文参数的 URI
        URI uri = new URIBuilder("https://mall.gdaee.com.cn/page/searchProject")
                .setParameter("keyWord", "棠下")   // URIBuilder 会自动编码
                .setParameter("type", "all")
                .build();
        /*创建httpclient对象*/
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /*创建http GET请求*/
        HttpGet httpGet = new HttpGet(uri);

        /*执行httpget请求*/
        CloseableHttpResponse response = null;
        try {
             response = httpClient.execute(httpGet);

             /*获取请求体中内容*/
            String context = EntityUtils.toString(response.getEntity(), "UTF-8");
            /*将请求体内容写入本地文件中*/
            FileUtils.writeStringToFile(new File("E:\\result\\demo(get-p).html"),context);
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
