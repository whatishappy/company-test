package day04;

import day04.Entity.Info;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

public class OraclePipeline implements Pipeline {

    private static final String URL = "jdbc:oracle:thin:@192.168.2.42:1521:orcl";
    private static final String USER = "bxkc";
    private static final String PASS = "bxkc";

    private long id = System.currentTimeMillis() / 1000;

    private synchronized long nextId() {
        return ++id;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        Info info = resultItems.get("info");
        if (info == null) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

            boolean exists = false;
            //数据库查重
            String checkSql = "SELECT COUNT(1) FROM XIN_XI_INFO_TEST WHERE DETAIL_LINK = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, info.getDETAIL_LINK());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            //存在更新
            if (exists) {
                String updateSql = "UPDATE XIN_XI_INFO_TEST SET " +
                        "SOURCE_NAME = ?, DETAIL_TITLE = ?, DETAIL_CONTENT = ?, " +
                        "PAGE_TIME = ?, LIST_TITLE = ?, CREATE_BY = ? " +
                        "WHERE DETAIL_LINK = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, info.getSOURCE_NAME());
                    ps.setString(2, info.getDETAIL_TITLE());

                    if (info.getDETAIL_CONTENT() != null) {
                        ps.setCharacterStream(3, new StringReader(info.getDETAIL_CONTENT()), info.getDETAIL_CONTENT().length());
                    } else {
                        ps.setNull(3, Types.CLOB);
                    }

                    if (info.getPAGE_TIME() != null) {
                        ps.setTimestamp(4, new Timestamp(info.getPAGE_TIME().getTime()));
                    } else {
                        ps.setNull(4, Types.DATE);
                    }

                    ps.setString(5, info.getLIST_TITLE());
                    ps.setString(6, info.getCREATE_BY());
                    ps.setString(7, info.getDETAIL_LINK());

                    ps.executeUpdate();
                }
                return;
            }


            //不存在插入
            String insertSql = "INSERT INTO XIN_XI_INFO_TEST " +
                    "(ID, SOURCE_NAME, DETAIL_LINK, DETAIL_TITLE, DETAIL_CONTENT, PAGE_TIME, CREATE_TIME, LIST_TITLE, CREATE_BY) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setLong(1, nextId());
                ps.setString(2, info.getSOURCE_NAME());
                ps.setString(3, info.getDETAIL_LINK());
                ps.setString(4, info.getDETAIL_TITLE());

                if (info.getDETAIL_CONTENT() != null) {
                    ps.setCharacterStream(5, new StringReader(info.getDETAIL_CONTENT()), info.getDETAIL_CONTENT().length());
                } else {
                    ps.setNull(5, Types.CLOB);
                }

                if (info.getPAGE_TIME() != null) {
                    ps.setTimestamp(6, new Timestamp(info.getPAGE_TIME().getTime()));
                } else {
                    ps.setNull(6, Types.DATE);
                }

                if (info.getCREATE_TIME() != null) {
                    ps.setTimestamp(7, new Timestamp(info.getCREATE_TIME().getTime()));
                } else {
                    ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                }

                ps.setString(8, info.getLIST_TITLE());
                ps.setString(9, info.getCREATE_BY());

                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}