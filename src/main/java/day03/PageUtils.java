package day03;

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
 * @Version: 1.1
 */
public class PageUtils {

    private static final int PAGE_SIZE = 10;

    private static final String URL =
            "http://www.ahsgh.com/ahghjtweb/web/list"
                    + "?strColId=20782f569264489f87995ad0773ff626"
                    + "&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d";

    /**
     * 获取【总页数】
     *   1. 用 GET 请求列表页
     *   2. Jsoup 解析 .pagenub i 得到【总条数】
     *   3. 用 getTotalPage() 把总条数换算成总页数
     *
     * ★ 关键修复：以前直接把总条数当页数返回，导致外层循环 2211 次
     */
    public static int getPage() {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpGet get = new HttpGet(URL);
            get.setHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            get.setHeader("Referer", "http://www.ahsgh.com/");

            try (CloseableHttpResponse response = client.execute(get)) {

                String html = EntityUtils.toString(response.getEntity(), "utf-8");

                // 解析总条数：如 <div class="pagenub"><i>2211</i>...</div>
                String text = Jsoup.parse(html)
                        .select(".pagenub i")
                        .first()
                        .text()
                        .trim();

                int totalCount = Integer.parseInt(text);
                System.out.println("总条数 = " + totalCount);

                // ★ 关键一行：换算成总页数（向上取整）
                int totalPage = getTotalPage(totalCount, PAGE_SIZE);
                System.out.println("总页数 = " + totalPage);

                return totalPage;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 总页数 = ceil(totalCount / pageSize) */
    public static int getTotalPage(int totalCount, int pageSize) {
        if (totalCount <= 0) return 0;
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must > 0");
        return (totalCount + pageSize - 1) / pageSize;
    }
}