package day04;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class demo1 implements PageProcessor {

    private Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setTimeOut(10000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

    static Integer PageNo = 0;

    @Override
    public void process(Page page) {
        page.putField("SOURCE_NAME", page.getHtml().xpath("//meta[@name='SiteName']/@content").get());

        if (page.getUrl().toString().contains("content/post_")) {
            String title = page.getHtml().xpath("//div[@class='con']/h3/text()").get();
            String content = page.getHtml().xpath("//div[@class='article']/html()").get();
            String timeStr = page.getHtml().xpath("//div[@class='massage clearfix']//span[@class='time']/text()").get();



            // 提取有效时间
            String cleanDate = "";
            if (timeStr != null) {
                Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                Matcher m = p.matcher(timeStr);
                if (m.find()) {
                    cleanDate = m.group();
                }
            }

            // 将提取的数据提交到 field 中
            page.putField("DETAIL_LINK", page.getUrl().toString());
            page.putField("DETAIL_TITLE", title);
            page.putField("DETAIL_CONTENT", content);
            page.putField("PAGE_TIME", cleanDate);
            page.putField("CREATE_TIME", cleanDate);
            page.putField("CREATE_BY", "伍芳正");
            page.putField("LIST_TITLE", title);
            //save


        } else {
            // 列表页
            List<String> links = page.getHtml().xpath("//ul[@class='list']//li//a/@href").all();
            // 判断是否属于该网站，否则解析网站名称会报错
            links.removeIf(link -> !link.contains("gdwc.gov.cn"));
            //去重URL
            page.addTargetRequests(links);

            // 分页 url
            // 爬取前三页
            for (int i = 1; i <= 3; i++) {
                if (i == 1) page.addTargetRequest("https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/index.html");
                page.addTargetRequest("https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/index_" + i + ".html");
            }
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
            if (items.get("DETAIL_LINK") == null) return;

            String SOURCE_NAME = items.get("SOURCE_NAME");  // 网站名称
            String DETAIL_LINK = items.get("DETAIL_LINK");  // 详细链接
            String DETAIL_TITLE = items.get("DETAIL_TITLE");  // 详细标题
            String DETAIL_CONTENT = items.get("DETAIL_CONTENT");  // 正文 html
            String PAGE_TIME = items.get("PAGE_TIME");  // 列表时间
            String CREATE_TIME = items.get("CREATE_TIME");  // 创建时间
            String LIST_TITLE = items.get("LIST_TITLE");  // 列表标题
            String CREATE_BY = items.get("CREATE_BY");  // 创建者名称

            /**
             * 由于初步解析得到的时间数据为 String 类型，需要将数据转换成 Date 类型
             */
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Timestamp pageTs = new Timestamp(System.currentTimeMillis());
            Timestamp createTs = new Timestamp(System.currentTimeMillis());
            try {
                if (PAGE_TIME != null && !PAGE_TIME.isEmpty()) {
                    pageTs = new Timestamp(sdf.parse(PAGE_TIME).getTime());
                }
                if (CREATE_TIME != null && !CREATE_TIME.isEmpty()) {
                    createTs = new Timestamp(sdf.parse(CREATE_TIME).getTime());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

                int existId = 0;
                // 先进行查询，查看是否有该链接，有则“更新”，否则“插入”
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT ID FROM XIN_XI_INFO_TEST WHERE DETAIL_LINK = ?")) {
                    check.setString(1, DETAIL_LINK);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {        // 游标下移
                            existId = rs.getInt(1); // 获得 id
                        }
                    }
                }

                if (existId > 0) {
                    String updateSql = "UPDATE XIN_XI_INFO_TEST SET " +
                            "DETAIL_TITLE = ?, DETAIL_CONTENT = ?, PAGE_TIME = ?, " +
                            "LIST_TITLE = ?, CREATE_BY = ? WHERE ID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, DETAIL_TITLE);
                        // 大文本 CLOB 字段，如果使用 setString 有大小限制 32kb，转化为字符流输入，防止截断
                        ps.setCharacterStream(2, new StringReader(DETAIL_CONTENT), DETAIL_CONTENT.length());
                        ps.setTimestamp(3, pageTs);
                        ps.setString(4, LIST_TITLE);
                        ps.setString(5, CREATE_BY);
                        ps.setInt(6, existId);
                        ps.executeUpdate();
                    }
                    System.out.println("更新成功: " + DETAIL_TITLE);
                } else {
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

                    StringBuilder sb = new StringBuilder();
                    sb.append("标题：").append(DETAIL_TITLE).append("\n")
                            .append("发布时间：").append(PAGE_TIME).append("\n")
                            .append("正文HTML：").append(DETAIL_CONTENT).append("\n")
                            .append("创建者名称：").append(CREATE_BY).append("\n");

                    System.out.println(sb);
                }
            } catch (Exception e) {
                System.out.println("失败: " + DETAIL_LINK);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Spider.create(new demo1())
                .addUrl("https://www.gdwc.gov.cn/zfxxgk/zjswcsczz/")
                .addPipeline(new OraclePipeline())
                .thread(2)
                .run();
    }
}