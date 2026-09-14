package day01;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 */

/*无参数get请求*/
public class demo1 {
    public static void main(String[] args) throws IOException {
        String URL = "http://www.hnsfzyyy.com/list-2037-1.html";
        /*创建httpclient对象*/
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /*创建http GET请求*/
        HttpGet httpGet = new HttpGet(URL);

        /*执行httpget请求*/
        CloseableHttpResponse response = null;
        try {
             response = httpClient.execute(httpGet);

             /*获取请求体中内容*/
            String context = EntityUtils.toString(response.getEntity(), "UTF-8");
            System.out.println(context);
            /*将请求体内容写入本地文件中*/
            FileUtils.writeStringToFile(new File("E:\\result\\demo(get).html"),context);
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
