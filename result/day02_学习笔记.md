# 第二天学习笔记：三种解析方式（Jsoup / XPath / 正则）

> 练习网站：安徽省港航集团有限公司 · 通知通告列表
> 列表页：`http://www.ahsgh.com/ahghjtweb/web/list`（POST，参数 listPage=list & intCurPage=1 & intPageSize=10 & strColId=20782f569264489f87995ad0773ff626 & strWebSiteId=4c5fcf57602b48a0acde5a4ef3ede48d & nowPage=1）
> 详情页：`http://www.ahsgh.com/ahghjtweb/web/view?strId=...&strColId=...&strWebSiteId=...`（GET，参数从列表页 `url_view(...)` 中用正则提取）

## 一、练习目标

结合 HttpClient 请求，用三种解析方式获取并打印：**标题、发布时间、链接、详情正文 html**。

| 数据 | 解析方式 | 定位语句 |
| --- | --- | --- |
| 列表标题 | Jsoup 精准定位（class） | `ul.tab01open.tab01tca li h2.fl` |
| 详情链接 | Jsoup 模糊定位（属性包含） | `a[href*=url_view]`，再用正则提取 `url_view('strId','strColId','strWebSiteId')` 拼接 |
| 发布时间 | 正则表达式 | `\d{4}-\d{2}-\d{2}`（匹配 `<i class="fr">2026-09-14</i>`） |
| 详情正文 html | Jsoup 注释锚点截取 | `<!-- 正文内容 -->` 到 `<!-- 文后 -->` 之间的所有元素 outerHtml 拼接 |
| （补充）XPath 全量解析 | XPath 精准/模糊定位 | 见 `XPathDemo.java`，如 `//h2[contains(@class,'fl')]`、`//a[contains(@href,'url_view')]/@href` |

## 二、三种解析方式对比

### 1. Jsoup（CSS 选择器，类 jQuery）
- **精准定位**：`标签.class`、`#id`、`[attr=value]`，如 `h2.fl`（class 必须完全等于 fl）
- **模糊定位**：`[attr*=value]`（包含）、`[attr^=value]`（开头）、`[attr$=value]`（结尾）、`:contains(文本)`、`:matches(正则)`，如 `a[href*=url_view]`
- 优点：API 简单、容错强；缺点：遇到多值 class / 结构变化时选择器要跟着改

### 2. XPath（本工程使用 JsoupXpath 库：`cn.wanghaomiao:JsoupXpath:2.5.3`）
- **语法速记**：`/` 直接子节点，`//` 任意子孙节点，`@` 属性，`[]` 条件，`text()` 文本，`position()`/`last()` 位置
- **精准定位**：`//h2[@class='fl']`（属性值完全相等）
- **模糊定位**：`//h2[contains(@class,'fl')]`（多值 class 用 contains）、`//a[contains(@href,'url_view')]`
- 用法：`new JXDocument(html)` → `selN(xpath)` 返回 `List<JXNode>`，元素节点用 `getElement().text() / .attr("href") / .outerHtml()`；**注意：取文本不要用 `/text()` 再 `getTextVal()`，JsoupXpath 对 text() 节点返回 null，直接选元素节点再 `.text()` 更稳**

### 3. 正则表达式（Pattern / Matcher）
- 适合"格式固定"的字符串：日期 `\d{4}-\d{2}-\d{2}`、url 参数 `url_view\('(.*?)',\s*'(.*?)',\s*'(.*?)'\)`
- 优点：不依赖 DOM 结构；缺点：只能处理文本，拿不到结构化节点

## 三、练习流程（demo.java）

```
HttpClient(POST) 抓列表页
      │
      ├─ Jsoup 精准定位  h2.fl            → 标题
      ├─ Jsoup 模糊定位  a[href*=url_view] → 详情链接（正则提取 3 个参数拼接真实地址）
      └─ 正则  \d{4}-\d{2}-\d{2}          → 发布时间
      │
HttpClient(GET) 抓详情页
      │
      └─ Jsoup 注释锚点截取（正文内容 ~ 文后）→ 详情正文 html
      │
      控制台打印（标题 / 发布时间 / 链接 / 正文html / 正文纯文本）
```

## 四、踩坑记录（重要）

1. **详情链接不是直接 href**：页面用 `javascript:url_view('strId','strColId','strWebSiteId')`，必须正则提取参数后拼接 `view?strId=...`。
2. **正文 p 嵌套是非法 HTML**：正文是一个 `<p>` 里再套一堆 `<p>`，jsoup 解析时会自动把外层 p 闭合，正文被拆成 20+ 个兄弟节点。所以：
   - 不能直接用 `.article-conca > p`（会选出 23 个含空壳节点）；
   - 稳妥做法：遍历子节点，收集 `<!-- 正文内容 -->` 与 `<!-- 文后 -->` 两个注释之间的所有元素，拼接 outerHtml；
   - XPath 版用 `//div[contains(@class,'article-conca')]/p[position()>1]`（第一个是自动闭合产生的空 `<p></p>`）。
3. **编码**：网站是 UTF-8，`EntityUtils.toString(entity, StandardCharsets.UTF_8)`；控制台运行加 `-Dfile.encoding=UTF-8` 避免中文乱码。
4. **请求头**：带上 User-Agent 和 Referer，避免被拒绝（403/429 时换 IP 或加大访问间隔）。

## 五、运行方式

```bash
# 主练习：HttpClient + Jsoup + 正则
mvn compile exec:java -Dexec.mainClass=day02.demo

# XPath 解析演示
mvn compile exec:java -Dexec.mainClass=day02.XPathDemo
```

完整控制台输出已保存：
- `result/day02_demo_console.txt`（13 万字符，10 条记录 × 标题/时间/链接/正文html/纯文本）
- `result/day02_xpath_console.txt`（12 万字符，10 条记录 × 列表+详情 XPath 解析）

## 六、参考链接

- Jsoup 用法详解：https://www.cnblogs.com/langtianya/p/3880132.html
- XPath 使用详细教学：https://blog.csdn.net/wangzhuanjia/article/details/122739797
- 正则表达式在线工具：https://tool.chinaz.com/regex/
- JsoupXpath 库：https://mvnrepository.com/artifact/cn.wanghaomiao/JsoupXpath
