package day04;

import day04.Entity.Info;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class test02_json implements PageProcessor {

    private Map<String, String> urlMap = new HashMap<>();

    private final Site site = Site.me()
            .setTimeOut(10000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setCharset("UTF-8")
            .setRetryTimes(3);

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();

        //详细页
        if (url.contains("/art/")) {
            Info info = new Info();
            //详细标题
            String detailTitle = page.getHtml().xpath("//title/text()").get();
            //详细时间
            String pagetime = page.getHtml().xpath("//span[@class='bt-date']/text()").get();
            //正文html
            String detailContent = page.getHtml().xpath("//div[contains(@class,'bt-content')]/html()").get();
            //网站名称
            String sourceName = page.getHtml().xpath("//meta[@name='SiteName']/@content").get();

            //转换格式
            Date pageTime = null;
            if (pagetime != null) {
                try {
                    pageTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(pagetime.trim());
                } catch (Exception e) {
                    try {
                        pageTime = new SimpleDateFormat("yyyy-MM-dd").parse(pagetime.trim());
                    } catch (Exception ignored) {
                    }
                }
            }

            //save
            info.setSOURCE_NAME(sourceName);
            info.setDETAIL_LINK(url);
            info.setDETAIL_TITLE(detailTitle);
            info.setDETAIL_CONTENT(detailContent);
            info.setPAGE_TIME(pageTime);
            info.setCREATE_TIME(new Date());
            info.setLIST_TITLE(detailTitle);
            info.setCREATE_BY("伍芳正");

            page.putField("info", info);

            System.out.println("详情页: " + detailTitle + " \n"+ "时间: " + pageTime);
            return;
        }

        //解析json
        String listHtml = page.getJson().jsonPath("$.data.html").get();
        //转换为html
        Html html = new Html(listHtml);

        //详细链接
        List<String> pageLinks = html.xpath("//table[@class='tableCSS']//a/@href").all();
        //列表标题
        List<String> pageTitles = html.xpath("//table[@class='tableCSS']//a/@title").all();


        //去重
        for (int i = 0; i < pageLinks.size(); i++) {
            //拼接详细链接
            String fullUrl = "http://jnjtj.jinan.gov.cn" + pageLinks.get(i);
            //使用Map集合去重
            if (urlMap.containsKey(fullUrl)) {
                continue;
            }
            //不重复
            urlMap.put(fullUrl, pageTitles.get(i));
            //添加下机链接
            page.addTargetRequest(fullUrl);
        }

        //翻页
        String countStr = listHtml.replaceAll("(?s).*?count=\"(\\d+)\".*", "$1");
        String rowsStr = listHtml.replaceAll("(?s).*?rows=\"(\\d+)\".*", "$1");

            int count = Integer.parseInt(countStr);
            int rows = Integer.parseInt(rowsStr);
            int totalPages = (count + rows - 1) / rows;
            for (int p = 1; p <= totalPages; p++) {
                    Request req = new Request("http://jnjtj.jinan.gov.cn/api-gateway/jpaas-publish-server/front/page/build/unit"
                            + "?parseType=bulidstatic&webId=22&tplSetId=vJkf1pHFnRNmHKHxZtXdK"
                            + "&pageType=column&tagId=%E4%BF%A1%E6%81%AF%E5%88%97%E8%A1%A8"
                            + "&editType=null&pageId=57329"+ p);
                    req.putExtra("pageNo", p);
                    page.addTargetRequest(req);
                }


    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new test02_json())
                .addUrl("http://jnjtj.jinan.gov.cn/api-gateway/jpaas-publish-server/front/page/build/unit?parseType=bulidstatic&webId=22&tplSetId=vJkf1pHFnRNmHKHxZtXdK&pageType=column&tagId=%E4%BF%A1%E6%81%AF%E5%88%97%E8%A1%A8&editType=null&pageId=57329")
                .addPipeline(new OraclePipeline())
                .thread(2)
                .run();
    }
}