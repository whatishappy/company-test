package day02;

import cn.wanghaomiao.xpath.model.JXDocument;
import cn.wanghaomiao.xpath.model.JXNode;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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
 * 第二天练习 —— XPath 解析方式
 *   通过 HttpClient 抓取列表页 / 详情页，使用 XPath（JsoupXpath 库）解析：
 *     列表标题、发布时间、详情链接、详情正文 html
 *   对比点：
 *     精准定位：@class='fl'   （属性值完全相等）
 *     模糊定位：contains(@class,'fl')、contains(@href,'url_view') （属性包含）
 *   XPath 语法：/ 直接子节点，// 任意子孙节点，@ 属性，[] 条件，text() 文本
 */
public class XPathDemo {

    private static final String LIST_URL = "http://www.ahsgh.com/ahghjtweb/web/list";       //列表页
    private static final String VIEW_URL = "http://www.ahsgh.com/ahghjtweb/web/view";       //项目详情页
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /* 正则：从 url_view('strId','strColId','strWebSiteId') 提取三个参数拼真实地址
       （链接参数提取属于字符串拼接，用正则最直接） */
    private static final Pattern URL_VIEW_PATTERN =
            Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");

    public static void main(String[] args) throws Exception {
        /* 1. HttpClient 抓取列表页 */
        String listHtml = postListPage(3);
        if (listHtml == null || listHtml.isEmpty()) {
            System.err.println("列表页抓取失败，程序退出");
            return;
        }
        System.out.println("=================== XPath 解析：列表页抓取成功，内容长度：" + listHtml.length() + " ===================");

        /* 2. XPath 解析列表页（JXDocument 可以直接接收 HTML 字符串） */
        JXDocument listJx = new JXDocument(listHtml);

        /* 2.1 模糊定位：ul 的 class 包含 tab01open（多值属性用 contains） */
        List<JXNode> items = listJx.selN("//ul[contains(@class,'tab01open')]//li");

        /* 精准定位与模糊定位对比：
         *   精准：//h2[@class='fl']
         *   模糊：//h2[contains(@class,'fl')]  class 里包含 fl 即可 */
        List<JXNode> preciseTitles = listJx.selN("//ul[contains(@class,'tab01open')]//li//h2[@class='fl']");
        List<JXNode> fuzzyTitles = listJx.selN("//ul[contains(@class,'tab01open')]//li//h2[contains(@class,'fl')]");
        System.out.println("精准定位 h2[@class='fl'] 数量：" + preciseTitles.size()
                + "，模糊定位 h2[contains(@class,'fl')] 数量：" + fuzzyTitles.size());

        List<JXNode> times = listJx.selN("//ul[contains(@class,'tab01open')]//li//i[contains(@class,'fr')]");
        List<JXNode> links = listJx.selN("//ul[contains(@class,'tab01open')]//li//a[contains(@href,'url_view')]");

        System.out.println("本次共解析到 " + items.size() + " 条记录\n");

        /* 3. 遍历每一条：XPath 解析 标题 / 发布时间 / 详情链接，并抓详情页解析正文 html */
        for (int i = 0; i < items.size(); i++) {
            System.out.println("============================== 第 " + (i + 1) + " 条 ==============================");

            /* 3.1 XPath 标题：取元素节点后 getElement().text() */
            String title = fuzzyTitles.get(i).getElement().text().trim();
            System.out.println("【标题】" + title);

            /* 3.2 XPath 发布时间 */
            String publishTime = times.get(i).getElement().text();
            System.out.println("【发布时间】" + publishTime);

            /* 3.3 XPath 详情链接：getElement().attr("href") 拿到 url_view 串，
             *     再用正则拼接出真实地址 */
            String href = links.get(i).getElement().attr("href");
            String detailUrl = buildDetailUrl(href);
            System.out.println("【详情链接】" + detailUrl);

            /* 3.4 HttpClient 抓详情页 + XPath 解析标题/发布时间/正文 html */
            String detailHtml = getDetailPage(detailUrl);
            if (detailHtml == null) {
                System.out.println("【正文html】详情页抓取失败");
                continue;
            }
            JXDocument detailJx = new JXDocument(detailHtml);

            /* 详情标题：div.article-conca 下的 h2 */
            String detailTitle = detailJx.selNOne("//div[contains(@class,'article-conca')]/h2").getElement().text().trim();
            /* 详情发布时间：p.article-information 下的第一个 span */
            String detailTime = detailJx.selNOne("//div[contains(@class,'article-conca')]//p[contains(@class,'article-information')]/span[1]").getElement().text();
            System.out.println("【详情页标题】" + detailTitle);
            System.out.println("【详情页发布时间】" + detailTime);

            /* 详情正文 html：
             *   正文里的 <p> 嵌套是非法 HTML，jsoup 解析时会把外层 p 自动闭合，
             *   正文被拆成多个兄弟 <p>，第一个是空壳，所以取 p[position()>1] 全部拼接 */
            StringBuilder contentHtml = new StringBuilder();
            List<JXNode> contentPs = detailJx.selN("//div[contains(@class,'article-conca')]/p[position()>1]");
            for (JXNode p : contentPs) {
                contentHtml.append(p.getElement().outerHtml());
            }
            System.out.println("【正文html】" + contentHtml);
            System.out.println();
        }
    }

    /* HttpClient POST 抓取列表页 */
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
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    /* HttpClient GET 抓取详情页 */
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

    /* 正则提取 url_view 三个参数，拼接真实详情地址 */
    private static String buildDetailUrl(String href) {
        Matcher matcher = URL_VIEW_PATTERN.matcher(href);
        if (matcher.find()) {
            return VIEW_URL + "?strId=" + matcher.group(1)
                    + "&strColId=" + matcher.group(2)
                    + "&strWebSiteId=" + matcher.group(3);
        }
        return href;
    }
}
