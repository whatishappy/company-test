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

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-16
 * @Version: 1.0
 */


public class WebMagic_demo4 implements PageProcessor {

    private Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
            .setCharset("utf-8")
            .setRetryTimes(3)
            .setTimeOut(10*1000);

    @Override
    public void process(Page page) {
        //获取列表页中的链接后缀
        List<String> baseurl = page.getHtml().css("span.fl a", "href").all();

        //详情链接

        for (String s : baseurl) {
            StringBuilder sb = new StringBuilder();
            sb.append("https://mall.gdaee.com.cn").append(s);
            page.addTargetRequest(sb.toString());
        }

        String detailurl = page.getUrl().get();
        if (detailurl.contains("info/detail/")) {
            //所有标题
            List<String> titles = page.getHtml().css("div.new_detail_titletext","text").all();
            //所有公布时间
            List<String> pubTime = page.getHtml().css("span.lightgrey", "text").all();
            /*//暴力获取详细页中所有的span标签，但是出现不在同行的情况
            List<String> all = page.getHtml().css("div.new_detail_cont span", "text").all();
            for (String s : all) {
                System.out.println(s);
            }*/

            for (int i = 0; i < titles.size(); i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("项目名称:").append(titles.get(i)).append("\n")
                        .append("公布时间：").append(pubTime.get(i)).append("\n")
                        .append("正文详情链接： ").append(detailurl).append("\n")
                        .append("正文详情：").append(page.getHtml().smartContent().get());
                System.out.println(sb);
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        Spider spider = Spider.create(new WebMagic_demo4());

        for (int i = 1; i <=3; i++) {
            Request request = new Request("https://mall.gdaee.com.cn/page/s/info/list_li");
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("id", "127");
            hm.put("pageNo",String.valueOf(i));
            hm.put("pageSize", "20");
            HttpRequestBody body = HttpRequestBody.form(hm, "UTF-8");
            request.setRequestBody(body);
            request.setMethod(HttpConstant.Method.POST);
            spider.addRequest(request);
        }

        spider.thread(2).run();






    }
}
