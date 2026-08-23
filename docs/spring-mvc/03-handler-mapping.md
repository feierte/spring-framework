# Spring MVC 第三阶段：HandlerMapping 路由映射

## 概述

在 Spring MVC 的请求处理流程中，`DispatcherServlet` 接收到 HTTP 请求后，首先要完成的工作就是**路由映射**——确定由哪个处理器（Handler）来处理当前请求。这个职责由 `HandlerMapping` 接口及其实现类来承担。

Spring Framework 6.0 提供了多种 `HandlerMapping` 实现，覆盖了从基于注解的 `@RequestMapping` 到函数式路由 `RouterFunction` 的多种映射方式。本文将深入分析各实现的源码工作原理。

> **类继承体系总览：**
> ```
> HandlerMapping (接口)
>   └── AbstractHandlerMapping (抽象基类, 模板方法)
>         ├── AbstractUrlHandlerMapping (基于URL模式匹配)
>         │     ├── SimpleUrlHandlerMapping (手动配置URL→Handler映射)
>         │     └── AbstractDetectingUrlHandlerMapping (自动检测)
>         │           └── BeanNameUrlHandlerMapping (Bean名称以"/"开头=URL)
>         ├── AbstractHandlerMethodMapping<T> (泛型, 方法级映射)
>         │     └── RequestMappingInfoHandlerMapping
>         │           └── RequestMappingHandlerMapping (@Controller + @RequestMapping)
>         └── RouterFunctionMapping (函数式路由, RouterFunction)
> ```

---

## 1. HandlerMapping 接口

### WHAT

`org.springframework.web.servlet.HandlerMapping` 是 Spring MVC 中定义请求与处理器之间映射关系的**顶层接口**。它只有一个核心方法 `getHandler(HttpServletRequest)`，返回一个 `HandlerExecutionChain` 对象。

### WHY

将路由逻辑抽象为接口，使得 Spring MVC 可以支持多种映射策略（注解、URL 模式、Bean 名称、函数式路由等），`DispatcherServlet` 无需关心具体映射方式，只需遍历所有 `HandlerMapping` 实现即可。

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerMapping.java

public interface HandlerMapping {

    // 请求属性常量：存储最佳匹配的处理器
    String BEST_MATCHING_HANDLER_ATTRIBUTE =
        HandlerMapping.class.getName() + ".bestMatchingHandler";

    // 请求属性常量：存储最佳匹配的 URL 模式
    String BEST_MATCHING_PATTERN_ATTRIBUTE =
        HandlerMapping.class.getName() + ".bestMatchingPattern";

    // 请求属性常量：存储 URI 模板变量（如 @PathVariable 值）
    String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
        HandlerMapping.class.getName() + ".uriTemplateVariables";

    /**
     * 核心方法：根据请求返回处理器执行链。
     * 返回 null 表示该 HandlerMapping 无法处理当前请求，
     * DispatcherServlet 会继续尝试下一个 HandlerMapping。
     */
    @Nullable
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

    /**
     * 是否启用 PathPattern 解析模式（Spring 5.3+ 引入）。
     * 默认返回 false，使用 AntPathMatcher 字符串匹配。
     */
    default boolean usesPathPatterns() {
        return false;
    }
}
```

**工作原理：**

1. `DispatcherServlet.doDispatch()` 调用 `getHandler(request)` 遍历所有已注册的 `HandlerMapping` Bean
2. 每个 `HandlerMapping` 返回 `HandlerExecutionChain`（包含处理器 + 拦截器链）
3. 第一个返回非 null 的 `HandlerExecutionChain` 即为匹配结果
4. `HandlerMapping` 按 `Ordered` 接口排序，优先级高的先执行

**代码示例：**

```java
// 自定义简单 HandlerMapping（根据请求参数决定处理器）
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerExecutionChain;
import jakarta.servlet.http.HttpServletRequest;

public class ParamBasedHandlerMapping implements HandlerMapping {

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        String action = request.getParameter("action");
        if ("list".equals(action)) {
            return new HandlerExecutionChain(new ListController());
        } else if ("detail".equals(action)) {
            return new HandlerExecutionChain(new DetailController());
        }
        return null; // 不匹配，交给下一个 HandlerMapping
    }
}
```

---

## 2. AbstractHandlerMapping

### WHAT

`org.springframework.web.servlet.handler.AbstractHandlerMapping` 是所有 `HandlerMapping` 实现的**抽象基类**。它实现了模板方法模式，定义了处理器查找、拦截器链组装、CORS 处理等通用逻辑，子类只需实现 `getHandlerInternal()`。

### WHY

将拦截器管理、CORS 支持、默认处理器、排序等通用功能集中实现，避免在各实现类中重复编写。

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/handler/AbstractHandlerMapping.java

public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
        implements HandlerMapping, Ordered, BeanNameAware {

    // 默认处理器（无匹配时的兜底）
    @Nullable
    private Object defaultHandler;

    // 路径模式解析器（6.0 默认启用 PathPatternParser）
    @Nullable
    private PathPatternParser patternParser = new PathPatternParser();

    // Ant 风格路径匹配器（patternParser 为 null 时使用）
    private PathMatcher pathMatcher = new AntPathMatcher();

    // 拦截器列表
    private final List<Object> interceptors = new ArrayList<>();

    // 适配后的拦截器列表（已将 WebRequestInterceptor 适配为 HandlerInterceptor）
    private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

    // CORS 处理器
    private CorsProcessor corsProcessor = new DefaultCorsProcessor();

    // 排序优先级
    private int order = Ordered.LOWEST_PRECEDENCE;
```

**核心方法 `getHandler()` 流程（模板方法）：**

```java
// 第 503 行
@Override
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    // 1. 调用子类的 getHandlerInternal() 查找处理器
    Object handler = getHandlerInternal(request);

    // 2. 无匹配时使用默认处理器
    if (handler == null) {
        handler = getDefaultHandler();
    }
    if (handler == null) {
        return null;
    }

    // 3. 如果返回的是 Bean 名称字符串，从容器获取实际 Bean
    if (handler instanceof String handlerName) {
        handler = obtainApplicationContext().getBean(handlerName);
    }

    // 4. 初始化 lookupPath（供拦截器使用）
    if (!ServletRequestPathUtils.hasCachedPath(request)) {
        initLookupPath(request);
    }

    // 5. 构建 HandlerExecutionChain，组装拦截器
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

    // 6. 处理 CORS（跨域）
    if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
        CorsConfiguration config = getCorsConfiguration(handler, request);
        if (getCorsConfigurationSource() != null) {
            CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
            config = (globalConfig != null ? globalConfig.combine(config) : config);
        }
        if (config != null) {
            config.validateAllowCredentials();
        }
        executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
    }

    return executionChain;
}

// 子类必须实现的抽象方法
@Nullable
protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;
```

**拦截器链组装 `getHandlerExecutionChain()`：**

```java
protected HandlerExecutionChain getHandlerExecutionChain(
        Object handler, HttpServletRequest request) {

    // 如果 handler 本身已经是 HandlerExecutionChain，则复用
    HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain hec ?
            hec : new HandlerExecutionChain(handler));

    // 遍历适配好的拦截器
    for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
        if (interceptor instanceof MappedInterceptor mappedInterceptor) {
            // 路径匹配的拦截器：仅匹配时添加
            if (mappedInterceptor.matches(request)) {
                chain.addInterceptor(mappedInterceptor.getInterceptor());
            }
        } else {
            // 全局拦截器：总是添加
            chain.addInterceptor(interceptor);
        }
    }
    return chain;
}
```

**初始化流程（`initApplicationContext`）：**

```java
@Override
protected void initApplicationContext() throws BeansException {
    extendInterceptors(this.interceptors);           // 扩展点，子类可覆盖
    detectMappedInterceptors(this.adaptedInterceptors); // 自动检测 MappedInterceptor Bean
    initInterceptors();                              // 适配 WebRequestInterceptor → HandlerInterceptor
}
```

**代码示例：**

```java
// 配置拦截器和 CORS
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/api/**")       // 匹配的路径
                .excludePathPatterns("/api/health"); // 排除的路径
    }
}

// 自定义拦截器
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        System.out.println("Request URI: " + request.getRequestURI());
        return true; // 返回 false 将中断请求
    }
}
```

---

## 3. AbstractHandlerMethodMapping

### WHAT

`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping<T>` 是专门为**方法级处理器**（`HandlerMethod`）设计的抽象基类。泛型 `T` 表示映射条件类型（如 `RequestMappingInfo`）。它负责在容器启动时扫描所有候选 Bean，检测其中的处理器方法并建立映射注册表。

### WHY

基于注解的 `@RequestMapping` 和函数式端点都需要在启动时扫描 + 运行时匹配方法级处理器。将扫描、注册、查找的通用逻辑抽象为泛型基类，避免重复代码。核心数据结构 `MappingRegistry` 使用读写锁保证并发安全。

### HOW（源码级工作原理）

**核心数据结构 `MappingRegistry`：**

```java
// 内部类，第 570 行
class MappingRegistry {
    // 主注册表：映射条件 T → 注册信息
    private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

    // 直接路径查找表："/users" → 映射条件列表（O(1) 查找）
    private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

    // 映射名称查找表："UC#getUser" → HandlerMethod 列表
    private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

    // CORS 配置查找表：HandlerMethod → CorsConfiguration
    private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

    // 读写锁（保证并发安全）
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
}
```

**启动扫描流程 `afterPropertiesSet()` → `initHandlerMethods()`：**

```java
// 实现 InitializingBean，容器启动时自动调用
@Override
public void afterPropertiesSet() {
    initHandlerMethods();
}

protected void initHandlerMethods() {
    // 1. 获取所有候选 Bean 名称
    for (String beanName : getCandidateBeanNames()) {
        if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
            processCandidateBean(beanName);
        }
    }
    // 2. 回调：所有映射初始化完成
    handlerMethodsInitialized(getHandlerMethods());
}

protected void processCandidateBean(String beanName) {
    Class<?> beanType = obtainApplicationContext().getType(beanName);
    // 判断是否为处理器类型（由子类 isHandler() 决定）
    if (beanType != null && isHandler(beanType)) {
        detectHandlerMethods(beanName);
    }
}
```

**检测处理器方法 `detectHandlerMethods()`：**

```java
protected void detectHandlerMethods(Object handler) {
    Class<?> handlerType = (handler instanceof String beanName ?
            obtainApplicationContext().getType(beanName) : handler.getClass());

    if (handlerType != null) {
        Class<?> userType = ClassUtils.getUserClass(handlerType);

        // 使用 MethodIntrospector.selectMethods 反射扫描所有方法
        Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
                (MethodIntrospector.MetadataLookup<T>) method -> {
                    try {
                        return getMappingForMethod(method, userType); // 子类实现
                    } catch (Throwable ex) {
                        throw new IllegalStateException(
                            "Invalid mapping on handler class [" +
                            userType.getName() + "]: " + method, ex);
                    }
                });

        // 注册每个找到的映射方法
        methods.forEach((method, mapping) -> {
            Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
            registerHandlerMethod(handler, invocableMethod, mapping);
        });
    }
}
```

**注册方法 `MappingRegistry.register()`：**

```java
public void register(T mapping, Object handler, Method method) {
    this.readWriteLock.writeLock().lock();
    try {
        HandlerMethod handlerMethod = createHandlerMethod(handler, method);
        validateMethodMapping(handlerMethod, mapping); // 检查重复映射

        // 1. 提取直接路径（非模式路径），加入 pathLookup
        Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
        for (String path : directPaths) {
            this.pathLookup.add(path, mapping);
        }

        // 2. 命名策略
        String name = null;
        if (getNamingStrategy() != null) {
            name = getNamingStrategy().getName(handlerMethod, mapping);
            addMappingName(name, handlerMethod);
        }

        // 3. CORS 配置
        CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
        if (corsConfig != null) {
            corsConfig.validateAllowCredentials();
            this.corsLookup.put(handlerMethod, corsConfig);
        }

        // 4. 存入主注册表
        this.registry.put(mapping,
            new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));
    }
    finally {
        this.readWriteLock.writeLock().unlock();
    }
}
```

**运行时查找 `getHandlerInternal()` → `lookupHandlerMethod()`：**

```java
@Override
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    String lookupPath = initLookupPath(request);
    this.mappingRegistry.acquireReadLock();
    try {
        HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
        return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
    }
    finally {
        this.mappingRegistry.releaseReadLock();
    }
}

protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) {
    List<Match> matches = new ArrayList<>();

    // 1. 优先从直接路径查找（O(1)，性能优化）
    List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
    if (directPathMatches != null) {
        addMatchingMappings(directPathMatches, matches, request);
    }

    // 2. 若无直接匹配，遍历所有注册映射进行模式匹配
    if (matches.isEmpty()) {
        addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
    }

    if (!matches.isEmpty()) {
        // 3. 排序，选出最佳匹配
        Match bestMatch = matches.get(0);
        if (matches.size() > 1) {
            Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
            matches.sort(comparator);
            bestMatch = matches.get(0);

            // 4. 检查是否有歧义（两个匹配得分相同）
            Match secondBestMatch = matches.get(1);
            if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                throw new IllegalStateException("Ambiguous handler methods mapped for '"
                    + request.getRequestURI() + "': ...");
            }
        }
        handleMatch(bestMatch.mapping, lookupPath, request);
        return bestMatch.getHandlerMethod();
    }
    else {
        return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), lookupPath, request);
    }
}
```

**代码示例：**

```java
// 查看所有已注册映射（调试用）
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.method.HandlerMethod;
import java.util.Map;

@Component
public class MappingInspector implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();
        mappings.forEach((info, method) -> {
            System.out.println(info + " -> " + method);
        });
    }
}
```

---

## 4. RequestMappingHandlerMapping

### WHAT

`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping` 是 Spring MVC 中**最核心的 HandlerMapping 实现**。它扫描所有标注 `@Controller` 的类，从类和方法级别提取 `@RequestMapping` 注解信息，构建 `RequestMappingInfo` 映射表。

### WHY

基于注解的声明式路由是 Spring MVC 的主流开发方式。`RequestMappingHandlerMapping` 将 `@RequestMapping`、`@GetMapping`、`@PostMapping` 等注解自动转换为路由映射，无需手动配置。

### HOW（源码级工作原理）

**类层次：**
```
AbstractHandlerMapping
  → AbstractHandlerMethodMapping<RequestMappingInfo>
    → RequestMappingInfoHandlerMapping
      → RequestMappingHandlerMapping
```

**判断是否为处理器类型（`isHandler`）：**

```java
// RequestMappingHandlerMapping.java 第 287 行
@Override
protected boolean isHandler(Class<?> beanType) {
    // 仅处理标注了 @Controller 注解的类
    return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
}
```

**从方法提取映射信息（`getMappingForMethod`）：**

```java
// RequestMappingHandlerMapping.java 第 301 行
@Override
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    // 1. 从方法上提取 @RequestMapping 注解，创建 RequestMappingInfo
    RequestMappingInfo info = createRequestMappingInfo(method);
    if (info != null) {
        // 2. 从类上提取 @RequestMapping 注解，与方法的合并
        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
        if (typeInfo != null) {
            info = typeInfo.combine(info);
        }
        // 3. 查找路径前缀（如通过 WebFlux 的 pathPrefix 配置）
        String prefix = getPathPrefix(handlerType);
        if (prefix != null) {
            info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
        }
    }
    return info;
}
```

**创建 `RequestMappingInfo`：**

```java
// RequestMappingHandlerMapping.java 第 383 行
protected RequestMappingInfo createRequestMappingInfo(
        RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

    return RequestMappingInfo
            .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
            .methods(requestMapping.method())
            .params(requestMapping.params())
            .headers(requestMapping.headers())
            .consumes(requestMapping.consumes())
            .produces(requestMapping.produces())
            .mappingName(requestMapping.name())
            .customCondition(customCondition)
            .options(this.config) // 使用当前配置（PathPattern 模式、尾斜杠匹配等）
            .build();
}
```

### 完整扫描启动流程（源码级深度分析）

```
DispatcherServlet.initStrategies()
  → initHandlerMappings()
    → ApplicationContext.getBean(HandlerMapping.class) // 触发所有 HandlerMapping Bean 初始化
      → RequestMappingHandlerMapping.afterPropertiesSet()  // InitializingBean
        → AbstractHandlerMethodMapping.afterPropertiesSet()
          → initHandlerMethods()
```

**详细步骤：**

1. **`afterPropertiesSet()`**（`RequestMappingHandlerMapping` 重写，第 203 行）：
   - 创建 `BuilderConfiguration`，配置 `PathPatternParser` 或 `PathMatcher`
   - 调用 `super.afterPropertiesSet()` 进入 `AbstractHandlerMethodMapping`

2. **`initHandlerMethods()`**（`AbstractHandlerMethodMapping` 第 221 行）：
   - 调用 `getCandidateBeanNames()` 获取容器中所有 Bean 名称
   - 遍历每个 Bean，跳过 `scopedTarget.` 前缀的代理目标

3. **`processCandidateBean(beanName)`**（第 253 行）：
   - 通过 `applicationContext.getType(beanName)` 获取类型（**不触发实例化**）
   - 调用 `isHandler(beanType)` 判断是否为 `@Controller`

4. **`detectHandlerMethods(beanName)`**（第 274 行）：
   - 获取用户类（处理 CGLIB 代理）
   - 使用 `MethodIntrospector.selectMethods()` 反射遍历所有方法
   - 对每个方法调用 `getMappingForMethod(method, userType)`：
     - `createRequestMappingInfo(method)` 从方法注解创建条件
     - `createRequestMappingInfo(handlerType)` 从类注解创建条件
     - `typeInfo.combine(info)` 合并类级别和方法级别的条件
   - 过滤掉返回 null 的方法（无 `@RequestMapping`）
   - 调用 `registerHandlerMethod()` 注册

5. **`MappingRegistry.register()`**（第 629 行）：
   - 创建 `HandlerMethod` 对象
   - 提取直接路径（如 `/users`）加入 `pathLookup`（O(1) 快速查找）
   - 提取 CORS 配置（`@CrossOrigin` 注解）
   - 存入 `registry` 主映射表

**代码示例：**

```java
@Controller
@RequestMapping("/users")  // 类级别映射：所有方法路径前缀为 /users
public class UserController {

    // 最终路径：GET /users
    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    // 最终路径：GET /users/{id}
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "users/detail";
    }

    // 最终路径：POST /users
    @PostMapping
    public String create(@Valid @ModelAttribute User user) {
        userService.save(user);
        return "redirect:/users";
    }

    // 最终路径：PUT /users/{id}
    @PutMapping("/{id}")
    @ResponseBody
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userService.update(user);
    }

    // 最终路径：DELETE /users/{id}
    @DeleteMapping("/{id}")
    @ResponseBody
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

---

## 5. BeanNameUrlHandlerMapping

### WHAT

`org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping` 是 Spring MVC 的**默认 HandlerMapping** 之一。它将 Bean 名称以 `/` 开头的 Bean 作为 URL 映射的处理器。

### WHY

提供一种无需注解的简单映射方式，适合简单的控制器场景。Bean 名称即 URL 路径，直观明了。

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/handler/BeanNameUrlHandlerMapping.java

public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

    @Override
    protected String[] determineUrlsForHandler(String beanName) {
        List<String> urls = new ArrayList<>();

        // Bean 名称以 "/" 开头即为 URL 路径
        if (beanName.startsWith("/")) {
            urls.add(beanName);
        }

        // 别名也检查
        String[] aliases = obtainApplicationContext().getAliases(beanName);
        for (String alias : aliases) {
            if (alias.startsWith("/")) {
                urls.add(alias);
            }
        }
        return StringUtils.toStringArray(urls);
    }
}
```

**工作原理：** 继承自 `AbstractDetectingUrlHandlerMapping`，在容器初始化时遍历所有 Bean，检查名称是否以 `/` 开头，如果是则自动注册为 URL 映射。

**代码示例：**

```java
// 方式一：实现 Controller 接口（传统方式）
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component("/hello")  // Bean 名称即 URL 路径
public class HelloController implements Controller {

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
                                       HttpServletResponse response) {
        return new ModelAndView("hello", "message", "Hello World!");
    }
}

// 方式二：使用 HttpRequestHandler
@Component("/download")
public class FileDownloadHandler implements HttpRequestHandler {

    @Override
    public void handleRequest(HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        // 文件下载逻辑...
    }
}
```

访问 `http://localhost:8080/hello` 时会自动映射到 `/hello` Bean。

---

## 6. RouterFunctionMapping

### WHAT

`org.springframework.web.servlet.function.support.RouterFunctionMapping` 是 Spring MVC 5.2 引入的**函数式路由**支持。它使用 `RouterFunction` 和 `HandlerFunction`（函数式编程风格）定义路由规则。

### WHY

函数式路由提供了与注解不同的编程范式：
- 请求处理逻辑在**一处定义**（路由 + 处理在一起）
- 避免反射调用开销，更接近原生 Lambda 调用
- 类型安全，编译时检查

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/function/support/RouterFunctionMapping.java

public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

    @Nullable
    private RouterFunction<?> routerFunction;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 1. 未手动设置 RouterFunction 时，从容器中自动检测
        if (this.routerFunction == null) {
            initRouterFunctions();
        }
        // 2. 初始化消息转换器
        if (CollectionUtils.isEmpty(this.messageConverters)) {
            initMessageConverters();
        }
        // 3. 强制使用 PathPatternParser（函数式路由必须用 PathPattern）
        if (this.routerFunction != null) {
            PathPatternParser patternParser = getPatternParser();
            if (patternParser == null) {
                patternParser = new PathPatternParser();
                setPatternParser(patternParser);
            }
            RouterFunctions.changeParser(this.routerFunction, patternParser);
        }
    }

    private void initRouterFunctions() {
        // 从容器中获取所有 RouterFunction Bean，按 @Order 排序
        List<RouterFunction<?>> routerFunctions = obtainApplicationContext()
                .getBeanProvider(RouterFunction.class)
                .orderedStream()
                .map(router -> (RouterFunction<?>) router)
                .collect(Collectors.toList());

        // 合并为一个 RouterFunction（通过 RouterFunction::andOther）
        this.routerFunction = routerFunctions.stream()
                .reduce(RouterFunction::andOther)
                .orElse(null);
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest servletRequest) throws Exception {
        if (this.routerFunction != null) {
            // 将 HttpServletRequest 包装为 ServerRequest
            ServerRequest request = ServerRequest.create(servletRequest, this.messageConverters);
            // 执行路由匹配
            HandlerFunction<?> handlerFunction = this.routerFunction.route(request).orElse(null);
            return handlerFunction;
        }
        return null;
    }
}
```

**核心流程：**
1. 容器启动时自动发现所有 `RouterFunction` Bean
2. 通过 `RouterFunction::andOther` 合并为一个组合路由
3. 请求到达时，包装为 `ServerRequest`，调用 `routerFunction.route(request)`
4. 返回匹配的 `HandlerFunction`（即 Lambda）

**代码示例：**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> userRoutes() {
        return route()
            .GET("/api/users", this::listUsers)
            .GET("/api/users/{id}", this::getUser)
            .POST("/api/users", this::createUser)
            .PUT("/api/users/{id}", this::updateUser)
            .DELETE("/api/users/{id}", this::deleteUser)
            .build();
    }

    private ServerResponse listUsers(ServerRequest request) {
        return ok().body(userService.findAll());
    }

    private ServerResponse getUser(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return ok().body(userService.findById(id));
    }

    private ServerResponse createUser(ServerRequest request) throws Exception {
        User user = request.body(User.class);
        userService.save(user);
        return ServerResponse.created().build();
    }

    private ServerResponse updateUser(ServerRequest request) throws Exception {
        Long id = Long.valueOf(request.pathVariable("id"));
        User user = request.body(User.class);
        user.setId(id);
        return ok().body(userService.update(user));
    }

    private ServerResponse deleteUser(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        userService.delete(id);
        return ServerResponse.noContent().build();
    }
}
```

---

## 7. SimpleUrlHandlerMapping

### WHAT

`org.springframework.web.servlet.handler.SimpleUrlHandlerMapping` 是**手动配置 URL 到处理器的映射**。通过 `urlMap` 或 `mappings` 属性显式指定映射关系。

### WHY

适合需要精确控制 URL 映射的场景，例如：
- 静态资源的精确映射
- 非注解风格的遗留项目迁移
- 需要 URL 参数化但不想用注解

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/handler/SimpleUrlHandlerMapping.java

public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

    private final Map<String, Object> urlMap = new LinkedHashMap<>();

    public void setMappings(Properties mappings) {
        CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
    }

    public void setUrlMap(Map<String, ?> urlMap) {
        this.urlMap.putAll(urlMap);
    }

    @Override
    public void initApplicationContext() throws BeansException {
        super.initApplicationContext();
        registerHandlers(this.urlMap);  // 注册所有映射
    }

    protected void registerHandlers(Map<String, Object> urlMap) {
        urlMap.forEach((url, handler) -> {
            if (!url.startsWith("/")) {
                url = "/" + url;  // 自动补全斜杠
            }
            if (handler instanceof String) {
                handler = ((String) handler).trim();
            }
            registerHandler(url, handler);  // 调用父类注册
        });
    }
}
```

**工作原理：** 继承自 `AbstractUrlHandlerMapping`，在 `initApplicationContext()` 阶段将配置的 URL 映射注册到 `handlerMap` 中。运行时通过最长路径匹配策略查找处理器。

**代码示例：**

```java
// XML 配置（传统方式）
// <bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
//     <property name="mappings">
//         <props>
//             <prop key="/welcome.htm">welcomeController</prop>
//             <prop key="/help.htm">helpController</prop>
//             <prop key="/*/account.htm">accountController</prop>
//         </props>
//     </property>
// </bean>

// Java Config 方式
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
public class UrlMappingConfig {

    @Bean
    public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(1);

        Map<String, Object> urlMap = new LinkedHashMap<>();
        urlMap.put("/welcome", new WelcomeController());
        urlMap.put("/admin/**", new AdminController());
        urlMap.put("/api/legacy/**", "legacyHandlerBean");  // 引用 Bean 名称

        mapping.setUrlMap(urlMap);
        return mapping;
    }
}
```

---

## 8. HandlerInterceptor 拦截器

### WHAT

`org.springframework.web.servlet.HandlerInterceptor` 是 Spring MVC 中**处理器拦截器接口**，允许在处理器执行前后插入自定义逻辑。与 Servlet Filter 不同，拦截器运行在 Spring MVC 上下文内，可以访问 `HandlerMethod` 等 Spring 特定对象。

### WHY

拦截器用于横切关注点的统一处理：日志、权限检查、性能监控、国际化、请求参数预处理等。与 `HandlerMapping` 紧密集成，每个 `HandlerMapping` 可以配置独立的拦截器链。

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerInterceptor.java

public interface HandlerInterceptor {

    /**
     * 前置处理：在 HandlerAdapter 调用处理器之前执行。
     * 返回 true：继续执行链中后续拦截器和处理器
     * 返回 false：中断请求，DispatcherServlet 认为拦截器已处理响应
     */
    default boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response, Object handler)
            throws Exception {
        return true;
    }

    /**
     * 后置处理：在处理器执行之后、视图渲染之前执行。
     * 可以修改 ModelAndView。
     */
    default void postHandle(HttpServletRequest request,
                            HttpServletResponse response, Object handler,
                            @Nullable ModelAndView modelAndView) throws Exception {
    }

    /**
     * 完成处理：在视图渲染完成后执行（无论成功或异常）。
     * 适合资源清理、请求日志记录。
     * 注意：只有在 preHandle 返回 true 时才会被调用。
     */
    default void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response, Object handler,
                                 @Nullable Exception ex) throws Exception {
    }
}
```

**拦截器执行顺序（由 `DispatcherServlet.doDispatch()` 控制）：**

```
请求到达
  → preHandle(拦截器1)
    → preHandle(拦截器2)
      → preHandle(拦截器3)
        → HandlerAdapter.handle()  // 实际处理器执行
      → postHandle(拦截器3)
    → postHandle(拦截器2)
  → postHandle(拦截器1)

视图渲染

  → afterCompletion(拦截器3)
    → afterCompletion(拦截器2)
      → afterCompletion(拦截器1)
```

**MappedInterceptor 原理：** 在 `AbstractHandlerMapping.getHandlerExecutionChain()` 中，`MappedInterceptor` 通过 `matches(request)` 决定是否加入拦截器链（基于路径匹配），未匹配的跳过。

**代码示例：**

```java
// 1. 性能监控拦截器
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler,
                                Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME);
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            System.out.println(request.getRequestURI() + " took " + duration + "ms");
            if (duration > 1000) {
                System.err.println("WARNING: Slow request: " + request.getRequestURI());
            }
        }
    }
}

// 2. 登录检查拦截器
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod hm) {
            // 检查方法是否有 @NoAuth 注解
            if (hm.getMethodAnnotation(NoAuth.class) != null) {
                return true;
            }
        }
        // 检查登录状态
        if (request.getSession().getAttribute("user") == null) {
            response.sendRedirect("/login");
            return false;  // 中断请求
        }
        return true;
    }
}

// 3. 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/admin/**", "/api/**")
                .excludePathPatterns("/api/public/**");

        registry.addInterceptor(new PerformanceInterceptor())
                .addPathPatterns("/**");
    }
}
```

---

## 9. PathPattern vs AntPathMatcher 路径匹配

### WHAT

Spring Framework 提供两种路径匹配机制：
- **AntPathMatcher**：基于字符串的模式匹配，语法类似 Ant 构建工具
- **PathPattern**：Spring 5.3 引入的预编译路径模式匹配器，Spring 6.0 成为默认

### WHY

`PathPattern` 相比 `AntPathMatcher` 有以下优势：
- **性能更高**：预编译路径模式为解析树，匹配时无需重新解析字符串
- **内存占用更少**：路径按段（segment）匹配，避免大字符串操作
- **更安全的匹配语义**：如 `**` 不能跨越路径分隔符之外的边界
- **更精确**：对路径参数（`{var}`）和正则约束（`{var:regex}`）有更好的支持

### HOW（源码级原理）

**`AntPathMatcher` 匹配规则：**

```
?     匹配单个字符
*     匹配零个或多个字符（不含路径分隔符）
**    匹配零个或多个路径段
{name}  路径变量
```

**`PathPattern` 匹配规则：**

```java
// 文件：spring-web/src/main/java/org/springframework/web/util/pattern/PathPattern.java
// 第 39-48 行

// ?      匹配一个字符
// *      匹配路径段内的零个或多个字符
// **     匹配零个或多个路径段，直到路径末尾
// {spring}         匹配一个路径段，捕获为变量 "spring"
// {spring:[a-z]+}  匹配正则 [a-z]+，捕获为变量 "spring"
// {*spring}        匹配零个或多个路径段，捕获为变量 "spring"
```

**`AbstractHandlerMapping` 中的模式切换：**

```java
// AbstractHandlerMapping.java

// 默认启用 PathPatternParser（Spring 6.0）
@Nullable
private PathPatternParser patternParser = new PathPatternParser();

// patternParser 不为 null → 使用 PathPattern 解析
// patternParser 为 null   → 使用 AntPathMatcher 字符串匹配
public void setPatternParser(@Nullable PathPatternParser patternParser) {
    this.patternParser = patternParser;
}

@Override
public boolean usesPathPatterns() {
    return getPatternParser() != null;
}
```

**性能差异原理：**

| 维度 | AntPathMatcher | PathPattern |
|------|---------------|-------------|
| 解析时机 | 运行时每次匹配解析字符串 | 启动时预编译为解析树 |
| 匹配算法 | 字符串遍历 + 通配符展开 | 路径段逐段比较 |
| 路径变量提取 | 正则匹配 | 预编译捕获组 |
| 内存占用 | 中间字符串对象多 | 对象复用率高 |

**代码示例：**

```java
// 配置使用 AntPathMatcher（Spring Boot 中）
spring.mvc.pathmatch.matching-strategy=ant_path_matcher

// 在 WebMvcConfigurer 中手动设置
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 切换回 AntPathMatcher（Spring 6.0 不推荐，仅用于兼容）
        configurer.setPatternParser(null);
    }
}
```

**路径模式示例对比：**

```java
// AntPathMatcher 和 PathPattern 都支持的模式：
"/users/{id}"         // 匹配 /users/123, 不匹配 /users/123/orders
"/users/**"           // 匹配 /users/123/orders
"/resources/*.png"    // 匹配 /resources/logo.png

// PathPattern 独有特性：
"/users/{id:[0-9]+}"  // 正则约束（AntPathMatcher 不支持）
"/files/{*path}"      // 捕获剩余所有路径段
```

---

## 10. @RequestMapping 注解

### WHAT

`org.springframework.web.bind.annotation.RequestMapping` 是 Spring MVC 中最核心的**请求映射注解**。可以标注在类和方法上，用于定义 URL 路径、HTTP 方法、请求参数、请求头等映射条件。

### WHY

声明式路由的核心：将 URL 路径、HTTP 方法、请求条件等与处理器方法关联，无需手动配置，框架自动完成映射。

### 注解属性详解（源码）

```java
// 文件：spring-web/src/main/java/org/springframework/web/bind/annotation/RequestMapping.java

@Target({ElementType.TYPE, ElementType.METHOD})  // 可用于类和方法
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

    /**
     * 映射名称。可用于 URL 构建（MvcUriComponentsBuilder）。
     */
    String name() default "";

    /**
     * URL 路径（value 和 path 互为别名）。
     * 支持 Ant 风格模式：?, *, **, {var}
     * 支持占位符：${property.name}
     */
    @AliasFor("path")
    String[] value() default {};

    @AliasFor("value")
    String[] path() default {};

    /**
     * HTTP 请求方法限制。
     * 支持：GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE
     */
    RequestMethod[] method() default {};

    /**
     * 请求参数条件。格式：
     * "myParam=myValue"  — 存在且值匹配
     * "myParam!=myValue" — 存在且值不匹配
     * "myParam"          — 存在即可
     * "!myParam"         — 不存在
     */
    String[] params() default {};

    /**
     * 请求头条件。格式同 params。
     */
    String[] headers() default {};

    /**
     * Content-Type 限制（消费的媒体类型）。
     */
    String[] consumes() default {};

    /**
     * Accept 限制（生产的媒体类型）。
     */
    String[] produces() default {};
}
```

**工作原理：** `RequestMapping` 被 `@GetMapping` 等注解元标注，Spring 通过 `AnnotatedElementUtils.findMergedAnnotation()` 递归查找合并。`RequestMappingHandlerMapping.createRequestMappingInfo()` 读取注解属性构建 `RequestMappingInfo`。

**代码示例：**

```java
@Controller
@RequestMapping(value = "/api", produces = "application/json")
public class ApiController {

    // 完整匹配：GET /api/users?page=1&size=10
    @RequestMapping(
        path = "/users",
        method = RequestMethod.GET,
        params = {"page", "size"},
        headers = "X-API-Version=1",
        produces = "application/json"
    )
    @ResponseBody
    public List<User> getUsers(@RequestParam int page, @RequestParam int size) {
        return userService.getPage(page, size);
    }

    // 多路径映射
    @RequestMapping({"/items", "/products"})
    public String items() {
        return "items";
    }
}
```

---

## 11. @GetMapping / @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping

### WHAT

这些是 Spring 4.3 引入的**组合注解**，每个注解将 `@RequestMapping` 与特定 HTTP 方法绑定。

### WHY

**简化代码：** 用 `@GetMapping("/users")` 替代 `@RequestMapping(value = "/users", method = RequestMethod.GET)`，语义更清晰。

### 组合注解的元标注机制

```java
// 文件：spring-web/src/main/java/org/springframework/web/bind/annotation/GetMapping.java

@Target(ElementType.METHOD)         // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET)  // 核心：元标注 RequestMapping
public @interface GetMapping {

    @AliasFor(annotation = RequestMapping.class)
    String name() default "";

    @AliasFor(annotation = RequestMapping.class)
    String[] value() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] path() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] params() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] headers() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] consumes() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] produces() default {};
}
```

**所有快捷注解一览：**

| 注解 | 对应的 HTTP 方法 | 包路径 |
|------|---------------|--------|
| `@GetMapping` | GET | `org.springframework.web.bind.annotation.GetMapping` |
| `@PostMapping` | POST | `org.springframework.web.bind.annotation.PostMapping` |
| `@PutMapping` | PUT | `org.springframework.web.bind.annotation.PutMapping` |
| `@DeleteMapping` | DELETE | `org.springframework.web.bind.annotation.DeleteMapping` |
| `@PatchMapping` | PATCH | `org.springframework.web.bind.annotation.PatchMapping` |

**工作原理：** `RequestMappingHandlerMapping.createRequestMappingInfo()` 通过 `AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class)` 查找合并注解。`@GetMapping` 被 Spring 的注解工具自动解析为其元标注的 `@RequestMapping(method = RequestMethod.GET)`，因此 `RequestMappingHandlerMapping` 无需为每个快捷注解编写特殊处理逻辑。

**代码示例：**

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    // GET /api/products
    @GetMapping
    public List<Product> list() { ... }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) { ... }

    // POST /api/products
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody @Valid Product product) { ... }

    // PUT /api/products/{id}
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) { ... }

    // PATCH /api/products/{id}
    @PatchMapping("/{id}")
    public Product partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) { ... }

    // DELETE /api/products/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { ... }
}
```

---

## 12. RequestMappingInfo

### WHAT

`org.springframework.web.servlet.mvc.method.RequestMappingInfo` 是请求映射信息的**复合条件对象**。它将 `@RequestMapping` 注解的各种条件（路径、HTTP 方法、参数、头、Content-Type、Accept、自定义条件）聚合为一个不可变对象，并实现了 `RequestCondition<RequestMappingInfo>` 接口。

### WHY

将多维度映射条件封装为统一对象，便于：
- 条件组合（`combine`）：类级注解 + 方法级注解的合并
- 请求匹配（`getMatchingCondition`）：判断当前请求是否匹配
- 优先级比较（`compareTo`）：多匹配时选出最佳匹配

### HOW（源码结构）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/RequestMappingInfo.java

public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

    // 映射名称
    @Nullable
    private final String name;

    // 路径条件（PathPattern 模式，优先）
    @Nullable
    private final PathPatternsRequestCondition pathPatternsCondition;

    // 路径条件（AntPathMatcher 模式，回退）
    @Nullable
    private final PatternsRequestCondition patternsCondition;

    // HTTP 方法条件
    private final RequestMethodsRequestCondition methodsCondition;

    // 请求参数条件
    private final ParamsRequestCondition paramsCondition;

    // 请求头条件
    private final HeadersRequestCondition headersCondition;

    // 消费的 Content-Type 条件
    private final ConsumesRequestCondition consumesCondition;

    // 生产的 Accept 条件
    private final ProducesRequestCondition producesCondition;

    // 自定义条件（扩展点）
    private final RequestConditionHolder customConditionHolder;

    // 构建器配置（PathPattern 或 PathMatcher 等选项）
    private final BuilderConfiguration options;
```

**核心方法：**

```java
// combine: 合并类级别和方法级别的条件
@Override
public RequestMappingInfo combine(RequestMappingInfo other) {
    // 合并名称："ClassName" + "#" + "methodName"
    // 合并路径：类路径作为前缀，方法路径作为后缀
    // 合并 HTTP 方法：继承类级方法限制（如果方法未指定）
    // 合并 params/headers/consumes/produces：继承类级限制
    ...
}

// getMatchingCondition: 判断请求是否匹配
@Override
public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
    // 依次检查所有条件：
    // 1. 路径匹配（pathPatternsCondition 或 patternsCondition）
    // 2. HTTP 方法匹配（methodsCondition）
    // 3. 请求参数匹配（paramsCondition）
    // 4. 请求头匹配（headersCondition）
    // 5. Content-Type 匹配（consumesCondition）
    // 6. Accept 匹配（producesCondition）
    // 7. 自定义条件匹配（customConditionHolder）
    // 所有条件通过 → 返回缩小后的 RequestMappingInfo
    ...
}

// compareTo: 比较两个匹配的优先级（更精确的匹配得分更高）
@Override
public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
    // 排序优先级（从高到低）：
    // 1. 路径匹配精度（PathPatternsRequestCondition / PatternsRequestCondition）
    // 2. 请求参数匹配精度（ParamsRequestCondition）
    // 3. 请求头匹配精度（HeadersRequestCondition）
    // 4. Content-Type 匹配精度（ConsumesRequestCondition）
    // 5. Accept 匹配精度（ProducesRequestCondition）
    // 6. HTTP 方法匹配精度（RequestMethodsRequestCondition）
    // 7. 自定义条件匹配精度（customConditionHolder）
    ...
}
```

**代码示例：**

```java
// 观察启动日志中打印的映射信息
// 日志格式示例：
// o.s.w.s.h.HandlerMapping.Mappings:
//   c.e.demo.UserController:
//     {GET [/api/users]}: list()
//     {GET [/api/users/{id}]}: getById(Long)
//     {POST [/api/users]}: create(User)

// 手动构建 RequestMappingInfo（编程式注册）
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMappingInfo info = RequestMappingInfo
        .paths("/api/custom")
        .methods(RequestMethod.GET)
        .params("type=admin")
        .headers("X-Custom=value")
        .produces("application/json")
        .consumes("application/json")
        .build();
```

---

## 13. RequestCondition 条件匹配

### WHAT

`org.springframework.web.servlet.mvc.condition.RequestCondition<T>` 是请求条件匹配的**策略接口**。每种映射条件都实现此接口，提供组合、匹配、比较三个核心操作。

### WHY

将匹配条件的创建、匹配、比较逻辑抽象为统一接口，使得：
- 新增条件类型（如自定义注解）只需实现接口
- 条件之间可以进行管线式组合
- 多条件场景下的优先级比较有统一规范

### HOW（源码级原理）

```java
// 文件：spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/condition/RequestCondition.java

public interface RequestCondition<T> {

    /**
     * 组合：将两个条件合并。
     * 例如：类级 @RequestMapping 和方法级 @RequestMapping 的合并。
     * - 对于路径：类路径为前缀，方法路径为后缀
     * - 对于 HTTP 方法：方法级会覆盖类级
     * - 对于 params/headers：取并集
     */
    T combine(T other);

    /**
     * 匹配：检查请求是否满足条件。
     * 返回 null → 不匹配
     * 返回新实例 → 匹配（可能缩小了范围，如通配符模式匹配后的具体模式）
     */
    @Nullable
    T getMatchingCondition(HttpServletRequest request);

    /**
     * 比较：在多个匹配中决定优先级。
     * 返回值 < 0：当前条件更优先
     * 返回值 = 0：同等优先级
     * 返回值 > 0：other 更优先
     */
    int compareTo(T other, HttpServletRequest request);
}
```

**内置条件实现类（都在 `org.springframework.web.servlet.mvc.condition` 包中）：**

| 条件类 | 功能 | 对应注解属性 |
|--------|------|-------------|
| `PatternsRequestCondition` | Ant 风格路径匹配 | `value/path` |
| `PathPatternsRequestCondition` | PathPattern 路径匹配 | `value/path` |
| `RequestMethodsRequestCondition` | HTTP 方法匹配 | `method` |
| `ParamsRequestCondition` | 请求参数匹配 | `params` |
| `HeadersRequestCondition` | 请求头匹配 | `headers` |
| `ConsumesRequestCondition` | Content-Type 匹配 | `consumes` |
| `ProducesRequestCondition` | Accept 匹配 | `produces` |
| `RequestConditionHolder` | 自定义条件容器 | 扩展点 |
| `CompositeRequestCondition` | 组合多个自定义条件 | 扩展点 |

**自定义条件示例：**

```java
// 自定义条件：基于 API 版本的请求头匹配
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import jakarta.servlet.http.HttpServletRequest;

public class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {

    private final int version;

    public ApiVersionCondition(int version) {
        this.version = version;
    }

    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        // 方法级版本覆盖类级版本
        return new ApiVersionCondition(other.version);
    }

    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        String header = request.getHeader("X-API-Version");
        if (header != null) {
            int requestVersion = Integer.parseInt(header);
            if (requestVersion >= this.version) {
                return this;
            }
        }
        return null; // 不匹配
    }

    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        // 版本号更高的更优先
        return other.version - this.version;
    }
}

// 在 Controller 中使用自定义条件
@RestController
@RequestMapping("/api")
public class VersionedController {

    @GetMapping("/data")
    @ApiVersion(2)  // 自定义注解，由 getCustomMethodCondition 处理
    public DataV2 getDataV2() { ... }
}
```

---

## 14. CorsProcessor 跨域处理

### WHAT

`org.springframework.web.cors.CorsProcessor` 是 CORS（跨域资源共享）处理的策略接口。`AbstractHandlerMapping` 在处理器匹配完成后，通过 `CorsProcessor` 对请求进行 CORS 验证和响应头设置。

### WHY

将 CORS 处理逻辑从 HandlerMapping 中解耦，允许自定义处理策略（如修改预检请求的响应方式）。Spring 提供默认实现 `DefaultCorsProcessor`。

### HOW（源码级原理）

**接口定义：**

```java
// 文件：spring-web/src/main/java/org/springframework/web/cors/CorsProcessor.java

public interface CorsProcessor {
    /**
     * 处理 CORS 请求。
     * @return false 表示请求被拒绝（如预检请求不通过），true 表示继续处理
     */
    boolean processRequest(@Nullable CorsConfiguration configuration,
            HttpServletRequest request, HttpServletResponse response) throws IOException;
}
```

**AbstractHandlerMapping 中的 CORS 处理流程（`getHandler()` 第 530-540 行）：**

```java
// 1. 检查是否需要 CORS 处理
if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {

    // 2. 获取处理器级别的 CORS 配置（来自 @CrossOrigin 或 CorsConfigurationSource）
    CorsConfiguration config = getCorsConfiguration(handler, request);

    // 3. 与全局 CORS 配置合并（setCorsConfigurations 配置的）
    if (getCorsConfigurationSource() != null) {
        CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
        config = (globalConfig != null ? globalConfig.combine(config) : config);
    }

    // 4. 验证 AllowCredentials 配置
    if (config != null) {
        config.validateAllowCredentials();
    }

    // 5. 构建 CORS 专用的 HandlerExecutionChain
    executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
}
```

**预检请求 vs 实际请求的处理差异：**

```java
protected HandlerExecutionChain getCorsHandlerExecutionChain(
        HttpServletRequest request, HandlerExecutionChain chain,
        @Nullable CorsConfiguration config) {

    if (CorsUtils.isPreFlightRequest(request)) {
        // 预检请求（OPTIONS）：替换处理器为 PreFlightHandler
        // PreFlightHandler 直接调用 corsProcessor.processRequest()
        HandlerInterceptor[] interceptors = chain.getInterceptors();
        return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
    }
    else {
        // 实际请求：在拦截器链最前面插入 CorsInterceptor
        // CorsInterceptor.preHandle() 调用 corsProcessor.processRequest()
        chain.addInterceptor(0, new CorsInterceptor(config));
        return chain;
    }
}
```

**CORS 配置来源（优先级从高到低）：**

1. 方法级 `@CrossOrigin` 注解
2. 类级 `@CrossOrigin` 注解
3. `WebMvcConfigurer.addCorsMappings()` 全局配置
4. `CorsFilter`（Servlet Filter 级别，在 HandlerMapping 之前）

**代码示例：**

```java
// 1. 全局 CORS 配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://example.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

// 2. @CrossOrigin 注解（Controller 级别）
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://example.com", maxAge = 3600)
public class ApiController {

    // 方法级别的 @CrossOrigin 会覆盖类级别
    @GetMapping("/public")
    @CrossOrigin(origins = "*")  // 公开接口允许所有来源
    public String publicData() { ... }
}
```

---

## RequestMappingHandlerMapping 完整启动扫描流程总结

```
Spring 容器启动
  │
  ├── RequestMappingHandlerMapping Bean 初始化
  │     │
  │     ├── 1. afterPropertiesSet()
  │     │     ├── 创建 RequestMappingInfo.BuilderConfiguration
  │     │     ├── 配置 PathPatternParser（默认启用）或 PathMatcher
  │     │     └── 调用 super.afterPropertiesSet()
  │     │
  │     └── 2. AbstractHandlerMethodMapping.afterPropertiesSet()
  │           │
  │           └── 3. initHandlerMethods()
  │                 │
  │                 ├── 4. getCandidateBeanNames()
  │                 │     └── 获取容器中所有 Bean 名称
  │                 │
  │                 ├── 5. 遍历每个 Bean
  │                 │     │
  │                 │     └── 6. processCandidateBean(beanName)
  │                 │           │
  │                 │           ├── 7. isHandler(beanType)
  │                 │           │     └── 检查是否有 @Controller 注解
  │                 │           │
  │                 │           └── 8. detectHandlerMethods(beanName)
  │                 │                 │
  │                 │                 ├── 9. MethodIntrospector.selectMethods()
  │                 │                 │     └── 反射遍历所有方法
  │                 │                 │
  │                 │                 ├── 10. getMappingForMethod(method, type)
  │                 │                 │     ├── createRequestMappingInfo(method)
  │                 │                 │     │     └── 解析方法级 @RequestMapping/@GetMapping 等
  │                 │                 │     ├── createRequestMappingInfo(handlerType)
  │                 │                 │     │     └── 解析类级 @RequestMapping
  │                 │                 │     ├── typeInfo.combine(info)
  │                 │                 │     │     └── 合并类级和方法级条件
  │                 │                 │     └── getPathPrefix(handlerType)
  │                 │                 │           └── 应用路径前缀
  │                 │                 │
  │                 │                 └── 11. registerHandlerMethod(handler, method, mapping)
  │                 │                       │
  │                 │                       └── 12. MappingRegistry.register()
  │                 │                             ├── 创建 HandlerMethod 对象
  │                 │                             ├── 提取直接路径 → pathLookup
  │                 │                             ├── 提取 CORS 配置 → corsLookup
  │                 │                             └── 存入 registry 主映射表
  │                 │
  │                 └── 13. handlerMethodsInitialized()
  │                       └── 日志输出映射总数
  │
  └── 容器启动完成，等待请求
```

---

## 文档总结

本文档深入分析了 Spring MVC 6.0 中 HandlerMapping 路由映射的所有核心组件：

| 组件 | 角色 | 包路径 |
|------|------|--------|
| `HandlerMapping` | 顶层接口，定义 `getHandler()` | `org.springframework.web.servlet` |
| `AbstractHandlerMapping` | 模板基类，拦截器/CORS/排序 | `org.springframework.web.servlet.handler` |
| `AbstractHandlerMethodMapping` | 方法级映射基类，MappingRegistry | `org.springframework.web.servlet.handler` |
| `RequestMappingHandlerMapping` | @Controller + @RequestMapping 实现 | `org.springframework.web.servlet.mvc.method.annotation` |
| `BeanNameUrlHandlerMapping` | Bean 名称即 URL | `org.springframework.web.servlet.handler` |
| `RouterFunctionMapping` | 函数式路由 | `org.springframework.web.servlet.function.support` |
| `SimpleUrlHandlerMapping` | 手动 URL→Handler 映射 | `org.springframework.web.servlet.handler` |
| `HandlerInterceptor` | 拦截器接口 | `org.springframework.web.servlet` |
| `RequestMappingInfo` | 复合映射条件 | `org.springframework.web.servlet.mvc.method` |
| `RequestCondition` | 条件匹配策略接口 | `org.springframework.web.servlet.mvc.condition` |
| `CorsProcessor` | CORS 跨域处理器 | `org.springframework.web.cors` |
