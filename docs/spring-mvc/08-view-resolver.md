# 第八阶段：ViewResolver — 视图解析

> 所属模块：`spring-webmvc/src/main/java/org/springframework/web/servlet/view/`
>
> 核心问题：Controller 返回了字符串 `"index"`，是怎么变成 HTML 页面的？

---

## 8.1 ViewResolver 接口

### 📌 WHAT

`org.springframework.web.servlet.ViewResolver` 把逻辑视图名（String）解析成真正的 `View` 对象。

```java
// ViewResolver.java
public interface ViewResolver {
    View resolveViewName(String viewName, Locale locale) throws Exception;
}
```

### 🎯 WHY

Controller 应该只返回一个**逻辑视图名**（如 `"index"`），不应该关心页面是 `.jsp`、`.html` 还是 `.ftl`。ViewResolver 负责屏蔽这个差异。

### ⚙️ 工作原理

DispatcherServlet 的 `resolveViewName()`：

```java
// DispatcherServlet.java
protected View resolveViewName(String viewName, Map<String, Object> model, 
        Locale locale, HttpServletRequest request) throws Exception {
    if (this.viewResolvers != null) {
        for (ViewResolver viewResolver : this.viewResolvers) {
            View view = viewResolver.resolveViewName(viewName, locale);
            if (view != null) {
                return view;
            }
        }
    }
    return null;
}
```

> ⚠️ 注意：遍历所有 ViewResolver，**第一个返回非 null 的就被采用**。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/ViewResolver.java
```

---

## 8.2 View 接口

### 📌 WHAT

`org.springframework.web.servlet.View` 代表一个视图，负责最终渲染。

```java
// View.java
public interface View {
    String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";
    String PATH_VARIABLES = View.class.getName() + ".pathVariables";

    String getContentType();
    
    // 核心方法：渲染视图
    void render(@Nullable Map<String, ?> model, 
                HttpServletRequest request, 
                HttpServletResponse response) throws Exception;
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/View.java
```

---

## 8.3 InternalResourceViewResolver — JSP 视图

### 📌 WHAT

解析 JSP 视图。虽然现在前后端分离基本不用 JSP 了，但这是理解视图解析最简单的入门。

### 🔧 HOW

```java
// Java Config
@Bean
public ViewResolver viewResolver() {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix("/WEB-INF/views/");  // 前缀
    resolver.setSuffix(".jsp");             // 后缀
    return resolver;
}

// Controller
@GetMapping("/index")
public String index() {
    return "index";  // → /WEB-INF/views/index.jsp
}
```

### ⚙️ 工作原理

`resolveViewName("index")` → 拼接前缀后缀 → `/WEB-INF/views/index.jsp` → `request.getRequestDispatcher(path).forward(request, response)`

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/view/InternalResourceViewResolver.java
spring-webmvc/src/main/java/org/springframework/web/servlet/view/InternalResourceView.java
```

---

## 8.4 ContentNegotiatingViewResolver — 内容协商视图

### 📌 WHAT

根据客户端请求的 `Accept` 头或其他策略，**动态选择**不同的 ViewResolver。

### 🔧 HOW

```java
@Bean
public ViewResolver contentNegotiatingViewResolver() {
    ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
    
    // 注册多个 ViewResolver
    List<ViewResolver> resolvers = new ArrayList<>();
    resolvers.add(new InternalResourceViewResolver());   // JSP
    resolvers.add(new BeanNameViewResolver());            // Bean 视图
    resolver.setViewResolvers(resolvers);
    
    return resolver;
}
```

**效果**：
- `Accept: text/html` → 返回 HTML 页面
- `Accept: application/json` → 返回 JSON 数据
- `Accept: application/xml` → 返回 XML 数据

### ⚙️ 工作原理

内部持有多个 `ViewResolver`，根据 `ContentNegotiationManager` 的策略决定用哪个。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/view/ContentNegotiatingViewResolver.java
```

---

## 8.5 BeanNameViewResolver — Bean 名称视图

### 📌 WHAT

把 View 对象注册为 Spring Bean，视图名就是 Bean 名称。

### 🔧 HOW

```java
@Component("excelReport")  // Bean 名称 = 视图名
public class ExcelReportView extends AbstractXlsxView {
    @Override
    protected void buildExcelDocument(Map<String, Object> model,
            Workbook workbook, HttpServletRequest request,
            HttpServletResponse response) {
        // 生成 Excel...
    }
}

// Controller
@GetMapping("/report")
public String report() {
    return "excelReport";  // → 找到名为 "excelReport" 的 Bean → 渲染 Excel
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/view/BeanNameViewResolver.java
```

---

## 8.6 RedirectView — 重定向视图

### 📌 WHAT

当视图名以 `redirect:` 开头时，Spring 自动使用 `RedirectView` 进行 HTTP 重定向。

### 🔧 HOW

```java
@PostMapping("/user")
public String createUser(User user) {
    userService.save(user);
    return "redirect:/user/" + user.getId();  // 302 重定向
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/view/RedirectView.java
```

---

## 8.7 其他视图类型一览

| 视图类型 | 类名 | 用途 |
|---------|------|------|
| FreeMarker | `FreeMarkerViewResolver` / `FreeMarkerView` | FreeMarker 模板引擎 |
| Groovy Markup | `GroovyMarkupViewResolver` / `GroovyMarkupView` | Groovy 模板引擎 |
| Script Template | `ScriptTemplateViewResolver` | 脚本模板（Nashorn 等） |
| JSON | `MappingJackson2JsonView` | JSON 序列化视图 |
| PDF | `AbstractPdfView` | 生成 PDF 输出 |
| Excel | `AbstractXlsxView` / `AbstractXlsView` | 生成 Excel 输出 |
| RSS Feed | `AbstractRssFeedView` | 生成 RSS 输出 |
| Atom Feed | `AbstractAtomFeedView` | 生成 Atom 输出 |
| XSLT | `XsltView` | XSLT 转换视图 |
| XML | `MarshallingView` | XML 序列化视图 |

### 🔧 HOW（PDF 示例）

```java
@Component("pdfReport")
public class PdfReportView extends AbstractPdfView {
    @Override
    protected void buildPdfDocument(Map<String, Object> model,
            Document document, PdfWriter writer,
            HttpServletRequest request, HttpServletResponse response) {
        document.add(new Paragraph("Hello PDF!"));
        document.add(new Paragraph("Data: " + model.get("data")));
    }
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/view/freemarker/
spring-webmvc/src/main/java/org/springframework/web/servlet/view/groovy/
spring-webmvc/src/main/java/org/springframework/web/servlet/view/document/  (PDF/Excel)
spring-webmvc/src/main/java/org/springframework/web/servlet/view/feed/      (RSS/Atom)
spring-webmvc/src/main/java/org/springframework/web/servlet/view/json/
spring-webmvc/src/main/java/org/springframework/web/servlet/view/xml/
spring-webmvc/src/main/java/org/springframework/web/servlet/view/xslt/
```

---

## 8.8 ViewResolver 注册顺序机制

在 `DispatcherServlet` 中，`viewResolvers` 是一个 `List`，按注册顺序遍历：

```java
// DispatcherServlet.initViewResolvers():
private void initViewResolvers(ApplicationContext context) {
    this.viewResolvers = new ArrayList<>();
    
    // 从容器中查找所有 ViewResolver Bean
    Map<String, ViewResolver> matchingBeans =
        BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class);
    
    for (ViewResolver vr : matchingBeans.values()) {
        this.viewResolvers.add(vr);
    }
    
    // 按 Order 排序
    AnnotationAwareOrderComparator.sort(this.viewResolvers);
}
```

---

## 8.9 前后端分离场景

现代开发中，Spring MVC 主要当"数据接口"用（返回 JSON），视图层交给前端框架（Vue/React）。此时：

- `@RestController` 下的方法返回 JSON
- `ViewResolver` 基本不用
- 静态资源（HTML/CSS/JS）由 Nginx 或 Spring 的静态资源处理器提供

---

> **下一阶段**：[第九阶段：数据绑定 & 类型转换 & 校验](09-data-binding-validation.md)
