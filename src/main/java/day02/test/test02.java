package day02.test;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 */


public class test02 {
    public static void main(String[] args) throws IOException {
        /*//练习就直接抛出异常,不采取try
        Document doc = Jsoup.connect(AboutURL.BAIDU_BASEURL)
                .timeout(1000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36 Edg/153.0.0.0")
                .get();

        System.out.println(doc);        //html页面源代码

        System.out.println(doc.select("title").first().text());     //百度一下，你就知道*/

        //使用jsoup的css选择器选择
        Elements all_Links = Jsoup.parse(AboutURL.TESTFILE_URL,"utf-8")
                .getElementsByTag("a[href]");

        for (Element allLink : all_Links) {
            System.out.println(allLink.attr("href"));
        }


    }
}
