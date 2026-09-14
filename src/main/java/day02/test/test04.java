package day02.test;


import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
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
 */


public class test04 {
    
    //测试抓当前页面标题
    public static void main(String[] args) throws IOException {
        //配置链接


        // 1. 创建一个 CookieStore（用来存服务器发过来的 Cookie）
        CookieStore cookieStore = new BasicCookieStore();

// 2. 创建 HttpClient 时，把这个 CookieStore 放进去

        /*创建httpclient*/
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)  // <--- 关键：让 HttpClient 自动管理 Cookie
                .build();;

        HttpPost httpPost = new HttpPost(AboutURL.URL);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("listPage", "list"));
        params.add(new BasicNameValuePair("intCurPage", "1"));
        params.add(new BasicNameValuePair("intPageSize", "10"));
        params.add(new BasicNameValuePair("strColId", "20782f569264489f87995ad0773ff626"));
        params.add(new BasicNameValuePair("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d"));
        params.add(new BasicNameValuePair("nowPage", "1"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);



        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\"");
        httpPost.setHeader("Referer", "http://www.ahsgh.com/ahghjtweb/web/list");
        httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        httpPost.setHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                //System.out.println(html);

                /*解析页面中的项目名称*/
                Elements projectTitle = Jsoup.parse(html).select("h2.fl");
                projectTitle.forEach(s-> System.out.println(s.text()));

                String attr = element.attr("href");
                /*将url_view中的数据提取出来*/
                Pattern pattern = Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");
                Matcher matcher = pattern.matcher(attr);

                if (matcher.find()) {
                    /*如果符合正则表达式，提取三个参数*/
                    String strId = matcher.group(1);
                    String strColId = matcher.group(2);
                    String strWebSiteId = matcher.group(3);

                    /*拼接真实访问地址*/
                    String resultUrl = "https://www.ahsgh.com/ahghjtweb/web/view?strId=" + strId
                            + "&strColId=" + strColId
                            + "&strWebSiteId=" + strWebSiteId;
                    System.out.println(resultUrl);
                }

                System.out.println("==================================");
                /*解析页面中详细链接*/
                Elements elements = Jsoup.parse(html).select("ul.tab01open.tab01tca li a");

                for (Element element : elements) {

                }



            } else if (statusCode == 403 || statusCode == 429) {
                System.err.println("被拒绝访问，状态码：" + statusCode + "，建议更换ip或者增大访问间隔");
            } else {
                System.err.println("请求失败，状态码：" + statusCode);
            }
        }
        
        

    }
}
