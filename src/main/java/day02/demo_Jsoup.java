package day02;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 *
 * 第二天练习主程序：
 *   1. 通过 HttpClient(POST) 获取列表页内容
 *   2. Jsoup  精准/模糊定位：列表标题、详情链接
 *   3. 正则表达式：解析发布时间、提取 url_view 链接参数
 *   4. 通过 HttpClient(GET) 获取详情页内容
 *   5. Jsoup  获取详情正文 html
 *   以上数据全部在控制台打印
 *
 * 练习网站：安徽省港航集团有限公司 - 通知通告列表
 *   http://www.ahsgh.com/ahghjtweb/web/list
 */
public class demo {

    /* 列表页地址（POST） */
    private static final String LIST_URL = "http://www.ahsgh.com/ahghjtweb/web/list";
    /* 详情页地址（GET，参数用正则从 url_view 中提取） */
    private static final String VIEW_URL = "http://www.ahsgh.com/ahghjtweb/web/view";
    /* 请求头 */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /* ========== 正则：发布时间（列表页 <i class="fr">2026-09-14</i>）========== */
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /* ========== 正则：url_view('strId','strColId','strWebSiteId') 三个参数 ========== */
    private static final Pattern URL_VIEW_PATTERN =
            Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");

    public static void main(String[] args) throws IOException {
        /* 1. HttpClient 抓取列表页 */
        String listHtml = postListPage(1);
        if (listHtml == null || listHtml.isEmpty()) {
            System.err.println("列表页抓取失败，程序退出");
            return;
        }
        System.out.println("=================== 列表页抓取成功，内容长度：" + listHtml.length() + " ===================");

        /* 2. Jsoup 精准定位：列表容器 ul.tab01open.tab01tca 下的每一个 li */
        Document listDoc = Jsoup.parse(listHtml);
        Elements items = listDoc.select("ul.tab01open.tab01tca li");
        System.out.println("本次共解析到 " + items.size() + " 条记录\n");

        /* 3. 遍历每一条记录：解析 标题 / 发布时间 / 详情链接 / 详情正文html */
        int index = 0;
        for (Element item : items) {
            index++;
            System.out.println("============================== 第 " + index + " 条 ==============================");

            /* 3.1 Jsoup 精准定位标题：li 下的 h2.fl（class 精确匹配） */
            String title = item.select("h2.fl").first().text().trim();
            System.out.println("【标题】" + title);

            /* 3.2 正则解析发布时间：<i class="fr">2026-09-14</i> */
            String iHtml = item.select("i.fr").first().outerHtml();
            String publishTime = parseTimeByRegex(iHtml);
            System.out.println("【发布时间】" + publishTime);

            /* 3.3 Jsoup 模糊定位链接：a[href*=url_view]（属性值包含 url_view 即可）
             *     再用正则提取三个参数，拼接出真实详情地址 */
            Element aTag = item.select("a[href*=url_view]").first();
            String detailUrl = buildDetailUrl(aTag.attr("href"));
            System.out.println("【详情链接】" + detailUrl);

            /* 3.4 通过 HttpClient(GET) 获取详情页，Jsoup 获取正文 html */
            String detailHtml = getDetailPage(detailUrl);
            if (detailHtml != null) {
                String contentHtml = parseContentHtml(detailHtml);
                System.out.println("【正文html】" + (contentHtml == null ? "解析失败" : contentHtml));
                System.out.println("【正文纯文本】" + (contentHtml == null ? "" : Jsoup.parse(contentHtml).text()));
            } else {
                System.out.println("【正文html】详情页抓取失败");
            }
            System.out.println();
        }
    }

    /**
     * HttpClient POST 抓取列表页（与第一天 demo4 相同参数）
     */
    private static String postListPage(int page) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(LIST_URL);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("listPage", "list"));
            params.add(new BasicNameValuePair("intCurPage", String.valueOf(page)));
            params.add(new BasicNameValuePair("intPageSize", "10"));
            params.add(new BasicNameValuePair("strColId", "20782f569264489f87995ad0773ff626"));
            params.add(new BasicNameValuePair("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d"));
            params.add(new BasicNameValuePair("nowPage", "1"));
            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            httpPost.setHeader("User-Agent", USER_AGENT);
            httpPost.setHeader("Referer", LIST_URL);
            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            httpPost.setHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    /**
     * HttpClient GET 抓取详情页
     */
    private static String getDetailPage(String detailUrl) throws IOException {
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

    /**
     * 正则解析发布时间：匹配 yyyy-MM-dd
     */
    private static String parseTimeByRegex(String html) {
        Matcher matcher = TIME_PATTERN.matcher(html);
        return matcher.find() ? matcher.group() : "未匹配到时间";
    }

    /**
     * 正则提取 url_view('strId','strColId','strWebSiteId') 三个参数，拼接真实详情地址
     */
    private static String buildDetailUrl(String href) {
        Matcher matcher = URL_VIEW_PATTERN.matcher(href);
        if (matcher.find()) {
            return VIEW_URL + "?strId=" + matcher.group(1)
                    + "&strColId=" + matcher.group(2)
                    + "&strWebSiteId=" + matcher.group(3);
        }
        return href;
    }

    /**
     * Jsoup 定位详情正文：
     *   结构上正文位于 <!-- 正文内容 --> 与 <!-- 文后 --> 两个注释之间；
     *   由于正文里的 <p> 嵌套是非法的，jsoup 会自动闭合，导致正文被拆成多个兄弟节点，
     *   因此用注释锚点把两段注释之间的所有元素拼接起来，得到完整正文 html
     */
    private static String parseContentHtml(String detailHtml) {
        Document detailDoc = Jsoup.parse(detailHtml);
        Element articleConca = detailDoc.selectFirst(".article-conca");
        if (articleConca == null) {
            return null;
        }
        StringBuilder contentHtml = new StringBuilder();
        boolean startCollect = false;
        for (org.jsoup.nodes.Node child : articleConca.childNodes()) {
            if (child instanceof org.jsoup.nodes.Comment) {
                String data = ((org.jsoup.nodes.Comment) child).getData();
                if (data.contains("正文内容")) {
                    startCollect = true;   // 遇到正文起点注释，开始收集
                    continue;
                }
                if (data.contains("文后") && startCollect) {
                    break;                 // 遇到文后注释，结束收集
                }
            }
            if (startCollect && child instanceof Element) {
                Element el = (Element) child;
                /* 跳过 jsoup 自动闭合产生的空 <p></p>（无子节点且无文本） */
                if (el.childNodeSize() == 0 && el.text().isEmpty()) {
                    continue;
                }
                contentHtml.append(el.outerHtml());
            }
        }
        return contentHtml.length() == 0 ? null : contentHtml.toString();
    }
}
