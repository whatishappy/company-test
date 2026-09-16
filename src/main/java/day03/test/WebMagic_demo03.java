package day03.test;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebMagic_demo03 implements PageProcessor {

    private final Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setTimeOut(10 * 1000)
            .setCharset("UTF-8")
            .addHeader("Referer", "https://mall.gdaee.com.cn/info/list/127.html")
            .addHeader("Origin", "https://mall.gdaee.com.cn")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Accept", "text/html, */*; q=0.01")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

    @Override
    public void process(Page page) {
        String url = page.getUrl().get();

        if (url.contains("/info/detail/")) {
            String title = first(page, "h1", ".title", ".article-title", ".detail-title");
            String time = first(page, ".time", ".date", ".news_time", ".publish-time");
            String content = first(page, "div.content", "div.article", "div.detail", "div#content", "div.main-content");

            System.out.println("标题：" + title);
            System.out.println("公布时间：" + time);
            System.out.println("正文链接：" + url);
            System.out.println("正文详细：" + content);
            if (content == null) {
                System.out.println("未找到正文，所有 div 的 class 如下：");
                System.out.println(page.getHtml().css("div", "class").all());
            }
            System.out.println("--------------------------------");
            return;
        }

        List<String> links = page.getHtml().css("span.fl a", "href").all();
        for (String link : links) {
            String full = link.startsWith("http") ? link : "https://mall.gdaee.com.cn" + link;
            page.addTargetRequest(full);
        }
    }

    private String first(Page page, String... selectors) {
        for (String s : selectors) {
            String v = page.getHtml().css(s, "text").get();
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider spider = Spider.create(new WebMagic_demo03()).thread(1);
        for (int i = 1; i <= 5; i++) {
            Request request = new Request("https://mall.gdaee.com.cn/page/s/info/list_li");
            request.setMethod(HttpConstant.Method.POST);
            Map<String, Object> hm = new HashMap<>();
            hm.put("id", "127");
            hm.put("pageNo", String.valueOf(i));
            hm.put("pageSize", "20");
            request.setRequestBody(HttpRequestBody.form(hm, "UTF-8"));
            spider.addRequest(request);
        }
        spider.run();
    }
}