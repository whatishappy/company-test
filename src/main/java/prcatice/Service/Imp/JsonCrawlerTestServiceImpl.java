package prcatice.Service.Imp;


import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-17
 * @Version: 1.0
 */


public class JsonCrawlerTestServiceImpl implements CrawlerService{

    @Override
    public void process(Page page) {


    }

    @Override
    public Site getSite() {
        return CrawlerService.super.getSite();
    }
}
