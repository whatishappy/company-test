package day04;


import org.apache.http.protocol.HTTP;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-16
 * @Version: 1.0
 */


public class demo02_oracel_jbc implements PageProcessor {

    private Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setTimeOut(10000)
            .setDisableCookieManagement(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

    @Override
    public void process(Page page) {

        // 判断是列表页还是详情页
        if (page.getUrl().toString().contains("content/post_")) {
            //如果是详细页
            String title = page.getHtml().xpath("//div[@class='con']/h3/text()").get();     //DETAIL_TITLE
            String time  = page.getHtml().xpath("//span[@class='time']/text()").get();      //DETAIL_TIME
            String content = page.getHtml().xpath("//div[@class='article']/html()").get();  //DETAIL_CONTENT

            //存入Filed集合中
            page.putField("DETAILTITLE", title);        //详细标题
            page.putField("DETAILTIME", time == null ? "" : time.replace("时间：", ""));       //详细时间
            page.putField("DETAILCONTENT", content);        //详细文本
            page.putField("DETAILLINK", page.getUrl().toString());      //详细链接
            page.putField("LISTTITLE", title);  // 详情页标题作为列表标题入库        //列表标题
        } else {
            List<String> links = page.getHtml()
                    .xpath("//ul[@class='list']/li/a/@href").all();
            page.addTargetRequests(links);

        }
    }

    @Override
    public Site getSite() {
        return site;
    }



    public class OraclePipeline implements PageProcessor {

        private static final String URL = "jdbc:oracle:thin:@192.168.2.42:1521:orcl";
        private static final String USER = "bxkc";
        private static final String PASS = "bxkc";

        @Override
        public void process(Page page) {

        }

    }


    public static void main(String[] args) {


        Request re = new Request("https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/");

        //添加请求头
        re.addHeader("Referer", "https://www.gdwc.gov.cn/");
        re.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        Spider.create(new demo02_oracel_jbc())
                .addRequest(re)
                .thread(1)
                .run();
    }
}
