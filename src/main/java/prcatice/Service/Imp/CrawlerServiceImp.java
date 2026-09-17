package prcatice.Service.Imp;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.util.List;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-17
 * @Version: 1.0
 */

public class CrawlerServiceImp implements prcatice.Service.Imp.CrawlerService {

    //设置网站信息
    private final Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setTimeOut(10000)
            .setDisableCookieManagement(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    //和兴执行流程
    @Override
    public void process(Page page) {
        //抽取总体父页面html
        Html html = page.getHtml();
        //获取所有record
        List<String> recordTexts = page.getHtml().xpath("//record/text()").all();
        for (String recordText : recordTexts) {
            System.out.println(recordText);
        }
        //进行判断是否为空 size

        //如果为空，则说明为详细页面
        //TODO 执行抽取出去的crawler爬取解析本网页方法

        //如果不为空，则为列表页
        //TODO 使用MAP<详细链接，列表标题> 去重URL（后期维护可能还要查看列表发布时间和详细页面发布时间）
        //TODO 添加url至任务队列中

    }

    @Override
    public Site getSite() {
        return site;
    }

    /*模糊一个线程试一下*/
    public static void main(String[] args) {
        //配置Spider
        Spider.create(new CrawlerServiceImp())
                .addUrl("http://slhhpj.jingmen.gov.cn/col/col11111/index.html")
                .thread(1)
                .run();
    }
}
