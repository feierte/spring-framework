# 第六阶段：HandlerMethodReturnValueHandler — 返回值处理

> 所属模块：`spring-web/src/main/java/org/springframework/web/method/support/` + `spring-webmvc`
>
> 核心问题：Controller 方法的返回值是怎么**变成 HTTP 响应**的？

---

## 6.1 HandlerMethodReturnValueHandler 接口

### 📌 WHAT

`org.springframework.web.method.support.HandlerMethodReturnValueHandler` 是返回值处理器的统一接口：

```java
// HandlerMethodReturnValueHandler.java (spring-web)
public interface HandlerMethodReturnValueHandler {
    // 这个处理器能处理这种返回值类型吗？
    boolean supportsReturnType(MethodParameter returnType);

    // 处理返回值，写入 HTTP 响应
    void handleReturnValue(@Nullable Object returnValue,
                           MethodParameter returnType,
                           ModelAndViewContainer mavContainer,
                           NativeWebRequest webRequest) throws Exception;
}
```

### 🎯 WHY

Controller 方法可以返回各种类型：

```java
public String page() { ... }                        // 返回视图名
public User getUser() { ... }                       // 返回 JSON（@ResponseBody）
public ResponseEntity<User> getUser() { ... }       // 返回完整 HTTP 实体
public ModelAndView handle() { ... }                // 老式写法
public void handle() { ... }                        // 自己写响应
public Callable<User> async() { ... }               // 异步响应
```

如果没有返回值处理器，每种类型都要写 `if-else` 判断。有了它，每种类型一个处理器，各司其职。

### ⚙️ 工作原理

关键方法 `ServletInvocableHandlerMethod.invokeAndHandle()`：

```java
// ServletInvocableHandlerMethod.java (spring-webmvc)
public void invokeAndHandle(ServletWebRequest webRequest, 
        ModelAndViewContainer mavContainer, Object... providedArgs) {
    
    // ① 解析参数 + 调用方法
    Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
    
    // ② 设置响应状态码（如果有 @ResponseStatus 注解）
    setResponseStatus(webRequest);
    
    if (returnValue == null) {
        if (isRequestNotModified(webRequest) || 
            getResponseStatus() != null || mavContainer.isRequestHandled()) {
            disableContentCachingIfNecessary(webRequest);
            mavContainer.setRequestHandled(true);
            return;
        }
    } else if (StringUtils.hasText(getResponseStatusReason())) {
        mavContainer.setRequestHandled(true);
        return;
    }
    
    mavContainer.setRequestHandled(false);
    
    // ③ ★ 遍历所有返回值处理器，找到支持的来处理
    try {
        this.returnValueHandlers.handleReturnValue(
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
    } catch (Exception ex) {
        throw ex;
    }
}
```

`HandlerMethodReturnValueHandlerComposite.handleReturnValue()`：

```java
// HandlerMethodReturnValueHandlerComposite.java
public void handleReturnValue(Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
    
    HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
    if (handler == null) {
        throw new IllegalArgumentException("Unknown return value type: " + 
            returnType.getParameterType().getName());
    }
    handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}

private HandlerMethodReturnValueHandler selectHandler(Object value, 
        MethodParameter returnType) {
    boolean isAsyncValue = isAsyncReturnValue(value, returnType);
    for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
        if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
            continue;
        }
        if (handler.supportsReturnType(returnType)) {
            return handler;  // 返回第一个匹配的
        }
    }
    return null;
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/support/HandlerMethodReturnValueHandler.java
```

---

## 6.2 @ResponseBody — 直接写响应体

### 📌 WHAT

将返回值直接写入 HTTP 响应体，**跳过视图解析**。

### 🔧 HOW

```java
@RestController  // = @Controller + @ResponseBody（类级别）
public class UserController {

    @GetMapping("/user/{id}")
    @ResponseBody
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // → 自动转 JSON: {"id":1,"name":"张三"}
    }

    // 返回集合
    @GetMapping("/users")
    public List<User> listUsers() {
        return userService.findAll();  // → [{"id":1,...}, {"id":2,...}]
    }

    // 返回字符串（不是视图名！）
    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "Hello World";  // → HTTP响应体直接是 "Hello World"
    }
}
```

> ⚠️ `@RestController` 下所有方法默认都是 `@ResponseBody`，不需要每个方法都加。

### ⚙️ 工作原理

`RequestResponseBodyMethodProcessor.handleReturnValue()`：

```java
// RequestResponseBodyMethodProcessor.handleReturnValue():
@Override
public void handleReturnValue(Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
    
    mavContainer.setRequestHandled(true);  // ← 关键：标记请求已处理，跳过视图解析
    
    ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
    ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
    
    // 使用 HttpMessageConverter 把返回值写入响应体
    writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
}
```

`writeWithMessageConverters()` 的协商逻辑：

```
① 获取 Accept 请求头（客户端想要什么格式）
② 获取返回值的 Content-Type
③ 遍历所有 HttpMessageConverter：
    ├→ canWrite(User.class, application/json) → MappingJackson2HttpMessageConverter → 序列化为JSON
    └→ canWrite(User.class, application/xml) → Jaxb2HttpMessageConverter → 序列化为XML
④ 把序列化结果写入 response.getOutputStream()
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/RequestResponseBodyMethodProcessor.java
```

---

## 6.3 ModelAndView — 同时返回视图和数据

### 📌 WHAT

老式返回类型，同时指定视图名和要传给视图的数据。**现代开发中很少使用。**

### 🔧 HOW

```java
@Controller
public class PageController {
    @GetMapping("/page")
    public ModelAndView showPage() {
        ModelAndView mav = new ModelAndView("hello");  // 视图名
        mav.addObject("message", "Welcome!");           // 数据
        mav.addObject("user", userService.getCurrentUser());
        return mav;
    }
}
```

### ⚙️ 工作原理

`ModelAndViewMethodReturnValueHandler` 把 `ModelAndView` 中的数据合并到 `ModelAndViewContainer`，设置视图名。后续由 `ViewResolver` 解析。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ModelAndViewMethodReturnValueHandler.java
```

---

## 6.4 String 作为视图名

### 📌 WHAT

返回 `String` 被解释为**视图名**（不是响应体），`ViewResolver` 负责找到真正的视图。

### 🔧 HOW

```java
@Controller
public class PageController {
    @GetMapping("/index")
    public String index(Model model) {  // String = 视图名
        model.addAttribute("title", "首页");
        return "index";  // → /WEB-INF/views/index.jsp（取决于 ViewResolver 配置）
    }

    @GetMapping("/redirect")
    public String redirect() {
        return "redirect:/index";  // redirect: 前缀 → 重定向
    }

    @GetMapping("/forward")
    public String forward() {
        return "forward:/page";  // forward: 前缀 → 转发
    }
}
```

### ⚙️ 工作原理

`ViewNameMethodReturnValueHandler` 处理：

```java
// ViewNameMethodReturnValueHandler.handleReturnValue():
@Override
public void handleReturnValue(Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
    if (returnValue instanceof CharSequence) {
        String viewName = returnValue.toString();
        mavContainer.setViewName(viewName);  // 只是把字符串设为视图名，不直接渲染!
        if (isRedirectViewName(viewName)) {
            mavContainer.setRedirectModelScenario(true);
        }
    }
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ViewNameMethodReturnValueHandler.java
```

---

## 6.5 ResponseEntity — 带状态码和响应头的完整响应

### 📌 WHAT

返回完整的 HTTP 响应：状态码 + 响应头 + 响应体。

### 🔧 HOW

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();  // 404
        }
        return ResponseEntity.ok(user);  // 200 OK + JSON
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.create(user);
        URI location = URI.create("/user/" + created.getId());
        return ResponseEntity.created(location).body(created);  // 201 Created
    }

    // 自定义响应头
    @GetMapping("/download")
    public ResponseEntity<byte[]> download() {
        byte[] data = fileService.getFile();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(data);
    }
}
```

### ⚙️ 工作原理

`HttpEntityMethodProcessor.handleReturnValue()`：

```java
// HttpEntityMethodProcessor.handleReturnValue():
@Override
public void handleReturnValue(Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
    
    mavContainer.setRequestHandled(true);  // 跳过视图
    
    HttpHeaders outputHeaders = null;
    Object body = null;
    
    if (returnValue instanceof HttpEntity) {
        HttpEntity<?> entity = (HttpEntity<?>) returnValue;
        outputHeaders = entity.getHeaders();
        body = entity.getBody();
    }
    
    // 设置响应状态码
    HttpStatusCode statusCode = determineStatusCode(returnValue, returnType, webRequest);
    // 设置响应头
    HttpHeaders headers = new HttpHeaders();
    if (outputHeaders != null) {
        headers.putAll(outputHeaders);
    }
    // 写入响应
    outputMessage.getServletResponse().setStatus(statusCode.value());
    if (body != null) {
        writeWithMessageConverters(body, returnType, inputMessage, outputMessage);
    }
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/HttpEntityMethodProcessor.java
```

---

## 6.6 void — 无返回值

### 📌 WHAT

方法没有返回值，通常配合 `HttpServletResponse` 直接写入响应。

### 🔧 HOW

```java
@GetMapping("/download")
public void download(HttpServletResponse response) throws IOException {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=report.pdf");
    try (OutputStream out = response.getOutputStream()) {
        out.write(fileService.getReportBytes());
    }
}
```

### ⚙️ 工作原理

`RequestResponseBodyMethodProcessor` 处理 `void` 返回值：直接设置 `mavContainer.setRequestHandled(true)`，不做任何额外处理。

---

## 6.7 Callable / DeferredResult / WebAsyncTask — 异步返回值

### 📌 WHAT

Controller 方法返回异步类型，释放 Servlet 线程去处理其他请求，等结果就绪后再写入响应。

### 🔧 HOW

```java
@GetMapping("/async")
public Callable<String> async() {
    return () -> {
        Thread.sleep(2000);  // 模拟耗时操作
        return "异步返回的结果";
    };
}

@GetMapping("/deferred")
public DeferredResult<String> deferred() {
    DeferredResult<String> result = new DeferredResult<>(5000L);  // 5秒超时
    // 在其他线程设置结果
    executorService.submit(() -> {
        String data = longRunningTask();
        result.setResult(data);
    });
    return result;
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/CallableMethodReturnValueHandler.java
spring-webmvc/.../servlet/mvc/method/annotation/DeferredResultMethodReturnValueHandler.java
```

---

## 6.8 SseEmitter / ResponseBodyEmitter — 服务端推送

### 📌 WHAT

SSE（Server-Sent Events）：服务端可以**多次推送**数据给客户端。

### 🔧 HOW

```java
@GetMapping("/sse")
public SseEmitter sse() {
    SseEmitter emitter = new SseEmitter(30000L);  // 30秒超时
    executorService.execute(() -> {
        try {
            for (int i = 0; i < 10; i++) {
                emitter.send("消息 " + i);
                Thread.sleep(1000);
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java
```

---

## 6.9 StreamingResponseBody — 流式下载大文件

### 📌 WHAT

大文件下载时，边读边写，不占内存。

### 🔧 HOW

```java
@GetMapping("/large-file")
public ResponseEntity<StreamingResponseBody> largeFile() {
    StreamingResponseBody stream = outputStream -> {
        try (InputStream in = new FileInputStream("/path/to/large/file")) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        }
    };
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(stream);
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/StreamingResponseBodyReturnValueHandler.java
```

---

## 6.10 其他返回值处理器一览

| 返回值类型 | 处理器 | 行为 |
|-----------|--------|------|
| `Model` 对象 | `ModelMethodProcessor` | 只传数据，视图名用 URL 推断 |
| `Map` 对象 | `MapMethodProcessor` | 同 Model |
| `View` 对象 | `ViewMethodReturnValueHandler` | 直接渲染视图 |
| `HttpHeaders` | `HttpHeadersReturnValueHandler` | 只设响应头，无响应体 |
| `ResponseEntityException`（Reactive） | `ResponseEntityExceptionHandler` | Reactive 错误处理 |

---

## 🔗 完整链路：从返回值到 HTTP 响应

```
Controller 方法 return 一个值
    │
    ▼
ServletInvocableHandlerMethod.invokeAndHandle()
    │
    ├── ① invokeForRequest() — 解析参数 + 反射调用方法
    │
    ├── ② setResponseStatus() — 检查 @ResponseStatus 注解
    │
    └── ③ this.returnValueHandlers.handleReturnValue()
         │
         └── 遍历所有 HandlerMethodReturnValueHandler：
              
              情况A：@ResponseBody / @RestController
              ┌──────────────────────────────────────┐
              │ RequestResponseBodyMethodProcessor   │
              │ → mavContainer.setRequestHandled(true)│
              │ → HttpMessageConverter.write()        │
              │ → JSON 写入 response.getOutputStream()│
              └──────────────────────────────────────┘
              
              情况B：返回 String "index"
              ┌──────────────────────────────────────┐
              │ ViewNameMethodReturnValueHandler     │
              │ → mavContainer.setViewName("index")  │
              │ → 后续 ViewResolver 解析渲染          │
              └──────────────────────────────────────┘
              
              情况C：返回 ResponseEntity<User>
              ┌──────────────────────────────────────┐
              │ HttpEntityMethodProcessor            │
              │ → 设置状态码                           │
              │ → 设置响应头                           │
              │ → 用 HttpMessageConverter 写响应体     │
              └──────────────────────────────────────┘

完成后，回到 DispatcherServlet.doDispatch()
    │
    └── processDispatchResult()
         ├── 如果 mavContainer.isRequestHandled() → 直接完成
         └── 否则 → ViewResolver 解析视图 → 渲染
```

**关键判断**：`mavContainer.setRequestHandled(true)` 决定是否跳过视图解析。`@ResponseBody`、`ResponseEntity`、`void` 都会设置这个标记。

---

> **下一阶段**：[第七阶段：HttpMessageConverter 消息转换](07-http-message-converter.md)
