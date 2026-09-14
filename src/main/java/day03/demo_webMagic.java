package day03;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 *
 * 第三天练习：熟悉 webmagic 基本框架 —— 四大组件
 *
 *   1. Downloader（下载器）：负责把 URL 对应的页面抓下来（HTTP 请求）。
 *      本 demo 不显式设置，使用 webmagic 内置的 DefaultDownloader（HttpClient 实现）。
 *
 *   2. PageProcessor（页面处理器）：四大组件中最核心、必须自己写的一个。
 *      职责：解析页面 -> 用 putField 提取数据 -> 用 addTargetRequests 把新链接交给调度器。
 *      本 demo 中 DemoPageProcessor 负责：列表页解析标题/时间/链接，详情页解析正文 html。
 *
 *   3. Scheduler（调度器）：管理待抓取的 URL 队列，并做去重。
 *      默认是 QueueScheduler（内存队列 + 哈希去重），这里显式写出来以说明组件。
 *
 *   4. Pipeline（管道）：接收 PageProcessor 抽取出的结果（ResultItems）并处理，
 *      比如控制台打印、存文件、存数据库。本 demo 中 DemoPipeline 负责控制台打印。
 *
 * 练习网站：安徽省港航集团 - 通知通告
 *   列表页：POST http://www.ahsgh.com/ahghjtweb/web/list
 *   详情页：GET  http://www.ahsgh.com/ahghjtweb/web/view?strId=...
 */
public class demo_webMagic {

    /* ===== 练习网站常量 ===== */
    private static final String LIST_URL = "http://www.ahsgh.com/ahghjtweb/web/list";
    private static final String VIEW_URL = "http://www.ahsgh.com/ahghjtweb/web/view";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /* 正则：从 javascript:url_view('strId','strColId','strWebSiteId') 中提取三个参数 */
    private static final Pattern URL_VIEW_PATTERN =
            Pattern.compile("url_view\\('(.*?)',\\s*'(.*?)',\\s*'(.*?)'\\)");

    /* 正则：从翻页 URL 中提取当前页码（GET 翻页时 URL 带 intCurPage 参数） */
    private static final Pattern CUR_PAGE_PATTERN = Pattern.compile("intCurPage=(\\d+)");

    /**
     * 本次演示爬取的页数（不是条数）。
     *   3   = 只爬前 3 页（演示分页用，跑得快）
     *   -1  = 全量爬取（自动使用站点分页信息里的总页数，共 222 页，会爬很久）
     */
    private static final int MAX_PAGES = 3;

    public static void main(String[] args) {
        /* ===== 1. PageProcessor：页面处理器（自己实现） ===== */
        DemoPageProcessor processor = new DemoPageProcessor();

        /* ===== 2. Pipeline：管道（自己实现，控制台打印） ===== */
        DemoPipeline pipeline = new DemoPipeline();

        /* ===== 3. Scheduler：调度器（显式指定；不写默认就是 QueueScheduler） ===== */
        QueueScheduler scheduler = new QueueScheduler();

        /* ===== 4. Downloader：下载器（不设置，用内置 DefaultDownloader） ===== */

        /* 组装 Spider：四大组件 + 初始请求（列表页是 POST 请求） + 线程数 */
        Spider spider = Spider.create(processor)          // 绑定 PageProcessor
                .addPipeline(pipeline)                    // 绑定 Pipeline
                .setScheduler(scheduler)                  // 绑定 Scheduler
                .thread(1)                                // 单线程，练习网站访问间隔已由 Site 控制
                .addRequest(buildListRequest());          // 初始 URL：列表页（POST）

        System.out.println("========== Spider 启动 ==========");
        spider.run();                                     // 同步运行，抓完为止
        System.out.println("========== Spider 运行结束 ==========");
    }

    /**
     * 构建列表页 POST 请求（webmagic 的 Request 支持设置 Method 和请求体）
     */
    private static Request buildListRequest() {
        Request request = new Request(LIST_URL);
        request.setMethod(HttpConstant.Method.POST);
        /* 表单参数：HttpRequestBody.form 会自动按 application/x-www-form-urlencoded 编码 */
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("listPage", "list");
        form.put("intCurPage", "1");
        form.put("intPageSize", "10");
        form.put("strColId", "20782f569264489f87995ad0773ff626");
        form.put("strWebSiteId", "4c5fcf57602b48a0acde5a4ef3ede48d");
        form.put("nowPage", "1");
        request.setRequestBody(HttpRequestBody.form(form, "UTF-8"));
        return request;
    }

    /**
     * 构建第 page 页列表页的 GET 翻页 URL（第一页用 POST，后续页用 GET 也能翻页，
     * 且每页 URL 不同，不会被 Scheduler 去重掉）
     */
    private static String buildListPageUrl(int page) {
        return LIST_URL + "?listPage=list&intCurPage=" + page
                + "&intPageSize=10&strColId=20782f569264489f87995ad0773ff626"
                + "&strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d&nowPage=" + page;
    }

    /** 从 URL 中解析当前页码（POST 首页 URL 不带 intCurPage，默认第 1 页） */
    private static int parsePageFromUrl(String url) {
        Matcher matcher = CUR_PAGE_PATTERN.matcher(url);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    /* =====================================================================
     * 四大组件之一：PageProcessor（页面处理器）
     *   - getSite()：站点配置（编码、UA、请求头、重试、访问间隔）
     *   - process(Page)：解析页面 -> putField 提取数据 -> addTargetRequests 发现新链接
     * ===================================================================== */
    static class DemoPageProcessor implements PageProcessor {

        private final Site site = Site.me()
                .setRetryTimes(3)                                          // 请求失败重试 3 次
                .setSleepTime(1500)                                        // 两次请求间隔 1.5s，避免被拒
                .setUserAgent(USER_AGENT)
                .setCharset("UTF-8")                                       // 页面编码
                .addHeader("Referer", LIST_URL);                           // POST 请求体由 HttpRequestBody.form 自动带 Content-Type

        @Override
        public Site getSite() {
            return site;
        }

        @Override
        public void process(Page page) {
            /* 根据 URL 区分是列表页还是详情页 */
            if (page.getUrl().toString().startsWith(VIEW_URL)) {
                processDetail(page);
            } else {
                processList(page);
            }
        }

        /** 列表页：解析标题 / 时间 / 详情链接，详情链接与后续分页交给 Scheduler 入队 */
        private void processList(Page page) {
            /* 当前是第几页：POST 首页 URL 不带参数 → 1；GET 翻页 URL 带 intCurPage */
            int curPage = parsePageFromUrl(page.getUrl().toString());

            /* webmagic 的 Selectable 支持 xpath / css / regex 三种选择器，与第二天学的正好对上 */
            List<String> titles = page.getHtml()
                    .xpath("//ul[contains(@class,'tab01open')]//li//h2[contains(@class,'fl')]/text()").all();
            List<String> times = page.getHtml()
                    .xpath("//ul[contains(@class,'tab01open')]//li//i[contains(@class,'fr')]/text()").all();
            List<String> hrefs = page.getHtml()
                    .xpath("//ul[contains(@class,'tab01open')]//li//a[contains(@href,'url_view')]/@href").all();

            /* 抽取的数据用 putField 交给 Pipeline */
            page.putField("页面", "列表页");
            page.putField("当前页码", curPage);
            page.putField("标题列表", titles);
            page.putField("时间列表", times);

            /* 正则提取 url_view 三个参数，拼接真实详情地址 */
            List<String> detailUrls = new ArrayList<>();
            for (String href : hrefs) {
                Matcher matcher = URL_VIEW_PATTERN.matcher(href);
                if (matcher.find()) {
                    detailUrls.add(VIEW_URL + "?strId=" + matcher.group(1)
                            + "&strColId=" + matcher.group(2)
                            + "&strWebSiteId=" + matcher.group(3));
                }
            }
            /* 新链接交给 Scheduler 入队，Downloader 会继续去抓 */
            page.addTargetRequests(detailUrls);

            /* 分页：只有第 1 页去解析总页数，并把后续页面入队（避免重复入队死循环） */
            if (curPage == 1) {
                /* 站点分页：<span class="pagenub">页次<i>1</i>/222</span>，<i> 里是当前页、总页数在 </i> 后；
                 * 更稳的做法：取「尾页」链接 javascript:gotoPage(222); 里的数字 */
                String lastPageHref = page.getHtml()
                        .xpath("//a[@title='尾页']/@href").toString();   // javascript:gotoPage(222);
                int totalPages = Integer.parseInt(lastPageHref.replaceAll("\\D", ""));  // 只留数字 → 222
                int limit = MAX_PAGES < 1 ? totalPages : Math.min(MAX_PAGES, totalPages);
                System.out.println(">>> 站点总页数：" + totalPages + "，本次爬取前 " + limit + " 页");
                for (int p = 2; p <= limit; p++) {
                    page.addTargetRequest(buildListPageUrl(p));
                }
            }

            System.out.println(">>> 第 " + curPage + " 页解析到 " + titles.size() + " 条记录，发现 "
                    + detailUrls.size() + " 条详情链接");
        }

        /** 详情页：提取标题 / 发布时间 / 正文 html */
        private void processDetail(Page page) {
            page.putField("页面", "详情页");

            /* css 选择器取标题 */
            page.putField("标题", page.getHtml().css(".article-conca h2", "text").toString().trim());

            /* 发布时间：先取 p.article-information 下第一个 span 的文本，再用 regex 抽出日期 */
            String publishTime = page.getHtml()
                    .css("p.article-information span", "text")
                    .regex("\\d{4}-\\d{2}-\\d{2}").toString();
            page.putField("发布时间", publishTime);

            /* 正文 html：p 嵌套会被 jsoup 自动闭合拆散成多个直接子 <p>，
             * Xsoup 不支持 position() 函数，这里选出全部直接子 <p>，在 Java 里过滤掉空壳节点再拼接 */
            List<String> allPs = page.getHtml()
                    .xpath("//div[contains(@class,'article-conca')]/p").all();
            List<String> contentPs = new ArrayList<>();
            for (String p : allPs) {
                String trimmed = p.trim();
                if (trimmed.isEmpty() || trimmed.equals("<p></p>")) {
                    continue;   // 跳过 jsoup 自动闭合产生的空 <p></p>
                }
                contentPs.add(p);
            }
            page.putField("正文html", String.join("", contentPs));
        }
    }

    /* =====================================================================
     * 四大组件之一：Pipeline（管道）
     *   - process(ResultItems, Task)：拿到 PageProcessor 抽取的结果，这里做控制台打印
     * ===================================================================== */
    static class DemoPipeline implements Pipeline {

        @Override
        public void process(ResultItems resultItems, Task task) {
            System.out.println("┌────────── Pipeline 收到一页结果 ──────────");
            System.out.println("│ 下载地址 = " + resultItems.getRequest().getUrl());
            for (Map.Entry<String, Object> entry : resultItems.getAll().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    System.out.println("│ " + entry.getKey() + "（共 " + list.size() + " 条）");
                    for (Object item : list) {
                        System.out.println("│   - " + item);
                    }
                } else {
                    System.out.println("│ " + entry.getKey() + " = " + value);
                }
            }
            System.out.println("└────────────────────────────────────────────");
        }
    }
}
