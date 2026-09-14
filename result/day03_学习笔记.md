# 第三天学习笔记：webmagic 基本框架与四大组件

> 参考文档：http://webmagic.io/docs/zh/
> 练习网站：安徽省港航集团 · 通知通告（同第二天）
> 源码：`src/main/java/day03/demo_webMagic.java`（Maven 依赖：`us.codecraft:webmagic-core:0.7.5`）

## 一、四大组件总览

| 组件 | 接口/类 | 作用 | 本 demo 中的实现 |
| --- | --- | --- | --- |
| Downloader 下载器 | `Downloader`（默认 `DefaultDownloader`） | 发 HTTP 请求、把 URL 对应的页面抓下来 | 不写，用内置默认（基于 HttpClient） |
| PageProcessor 页面处理器 | `PageProcessor`（必须自己写） | 解析页面：`putField` 提取数据、`addTargetRequests` 发现新链接 | `DemoPageProcessor` |
| Scheduler 调度器 | `Scheduler`（默认 `QueueScheduler`） | 管理待抓 URL 队列 + 去重 | 显式指定 `QueueScheduler` |
| Pipeline 管道 | `Pipeline` | 处理 PageProcessor 抽取出的 `ResultItems`（打印/存文件/存库） | `DemoPipeline`（控制台打印） |

## 二、一次抓取的数据流

```
Spider 启动
  → 初始 Request（列表页，POST）交给 Scheduler 入队
  → Downloader 从 Scheduler 取出 URL 抓取页面
  → PageProcessor.process(page) 解析：
      列表页 → putField(标题/时间) + addTargetRequests(详情链接)
      详情页 → putField(标题/发布时间/正文html)
  → 每页抽取结果交给 Pipeline 输出
  → 新链接回到 Scheduler，循环直到队列为空 → Spider 结束
```

## 三、核心代码要点

1. **Site（站点配置）**：`Site.me().setRetryTimes(3).setSleepTime(1500).setUserAgent(...).setCharset("UTF-8").addHeader("Referer", ...)`
   - 编码必须设 UTF-8，否则中文乱码
   - 访问间隔 1.5s，避免被目标网站拒绝

2. **POST 请求**：列表页是 POST，webmagic 的 `Request` 可以：
   ```java
   Request request = new Request(LIST_URL);
   request.setMethod(HttpConstant.Method.POST);
   Map<String,Object> form = new LinkedHashMap<>();
   form.put("listPage","list"); form.put("intCurPage","1"); ...
   request.setRequestBody(HttpRequestBody.form(form, "UTF-8"));  // 自动带 form 编码
   spider.addRequest(request);   // 用 addRequest 加初始请求
   ```

3. **Selectable 选择器**：`page.getHtml()` 支持三种选择器，与第二天完全对应
   - `xpath("//h2[contains(@class,'fl')]/text()")` → 文本
   - `xpath("//a[contains(@href,'url_view')]/@href")` → 属性
   - `css(".article-conca h2", "text")` → 文本
   - `.regex("\\d{4}-\\d{2}-\\d{2}")` → 正则

4. **踩坑：Xsoup 不支持 `position()` 函数**
   - `//div[contains(@class,'article-conca')]/p[position()>1]` 会抛 `SelectorParseException`
   - 改法：先 `xpath(".../p").all()` 选出全部直接子 `<p>`，在 Java 里过滤掉空壳 `<p></p>` 再拼接

5. **多页爬取（本次重点补充）**
   - 站点是 POST 翻页且每页 URL 相同，Scheduler 会按 URL 去重，直接加同 URL 会只剩 1 页
   - 实测该站 GET 也能翻页（`?listPage=list&intCurPage=2&intPageSize=10&...`），所以后续页用 GET + 唯一 URL 入队即可
   - 总页数从第 1 页分页信息里取：`//a[@title='尾页']/@href` → `javascript:gotoPage(222)`，正则/去非数字得 `222`
   - 用 `MAX_PAGES` 常量控制演示页数（3 = 前 3 页；-1 = 全量 222 页）
   - 只让第 1 页触发"发现后续页"逻辑（`if (curPage == 1)`），避免循环入队

## 四、运行方式

```bash
mvn compile exec:java -Dexec.mainClass=day03.demo_webMagic
```

运行结果（默认 MAX_PAGES=3）：3 个列表页（第 1 页 POST + 第 2/3 页 GET）× 10 条 + 30 个详情页，全部由自定义 Pipeline 打印。
完整输出见 `result/day03_webmagic_console.txt`。想全量爬把 `demo_webMagic.java` 里的 `MAX_PAGES` 改成 `-1` 即可（222 页，会跑较久）。

## 五、进阶方向（后续可练）

- `FileCacheQueueScheduler`：URL 落盘，断点续爬
- `OOSpider` + 注解模式：`@TargetUrl` / `@ExtractBy` 自动映射实体
- 自定义 `Downloader`：对接 Selenium / 登录态
- `Pipeline` 存文件（`FilePipeline`）或存数据库
