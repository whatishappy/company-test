package day03.test;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-15
 * @Version: 1.0
 */


public class PageUtils {

    private static final Integer PAGE_SIZE = 10;
    private static  Integer TOTALCOUNT = 2211;

    private static final String URL = "http://www.ahsgh.com/ahghjtweb/web/list?strColId=20782f569264489f87995ad0773ff626&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d";
    public static void main(String[] args) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        //使用Get请求获取网页详情
        CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        HttpGet get = new HttpGet(URL);

        CloseableHttpResponse response = null;

        //尝试链接
        try {
            response = client.execute(get);
            String html = EntityUtils.toString(response.getEntity(), "utf-8");
            //System.out.println(html);
            //使用Jsoup解析网页总数据数
            String text = Jsoup.parse(html).select(".pagenub i").first().text();
            //System.out.println(text);

            //每页有10条，取模
            TOTALCOUNT = Integer.parseInt(text);
            System.out.println(getTotalPage(TOTALCOUNT, PAGE_SIZE));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static int getTotalPage(int totalCount, int pageSize) {
        if (totalCount <= 0) return 0;
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must > 0");
        return (totalCount + pageSize - 1) / pageSize;
    }
}
