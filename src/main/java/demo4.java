import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class demo4 {
    private static final String URL = "http://www.ahsgh.com/ahghjtweb/web/list";    //访问URL
    private static final String SAVE_DIR = "E:\\result"; // 本地保存目录
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0"
    };          //UA列表
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws IOException {
        /*配置httpclient*/
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10 * 1000)
                .build();

        // 1. 创建异步保存文件的线程池
        ExecutorService fileSaveExecutor = Executors.newFixedThreadPool(4);

        // 创建本地目录
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. 创建 HttpClient (try-with-resources 会在最后自动关闭)
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {

            /*当前手动输入，后续可以使用jsoup动态获取totalPage*/
            System.out.println("请输入被爬取网页的总页数：");
            Scanner sc = new Scanner(System.in);
            int totalPage = sc.nextInt();

            // 3. 循环爬取（这里改成了 <=，否则会漏掉最后一页）
            for (int page = 1; page <= totalPage; page++) {
                System.out.println("========当前正在爬取第" + page + "页数据==========");

                // 调用爬虫方法，传入当前页码 page
                String html = crawler(httpclient, page, URL);

                //先进行非空判断
                if (html == null || html.isEmpty()) {
                    System.err.println("第" + page + "页无数据或请求失败，跳过!");

                    continue; // 失败就跳过这一页，不中断整个程序
                }

                System.out.println("第" + page + "页爬取成功，长度" + html.length());

                // 4. 将保存文件的任务提交给异步线程池
                final int currentPage = page;
                fileSaveExecutor.submit(() -> {
                    try {
                        File saveFile = new File(SAVE_DIR, "page_" + currentPage + ".html");
                        // 使用 FileUtils 异步写入本地
                        FileUtils.writeStringToFile(saveFile, html,"utf-8");
                        System.out.println("异步保存成功 第" + currentPage + "页 -> " + saveFile.getAbsolutePath());
                    } catch (IOException e) {
                        System.err.println("异步保存失败 第" + currentPage + "页: " + e.getMessage());
                    }
                });

                // 5. 随机访问间隔
                if (page < totalPage) {
                    long sleepTime = 2000 + RANDOM.nextInt(3000);
                    System.out.println("等待" + sleepTime + "ms 后继续...");
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 6. 所有页面爬取完毕，关闭线程池
        fileSaveExecutor.shutdown();

        try {
            if (fileSaveExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.out.println("所有页面爬取完成！");
            }else {
                System.out.println("等待超时，部分文件可能未保存完毕");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 爬取单页数据
     * @param httpClient HttpClient对象
     * @param currentPage 当前页码
     * @param URL 目标URL
     */
    private static String crawler(CloseableHttpClient httpClient, int currentPage, String URL) throws IOException {


        HttpPost httpPost = new HttpPost(URL);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("listPage", "list"));
        params.add(new BasicNameValuePair("intCurPage", String.valueOf(currentPage)));
        params.add(new BasicNameValuePair("intPageSize", "10"));
        params.add(new BasicNameValuePair("strColId", "20782f569264489f87995ad0773ff626"));
        params.add(new BasicNameValuePair("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d"));
        params.add(new BasicNameValuePair("nowPage", "1"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        httpPost.setHeader("User-Agent", USER_AGENTS[RANDOM.nextInt(USER_AGENTS.length)]);
        httpPost.setHeader("Referer", "http://www.ahsgh.com/ahghjtweb/web/list");
        httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        httpPost.setHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } else if (statusCode == 403 || statusCode == 429) {
                System.err.println("被拒绝访问，状态码：" + statusCode + "，建议更换ip或者增大访问间隔");
                return null;
            } else {
                System.err.println("请求失败，状态码：" + statusCode);
                return null;
            }
        }
        //不能在这里使用finally代码块关闭httpclient，不然循环无法进行
    }
}