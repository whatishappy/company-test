package day02.test.Xpathtest;


import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import static day02.demo_Jsoup.getDetailPage;
import static day02.demo_Jsoup.parseContentHtml;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-15
 * @Version: 1.0
 */


public class test01 {
    public static void main(String[] args) {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        //发起httpclien请求获取网页
        HttpPost post = new HttpPost("http://www.ahsgh.com/ahghjtweb/web/list?strColId=20782f569264489f87995ad0773ff626&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d");
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("listPage", "list"));
        params.add(new BasicNameValuePair("intCurPage", "1"));
        params.add(new BasicNameValuePair("intPageSize", "10"));
        params.add(new BasicNameValuePair("strColId", "20782f569264489f87995ad0773ff626"));
        params.add(new BasicNameValuePair("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d"));
        params.add(new BasicNameValuePair("nowPage", "1"));

        String html = null;

        try(CloseableHttpResponse resp = httpclient.execute(post)){
            html = EntityUtils.toString(resp.getEntity(), "utf-8");
        }catch (Exception e) {
            e.printStackTrace();
        }

        //使用jsoup解析
        Document doc = Jsoup.parse(html);

        //解析dom对象，获取其中的“标题”、“发布时间”、“详细链接”、“详细正文”

        //获取每个li 下的h2 fl元素
        Elements elements1 = doc.selectXpath("//h2[contains(@class,'fl')]");
        ArrayList<String > titles = new ArrayList<>();
        System.out.println("获取当前页中列表数:" + elements1.size() );
        System.out.println("=============================================================================");
        elements1.forEach(s->{
            titles.add(s.text());
        });
        titles.forEach(System.out::println);

        //获取详情页面中的信息：“发布时间”、“正文详细”、“详细链接”



    }

    private static String handleOneDetail(int idx, String title, String publishTime, String detailUrl) {

        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n========== 第 ").append(idx).append(" 条 ==========\n");
        sb.append("【标题】：").append(title).append('\n');
        sb.append("【发布时间】：").append(publishTime).append('\n');
        sb.append("【详情链接】：").append(detailUrl).append('\n');

        //打印正文html或正文文本
        try {
            String detailHtml = getDetailPage(detailUrl);
            if (detailHtml != null) {
                String contentHtml = parseContentHtml(detailHtml);
                /*sb.append("【正文html】")
                        .append(contentHtml == null ? "解析失败" : contentHtml).append('\n');*/
                sb.append("【正文详细】：")
                        .append(contentHtml == null ? "" : Jsoup.parse(contentHtml).text()).append('\n');
            } else {
                sb.append("【正文html】详情页抓取失败\n");
            }
        } catch (Exception e) {
            sb.append("【异常】").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }
}
