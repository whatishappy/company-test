package day04;

import day04.Entity.Info;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.StringReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

public class demo1 implements PageProcessor {

    //全局map， Map<列表链接，列表标题>
    private static Map<String, String> visitedLinks = new ConcurrentHashMap<>();

    //全局统计总爬取数量
    private static Integer Pagecount=0;

    private final Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setTimeOut(10000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

    static AtomicInteger countsql = new AtomicInteger(0);
    static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void process(Page page) {
        /**
         *将获取的数据通过set方法存入Info对象中
         * 然后再使用putFiled保存
         * PipeLine直接通过"info"取出
         */

        System.out.println("正在解析链接：" + page.getUrl().get());

        if (page.getUrl().toString().contains("content/post_")) {
            System.out.println("当前解析");

            //获取详细标题
            String title = page.getHtml().xpath("//div[@class='con']/h3/allText()").get();    //有的标题存放在<h3>标签中的<strong>标签，需要获取全部文本
            //获取详细正文html
            String content = page.getHtml().xpath("//div[@class='article']/html()").get();
            //详细时间
            String timeStr = page.getHtml().xpath("//div[@class='massage clearfix']//span[@class='time']/text()").get();
            //网站名称
            String sourceName = page.getHtml().xpath("//meta[@name='SiteName']/@content").get();

            // 提取有效时间
            String cleanDate = "";
            if (timeStr != null) {
                Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                Matcher m = p.matcher(timeStr);
                if (m.find()) {
                    cleanDate = m.group();
                }
            }

            String detailLink = page.getUrl().toString();
            // 从全局 Map 中获取列表页的标题
            String listTitle = visitedLinks.get(detailLink);
            if (listTitle == null || listTitle.isEmpty()) {
                listTitle = title;
            }

            //封装save
            Info info = new Info();
            info.setSOURCE_NAME(sourceName);
            info.setDETAIL_LINK(detailLink);
            info.setDETAIL_TITLE(title);
            info.setDETAIL_CONTENT(content);
            info.setLIST_TITLE(listTitle);
            info.setCREATE_BY("伍芳正");

            // 日期字符串转 Date 对象
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                if (!cleanDate.isEmpty()) {
                    info.setPAGE_TIME(sdf.parse(cleanDate));
                    info.setCREATE_TIME((sdf.parse(cleanDate)));
                } else {
                    info.setPAGE_TIME(new Date());
                    info.setCREATE_TIME(new Date());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // 只提交一个 info 对象
            page.putField("info", info);

            //解析成功计数器+1
            count.incrementAndGet();
            System.out.println("解析成功!已经解析了"+count+"条数据!");
        } else {
            // 获取列表页所有的链接和标题
            List<String> links = page.getHtml().xpath("//ul[@class='list']//li//a/@href").all();
            List<String> titleList = page.getHtml().xpath("//ul[@class='list']//li//a/text()").all();

            ArrayList<String> newLinks = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) {
                String link = links.get(i);
                String listTitle = (i < titleList.size()) ? titleList.get(i) : "";
                // 过滤外站链接（https://mp.weixin.qq.com/s/K5Dav0tvh1Bgkg-IjeuYJQ）和非详情页

                if (!link.startsWith("http")) {
                    if (link.startsWith("/")) {
                        link = "https://www.gdwc.gov.cn" + link;
                    } else {
                        link = "https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/" + link;
                    }
                }


                // 过滤外站链接和非详情页
                if (!link.contains("gdwc.gov.cn") || !link.contains("content/post_")) {
                    System.out.println("非官方链接：" + link);
                    //标题
                    String title = page.getHtml().xpath("//div[contains(@class='rich_media_title')]//span/text()").get();
                    //时间
                    String time = page.getHtml().xpath("//em[contains(@id='publish_time')]/text()").get();
                    //获取正文html
                    String html = page.getHtml().xpath("//div[contain(@id='js_content')]/html").get();

                    continue;
                }

                // 去重
                if (visitedLinks.putIfAbsent(link, listTitle) == null) {
                    newLinks.add(link);
                } else {
                    System.out.println("拦截到重复链接：" + link);
                }
            }

            page.addTargetRequests(newLinks);

            // 自动翻页逻辑
            String nextPageUrl = page.getHtml().xpath("//a[@class='next']/@href").get();
            if (nextPageUrl != null && !nextPageUrl.isEmpty()) {
                if (!nextPageUrl.startsWith("http")) {
                    nextPageUrl = "https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/" + nextPageUrl;
                }
                // 对“下一页”也执行去重
                if (visitedLinks.putIfAbsent(nextPageUrl, "下一页") == null) {
                    System.out.println("发现下一页: " + nextPageUrl);
                    page.addTargetRequest(nextPageUrl);
                }
            } else {
                System.out.println("已到达最后一页: " + page.getUrl());
            }

            System.out.println("本页有效连接数：" +  newLinks.size());
        }

    }

    @Override
    public Site getSite() {
        return site;
    }

    static class OraclePipeline implements Pipeline {
        String URL = "jdbc:oracle:thin:@192.168.2.42:1521:orcl";
        String USER = "bxkc";
        String PASS = "bxkc";

        @Override
        public void process(ResultItems items, Task task) {

            //获取Info封装后的对象
            Info info = items.get("info");
            if (info == null) return; // 说明是列表页，直接跳过

            String SOURCE_NAME = info.getSOURCE_NAME();
            String DETAIL_LINK = info.getDETAIL_LINK();
            String DETAIL_TITLE = info.getDETAIL_TITLE();
            String DETAIL_CONTENT = info.getDETAIL_CONTENT();
            String LIST_TITLE = info.getLIST_TITLE();
            String CREATE_BY = info.getCREATE_BY();

            // 处理日期：将Date类型转换为Timestamp类型
            Timestamp pageTs = info.getPAGE_TIME() != null ? new Timestamp(info.getPAGE_TIME().getTime()) : new Timestamp(System.currentTimeMillis());
            Timestamp createTs = info.getCREATE_TIME() != null ? new Timestamp(info.getCREATE_TIME().getTime()) : new Timestamp(System.currentTimeMillis());

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                //数据库去重
                int existId = 0;
                try (PreparedStatement check = conn.prepareStatement(
                        //先查询有没有重复的详情链接
                        "SELECT ID FROM XIN_XI_INFO_TEST WHERE DETAIL_LINK = ?")) {
                    check.setString(1, DETAIL_LINK);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            existId = rs.getInt(1);
                        }
                    }
                }

                if (existId > 0) {
                    //如果存在执行更新操作
                    String updateSql = "UPDATE XIN_XI_INFO_TEST SET " +
                            "DETAIL_TITLE = ?, DETAIL_CONTENT = ?, PAGE_TIME = ?, " +
                            "LIST_TITLE = ?, CREATE_BY = ? WHERE ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, DETAIL_TITLE);
                        ps.setCharacterStream(2, new StringReader(DETAIL_CONTENT), DETAIL_CONTENT.length());
                        ps.setTimestamp(3, pageTs);
                        ps.setString(4, LIST_TITLE);
                        ps.setString(5, CREATE_BY);
                        ps.setInt(6, existId);
                        ps.executeUpdate();
                    }
                    System.out.println("更新成功: " + DETAIL_TITLE);
                    countsql.incrementAndGet(); //更新成功计数器+1
                } else {
                    // 插入
                    String insertSql = "INSERT INTO XIN_XI_INFO_TEST " +
                            "(ID, SOURCE_NAME, DETAIL_LINK, DETAIL_TITLE, DETAIL_CONTENT, " +
                            " PAGE_TIME, CREATE_TIME, LIST_TITLE, CREATE_BY) " +
                            "VALUES (SEQ_XIN_XI_INFO.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, SOURCE_NAME);
                        ps.setString(2, DETAIL_LINK);
                        ps.setString(3, DETAIL_TITLE);
                        ps.setCharacterStream(4, new StringReader(DETAIL_CONTENT), DETAIL_CONTENT.length());
                        ps.setTimestamp(5, pageTs);
                        ps.setTimestamp(6, createTs);
                        ps.setString(7, LIST_TITLE);
                        ps.setString(8, CREATE_BY);
                        ps.executeUpdate();
                    }
                    System.out.println("插入成功: " + DETAIL_TITLE);
                    countsql.incrementAndGet();     //插入成功计数器+1\
                }
            } catch (Exception e) {
                System.out.println("入库失败: " + DETAIL_LINK);
                e.printStackTrace();
            }

            System.out.println("已成功入库"+countsql+"条数据");
        }
    }

    public static void main(String[] args) {
        Spider.create(new demo1())
                .addUrl("https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/")    //起始首页链接
                .addPipeline(new OraclePipeline())
                .thread(4)
                .run();
    }
}