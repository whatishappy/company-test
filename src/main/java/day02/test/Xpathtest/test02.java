package day02.test.Xpathtest;

import day03.PageUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 2.0   —— 引入线程池，两层并行
 */
public class test02 {

    /* 列表页地址（POST） */
    private static final String LIST_URL = "http://www.ahsgh.com/ahghjtweb/web/list";
    /* 详情页地址（GET） */
    private static final String VIEW_URL = "http://www.ahsgh.com/ahghjtweb/web/view";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern URL_VIEW_PATTERN =
            Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");

    private static final int PAGE_SIZE = 10;

    /* ================== 线程池参数 ================== */
    private static final int LIST_THREADS = 4;    // 列表页并发线程数
    private static final int DETAIL_THREADS = 6;   // 详情页并发线程数

    /* ================== 全局共享 HttpClient ================== */
    private static final CloseableHttpClient HTTP_CLIENT = buildHttpClient();

    /*====================http配置信息==========================*/
    private static CloseableHttpClient buildHttpClient() {
        RequestConfig cfg = RequestConfig.custom()
                .setConnectTimeout(8000)
                .setConnectionRequestTimeout(8000)
                .setSocketTimeout(10000)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(cfg)
                .setMaxConnTotal(200)       // 连接池总连接数
                .setMaxConnPerRoute(100)    // 每个目标 host 最大连接数
                .build();
    }

    /* ====================================================================== */

    public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();

        /* 1. 拿总页数 */
        int totalPage = PageUtils.getPage();
        if (totalPage <= 0) {
            System.err.println("获取总页数失败，退出");
            return;
        }

        /* 2. 并行抓所有列表页 */
        String[] pageHtmls = fetchAllListPages(totalPage);

        /* 3. 并行抓所有详情页并解析 */
        int totalItems = crawlAllDetails(pageHtmls);

        /* 4. 抓取结束关闭链接 */
        HTTP_CLIENT.close();

        long cost = System.currentTimeMillis() - start;
        System.out.printf("%n================ 全部完成，共 %d 条，耗时 %.2f 秒 ================%n",
                totalItems, cost / 1000.0);
    }

    /*                        第一层：并行抓列表页                          */

    private static String[] fetchAllListPages(Integer totalPage) throws InterruptedException {

        // index 1..totalPage 存放每页 html，index 0 空着
        String[] result = new String[totalPage + 1];

        ExecutorService pool = Executors.newFixedThreadPool(LIST_THREADS);  //核心线程数为4
        CountDownLatch latch = new CountDownLatch(totalPage);               //设置任务数

        for (int p = 1; p <= totalPage; p++) {
            final int curPage = p;
            pool.submit(() -> {
                try {
                    result[curPage] = postOneListPage(curPage); //分页存入，页码从1开始存
                } catch (Exception e) {
                    System.err.println("列表 第 " + curPage + " 页异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        //等待所有线程任务执行完毕
        latch.await();
        pool.shutdown();

        // 统计成功页数
        int ok = 0;
        for (int i = 1; i < result.length; i++) if (result[i] != null) ok++;
        System.out.println("列表 抓取完成 " + ok + "/" + totalPage + " 页\n");

        return result;
    }
    /**
     * 抓取单个列表页
     */
    private static String postOneListPage(int curPage) throws IOException {

        HttpPost post = new HttpPost(LIST_URL);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("listPage", "list"));
        params.add(new BasicNameValuePair("intCurPage", String.valueOf(curPage)));
        params.add(new BasicNameValuePair("intPageSize", String.valueOf(PAGE_SIZE)));
        params.add(new BasicNameValuePair("strColId", "20782f569264489f87995ad0773ff626"));
        params.add(new BasicNameValuePair("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d"));
        params.add(new BasicNameValuePair("nowPage", "1"));
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("Referer", LIST_URL);
        post.setHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        post.setHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");

        try (CloseableHttpResponse resp = HTTP_CLIENT.execute(post)) {
            if (resp.getStatusLine().getStatusCode() == 200) {
                String html = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                System.out.println("列表| 第 " + curPage + " 页 OK, 长度=" + html.length());
                return html;
            }
            System.err.println("列表| 第 " + curPage + " 页失败, 状态码=" + resp.getStatusLine().getStatusCode());
        }
        return null;
    }

    /*                        第二层：并行抓详情页                          */
    private static int crawlAllDetails(String[] pageHtmls) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(DETAIL_THREADS);

        int totalItems = 0;

        // ★ 最外层：按页循环
        for (int p = 1; p < pageHtmls.length; p++) {

            String listHtml = pageHtmls[p];
            if (listHtml == null) {
                // 这一页抓失败了，也输出一条分割线，保持结构完整
                System.out.println("\n==================== 第 " + p + " 页（抓取失败） ====================\n");
                continue;
            }

            Elements items = Jsoup.parse(listHtml)
                    .select("ul.tab01open.tab01tca li");

            // ★ 页内所有详情任务的结果，按提交顺序存到 futures
            List<Future<String>> futures = new ArrayList<>();

            for (Element item : items) {

                Element h2   = item.select("h2.fl").first();
                Element iTag = item.select("i.fr").first();
                Element aTag = item.select("a[href*=url_view]").first();

                if (h2 == null || aTag == null) continue;

                final String title       = h2.text().trim();
                final String publishTime = (iTag == null) ? "未匹配到时间"
                        : parseTimeByRegex(iTag.outerHtml());
                final String detailUrl   = buildDetailUrl(aTag.attr("href"));

                // 提交任务，拿到 Future<String>
                futures.add(pool.submit(() ->
                        handleOneDetail(title, publishTime, detailUrl)
                ));
            }

            // ★ 拼一页的内容
            StringBuilder pageSb = new StringBuilder(8192);
            pageSb.append("\n");
            pageSb.append("==================== 第 ").append(p).append(" 页 ====================\n\n");

            // ★ 按提交顺序取回每条新闻的结果（保证输出顺序和页面顺序一致）
            for (Future<String> f : futures) {
                try {
                    pageSb.append(f.get());   // f.get() 会阻塞，直到这条任务完成
                    pageSb.append('\n');      // 每条新闻之间空一行
                    totalItems++;
                } catch (Exception e) {
                    pageSb.append("【异常】").append(e.getMessage()).append('\n');
                }
            }

            // ★ 整页一次性输出（避免多线程交错）
            System.out.print(pageSb);
        }

        pool.shutdown();
        return totalItems;
    }

    /**
     * 处理一条：抓详情 + 解析正文 + 拼装输出字符串。
     * 用 StringBuilder 一次性返回，避免多线程下多行输出互相穿插。
     */
    private static String handleOneDetail(String title, String publishTime, String detailUrl) {

        StringBuilder sb = new StringBuilder(1024);
        sb.append("【标题】：").append(title).append('\n');
        sb.append("【发布时间】：").append(publishTime).append('\n');
        sb.append("【详情链接】：").append(detailUrl).append('\n');

        try {
            String detailHtml = getDetailPage(detailUrl);
            if (detailHtml != null) {
                String contentHtml = parseContentHtml(detailHtml);
                /* 打印 html 源码（已注释） */
            /*sb.append("【正文html】")
                    .append(contentHtml == null ? "解析失败" : contentHtml).append('\n');*/
                sb.append("【正文详细】：")
                        .append(contentHtml == null ? "" : Jsoup.parse(contentHtml).text())
                        .append('\n');
            } else {
                sb.append("【正文html】详情页抓取失败\n");
            }
        } catch (Exception e) {
            sb.append("【异常】").append(e.getMessage()).append('\n');
        }

        return sb.toString();
    }

    /*                            工具方法                                 */

    /**
     * HttpClient GET 抓取详情页
     */
    public static String getDetailPage(String detailUrl) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(detailUrl);
            httpGet.setHeader("User-Agent", USER_AGENT);
            httpGet.setHeader("Referer", LIST_URL);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private static String parseTimeByRegex(String html) {
        Matcher m = TIME_PATTERN.matcher(html);
        return m.find() ? m.group() : "未匹配到时间";
    }

    /**
     * Jsoup 定位详情正文：
     *   结构上正文位于 <!-- 正文内容 --> 与 <!-- 文后 --> 两个注释之间；
     *   由于正文里的 <p> 嵌套是非法的，jsoup 会自动闭合，导致正文被拆成多个兄弟节点，
     *   因此用注释锚点把两段注释之间的所有元素拼接起来，得到完整正文 html
     *
     * @Param: 单个html源码
     */
    public static String parseContentHtml(String detailHtml) {
        // 1. 解析当前网页，获取 Document 对象
        Document detailDoc = Jsoup.parse(detailHtml);
        // 2. 选择 class 为 article-conca 的容器
        Element articleConca = detailDoc.selectFirst(".article-conca");
        if (articleConca == null) {
            return null;
        }
        StringBuilder contentHtml = new StringBuilder();
        boolean startCollect = false;
        // 3. 遍历容器下的直接子节点（含注释、文本、元素）
        for (Node child : articleConca.childNodes()) {
            // 3.1 根据源码中注释判断是否停止采集
            if (child instanceof Comment) {
                String data = ((Comment) child).getData();  //使用getData方法获取comment注释
                if (data.contains("正文内容")) {
                    startCollect = true;   // 遇到起点注释，开始收集
                    continue;
                }
                if (data.contains("文后") && startCollect) {          //判断当前读取是否包含“文后”
                    break;                 // 遇到终点注释，结束收集
                }
            }
            // 3.2 正在收集，且是元素节点拼接
            if (startCollect && child instanceof Element) {
                Element el = (Element) child;
                // 跳过 jsoup 自动闭合产生的空 <p></p>
                if (el.childNodeSize() == 0 && el.text().isEmpty()) {
                    continue;
                }
                contentHtml.append(el.outerHtml());
            }
        }
        return contentHtml.length() == 0 ? null : contentHtml.toString();
    }

    private static String buildDetailUrl(String href) {
        Matcher m = URL_VIEW_PATTERN.matcher(href);
        if (m.find()) {
            return VIEW_URL + "?strId=" + m.group(1)
                    + "&strColId=" + m.group(2)
                    + "&strWebSiteId=" + m.group(3);
        }
        return href;
    }

}