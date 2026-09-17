package day04;


import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-17
 * @Version: 1.0
 */


public class test implements PageProcessor {


    @Override
    public void process(Page page) {

    }

    @Override
    public Site getSite() {
        return PageProcessor.super.getSite();
    }


    public static void main(String[] args) {
        Spider.create(new test())
                .addUrl("");
    }
}
