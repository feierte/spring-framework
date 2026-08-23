# 第四阶段：HandlerAdapter — 处理器适配

> 所属模块：`spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/`
>
> 核心问题：找到了 Handler（Controller 方法），**怎么调用它？**

---

## 4.1 HandlerAdapter 接口

### 📌 WHAT

`org.springframework.web.servlet.HandlerAdapter` 是 Spring MVC 处理器适配器的**统一接口**。它定义了三个方法：

```java
// HandlerAdapter.java (spring-webmvc)
public interface HandlerAdapter {
    boolean supports(Object handler);    // 这个适配器能处理该 Handler 吗？
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, 
                        Object handler) throws Exception;  // 执行 Handler
    long getLastModified(HttpServletRequest request, Object handler);  // 已废弃
}
```

**大白话**：不同 Handler 长得不一样——有的是 `@RequestMapping` 方法，有的是实现 `Controller` 接口的类，有的是 `HttpRequestHandler`。HandlerAdapter 就像一个"万能遥控器"，不管你是什么牌子的电器，它都能操作。

### 🎯 WHY

如果没有 HandlerAdapter，`DispatcherServlet` 就需要写一堆 `if-else`：

```java
// 如果没有适配器模式，代码会变成这样：
if (handler instanceof HandlerMethod) {
    // 处理 @RequestMapping 方法
} else if (handler instanceof Controller) {
    // 处理旧式 Controller
} else if (handler instanceof HttpRequestHandler) {
    // 处理 HttpRequestHandler
}
// ...每增加一种 Handler 类型就要改 DispatcherServlet
```

引入 HandlerAdapter 后，`DispatcherServlet` 只需要遍历所有适配器，找到支持该 Handler 的那个，调用它的 `handle()` 方法即可——**对扩展开放，对修改关闭**。

### 🔧 HOW

DispatcherServlet 的 `getHandlerAdapter()` 方法：

```java
// DispatcherServlet.java
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter adapter : this.handlerAdapters) {
            if (adapter.supports(handler)) {
                return adapter;
            }
        }
    }
    throw new ServletException("No adapter for handler [" + handler + "]");
}
```

### ⚙️ 工作原理

HandlerAdapter 的核心价值在于**适配器模式**：

```
DispatcherServlet.doDispatch()
    │
    ├── getHandler()        → 获取 Handler（HandlerMapping 负责）
    ├── getHandlerAdapter() → 遍历所有 HandlerAdapter，找到 supports() 返回 true 的那个
    └── ha.handle()         → 委托给适配器执行
```

每个适配器只负责一种 Handler 类型：

| 适配器 | 支持的 Handler 类型 |
|--------|-------------------|
| `RequestMappingHandlerAdapter` | HandlerMethod（@RequestMapping 方法） |
| `HandlerFunctionAdapter` | HandlerFunction（函数式端点） |
| `HttpRequestHandlerAdapter` | HttpRequestHandler 接口 |
| `SimpleControllerHandlerAdapter` | Controller 接口（旧式） |

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerAdapter.java
```

---

## 4.2 AbstractHandlerMethodAdapter

### 📌 WHAT

`org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter` 是所有"方法级"适配器的**抽象基类**。它专门处理 `HandlerMethod` 类型的 Handler。

### 🎯 WHY

所有基于方法调用的适配器（`RequestMappingHandlerAdapter`、`HandlerFunctionAdapter`）都需要把 Handler 转成 `HandlerMethod`，AbstractHandlerMethodAdapter 提取了这部分公共逻辑。

### ⚙️ 工作原理

```java
// AbstractHandlerMethodAdapter.java
public final boolean supports(Object handler) {
    return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
}

@Override
public final ModelAndView handle(HttpServletRequest request, 
                                  HttpServletResponse response, Object handler) {
    return handleInternal(request, response, (HandlerMethod) handler);
}

// 子类只需实现这两个方法：
protected abstract boolean supportsInternal(HandlerMethod handlerMethod);
protected abstract ModelAndView handleInternal(HttpServletRequest request,
        HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;
```

**模板方法模式**：`supports()` 和 `handle()` 是 `final` 的，子类不能改；`supportsInternal()` 和 `handleInternal()` 是抽象方法，子类必须实现。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter.java
```

---

## 4.3 RequestMappingHandlerAdapter（重点）

### 📌 WHAT

`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter` 是 Spring MVC 中**最重要的适配器**，专门处理 `@RequestMapping` 注解标注的方法（即我们日常写的 Controller 方法）。

它继承了 `AbstractHandlerMethodAdapter`，实现了 `BeanFactoryAware` 和 `InitializingBean`。

### 🎯 WHY

90% 以上的 Spring MVC 请求都通过这个适配器处理。它不仅调用方法，还负责：

1. **注册参数解析器**（`HandlerMethodArgumentResolver`）—— 决定方法参数从哪来
2. **注册返回值处理器**（`HandlerMethodReturnValueHandler`）—— 决定返回值怎么变成 HTTP 响应
3. **注册消息转换器**（`HttpMessageConverter`）—— JSON ↔ 对象的翻译
4. **管理 `@InitBinder` 和 `@ModelAttribute` 方法**

### 🔧 HOW

核心方法 `handleInternal()` 的执行流程：

```java
// RequestMappingHandlerAdapter.java (简化版)
@Override
protected ModelAndView handleInternal(HttpServletRequest request,
        HttpServletResponse response, HandlerMethod handlerMethod) {
    
    ModelAndView mav;
    // 1. 检查请求方法是否支持（GET/POST等）
    checkRequest(request);
    
    // 2. 如果需要 Session 同步，加锁
    if (this.synchronizeOnSession) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                mav = invokeHandlerMethod(request, response, handlerMethod);
            }
        }
    } else {
        // 3. 调用 invokeHandlerMethod 真正执行
        mav = invokeHandlerMethod(request, response, handlerMethod);
    }
    
    // 4. 处理 Cache-Control 头
    if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
        if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
            applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
        } else {
            prepareResponse(response);
        }
    }
    
    return mav;
}
```

### ⚙️ 工作原理（源码深入）

**核心方法 `invokeHandlerMethod()`**：

```java
// RequestMappingHandlerAdapter.java
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
        HttpServletResponse response, HandlerMethod handlerMethod) {
    
    // 1. 包装请求/响应为 ServletWebRequest
    ServletWebRequest webRequest = new ServletWebRequest(request, response);
    
    // 2. 创建 WebDataBinderFactory（处理 @InitBinder 方法）
    WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
    
    // 3. 创建 ModelFactory（处理 @ModelAttribute 方法）
    ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);
    
    // 4. 创建 ServletInvocableHandlerMethod（可调用的方法包装）
    ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
    
    // 5. 设置参数解析器和返回值处理器
    //    这是最关键的一步！
    invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
    invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
    
    // 6. 创建 ModelAndViewContainer（数据容器）
    ModelAndViewContainer mavContainer = new ModelAndViewContainer();
    
    // 7. 执行 @ModelAttribute 方法
    modelFactory.initModel(webRequest, mavContainer, invocableMethod);
    
    // 8. 处理异步请求
    if (invocableMethod.isAsync()) {
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        // ...异步处理...
    }
    
    // 9. ★ 核心：调用 Controller 方法
    invocableMethod.invokeAndHandle(webRequest, mavContainer);
    
    // 10. 创建 ModelAndView 返回
    return getModelAndView(mavContainer, modelFactory, webRequest);
}
```

**`afterPropertiesSet()` — 初始化默认组件**：

```java
// RequestMappingHandlerAdapter.java (afterPropertiesSet 方法)
@Override
public void afterPropertiesSet() {
    // 初始化默认的 Controller 相关 Bean
    initControllerAdviceCache();
    
    if (this.argumentResolvers == null) {
        // 获取所有默认参数解析器
        List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
        this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
    }
    if (this.initBinderArgumentResolvers == null) {
        List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
        this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
    }
    if (this.returnValueHandlers == null) {
        // 获取所有默认返回值处理器
        List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
        this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
    }
}
```

**`getDefaultArgumentResolvers()` — 注册默认参数解析器（共 30+ 个）**：

```java
// RequestMappingHandlerAdapter.java (getDefaultArgumentResolvers 方法)
private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
    List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
    
    // 按优先级从高到低排列
    resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
    resolvers.add(new RequestParamMapMethodArgumentResolver());
    resolvers.add(new PathVariableMethodArgumentResolver());
    resolvers.add(new PathVariableMapMethodArgumentResolver());
    resolvers.add(new MatrixVariableMethodArgumentResolver());
    resolvers.add(new MatrixVariableMapMethodArgumentResolver());
    resolvers.add(new ServletModelAttributeMethodProcessor(false));
    resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
    resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
    resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
    resolvers.add(new RequestHeaderMapMethodArgumentResolver());
    resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
    resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
    resolvers.add(new SessionAttributeMethodArgumentResolver());
    resolvers.add(new RequestAttributeMethodArgumentResolver());
    
    // 还有更多...省略
    
    // 最后加自定义的
    if (getCustomArgumentResolvers() != null) {
        resolvers.addAll(getCustomArgumentResolvers());
    }
    
    return resolvers;
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter.java
```

---

## 4.4 HandlerFunctionAdapter

### 📌 WHAT

`org.springframework.web.servlet.function.support.HandlerFunctionAdapter` 处理**函数式端点**（Spring 5+ 引入的 `RouterFunction` 风格）。

### 🎯 WHY

传统 `@RequestMapping` 写法需要注解 + 反射，函数式风格更直观、类型安全：

```java
// 传统方式
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) { ... }
}

// 函数式方式（无需注解，无需反射扫描）
@Bean
public RouterFunction<ServerResponse> route() {
    return RouterFunctions.route()
        .GET("/user/{id}", request -> {
            Long id = Long.valueOf(request.pathVariable("id"));
            return ServerResponse.ok().body(userService.getUser(id));
        })
        .build();
}
```

### ⚙️ 工作原理

`HandlerFunctionAdapter.handle()` 把请求包装成 `ServerRequest`，调用 `HandlerFunction.handle()`，得到 `ServerResponse` 后写入 `HttpServletResponse`。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/function/support/HandlerFunctionAdapter.java
```

---

## 4.5 HttpRequestHandlerAdapter

### 📌 WHAT

`org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter` 处理实现 `org.springframework.web.HttpRequestHandler` 接口的 Bean。

### 🔧 HOW

```java
// HttpRequestHandlerAdapter.java
public class HttpRequestHandlerAdapter implements HandlerAdapter {
    @Override
    public boolean supports(Object handler) {
        return (handler instanceof HttpRequestHandler);
    }
    
    @Override
    public ModelAndView handle(HttpServletRequest request, 
            HttpServletResponse response, Object handler) {
        ((HttpRequestHandler) handler).handleRequest(request, response);
        return null;  // 返回 null 表示请求已处理完毕
    }
}
```

**使用示例**：

```java
@Component("/api/export")
public class ExportHandler implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request, 
            HttpServletResponse response) {
        // 直接操作原生 Servlet API
        response.setContentType("application/octet-stream");
        // ...写入文件流...
    }
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/HttpRequestHandlerAdapter.java
```

---

## 4.6 SimpleControllerHandlerAdapter

### 📌 WHAT

`org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter` 处理实现 `org.springframework.web.servlet.mvc.Controller` 接口的 Bean（Spring 2.x 时代的老式写法，基本不用了）。

### 🔧 HOW

```java
// SimpleControllerHandlerAdapter.java
public class SimpleControllerHandlerAdapter implements HandlerAdapter {
    @Override
    public boolean supports(Object handler) {
        return (handler instanceof Controller);
    }
    
    @Override
    public ModelAndView handle(HttpServletRequest request, 
            HttpServletResponse response, Object handler) {
        return ((Controller) handler).handleRequest(request, response);
    }
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/SimpleControllerHandlerAdapter.java
```

---

## 🔗 适配器与 doDispatch 的关系

回到 `DispatcherServlet.doDispatch()`，适配器被调用的位置：

```java
// DispatcherServlet.java (doDispatch 方法，关键片段)
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    
    try {
        ModelAndView mv = null;
        
        // ① 通过 HandlerMapping 找到 Handler
        mappedHandler = getHandler(processedRequest);
        
        // ② 通过 HandlerAdapter 找到能处理它的适配器
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        
        // ③ 执行前置拦截器
        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return;
        }
        
        // ④ ★ 适配器真正调用 Handler 方法
        mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        
        // ⑤ 执行后置拦截器
        mappedHandler.applyPostHandle(processedRequest, response, mv);
        
    } catch (Exception ex) {
        // ⑥ 异常处理
        processDispatchResult(processedRequest, response, mappedHandler, mv, ex);
    }
    // ⑦ 渲染视图
    processDispatchResult(processedRequest, response, mappedHandler, mv, null);
}
```

**完整的调用链路**：

```
DispatcherServlet.doDispatch()
  └→ getHandlerAdapter(handler)
       └→ 遍历 handlerAdapters 列表
            ├→ adapter1.supports(handler) → false → 跳过
            ├→ adapter2.supports(handler) → true  → 返回 adapter2
            └→ ...
  └→ ha.handle(request, response, handler)
       └→ RequestMappingHandlerAdapter.handleInternal()
            └→ invokeHandlerMethod()
                 └→ invocableMethod.invokeAndHandle()
                      ├→ 解析每个参数 (HandlerMethodArgumentResolver)
                      ├→ java.lang.reflect.Method.invoke()  ← 真正的反射调用
                      └→ 处理返回值 (HandlerMethodReturnValueHandler)
```

---

> **下一阶段**：[第五阶段：参数解析 HandlerMethodArgumentResolver](05-argument-resolver.md) — 了解 `invokeAndHandle()` 中参数是怎么被自动注入的
