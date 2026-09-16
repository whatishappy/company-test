package day03.test;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebMagic_demo02 implements PageProcessor {
    private final Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setTimeOut(10*1000)
            .setCharset("UTF-8")
            ;
    @Override
    public void process(Page page) {
        Matcher m = Pattern.compile("list-2037-(\\d+)\\.html").matcher(page.getUrl().get());
        if (!m.find()) {
            //没找到该页面
            return;
        }
        int pageNo = Integer.parseInt(m.group(1));
        String s = page.getHtml().xpath("//span[@id='Label2']/text()").get();
        List<String> titles = page.getHtml().xpath("//ul[@id='newslist']//div[1]//a/text()").all();
        List<String> links  = page.getHtml().xpath("//ul[@id='newslist']//li").links().all();
        List<String> pubTime = page.getHtml().xpath("//ul[@id='newslist']//div[2]/text()").all();

        StringBuilder sb = new StringBuilder();
        sb.append("第").append(pageNo).append("页————").append(s).append("\n");


        for (int i = 0; i < titles.size(); i++) {
            sb.append("标题:").append(titles.get(i)).append("\n");
            sb.append("公布时间:").append(pubTime.get(i)).append("\n");
            sb.append("详细连接:").append(links.get(i)).append("\n");
        }

        System.out.println(sb);

        List<String> pages = page.getHtml().links().regex(".*/list-2037-\\d+\\.html").all();
        for (String p : pages) {
            page.addTargetRequest(p);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        Spider.create(new WebMagic_demo02())
                .addUrl("http://www.hnsfzyyy.com/list-2037-1.html")
                .thread(1)
                .run();
    }
}