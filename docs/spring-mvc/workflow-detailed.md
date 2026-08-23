# Spring MVC 完整请求处理流程（源码级逐步拆解）

> 基于 `DispatcherServlet.doDispatch()` 源码，逐步骤详解。
>
> 源码路径：`spring-webmvc/src/main/java/org/springframework/web/servlet/DispatcherServlet.java:1040-1120`

---

## 总流程图

```
HTTP 请求到达 Tomcat
    │
    ▼
doService(request, response)                              ← FrameworkServlet 模板方法
    │
    ├── 设置 request attributes（WebApplicationContext、Locale 等）
    ├── 解析 RequestPath
    └── 调用 doDispatch()  ← ★ 核心方法
         │
         ├── 步骤① checkMultipart()     → 处理文件上传
         ├── 步骤② getHandler()         → 找到处理器
         ├── 步骤③ getHandlerAdapter()  → 找到适配器
         ├── 步骤④ Last-Modified 检查   → 304 缓存优化
         ├── 步骤⑤ applyPreHandle()     → 拦截器前置处理
         ├── 步骤⑥ ha.handle()          → ★ 真正执行业务逻辑
         ├── 步骤⑦ applyDefaultViewName() → 默认视图名
         ├── 步骤⑧ applyPostHandle()    → 拦截器后置处理
         └── 步骤⑨ processDispatchResult() → 异常处理 + 视图渲染
              │
              ├── 步骤⑨-a 异常处理（如果有异常）
              └── 步骤⑨-b render() 渲染视图
```

---

## 步骤 0：doService() — 请求正式进入 DispatcherServlet

### 源码位置
`DispatcherServlet.java:933-986` ← `FrameworkServlet.processRequest()` 调用：

```java
// FrameworkServlet.java
protected final void processRequest(HttpServletRequest request, HttpServletResponse response) {
    // ... 线程上下文初始化 ...
    
    doService(request, response);  // ← 调用 DispatcherServlet 的 doService()
    
    // ... 发布 ServletRequestHandledEvent 事件 ...
}
```

### 干了什么

`doService()` 在 `doDispatch()` 之前做**环境准备**：

```java
// DispatcherServlet.java:933-986
protected void doService(HttpServletRequest request, HttpServletResponse response) {
    // 1. 记录请求日志
    logRequest(request);

    // 2. 如果是 include 请求，备份原始 request attributes
    Map<String, Object> attributesSnapshot = null;
    if (WebUtils.isIncludeRequest(request)) {
        attributesSnapshot = new HashMap<>();
        // ... 备份属性 ...
    }

    // 3. 把 Spring 框架对象注册为 request attribute，供后续 Handler/View 使用
    request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
    request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
    request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
    request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

    // 4. 处理重定向传参（FlashMap）
    if (this.flashMapManager != null) {
        FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
        request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, ...);
        request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
    }

    // 5. 解析 RequestPath（Spring 6 默认使用 PathPattern 解析）
    RequestPath previousRequestPath = null;
    if (this.parseRequestPath) {
        previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        ServletRequestPathUtils.parseAndCache(request);  // ← 解析并缓存路径信息
    }

    // 6. 进入核心分发方法
    try {
        doDispatch(request, response);  // ← ★ 下钻
    } finally {
        // 恢复 request attributes（include 场景）
        if (attributesSnapshot != null) {
            restoreAttributesAfterInclude(request, attributesSnapshot);
        }
        // 恢复 RequestPath
        if (this.parseRequestPath) {
            ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
        }
    }
}
```

**大白话**：doService 就是在上战场之前，先给士兵（Handler）发好装备——告诉请求"你现在在哪个容器里、用哪个语言环境、有没有重定向传参需要恢复"。

---

## 步骤 ①：checkMultipart() — 处理文件上传

### 源码
`DispatcherServlet.java:1051-1053`

```java
processedRequest = checkMultipart(request);
multipartRequestParsed = (processedRequest != request);
```

### 干了什么

```java
// DispatcherServlet.java:1205-1233
protected HttpServletRequest checkMultipart(HttpServletRequest request) {
    if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
        // 检查 Content-Type 是否为 multipart/form-data
        
        // 如果已经被 MultipartFilter 处理过了，跳过
        if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
            // 已经处理过了
        }
        // 如果之前处理失败过，跳过（避免在错误页面渲染时再次失败）
        else if (hasMultipartException(request)) {
            // 跳过
        }
        else {
            // ★ 真正解析 multipart 请求
            return this.multipartResolver.resolveMultipart(request);
            // StandardServletMultipartResolver 内部：
            //   调用 request.getParts() 获取所有 Part
            //   包装为 StandardMultipartHttpServletRequest
        }
    }
    return request;  // 不是文件上传请求，原样返回
}
```

**大白话**：如果是文件上传请求（`Content-Type: multipart/form-data`），就把原生的 `HttpServletRequest` 包装成能取到文件内容的 `MultipartHttpServletRequest`。不是文件上传就原样返回，跳过。

---

## 步骤 ②：getHandler() — 找到谁处理这个请求

### 源码
`DispatcherServlet.java:1055-1060`

```java
mappedHandler = getHandler(processedRequest);
if (mappedHandler == null) {
    noHandlerFound(processedRequest, response);  // → 404
    return;
}
```

### getHandler() 内部实现

```java
// DispatcherServlet.java:1270-1281
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    if (this.handlerMappings != null) {
        for (HandlerMapping mapping : this.handlerMappings) {
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;  // ← 第一个匹配的就返回！
            }
        }
    }
    return null;  // 没找到 → 404
}
```

### 遍历的 HandlerMapping 列表（按 Order 排序）

| 优先级 | HandlerMapping | 匹配方式 |
|--------|---------------|---------|
| 1 | `RequestMappingHandlerMapping` | URL → `@RequestMapping` 方法 |
| 2 | `BeanNameUrlHandlerMapping` | Bean 名称以 `/` 开头 |
| 3 | `RouterFunctionMapping` | 函数式路由 `RouterFunction` |
| 4 | `SimpleUrlHandlerMapping` | 手动配置的 URL 映射 |
| ... | 自定义 HandlerMapping | 自定义逻辑 |

### RequestMappingHandlerMapping.getHandler() 内部做了啥

```java
// AbstractHandlerMapping.getHandler() — 模板方法
public final HandlerExecutionChain getHandler(HttpServletRequest request) {
    // 1. 调用子类的 getHandlerInternal() 查找 Handler
    Object handler = getHandlerInternal(request);
    
    // 2. 如果没找到，用默认 Handler
    if (handler == null) {
        handler = getDefaultHandler();
    }
    
    // 3. 组装 HandlerExecutionChain
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
    
    // 4. ★ CORS 检查：如果是 OPTIONS 预检请求，添加 CORS 拦截器
    if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
        CorsConfiguration config = getCorsConfiguration(handler, request);
        executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
    }
    
    return executionChain;
}
```

```java
// AbstractHandlerMethodMapping.getHandlerInternal()
protected HandlerMethod getHandlerInternal(HttpServletRequest request) {
    // 1. 从 request 获取 lookup path（如 "/user/123"）
    String lookupPath = initLookupPath(request);
    
    // 2. 加读锁，从 MappingRegistry 中查找
    this.mappingRegistry.acquireReadLock();
    try {
        HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
        // 3. 创建 HandlerMethod 实例（bean + Method 的包装）
        return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
    } finally {
        this.mappingRegistry.releaseReadLock();
    }
}

// lookupHandlerMethod() 内部：
protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) {
    List<Match> matches = new ArrayList<>();
    
    // ① 从 MappingRegistry 获取该路径的所有匹配
    //    MappingRegistry.urlLookup: Map<String, List<RequestMappingInfo>>
    List<RequestMappingInfo> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
    if (directPathMatches != null) {
        // ② 精确路径匹配（如 /user/123 精确匹配 @GetMapping("/user/123")）
        addMatchingMappings(directPathMatches, matches, request);
    }
    
    if (matches.isEmpty()) {
        // ③ 如果没有精确匹配，遍历所有注册的映射做模式匹配
        //    如 @GetMapping("/user/{id}") 匹配 /user/123
        addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
    }
    
    if (!matches.isEmpty()) {
        // ④ 对匹配结果排序：PathPattern 更具体的排前面
        Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
        matches.sort(comparator);
        
        // ⑤ 取最佳匹配
        Match bestMatch = matches.get(0);
        
        // ⑥ 如果最佳匹配有多个（两个路径模式完全相同），抛异常
        if (matches.size() > 1) {
            Match secondBestMatch = matches.get(1);
            if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                throw new IllegalStateException("Ambiguous handler methods mapped for ...");
            }
        }
        
        // ⑦ 把 URI 模板变量存入 request attribute
        //    如 /user/123 → {id: "123"} 存入 request
        handleMatch(bestMatch.mapping, lookupPath, request);
        return bestMatch.getHandlerMethod();
    }
    
    return null;
}
```

**大白话**：这个步骤就像快递分拣——每个 `HandlerMapping` 看一遍 URL，能匹配的就接走。`RequestMappingHandlerMapping` 是主分拣员，它维护了一张巨大的对照表（MappingRegistry），记录了每个 URL 对应哪个 Controller 的哪个方法。请求 `/user/123` 来了，一查表：哦，这是 `UserController.getUser(Long id)` 的活。

返回值 `HandlerExecutionChain` 是一个包装对象，包含：
- `handler` — 处理器本身（`HandlerMethod`）
- `interceptorList` — 这个 URL 需要经过的拦截器列表

---

## 步骤 ③：getHandlerAdapter() — 找到怎么调用处理器

### 源码
`DispatcherServlet.java:1062-1063`

```java
HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
```

### 内部实现

```java
// DispatcherServlet.java:1307-1317
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter adapter : this.handlerAdapters) {
            if (adapter.supports(handler)) {
                return adapter;  // ← 第一个支持的就用
            }
        }
    }
    throw new ServletException("No adapter for handler [" + handler + "]");
}
```

### 遍历的 HandlerAdapter 列表

| 优先级 | HandlerAdapter | 支持的 Handler 类型 |
|--------|---------------|-------------------|
| 1 | `RequestMappingHandlerAdapter` | `HandlerMethod`（`@RequestMapping` 方法） |
| 2 | `HandlerFunctionAdapter` | `HandlerFunction`（函数式） |
| 3 | `HttpRequestHandlerAdapter` | `HttpRequestHandler` 接口 |
| 4 | `SimpleControllerHandlerAdapter` | `Controller` 接口（旧式） |

**大白话**：拿到了快递（Handler），现在需要一个能打开这个快递的工具（Adapter）。有人用剪刀（RequestMappingHandlerAdapter），有人直接撕（HandlerFunctionAdapter）。DispatcherServlet 不在乎怎么打开的，它只要一个能用的工具。

---

## 步骤 ④：Last-Modified 检查 — 304 缓存优化

### 源码
`DispatcherServlet.java:1065-1073`

```java
String method = request.getMethod();
boolean isGet = HttpMethod.GET.matches(method);
if (isGet || HttpMethod.HEAD.matches(method)) {
    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
        return;  // ← 资源没变，直接返回 304，不再执行 Controller
    }
}
```

**大白话**：如果浏览器请求的是 GET，且资源没有变化（浏览器传的 `If-Modified-Since` 和资源的最后修改时间匹配），直接返回 304（Not Modified），省掉了执行 Controller 和渲染视图的开销。这是 HTTP 标准缓存机制，不是 Spring 特有的。

---

## 步骤 ⑤：applyPreHandle() — 拦截器前置处理

### 源码
`DispatcherServlet.java:1075-1077`

```java
if (!mappedHandler.applyPreHandle(processedRequest, response)) {
    return;  // ← 任何一个拦截器返回 false，请求终止！
}
```

### HandlerExecutionChain.applyPreHandle() 内部

```java
// HandlerExecutionChain.java
boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) {
    for (int i = 0; i < this.interceptorList.size(); i++) {
        HandlerInterceptor interceptor = this.interceptorList.get(i);
        if (!interceptor.preHandle(request, response, this.handler)) {
            triggerAfterCompletion(request, response, null);  // 调用已执行的拦截器的 afterCompletion
            return false;  // ← 停止！
        }
        this.interceptorIndex = i;  // 记录执行到第几个了
    }
    return true;
}
```

**大白话**：控制器执行前，所有拦截器的 `preHandle()` 按注册顺序依次执行。任何一个返回 `false`，后面的拦截器和 Controller 都不会执行。这是**登录检查**的最佳位置。

```
拦截器1.preHandle() → 拦截器2.preHandle() → 拦截器3.preHandle()
                                                     │
                                            如果返回 false，请求终止
                                            如果全部返回 true，进入步骤 ⑥
```

---

## 步骤 ⑥：ha.handle() — ★ 真正执行业务逻辑

### 源码
`DispatcherServlet.java:1079-1084`

```java
// Actually invoke the handler.
mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

if (asyncManager.isConcurrentHandlingStarted()) {
    return;  // 异步处理已启动，Servlet 线程释放
}
```

### RequestMappingHandlerAdapter.handle() 执行链路

这是整个 Spring MVC 中最复杂的部分，分多步执行：

```
ha.handle(request, response, handlerMethod)
    │
    ▼
AbstractHandlerMethodAdapter.handle()
    → handleInternal(request, response, (HandlerMethod) handler)
    │
    ▼
RequestMappingHandlerAdapter.handleInternal()
    │
    ├── ① checkRequest(request)               → 检查请求方法是否支持
    │        检查 HTTP Method（GET/POST/DELETE 等）
    │
    ├── ② invokeHandlerMethod(request, response, handlerMethod)
    │    │
    │    ├── ②-a 包装 request/response 为 ServletWebRequest
    │    │
    │    ├── ②-b 创建 WebDataBinderFactory
    │    │       扫描 Controller 的 @InitBinder 方法
    │    │       扫描 @ControllerAdvice 的 @InitBinder 方法
    │    │
    │    ├── ②-c 创建 ModelFactory
    │    │       扫描 Controller 的 @ModelAttribute 方法
    │    │       扫描 @ControllerAdvice 的 @ModelAttribute 方法
    │    │
    │    ├── ②-d 创建 ServletInvocableHandlerMethod
    │    │       设置参数解析器列表（argumentResolvers）
    │    │       设置返回值处理器列表（returnValueHandlers）
    │    │
    │    ├── ②-e 执行 @ModelAttribute 方法
    │    │       modelFactory.initModel() → 把 @ModelAttribute 方法的结果放入 Model
    │    │
    │    └── ②-f invocableMethod.invokeAndHandle()
    │         │
    │         ├── invokeForRequest()  ← ★ 解析参数 + 反射调用
    │         │   │
    │         │   ├── getMethodArgumentValues()
    │         │   │   for each parameter：
    │         │   │   ├── findProvidedArgument()   → 检查直接提供的参数
    │         │   │   └── resolvers.resolveArgument() → 用解析器从 request 取值
    │         │   │       遍历 30+ 个 HandlerMethodArgumentResolver：
    │         │   │       @RequestParam    → RequestParamMethodArgumentResolver
    │         │   │       @PathVariable    → PathVariableMethodArgumentResolver
    │         │   │       @RequestBody     → RequestResponseBodyMethodProcessor
    │         │   │       @ModelAttribute  → ModelAttributeMethodProcessor
    │         │   │       HttpSession      → ServletRequestMethodArgumentResolver
    │         │   │       ... 其他
    │         │   │
    │         │   └── doInvoke(args)  ← 反射调用 Controller 方法
    │         │       method.invoke(bean, args)  ← Java 原生反射
    │         │
    │         └── handleReturnValue()   ← ★ 处理返回值
    │             returnValueHandlers.handleReturnValue()
    │             遍历 15+ 个返回值处理器：
    │             @ResponseBody   → RequestResponseBodyMethodProcessor
    │                                → HttpMessageConverter 序列化 JSON
    │                                → 写入 response.getOutputStream()
    │                                → mavContainer.setRequestHandled(true)
    │             String          → ViewNameMethodReturnValueHandler
    │                                → mavContainer.setViewName("index")
    │             ResponseEntity  → HttpEntityMethodProcessor
    │             ModelAndView    → ModelAndViewMethodReturnValueHandler
    │             异步            → AsyncHandlerMethodReturnValueHandler
    │
    └── ③ getModelAndView() → 创建 ModelAndView 返回
```

### 大白话（完整拆解步骤 ⑥）

**⑥-a 创建 WebDataBinderFactory**：找出 Controller 里所有 `@InitBinder` 方法。比如你有个方法 `initBinder(WebDataBinder binder) { binder.setDisallowedFields("id"); }`，Spring 会记录下来，等数据绑定时自动调用。

**⑥-b 创建 ModelFactory**：找出所有 `@ModelAttribute` 方法并执行。比如你定义了 `@ModelAttribute("categories") public List<Category> categories() { ... }`，Spring 会在调用目标 Controller 方法之前，先执行这个方法，把返回的 categories 放到 Model 里——这样每个请求都能拿到这个数据。

**⑥-c 解析参数（getMethodArgumentValues）**：

| 参数类型 | 解析器 | 数据来源 |
|---------|--------|---------|
| `@RequestParam String name` | `RequestParamMethodArgumentResolver` | `request.getParameter("name")` |
| `@PathVariable Long id` | `PathVariableMethodArgumentResolver` | 从 URI 模板变量提取 `{id}` |
| `@RequestBody User user` | `RequestResponseBodyMethodProcessor` | `HttpMessageConverter.read()`→ Jackson 反序列化 |
| `@ModelAttribute User user` | `ModelAttributeMethodProcessor` | `DataBinder.bind(request)` 自动绑定 |
| `HttpSession session` | `ServletRequestMethodArgumentResolver` | `request.getSession()` |
| `@RequestHeader String ua` | `RequestHeaderMethodArgumentResolver` | `request.getHeader("User-Agent")` |
| `@CookieValue String sid` | `ServletCookieValueMethodArgumentResolver` | `request.getCookies()` |
| `BindingResult result` | `ErrorsMethodArgumentResolver` | 前一个参数的绑定/校验结果 |

**⑥-d 调用 Controller 方法（doInvoke）**：本质就是 Java 原生的 `method.invoke(controllerBean, args)`。

**⑥-e 处理返回值（handleReturnValue）**：

| 返回值类型 | 处理器 | 行为 |
|-----------|--------|------|
| `@ResponseBody User` | `RequestResponseBodyMethodProcessor` | Jackson → JSON → 写入 response |
| `String "index"` | `ViewNameMethodReturnValueHandler` | `mavContainer.setViewName("index")` |
| `ResponseEntity<User>` | `HttpEntityMethodProcessor` | 设状态码 + 写 JSON |
| `Callable<User>` | `CallableMethodReturnValueHandler` | 异步处理 |
| `void` | `RequestResponseBodyMethodProcessor` | `mavContainer.setRequestHandled(true)` |

---

## 步骤 ⑦：applyDefaultViewName() — 默认视图名

### 源码
`DispatcherServlet.java:1086`

```java
applyDefaultViewName(processedRequest, mv);
```

### 内部

```java
// DispatcherServlet.java:1125-1131
private void applyDefaultViewName(HttpServletRequest request, ModelAndView mv) {
    if (mv != null && !mv.hasView()) {
        String defaultViewName = getDefaultViewName(request);
        if (defaultViewName != null) {
            mv.setViewName(defaultViewName);
        }
    }
}
```

**大白话**：如果 Handler 返回的 `ModelAndView` 有数据但没有视图名（比如只返回了 `ModelAndView` 没设视图名），就用 `RequestToViewNameTranslator` 根据 URL 自动生成一个视图名。比如请求 `/user/profile` → 默认视图名 `user/profile`。现代 `@RestController` 场景中 `mavContainer.isRequestHandled() = true`，这步就跳过了。

---

## 步骤 ⑧：applyPostHandle() — 拦截器后置处理

### 源码
`DispatcherServlet.java:1087`

```java
mappedHandler.applyPostHandle(processedRequest, response, mv);
```

### 内部

```java
// HandlerExecutionChain.java
void applyPostHandle(HttpServletRequest request, HttpServletResponse response, ModelAndView mv) {
    for (int i = this.interceptorList.size() - 1; i >= 0; i--) {  // ← 逆序执行！
        HandlerInterceptor interceptor = this.interceptorList.get(i);
        interceptor.postHandle(request, response, this.handler, mv);
    }
}
```

**大白话**：和 preHandle **执行顺序相反**。Controller 执行完了，视图还没渲染，这个时候可以修改 `ModelAndView`（比如加个全局的公共数据）。

```
preHandle:  拦截器1 → 拦截器2 → 拦截器3  (正序)
postHandle: 拦截器3 → 拦截器2 → 拦截器1  (逆序)
```

---

## 步骤 ⑨：processDispatchResult() — 最终处理（异常 + 渲染）

### 源码
`DispatcherServlet.java:1089-1097`

```java
catch (Exception ex) {
    dispatchException = ex;     // 步骤⑥-⑧ 中的异常在这里捕获
}
catch (Throwable err) {
    dispatchException = new ServletException("Handler dispatch failed: " + err, err);
}
processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
// ↑ 不管有没有异常，都走这个方法做最终处理
```

### processDispatchResult() 完整源码

```java
// DispatcherServlet.java:1138-1178
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
        HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) {

    boolean errorView = false;

    // ━━━━ 分支 A：有异常 ━━━━
    if (exception != null) {
        if (exception instanceof ModelAndViewDefiningException) {
            // 特殊异常：异常本身就携带了 ModelAndView
            mv = ((ModelAndViewDefiningException) exception).getModelAndView();
        } else {
            // ★ 正常异常处理：遍历 HandlerExceptionResolver
            Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
            mv = processHandlerException(request, response, handler, exception);
            errorView = (mv != null);
        }
    }

    // ━━━━ 分支 B：渲染视图 ━━━━
    if (mv != null && !mv.wasCleared()) {
        render(mv, request, response);  // ← 渲染
        if (errorView) {
            WebUtils.clearErrorRequestAttributes(request);
        }
    }

    // ━━━━ 分支 C：没有视图需要渲染 ━━━━
    else {
        // 比如 @ResponseBody 已经直接写响应了，这里就不需要渲染
    }

    // ━━━━ 最终：触发 afterCompletion ━━━━
    if (mappedHandler != null) {
        mappedHandler.triggerAfterCompletion(request, response, null);
    }
}
```

### 步骤 ⑨-a：异常处理 processHandlerException()

```java
// DispatcherServlet.java:1330-1369
protected ModelAndView processHandlerException(..., Exception ex) {
    
    request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

    ModelAndView exMv = null;
    if (this.handlerExceptionResolvers != null) {
        // 遍历所有 HandlerExceptionResolver，按 Order 排序
        for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
            exMv = resolver.resolveException(request, response, handler, ex);
            if (exMv != null) {
                break;  // ← 第一个返回非 null 的就用
            }
        }
    }
    
    if (exMv != null) {
        if (exMv.isEmpty()) {  // 空 ModelAndView = 已在 Resolver 中处理完毕
            request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
            return null;
        }
        // 为异常视图生成默认视图名（如果需要）
        if (!exMv.hasView()) {
            String defaultViewName = getDefaultViewName(request);
            if (defaultViewName != null) {
                exMv.setViewName(defaultViewName);
            }
        }
        // 暴露异常详情给 request（JSP 错误页面可能用到）
        WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
        return exMv;
    }

    // 没有 Resolver 处理 → 异常继续往上抛
    throw ex;
}
```

**遍历的 HandlerExceptionResolver（按 Order 排序）**：

| 优先级 | Resolver | 处理逻辑 |
|--------|---------|---------|
| 1 | `ExceptionHandlerExceptionResolver` | 找 `@ExceptionHandler` 方法执行 |
| 2 | `ResponseStatusExceptionResolver` | 检查异常上有没有 `@ResponseStatus` 注解 |
| 3 | `DefaultHandlerExceptionResolver` | 标准 Spring 异常 → HTTP 状态码 |
| 4 | `SimpleMappingExceptionResolver`（可选） | 异常类型 → 错误视图名映射 |

**大白话**：`@ControllerAdvice` 干的就是这事儿。抛了 `ResourceNotFoundException` → `@ExceptionHandler(ResourceNotFoundException.class)` 接住 → 返回 404。`@Valid` 校验失败抛 `MethodArgumentNotValidException` → `@ExceptionHandler` 接住 → 返回 400。

### 步骤 ⑨-b：render() — 视图渲染

```java
// DispatcherServlet.java:1380-1422
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) {
    
    // 1. 确定 Locale 并设置到响应
    Locale locale = (this.localeResolver != null ? 
        this.localeResolver.resolveLocale(request) : request.getLocale());
    response.setLocale(locale);

    // 2. 获取 View 对象
    View view;
    String viewName = mv.getViewName();
    if (viewName != null) {
        // 有视图名 → 解析为 View 对象
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
        if (view == null) {
            throw new ServletException("Could not resolve view with name '" + viewName + "'");
        }
    } else {
        // 没有视图名但有 View 对象（ModelAndView 直接包含了 View）
        view = mv.getView();
    }

    // 3. ★ 渲染视图
    try {
        if (mv.getStatus() != null) {
            response.setStatus(mv.getStatus().value());  // 设置 HTTP 状态码
        }
        view.render(mv.getModelInternal(), request, response);  // ← 真正渲染！
    } catch (Exception ex) {
        throw ex;
    }
}

// resolveViewName() — 遍历所有 ViewResolver
protected View resolveViewName(String viewName, Map<String, Object> model, 
        Locale locale, HttpServletRequest request) {
    if (this.viewResolvers != null) {
        for (ViewResolver viewResolver : this.viewResolvers) {
            View view = viewResolver.resolveViewName(viewName, locale);
            if (view != null) {
                return view;  // 第一个匹配的就用
            }
        }
    }
    return null;
}
```

**注意**：对于 `@ResponseBody` / `@RestController`，`handleReturnValue` 阶段已经设置了 `mavContainer.setRequestHandled(true)`，所以 `mv.wasCleared()` 返回 `true`，**render 方法根本不会执行**。视图渲染只针对返回视图名的场景（JSP/Thymeleaf）。

---

## 步骤 ⑩：afterCompletion — 拦截器收尾

### 在 processDispatchResult() 的最后

```java
// DispatcherServlet.java:1174-1177
if (mappedHandler != null) {
    mappedHandler.triggerAfterCompletion(request, response, null);
}
```

### 内部

```java
// HandlerExecutionChain.java
void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {
    for (int i = this.interceptorIndex; i >= 0; i--) {  // 逆序遍历已执行的拦截器
        HandlerInterceptor interceptor = this.interceptorList.get(i);
        try {
            interceptor.afterCompletion(request, response, this.handler, ex);
        } catch (Throwable ex2) {
            // 不中断，继续执行其他拦截器的 afterCompletion
        }
    }
}
```

**大白话**：不管有没有异常，`afterCompletion` 必定执行（在 finally 语义下）。适合做**资源清理**——比如记录请求耗时、关闭数据库连接等。

---

## 📊 完整时序图

```
  Browser              DispatcherServlet         HandlerMapping    HandlerAdapter       Controller       ViewResolver
    │                        │                        │                 │                   │                │
    │  GET /user/123         │                        │                 │                   │                │
    │───────────────────────►│                        │                 │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ① checkMultipart()     │                 │                   │                │
    │                        │──┐                     │                 │                   │                │
    │                        │◄─┘ (不处理，非文件上传)  │                 │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ② getHandler(request)   │                 │                   │                │
    │                        │───────────────────────►│                 │                   │                │
    │                        │◄── HandlerExecutionChain│                 │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ③ getHandlerAdapter()  │                 │                   │                │
    │                        │────────────────────────────────────────►│                   │                │
    │                        │◄── supports()=true ───────────────────│                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ④ lastModified? → no   │                 │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑤ preHandle() ─ 拦截器链                  │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑥ ha.handle()          │                 │                   │                │
    │                        │────────────────────────────────────────►│                   │                │
    │                        │                        │                 │                   │                │
    │                        │                        │                 │ invokeAndHandle() │                │
    │                        │                        │                 │──────────────────►│                │
    │                        │                        │                 │     解析参数       │                │
    │                        │                        │                 │  method.invoke()  │                │
    │                        │                        │                 │◄── return User ──│                │
    │                        │                        │                 │     处理返回值     │                │
    │                        │                        │                 │   JSON → response │                │
    │                        │                        │                 │                   │                │
    │                        │◄── ModelAndView ───────────────────────│                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑦ applyDefaultViewName │                 │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑧ postHandle() ─ 拦截器链 (逆序)          │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑨ processDispatchResult(mv)             │                   │                │
    │                        │    (mv 被标记为 handled，跳过渲染)         │                   │                │
    │                        │                        │                 │                   │                │
    │                        │ ⑩ afterCompletion() ─ 拦截器收尾         │                   │                │
    │                        │                        │                 │                   │                │
    │◄── 200 OK {...} ──────│                        │                 │                   │                │
```

---

## 🔑 关键设计模式总结

| 模式 | 应用场景 |
|------|---------|
| **前端控制器（Front Controller）** | `DispatcherServlet` 统一接收所有请求 |
| **策略模式（Strategy）** | `HandlerMapping`、`HandlerAdapter`、`ViewResolver`、`HandlerExceptionResolver` |
| **模板方法（Template Method）** | `AbstractHandlerMapping.getHandler()`、`AbstractHandlerMethodAdapter.handle()` |
| **适配器模式（Adapter）** | `HandlerAdapter` 适配不同类型的 Handler |
| **责任链模式（Chain of Responsibility）** | `HandlerExecutionChain` + `HandlerInterceptor` |
| **组合模式（Composite）** | `HandlerMethodArgumentResolverComposite`、`HandlerMethodReturnValueHandlerComposite` |
