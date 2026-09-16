package day03.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebMagic_demo01 implements PageProcessor {

    private static final Pattern URL_VIEW_PATTERN =
            Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");       //提取url_view中的参数正则

    private static final String LIST_URL = "http://www.ahsgh.com/ahghjtweb/web/list";       //列表URL
    private static final String VIEW_URL = "http://www.ahsgh.com/ahghjtweb/web/view";       //详情页面URL


    private static final int MAX_PAGE = 3;

    private final Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(100)
            .setTimeOut(10 * 1000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0");

    @Override
    public void process(Page page) {
        String url = page.getUrl().get();
        if (url.contains("/web/list")) {
            processListPage(page);
        } else if (url.contains("/web/view")) {
            processDetailPage(page);
        }
    }

    private void processListPage(Page page) {

        Object extra = page.getRequest().getExtra("curPage");
        int curPage = extra == null ? 1 : (Integer) extra;

        System.out.println("\n========== 第 " + curPage + " 页 ==========");

        List<String> titles = page.getHtml()
                .xpath("//ul[contains(@class,'tab01open')]//li//h2[contains(@class,'fl')]/text()")
                .all();
        List<String> times = page.getHtml()
                .xpath("//ul[contains(@class,'tab01open')]//li//i[contains(@class,'fr')]/text()")
                .all();
        List<String> hrefs = page.getHtml()
                .xpath("//ul[contains(@class,'tab01open')]//li//a[contains(@href,'url_view')]/@href")
                .all();

        int n = Math.min(titles.size(), Math.min(times.size(), hrefs.size()));  //可能有点数据没有详情链接，需要做最小值判断

        for (int i = 0; i < n; i++) {
            String detailUrl = buildDetailUrl(hrefs.get(i));
            if (detailUrl == null) continue;

            Request req = new Request(detailUrl);
            req.putExtra("title", titles.get(i));
            req.putExtra("publishTime", times.get(i));
            page.addTargetRequest(req);
        }

        // 翻页：投递下一页列表请求
        if (curPage < MAX_PAGE) {
            int next = curPage + 1;
            String nextUrl = LIST_URL
                    + "?listPage=list"
                    + "&intCurPage=" + next
                    + "&intPageSize=10"
                    + "&strColId=20782f569264489f87995ad0773ff626"
                    + "&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d"
                    + "&nowPage=" + next;

            Request nextReq = new Request(nextUrl);
            nextReq.putExtra("curPage", next);
            page.addTargetRequest(nextReq);     //发送新的request请求
        }
    }

    private void processDetailPage(Page page) {

        String title       = page.getRequest().getExtra("title");
        String publishTime = page.getRequest().getExtra("publishTime");
        String contentText = parseContentText(page);

        StringBuilder sb = new StringBuilder();
        sb.append("【标题】：").append(title).append('\n');
        sb.append("【发布时间】：").append(publishTime).append('\n');
        sb.append("【详情链接】：").append(page.getUrl()).append('\n');
        sb.append("【正文详细】：").append(contentText).append("\n\n");

        System.out.print(sb);
    }

    private static String parseContentText(Page page) {

        Document doc = Jsoup.parse(page.getHtml().toString());
        Element articleConca = doc.selectFirst(".article-conca");
        if (articleConca == null) return "";

        StringBuilder contentHtml = new StringBuilder();
        boolean startCollect = false;

        for (org.jsoup.nodes.Node child : articleConca.childNodes()) {
            if (child instanceof Comment) {
                String data = ((Comment) child).getData();
                if (data.contains("正文内容")) {
                    startCollect = true;
                    continue;
                }
                if (data.contains("文后") && startCollect) break;
            }
            if (startCollect && child instanceof Element) {
                Element el = (Element) child;
                if (el.childNodeSize() == 0 && el.text().isEmpty()) continue;
                contentHtml.append(el.outerHtml());
            }
        }

        if (contentHtml.length() == 0) return "";
        return Jsoup.parse(contentHtml.toString()).text();
    }

    private static String buildDetailUrl(String href) {
        if (href == null) return null;
        Matcher m = URL_VIEW_PATTERN.matcher(href);
        if (m.find()) {
            return VIEW_URL + "?strId=" + m.group(1)
                    + "&strColId=" + m.group(2)
                    + "&strWebSiteId=" + m.group(3);
        }
        return null;
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        String url = LIST_URL
                + "?listPage=list"
                + "&intCurPage=1"
                + "&intPageSize=10"
                + "&strColId=20782f569264489f87995ad0773ff626"
                + "&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d"
                + "&nowPage=1";

        Request first = new Request(url);
        first.putExtra("curPage", 1);

        Spider.create(new WebMagic_demo01())
                .addRequest(first)
                .thread(1)
                .run();
    }
}