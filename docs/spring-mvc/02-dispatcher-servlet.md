# 第二阶段：DispatcherServlet 与请求分发

> **Spring Framework 6.0.0 源码分析**

本文档深入剖析 Spring MVC 请求分发的核心组件——`DispatcherServlet`，从 Servlet 基础规范出发，逐层拆解 Spring MVC 如何将 HTTP 请求准确路由到业务处理器。重点分析 `doDispatch()` 方法的完整执行流程，揭示请求分发、处理器适配、视图渲染和异常处理的全链路机制。

---

## 1. Servlet 基础

### WHAT

Servlet（`jakarta.servlet.Servlet`）是 Jakarta EE（原 Java EE）规范中定义的 Web 组件接口，是所有 Java Web 应用的基石。它是一个运行在 Servlet 容器（如 Tomcat、Jetty）中的 Java 类，用于接收和响应 HTTP 请求。

核心类型：
- **`jakarta.servlet.Servlet`**：Servlet 生命周期接口，定义了 `init()`、`service()`、`destroy()` 方法
- **`jakarta.servlet.http.HttpServletRequest`**：封装 HTTP 请求信息（请求行、请求头、请求体、参数等）
- **`jakarta.servlet.http.HttpServletResponse`**：封装 HTTP 响应信息（状态码、响应头、响应体）

### WHY

为什么 Spring MVC 要建立在 Servlet 规范之上？

1. **标准化**：Servlet 规范是 Java Web 的行业标准，所有主流 Web 容器都实现了它。构建在 Servlet 之上意味着 Spring MVC 可以在任何符合规范的容器中运行。
2. **请求抽象**：Servlet 规范提供了完备的 HTTP 请求/响应模型，包括请求参数解析、Session 管理、Cookie 处理、Filter 链等——这些都是 Web 框架的基础设施。
3. **与容器集成**：通过实现 Servlet 接口，Spring 可以参与容器的生命周期管理（启动、销毁）、资源管理和安全控制。
4. **分层解耦**：将 Servlet 规范作为底层抽象，Spring 在其上构建更高层次的 MVC 抽象（Controller、ModelAndView 等），实现了关注点分离。

### HOW

在 Spring MVC 中，`DispatcherServlet` 虽然不在代码中显式实现 `jakarta.servlet.Servlet`，但其继承链路最终追溯到 `HttpServlet`：

```
DispatcherServlet
  └── FrameworkServlet (abstract)
       └── HttpServletBean (abstract)
            └── HttpServlet  (Servlet 规范的实现)
                 └── GenericServlet
                      └── implements Servlet, ServletConfig
```

实际使用时，在 `web.xml` 中配置或在 Servlet 3.0+ 环境中通过 `WebApplicationInitializer` 编程式注册 `DispatcherServlet`。

**web.xml 配置示例：**

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/dispatcher-servlet.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

### 工作原理（源码分析）

Servlet 容器收到 HTTP 请求后的处理链路：

1. **容器调用 `Servlet.service(ServletRequest, ServletResponse)`**
2. **`GenericServlet`** 和 **`HttpServlet`** 将 `ServletRequest`/`ServletResponse` 强制转换为 `HttpServletRequest`/`HttpServletResponse`
3. **`HttpServlet.service()`** 根据 HTTP 方法（GET/POST/PUT/DELETE 等）分发到对应的 `doXxx()` 方法
4. **`FrameworkServlet`** 重写了所有 `doXxx()` 方法，统一委托给 `processRequest()`

Spring MVC 的请求处理入口就是 `FrameworkServlet.processRequest()`：

```java
// FrameworkServlet.java 第985行
protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    Throwable failureCause = null;

    // 1. 保存并设置线程级别的 LocaleContext
    LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
    LocaleContext localeContext = buildLocaleContext(request);

    // 2. 保存并设置线程级别的 RequestAttributes
    RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
    ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

    // 3. 初始化异步管理器
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

    initContextHolders(request, localeContext, requestAttributes);

    try {
        doService(request, response);  // 核心：委托给子类的 doService()
    }
    catch (ServletException | IOException ex) {
        failureCause = ex;
        throw ex;
    }
    finally {
        // 4. 恢复线程上下文
        resetContextHolders(request, previousLocaleContext, previousAttributes);
        // 5. 发布 ServletRequestHandledEvent
        publishRequestHandledEvent(request, response, startTime, failureCause);
    }
}
```

`HttpServletRequest` 和 `HttpServletResponse` 贯穿整个 Spring MVC 请求处理过程，作为方法参数在各层之间传递。

### 源码位置

| 组件 | 包路径 |
|------|--------|
| `jakarta.servlet.Servlet` | `jakarta.servlet`（Servlet API，非 Spring 源码） |
| `jakarta.servlet.http.HttpServlet` | `jakarta.servlet.http`（Servlet API，非 Spring 源码） |

---

## 2. WebApplicationContext

### WHAT

`org.springframework.web.context.WebApplicationContext` 是 Spring 专门为 Web 应用定制的 `ApplicationContext` 子接口。它在标准 `ApplicationContext` 的基础上增加了：

- **`getServletContext()` 方法**：获取 Web 应用的 `ServletContext`
- **预定义的 Bean 名称常量**：如 `SERVLET_CONTEXT_BEAN_NAME = "servletContext"`
- **Web 特有的作用域**：`request`、`session`、`application`、`websocket`

```java
// WebApplicationContext.java 第45行
public interface WebApplicationContext extends ApplicationContext {

    String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

    String SCOPE_REQUEST = "request";
    String SCOPE_SESSION = "session";
    String SCOPE_APPLICATION = "application";

    String SERVLET_CONTEXT_BEAN_NAME = "servletContext";
    String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";
    String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";

    @Nullable
    ServletContext getServletContext();
}
```

### WHY

为什么 Spring MVC 需要专门的 `WebApplicationContext`？

1. **Servlet 集成**：标准 `ApplicationContext` 不了解 Servlet 容器。`WebApplicationContext` 通过 `getServletContext()` 打通了 Spring IoC 容器与 Servlet 容器的连接。
2. **分层上下文**：Spring Web 应用采用双层上下文架构：
   - **Root WebApplicationContext**：由 `ContextLoaderListener` 创建，包含 Service、Repository、DataSource 等基础设施 Bean，被所有 Servlet 共享
   - **Servlet WebApplicationContext**：由 `DispatcherServlet` 创建，包含 Controller、HandlerMapping、ViewResolver 等 Web 层 Bean，可以访问 Root Context 中的 Bean（通过父子上下文机制）
3. **Web 作用域**：`request`、`session`、`application` 等作用域只有在 Web 环境下才有意义，`WebApplicationContext` 提供了这些作用域的支持。
4. **生命周期绑定**：WebApplicationContext 的生命周期与 ServletContext 绑定，随着 Web 应用的启动和销毁而初始化和关闭。

### HOW

Spring Web 应用的典型上下文层次结构：

```
Root WebApplicationContext（父容器）
    ├── DataSource
    ├── Service 层 Bean
    ├── DAO 层 Bean
    └── 安全配置等
    │
    ├── DispatcherServlet WebApplicationContext（子容器1）
    │   ├── Controller
    │   ├── HandlerMapping
    │   └── ViewResolver
    │
    └── DispatcherServlet WebApplicationContext（子容器2，可选）
        ├── REST Controller
        ├── HandlerMapping
        └── HandlerAdapter
```

**配置示例（web.xml）：**

```xml
<!-- Root WebApplicationContext 配置 -->
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>/WEB-INF/applicationContext.xml</param-value>
</context-param>

<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>

<!-- Servlet WebApplicationContext 配置 -->
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/dispatcher-servlet.xml</param-value>
    </init-param>
</servlet>
```

### 工作原理（源码分析）

**父子上下文关系的建立**，在 `FrameworkServlet.initWebApplicationContext()` 中：

```java
// FrameworkServlet.java 第560行
protected WebApplicationContext initWebApplicationContext() {
    // 1. 获取 Root WebApplicationContext
    WebApplicationContext rootContext =
            WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    WebApplicationContext wac = null;

    if (this.webApplicationContext != null) {
        // 场景A：通过构造函数注入的上下文
        wac = this.webApplicationContext;
        if (wac instanceof ConfigurableWebApplicationContext cwac && !cwac.isActive()) {
            if (cwac.getParent() == null) {
                cwac.setParent(rootContext);  // 将 Root Context 设为父容器
            }
            configureAndRefreshWebApplicationContext(cwac);
        }
    }
    if (wac == null) {
        // 场景B：从 ServletContext 属性中查找
        wac = findWebApplicationContext();
    }
    if (wac == null) {
        // 场景C：创建新的 WebApplicationContext
        wac = createWebApplicationContext(rootContext);
    }

    return wac;
}
```

**关键机制**：子容器（Servlet WebApplicationContext）可以通过 `getParent()` 获取父容器（Root WebApplicationContext），子容器可以访问父容器中的 Bean，但父容器不能访问子容器中的 Bean。

### 源码位置

| 组件 | 文件 |
|------|------|
| `WebApplicationContext` | `spring-web/src/main/java/org/springframework/web/context/WebApplicationContext.java` |
| `ConfigurableWebApplicationContext` | `spring-web/src/main/java/org/springframework/web/context/ConfigurableWebApplicationContext.java` |

---

## 3. ContextLoaderListener

### WHAT

`org.springframework.web.context.ContextLoaderListener` 是一个 `ServletContextListener` 实现，负责在 Web 应用启动时创建和初始化 **Root WebApplicationContext**，并在 Web 应用销毁时关闭它。

```java
// ContextLoaderListener.java 第37行
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

    public ContextLoaderListener() {
    }

    public ContextLoaderListener(WebApplicationContext context) {
        super(context);  // 支持编程式传入预配置的 WebApplicationContext
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initWebApplicationContext(event.getServletContext());  // 启动时初始化
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        closeWebApplicationContext(event.getServletContext());     // 销毁时关闭
        ContextCleanupListener.cleanupAttributes(event.getServletContext());
    }
}
```

### WHY

为什么需要 `ContextLoaderListener`？

1. **与 Servlet 生命周期集成**：`ServletContextListener.contextInitialized()` 在 Servlet 容器启动时、任何 Servlet 初始化之前被调用。这是初始化 Spring 根容器的最佳时机。
2. **避免重复初始化**：如果不使用 `ContextLoaderListener`，每个 `DispatcherServlet` 都会创建自己的 `WebApplicationContext`，但 Service、Repository 等共享 Bean 会被重复创建，浪费资源。
3. **分层架构需求**：典型的 Spring Web 应用需要将业务层（Service、DAO）与 Web 层（Controller）分离。Root Context 承载业务层，Servlet Context 承载 Web 层。
4. **容器无关性**：`ContextLoaderListener` 使用标准 Servlet API，不依赖任何特定容器实现。

### HOW

**传统 web.xml 配置：**

```xml
<web-app>
    <!-- 指定 Root Context 的配置文件位置 -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>
            /WEB-INF/spring/root-context.xml
            classpath:applicationContext-datasource.xml
        </param-value>
    </context-param>

    <!-- 注册 ContextLoaderListener -->
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <!-- DispatcherServlet 配在后面 -->
    <servlet>
        <servlet-name>appServlet</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
</web-app>
```

**Spring Boot / Servlet 3.0+ 编程式注册：**

```java
public class MyWebAppInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) {
        // 创建 Root WebApplicationContext
        AnnotationConfigWebApplicationContext rootContext =
                new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);

        // 注册 ContextLoaderListener
        servletContext.addListener(new ContextLoaderListener(rootContext));

        // 创建 DispatcherServlet 的 WebApplicationContext
        AnnotationConfigWebApplicationContext dispatcherContext =
                new AnnotationConfigWebApplicationContext();
        dispatcherContext.register(WebConfig.class);

        // 注册 DispatcherServlet
        DispatcherServlet dispatcherServlet = new DispatcherServlet(dispatcherContext);
        ServletRegistration.Dynamic registration =
                servletContext.addServlet("dispatcher", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
```

### 工作原理（源码分析）

`ContextLoaderListener` 继承自 `ContextLoader`，核心初始化逻辑在 `ContextLoader.initWebApplicationContext()` 中：

```java
// ContextLoader.java 第259行
public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
    // 1. 防重检查：确保 Root Context 只初始化一次
    if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
        throw new IllegalStateException(
                "Cannot initialize context because there is already a root application context present");
    }

    try {
        if (this.context == null) {
            // 2. 创建 WebApplicationContext 实例
            //    默认是 XmlWebApplicationContext
            //    可通过 contextClass 参数指定 AnnotationConfigWebApplicationContext 等
            this.context = createWebApplicationContext(servletContext);
        }
        if (this.context instanceof ConfigurableWebApplicationContext cwac && !cwac.isActive()) {
            if (cwac.getParent() == null) {
                ApplicationContext parent = loadParentContext(servletContext);
                cwac.setParent(parent);
            }
            // 3. 配置并刷新上下文
            configureAndRefreshWebApplicationContext(cwac, servletContext);
        }
        // 4. 将上下文注册为 ServletContext 属性
        servletContext.setAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
        return this.context;
    }
    catch (RuntimeException | Error ex) {
        // 失败时将异常也注册为属性，方便诊断
        servletContext.setAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
        throw ex;
    }
}
```

**`configureAndRefreshWebApplicationContext()` 方法的执行步骤：**

```java
// ContextLoader.java 第366行
protected void configureAndRefreshWebApplicationContext(
        ConfigurableWebApplicationContext wac, ServletContext sc) {
    // 1. 设置 context ID
    wac.setId(...);
    // 2. 绑定 ServletContext
    wac.setServletContext(sc);
    // 3. 设置配置文件位置
    String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
    if (configLocationParam != null) {
        wac.setConfigLocation(configLocationParam);
    }
    // 4. 初始化 PropertySources（包含 ServletContext 和 ServletConfig 参数）
    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment cwe) {
        cwe.initPropertySources(sc, null);
    }
    // 5. 调用 customizeContext() — 应用 ApplicationContextInitializer
    customizeContext(sc, wac);
    // 6. 刷新容器 — 触发 IoC 容器的完整初始化流程
    wac.refresh();
}
```

**默认上下文类型**来自 `ContextLoader.properties`，在静态初始化块中加载：

```java
// ContextLoader.java 第137行
static {
    ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
    defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
}
```

### 源码位置

| 组件 | 文件 |
|------|------|
| `ContextLoaderListener` | `spring-web/src/main/java/org/springframework/web/context/ContextLoaderListener.java` |
| `ContextLoader` | `spring-web/src/main/java/org/springframework/web/context/ContextLoader.java` |

---

## 4. FrameworkServlet

### WHAT

`org.springframework.web.servlet.FrameworkServlet` 是 `DispatcherServlet` 的抽象父类，作为 Spring Web 框架的 Servlet 基类。它整合了 Spring `ApplicationContext` 与 Servlet 规范，承担以下职责：

1. **管理每个 Servlet 专属的 `WebApplicationContext`**：创建、初始化、刷新、销毁
2. **重写所有 HTTP 方法对应的 `doXxx()` 方法**：统一委托给 `processRequest()` → `doService()`
3. **管理请求级别的线程上下文**：`LocaleContext` 和 `RequestAttributes`
4. **发布请求处理事件**：`ServletRequestHandledEvent`

```java
// FrameworkServlet.java 第143行
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

    public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";
    public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

    @Nullable
    private WebApplicationContext webApplicationContext;

    // ... 大量配置属性和生命周期方法
}
```

### WHY

为什么需要一个介于 `HttpServlet` 和 `DispatcherServlet` 之间的抽象层？

1. **关注点分离**：`FrameworkServlet` 专注于 Spring 上下文管理与 Servlet 容器集成，`DispatcherServlet` 专注于请求分发策略。每个类职责单一清晰。
2. **模板方法模式**：`FrameworkServlet` 定义了请求处理骨架（`service()` → `processRequest()` → `doService()`），子类（`DispatcherServlet`）只需实现 `doService()` 即可获得完整的生命周期管理。
3. **复用性**：如果一个应用中需要自定义的非 MVC Servlet，可以直接继承 `FrameworkServlet` 获取 Spring 上下文集成能力，而不需要拖入 `DispatcherServlet` 的全部 MVC 策略组件。
4. **可配置的上下文创建**：通过 `contextClass`、`contextConfigLocation`、`namespace` 等参数，可以灵活控制如何创建 Servlet 级别的 WebApplicationContext。

### HOW

继承层次：

```
jakarta.servlet.Servlet (接口)
  └── jakarta.servlet.GenericServlet
       └── jakarta.servlet.http.HttpServlet
            └── org.springframework.web.servlet.HttpServletBean   ← 将 init-param 映射为 Bean 属性
                 └── org.springframework.web.servlet.FrameworkServlet  ← 添加上下文管理
                      └── org.springframework.web.servlet.DispatcherServlet  ← 添加请求分发
```

```java
// 每个 DispatcherServlet 自动继承 FrameworkServlet 的所有能力
DispatcherServlet dispatcherServlet = new DispatcherServlet();
// 通过 setter 配置（继承自 FrameworkServlet）：
dispatcherServlet.setContextConfigLocation("classpath:web-config.xml");
dispatcherServlet.setPublishEvents(true);
dispatcherServlet.setThreadContextInheritable(false);
```

### 工作原理（源码分析）

#### 初始化流程

```
Web 容器加载 Servlet
  → HttpServletBean.init()                    ← 将 init-param 映射为 Bean 属性
    → FrameworkServlet.initServletBean()       ← 创建/查找 WebApplicationContext
      → FrameworkServlet.initWebApplicationContext()  ← 核心：上下文创建逻辑
        → createWebApplicationContext(rootContext)    ← 创建子上下文
      → FrameworkServlet.initFrameworkServlet()       ← 子类扩展点（空实现）
    → DispatcherServlet.onRefresh(context)    ← 回调：初始化 MVC 策略组件
      → initStrategies(context)
        → initHandlerMappings() / initHandlerAdapters() / ...
```

**关键初始化代码（FrameworkServlet.initServletBean()）：**

```java
// FrameworkServlet.java 第522行
@Override
protected final void initServletBean() throws ServletException {
    getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
    long startTime = System.currentTimeMillis();

    try {
        this.webApplicationContext = initWebApplicationContext();  // 核心步骤
        initFrameworkServlet();  // 空实现，子类扩展点
    }
    catch (ServletException | RuntimeException ex) {
        logger.error("Context initialization failed", ex);
        throw ex;
    }
}
```

#### 请求处理流程

**FrameworkServlet 重写了所有 HTTP 方法的 doXxx()：**

```java
// FrameworkServlet.java 第892-929行
@Override
protected final void doGet(HttpServletRequest request, HttpServletResponse response) {
    processRequest(request, response);
}

@Override
protected final void doPost(HttpServletRequest request, HttpServletResponse response) {
    processRequest(request, response);
}

@Override
protected final void doPut(HttpServletRequest request, HttpServletResponse response) {
    processRequest(request, response);
}

@Override
protected final void doDelete(HttpServletRequest request, HttpServletResponse response) {
    processRequest(request, response);
}
```

所有请求都汇聚到 `processRequest()` 方法：

```java
// FrameworkServlet.java 第985行
protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    Throwable failureCause = null;

    // 步骤1：记录并替换线程上下文
    LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
    LocaleContext localeContext = buildLocaleContext(request);

    RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
    ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

    initContextHolders(request, localeContext, requestAttributes);

    try {
        // 步骤2：委托给 doService()（由 DispatcherServlet 实现）
        doService(request, response);
    }
    catch (ServletException | IOException ex) {
        failureCause = ex;
        throw ex;
    }
    catch (Throwable ex) {
        failureCause = ex;
        throw new ServletException("Request processing failed: " + ex, ex);
    }
    finally {
        // 步骤3：清理线程上下文
        resetContextHolders(request, previousLocaleContext, previousAttributes);
        // 步骤4：完成请求属性生命周期
        if (requestAttributes != null) {
            requestAttributes.requestCompleted();
        }
        // 步骤5：发布请求处理完成事件
        publishRequestHandledEvent(request, response, startTime, failureCause);
    }
}
```

**PATCH 请求的特殊处理**——因为 `HttpServlet` 默认不支持 PATCH 方法：

```java
// FrameworkServlet.java 第872行
@Override
protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
    if (HttpMethod.PATCH.equals(httpMethod)) {
        processRequest(request, response);  // PATCH 直接走 processRequest
    }
    else {
        super.service(request, response);   // 其他方法走 HttpServlet 的标准分发
    }
}
```

### 源码位置

| 组件 | 文件 |
|------|------|
| `FrameworkServlet` | `spring-webmvc/src/main/java/org/springframework/web/servlet/FrameworkServlet.java`（共 1216 行） |
| `HttpServletBean` | `spring-webmvc/src/main/java/org/springframework/web/servlet/HttpServletBean.java` |

---

## 5. DispatcherServlet

### WHAT

`org.springframework.web.servlet.DispatcherServlet` 是 Spring MVC 框架的**核心前端控制器（Front Controller）**。它作为所有 HTTP 请求的统一入口，负责将请求分发给相应的处理器（Controller），协调整个请求-响应生命周期。

```java
// DispatcherServlet.java 第164行
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

    // 核心属性 —------ 9 大策略组件
    @Nullable private MultipartResolver multipartResolver;           // 文件上传解析
    @Nullable private LocaleResolver localeResolver;                 // 国际化/本地化解析
    @Nullable private ThemeResolver themeResolver;                   // 主题解析（6.0 已废弃）
    @Nullable private List<HandlerMapping> handlerMappings;          // 处理器映射
    @Nullable private List<HandlerAdapter> handlerAdapters;          // 处理器适配器
    @Nullable private List<HandlerExceptionResolver> handlerExceptionResolvers;  // 异常解析器
    @Nullable private RequestToViewNameTranslator viewNameTranslator; // 视图名转换器
    @Nullable private FlashMapManager flashMapManager;               // Flash 属性管理
    @Nullable private List<ViewResolver> viewResolvers;              // 视图解析器

    // 标准 Bean 名称常量
    public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";
    public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";
    public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";
    public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";
    // ...
}
```

### WHY

为什么 Spring MVC 需要一个中央调度器（DispatcherServlet）？

1. **前端控制器模式（Front Controller Pattern）**：将所有请求集中到单一入口，便于统一处理横切关注点（安全认证、日志记录、国际化、异常处理）。
2. **策略模式（Strategy Pattern）**：DispatcherServlet 通过可插拔的策略组件（HandlerMapping、HandlerAdapter、ViewResolver 等）实现高度灵活性。用户可以通过替换策略组件来改变整个框架行为，而无需修改核心代码。
3. **职责分离**：DispatcherServlet 本身不做任何实际的业务处理——它只负责"调度"：
   - `HandlerMapping` 负责"找谁处理"
   - `HandlerAdapter` 负责"怎么调用"
   - `ViewResolver` 负责"怎么展示"
   - `HandlerExceptionResolver` 负责"出错了怎么办"
4. **统一的非功能性处理**：多文件上传解析（`MultipartResolver`）、国际化（`LocaleResolver`）、Flash 属性传递——这些 Web 应用共同的横切需求由 DispatcherServlet 统一处理。

### HOW

DispatcherServlet 通过**9 大策略组件**协同工作：

```
请求进入 DispatcherServlet
    │
    ├─ [1] MultipartResolver: 检查是否为文件上传请求，如果是则包装请求
    │
    ├─ [2] LocaleResolver: 解析请求的 Locale（国际化）
    │
    ├─ [3] ThemeResolver: 解析主题（6.0 已废弃）
    │
    ├─ [4] HandlerMapping: 根据请求 URL 匹配处理器（Controller）
    │       ├─ RequestMappingHandlerMapping（基于 @RequestMapping 注解）
    │       ├─ BeanNameUrlHandlerMapping（基于 Bean 名称匹配 URL）
    │       └─ 其他自定义 HandlerMapping
    │
    ├─ [5] HandlerAdapter: 调用找到的处理器
    │       ├─ RequestMappingHandlerAdapter（处理 @RequestMapping 方法）
    │       ├─ HttpRequestHandlerAdapter（处理 HttpRequestHandler）
    │       ├─ SimpleControllerHandlerAdapter（处理 Controller 接口）
    │       └─ 其他自定义 HandlerAdapter
    │
    ├─ [6] HandlerExceptionResolver: 处理执行过程中的异常
    │
    ├─ [7] RequestToViewNameTranslator: 如果处理器未返回视图名，从请求推断
    │
    ├─ [8] ViewResolver: 将逻辑视图名解析为实际的 View 对象
    │
    └─ [9] FlashMapManager: 管理跨重定向的 Flash 属性
```

### 工作原理（源码分析）

#### 1. 策略组件初始化 — `onRefresh()` → `initStrategies()`

DispatcherServlet 重写了 `FrameworkServlet` 的 `onRefresh()` 方法，在上下文刷新时初始化所有策略组件：

```java
// DispatcherServlet.java 第495行
@Override
protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
}

// DispatcherServlet.java 第503行
protected void initStrategies(ApplicationContext context) {
    initMultipartResolver(context);          // 多部件解析器
    initLocaleResolver(context);             // 本地化解析器
    initThemeResolver(context);              // 主题解析器
    initHandlerMappings(context);            // 处理器映射器（列表）
    initHandlerAdapters(context);            // 处理器适配器（列表）
    initHandlerExceptionResolvers(context);  // 异常解析器（列表）
    initRequestToViewNameTranslator(context); // 请求到视图名转换器
    initViewResolvers(context);              // 视图解析器（列表）
    initFlashMapManager(context);            // FlashMap 管理器
}
```

**初始化策略**（以 `initHandlerMappings` 为例，其余类似）：

```java
// DispatcherServlet.java 第595行
private void initHandlerMappings(ApplicationContext context) {
    this.handlerMappings = null;

    if (this.detectAllHandlerMappings) {
        // 策略1：自动检测所有 HandlerMapping 类型的 Bean（包括祖先容器中的）
        Map<String, HandlerMapping> matchingBeans =
                BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
        if (!matchingBeans.isEmpty()) {
            this.handlerMappings = new ArrayList<>(matchingBeans.values());
            AnnotationAwareOrderComparator.sort(this.handlerMappings);  // 按 @Order 排序
        }
    }
    else {
        // 策略2：只查找名为 "handlerMapping" 的单个 Bean
        try {
            HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
            this.handlerMappings = Collections.singletonList(hm);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // 忽略
        }
    }

    // 兜底策略：使用 DispatcherServlet.properties 中定义的默认策略
    if (this.handlerMappings == null) {
        this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
    }
}
```

**默认策略加载**（从 `DispatcherServlet.properties` 文件）：

```java
// DispatcherServlet.java 第871行
protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
    if (defaultStrategies == null) {
        // 从 DispatcherServlet.properties 文件加载默认策略类名
        ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
        defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
    }

    String key = strategyInterface.getName();
    String value = defaultStrategies.getProperty(key);
    if (value != null) {
        String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
        List<T> strategies = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
            Object strategy = createDefaultStrategy(context, clazz);
            strategies.add((T) strategy);
        }
        return strategies;
    }
    return Collections.emptyList();
}
```

#### 2. doService() — 请求进入的入口

```java
// DispatcherServlet.java 第934行
@Override
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
    logRequest(request);  // 记录请求日志

    // 保存 include 请求的属性快照（用于 include 后的恢复）
    Map<String, Object> attributesSnapshot = null;
    if (WebUtils.isIncludeRequest(request)) {
        attributesSnapshot = new HashMap<>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                attributesSnapshot.put(attrName, request.getAttribute(attrName));
            }
        }
    }

    // 暴露框架对象给 Handler 和 View 使用
    request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
    request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
    request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
    request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

    // FlashMap 管理
    if (this.flashMapManager != null) {
        FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
        if (inputFlashMap != null) {
            request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
        }
        request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
    }

    // 解析 request path
    RequestPath previousRequestPath = null;
    if (this.parseRequestPath) {
        previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        ServletRequestPathUtils.parseAndCache(request);
    }

    try {
        doDispatch(request, response);  // ★ 核心：执行分发
    }
    finally {
        // 恢复 include 请求属性
        if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
            if (attributesSnapshot != null) {
                restoreAttributesAfterInclude(request, attributesSnapshot);
            }
        }
    }
}
```

### 源码位置

| 组件 | 文件 | 行数 |
|------|------|------|
| `DispatcherServlet` | `spring-webmvc/src/main/java/org/springframework/web/servlet/DispatcherServlet.java` | 1515 行 |
| `DispatcherServlet.properties` | `spring-webmvc/src/main/resources/org/springframework/web/servlet/DispatcherServlet.properties` | 默认策略配置 |

---

## 6. doDispatch() 执行流程 — 深度分析

### WHAT

`doDispatch()` 是 `DispatcherServlet` 中最核心的方法——它是真正执行请求分发的入口。从 `doService()` 委托过来后，整个过程包含以下完整步骤：

```
doDispatch(request, response)
    │
    ├─ [Step 1] checkMultipart(request)
    │     └─ 检查是否为 multipart 请求，如果是则包装为 MultipartHttpServletRequest
    │
    ├─ [Step 2] getHandler(processedRequest)
    │     └─ 遍历 handlerMappings，找到第一个能处理该请求的 HandlerExecutionChain
    │     └─ 如果找不到 Handler → noHandlerFound() → 返回 404
    │
    ├─ [Step 3] getHandlerAdapter(handler)
    │     └─ 遍历 handlerAdapters，找到第一个 supports(handler) 的适配器
    │
    ├─ [Step 4] 检查 Last-Modified 头（仅 GET/HEAD）
    │     └─ 如果资源未修改，直接返回 304 Not Modified
    │
    ├─ [Step 5] mappedHandler.applyPreHandle(request, response)
    │     └─ 执行拦截器的 preHandle() 方法
    │     └─ 如果任一拦截器返回 false，终止执行
    │
    ├─ [Step 6] ha.handle(request, response, handler)
    │     └─ 由 HandlerAdapter 实际调用处理器（Controller 方法）
    │     └─ 返回 ModelAndView 对象
    │
    ├─ [Step 7] 检查是否启动了异步处理
    │     └─ 如果 asyncManager.isConcurrentHandlingStarted()，直接返回
    │
    ├─ [Step 8] applyDefaultViewName(request, mv)
    │     └─ 如果 ModelAndView 没有视图名，从请求推断默认视图名
    │
    ├─ [Step 9] mappedHandler.applyPostHandle(request, response, mv)
    │     └─ 执行拦截器的 postHandle() 方法（逆序执行）
    │
    └─ [Step 10] processDispatchResult(request, response, mappedHandler, mv, exception)
          ├─ 如果有异常 → processHandlerException() → 解析异常为 ModelAndView
          ├─ 如果 ModelAndView 不为空 → render() → 视图渲染（JSP/Thymeleaf/JSON...）
          └─ mappedHandler.triggerAfterCompletion() → 执行拦截器的 afterCompletion()
```

### WHY

为什么 `doDispatch()` 要这样设计？

1. **流程标准化**：将请求处理抽象为一系列标准步骤（查找Handler → 适配 → 前置拦截 → 执行 → 后置拦截 → 视图渲染），无论使用什么类型的 Controller 都走同一套流程。
2. **扩展性**：通过 `HandlerMapping` 和 `HandlerAdapter` 两大策略接口，可以支持任意类型的处理器（不仅仅是 `@Controller` 注解的方法）。
3. **拦截器链**：`HandlerExecutionChain` 机制允许在处理器执行前后插入任意横切逻辑（日志、权限、事务等），类似于 Servlet Filter 但粒度更细。
4. **异常统一处理**：无论哪个步骤抛出异常，最终都会进入 `processDispatchResult()` 方法，由 `HandlerExceptionResolver` 链统一处理。
5. **异步支持**：在处理过程中随时可以启动异步处理（`asyncManager.isConcurrentHandlingStarted()`），DispatcherServlet 会感知并提前退出。

### 工作原理（源码分析）

#### 完整 `doDispatch()` 方法源码

```java
// DispatcherServlet.java 第1040行
@SuppressWarnings("deprecation")
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // ====================== Step 1: 文件上传处理 ======================
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // ====================== Step 2: 获取处理器 ======================
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            // ====================== Step 3: 获取处理器适配器 ======================
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // ====================== Step 4: 处理 Last-Modified ======================
            String method = request.getMethod();
            boolean isGet = HttpMethod.GET.matches(method);
            if (isGet || HttpMethod.HEAD.matches(method)) {
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                    return;  // 304 Not Modified
                }
            }

            // ====================== Step 5: 执行拦截器 preHandle ======================
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;  // 拦截器拦截了请求
            }

            // ====================== Step 6: 调用处理器 ======================
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            // ====================== Step 7: 异步处理检查 ======================
            if (asyncManager.isConcurrentHandlingStarted()) {
                return;
            }

            // ====================== Step 8: 默认视图名 ======================
            applyDefaultViewName(processedRequest, mv);

            // ====================== Step 9: 执行拦截器 postHandle ======================
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        catch (Throwable err) {
            dispatchException = new ServletException("Handler dispatch failed: " + err, err);
        }

        // ====================== Step 10: 处理分发结果 ======================
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    }
    catch (Exception ex) {
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    }
    catch (Throwable err) {
        triggerAfterCompletion(processedRequest, response, mappedHandler,
                new ServletException("Handler processing failed: " + err, err));
    }
    finally {
        // 异步处理清理
        if (asyncManager.isConcurrentHandlingStarted()) {
            if (mappedHandler != null) {
                mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
            }
        }
        else {
            // 清理文件上传资源
            if (multipartRequestParsed) {
                cleanupMultipart(processedRequest);
            }
        }
    }
}
```

#### 各子步骤深入分析

---

##### Step 2：`getHandler()` — 处理器查找

```java
// DispatcherServlet.java 第1271行
@Nullable
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    if (this.handlerMappings != null) {
        for (HandlerMapping mapping : this.handlerMappings) {
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;  // 返回第一个匹配的
            }
        }
    }
    return null;
}
```

**工作原理**：
- 遍历 `handlerMappings` 列表（按 `@Order` 排序）
- 调用每个 `HandlerMapping.getHandler(request)` 方法
- 返回第一个非 `null` 的 `HandlerExecutionChain`
- `HandlerExecutionChain` 包含处理器对象（Handler）和拦截器列表（`HandlerInterceptor[]`）

**以 `RequestMappingHandlerMapping` 为例**：它会根据 `@RequestMapping` 注解的 URL 模式匹配请求路径，找到对应的 `HandlerMethod`（封装了 Controller 类、方法、参数等元信息）。

`noHandlerFound()` 处理 404：

```java
// DispatcherServlet.java 第1289行
protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
    if (pageNotFoundLogger.isWarnEnabled()) {
        pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
    }
    if (this.throwExceptionIfNoHandlerFound) {
        throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
                new ServletServerHttpRequest(request).getHeaders());
    }
    else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
```

---

##### Step 3：`getHandlerAdapter()` — 适配器查找

```java
// DispatcherServlet.java 第1307行
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter adapter : this.handlerAdapters) {
            if (adapter.supports(handler)) {
                return adapter;  // 返回第一个支持该 handler 类型的适配器
            }
        }
    }
    throw new ServletException("No adapter for handler [" + handler +
            "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
}
```

**工作原理**：
- 遍历 `handlerAdapters` 列表
- 调用每个 `HandlerAdapter.supports(handler)` 检查是否支持该处理器类型
- 适配器模式（Adapter Pattern）：将不同类型的处理器统一为 `HandlerAdapter.handle()` 调用接口

**三大默认适配器**：

| 适配器 | 支持的处理器类型 | 说明 |
|--------|----------------|------|
| `RequestMappingHandlerAdapter` | `HandlerMethod` | 处理 `@RequestMapping` 注解的方法 |
| `HttpRequestHandlerAdapter` | `HttpRequestHandler` | 处理 `HttpRequestHandler` 接口 |
| `SimpleControllerHandlerAdapter` | `Controller` | 处理 `org.springframework.web.servlet.mvc.Controller` 接口 |

---

##### Step 5：`mappedHandler.applyPreHandle()` — 拦截器前置处理

`HandlerExecutionChain.applyPreHandle()` 按拦截器注册顺序执行所有拦截器的 `preHandle()` 方法。如果任一拦截器返回 `false`，后续拦截器和处理器都不会执行。

---

##### Step 6：`ha.handle()` — 实际执行处理器

适配器调用处理器方法。以 `RequestMappingHandlerAdapter.handle()` 为例：
1. 通过反射调用 Controller 方法
2. 处理参数解析（`@RequestParam`、`@PathVariable`、`@RequestBody` 等）
3. 处理返回值（`ModelAndView`、`@ResponseBody`、String 视图名等）
4. 返回 `ModelAndView` 对象

---

##### Step 10：`processDispatchResult()` — 结果处理

```java
// DispatcherServlet.java 第1138行
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
        @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
        @Nullable Exception exception) throws Exception {

    boolean errorView = false;

    if (exception != null) {
        if (exception instanceof ModelAndViewDefiningException) {
            // 直接使用异常中携带的 ModelAndView
            logger.debug("ModelAndViewDefiningException encountered", exception);
            mv = ((ModelAndViewDefiningException) exception).getModelAndView();
        }
        else {
            // 尝试通过 HandlerExceptionResolver 解析异常为 ModelAndView
            Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
            mv = processHandlerException(request, response, handler, exception);
            errorView = (mv != null);
        }
    }

    // 渲染视图
    if (mv != null && !mv.wasCleared()) {
        render(mv, request, response);
        if (errorView) {
            WebUtils.clearErrorRequestAttributes(request);
        }
    }

    if (mappedHandler != null) {
        // 触发 afterCompletion（异常已处理，传 null）
        mappedHandler.triggerAfterCompletion(request, response, null);
    }
}
```

**`processHandlerException()` 异常解析源码**：

```java
// DispatcherServlet.java 第1330行
@Nullable
protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
        @Nullable Object handler, Exception ex) throws Exception {

    request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

    ModelAndView exMv = null;
    if (this.handlerExceptionResolvers != null) {
        for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
            exMv = resolver.resolveException(request, response, handler, ex);
            if (exMv != null) {
                break;  // 第一个成功解析的就用
            }
        }
    }
    if (exMv != null) {
        if (exMv.isEmpty()) {
            request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
            return null;
        }
        WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
        return exMv;
    }

    throw ex;  // 无法解析的异常继续向上抛出
}
```

**`render()` 视图渲染源码**：

```java
// DispatcherServlet.java 第1380行
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
    // 1. 解析 Locale
    Locale locale = (this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
    response.setLocale(locale);

    View view;
    String viewName = mv.getViewName();
    if (viewName != null) {
        // 2. 通过 ViewResolver 将逻辑视图名解析为 View 对象
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
        if (view == null) {
            throw new ServletException("Could not resolve view with name '" + mv.getViewName() + "'");
        }
    }
    else {
        view = mv.getView();
        if (view == null) {
            throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a View object");
        }
    }

    // 3. 设置响应状态码
    if (mv.getStatus() != null) {
        response.setStatus(mv.getStatus().value());
    }

    // 4. 委托 View 对象渲染
    view.render(mv.getModelInternal(), request, response);
}
```

### 完整的请求处理时序

```
HTTP 请求
  │
  ▼
HttpServlet.service()
  ▼
FrameworkServlet.processRequest()         ← 线程上下文设置
  │
  ▼
DispatcherServlet.doService()             ← 暴露框架对象到 request 属性
  │
  ▼
DispatcherServlet.doDispatch()            ← ★ 核心分发方法
  │
  ├─ checkMultipart()                     ← multipart 处理
  ├─ getHandler()                         ← [HandlerMapping 链] 查找处理器
  ├─ getHandlerAdapter()                  ← [HandlerAdapter 链] 查找适配器
  ├─ getLastModified()                    ← Last-Modified 检查
  ├─ applyPreHandle()                     ← [拦截器链] preHandle
  ├─ ha.handle()                          ← 执行处理器
  ├─ applyPostHandle()                    ← [拦截器链] postHandle
  │
  └─ processDispatchResult()              ← 结果处理
       ├─ processHandlerException()       ← [HandlerExceptionResolver 链] 异常解析
       ├─ render()
       │    ├─ resolveViewName()          ← [ViewResolver 链] 视图解析
       │    └─ view.render()              ← 视图渲染
       └─ triggerAfterCompletion()        ← [拦截器链] afterCompletion
```

### 源码位置

| 方法/组件 | DispatcherServlet.java 行号 |
|-----------|---------------------------|
| `doService()` | 第 934-986 行 |
| `doDispatch()` | 第 1040-1120 行 |
| `getHandler()` | 第 1271-1281 行 |
| `getHandlerAdapter()` | 第 1307-1317 行 |
| `processDispatchResult()` | 第 1138-1178 行 |
| `processHandlerException()` | 第 1330-1369 行 |
| `render()` | 第 1380-1422 行 |
| `resolveViewName()` | 第 1450-1462 行 |
| `checkMultipart()` | 第 1205-1233 行 |
| `initStrategies()` | 第 503-513 行 |

---

## 总结

Spring MVC 的请求分发架构体现了经典的设计模式：

| 设计模式 | 体现位置 | 说明 |
|----------|---------|------|
| **前端控制器** | `DispatcherServlet` | 统一入口，集中处理请求 |
| **策略模式** | 9 大策略组件 | 可插拔的 HandlerMapping、HandlerAdapter、ViewResolver 等 |
| **模板方法** | `FrameworkServlet.processRequest()` | 定义请求处理骨架，子类实现 `doService()` |
| **适配器模式** | `HandlerAdapter` | 将不同类型的处理器统一为一致的调用接口 |
| **责任链模式** | `HandlerExecutionChain` + `HandlerInterceptor` | 拦截器链按顺序执行 |
| **组合模式** | `WebApplicationContext` 父子容器 | Root Context 共享，Servlet Context 隔离 |

### 请求分发核心流程速记

```
1. checkMultipart() → 2. getHandler() → 3. getHandlerAdapter()
→ 4. Last-Modified 检查 → 5. applyPreHandle()
→ 6. ha.handle() → 7. applyPostHandle()
→ 8. processDispatchResult() → 9. render() → 10. afterCompletion()
```

### 关键源码文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| `DispatcherServlet.java` | `spring-webmvc/.../web/servlet/` | 核心前端控制器，1515 行 |
| `FrameworkServlet.java` | `spring-webmvc/.../web/servlet/` | Servlet 基类，上下文管理，1216 行 |
| `HttpServletBean.java` | `spring-webmvc/.../web/servlet/` | init-param 到 Bean 属性的映射 |
| `WebApplicationContext.java` | `spring-web/.../web/context/` | Web 上下文接口定义 |
| `ContextLoaderListener.java` | `spring-web/.../web/context/` | Root Context 引导监听器 |
| `ContextLoader.java` | `spring-web/.../web/context/` | Root Context 创建逻辑 |
| `ConfigurableWebApplicationContext.java` | `spring-web/.../web/context/` | 可配置的 Web 上下文接口 |
