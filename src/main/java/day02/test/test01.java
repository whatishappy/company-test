package day02.test;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-14
 * @Version: 1.0
 */


public class test01 {
    public static void main(String[] args) {

        //TODO直接解析静态网页
        String html = "<html><head><title> 开源中国社区 </title></head>"
                + "<body><p> 这里是 jsoup 项目的相关文章 </p></body></html>";
        String text1 = Jsoup.parse(html).text();
        System.out.println(text1);

        //TODO从URL直接加载HTML文档
        try {
            Document doc = Jsoup.connect("http://www.baidu.com").get();
            String title = doc.title(); //获取网页中title标签

            Jsoup.connect("http://www.baidu.com")
                    .data("query", "Java")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36 Edg/153.0.0.0")
                    .cookie("auth", "token")
                    .timeout(3*1000);

            System.out.println(title);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //TODO从文件中加载HTML文档
        try {
            System.out.println(Jsoup.parse(new File("E:\\firstday\\result\\demo(get).html"),"utf-8").text());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
