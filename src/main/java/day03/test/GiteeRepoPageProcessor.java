package day03.test;


import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.example.GithubRepoPageProcessor;

import java.io.File;
import java.util.List;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-15
 * @Version: 1.0
 */


public class test01 {

    public static void main(String[] args) {
        Spider.create(new GithubRepoPageProcessor())
                .addUrl("https://gitee.com/dashboard/projects")
                .setPipelines()
                //开启5个线程同时执行
                .thread(5)
                //启动爬虫
                .run();
    }
}
