# 第十二阶段：高级特性合集

> 覆盖：异步请求、函数式端点、Spring 6 新特性、配置定制、国际化、文件上传、FlashMap 等

---

## 12.1 异步请求

### 📌 WHAT

Servlet 线程是有限的资源。当一个请求需要长时间处理时，可以用异步方式**释放 Servlet 线程**去处理其他请求，等结果就绪后再写入响应。

### 12.1.1 Callable — 最简异步方式

```java
@GetMapping("/async")
public Callable<String> async() {
    return () -> {
        Thread.sleep(3000);  // 模拟耗时操作（比如调远程 API）
        return "异步处理完成";
    };
}
```

**原理**：Spring 把 `Callable` 提交到 `TaskExecutor` 线程池，Servlet 线程立即释放。等 `Callable` 返回结果，Spring 再分发一次请求，把结果写入响应。

### 12.1.2 DeferredResult — 完全手动控制

```java
@GetMapping("/long-poll")
public DeferredResult<String> deferred() {
    DeferredResult<String> result = new DeferredResult<>(30000L);  // 30秒超时
    
    result.onTimeout(() -> result.setResult("请求超时"));
    result.onError(e -> result.setResult("出错了"));
    
    // 在其他线程设置结果（比如消息队列消费到结果后）
    taskExecutor.execute(() -> {
        String data = waitForExternalEvent();
        result.setResult(data);  // 手动设置结果
    });
    
    return result;  // Servlet 线程立即释放
}
```

### 12.1.3 SseEmitter — 服务端推送事件

```java
@GetMapping("/stream")
public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter();
    
    executor.execute(() -> {
        try {
            for (int i = 1; i <= 10; i++) {
                emitter.send(SseEmitter.event()
                    .id(String.valueOf(i))
                    .name("progress")
                    .data("步骤 " + i + " 完成"));
                Thread.sleep(1000);
            }
            emitter.complete();  // 发送完毕
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    
    return emitter;
}
```

**客户端（JavaScript）**：

```javascript
const source = new EventSource("/stream");
source.addEventListener("progress", e => {
    console.log(e.data);  // "步骤 1 完成", "步骤 2 完成"...
});
```

### 12.1.4 StreamingResponseBody — 大文件流式下载

```java
@GetMapping("/download-large")
public ResponseEntity<StreamingResponseBody> download() {
    StreamingResponseBody stream = output -> {
        // 边读边写，不占内存
        try (InputStream in = new FileInputStream("/large/file.zip")) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                output.write(buf, 0, n);
                output.flush();
            }
        }
    };
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=large.zip")
        .body(stream);
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/context/request/async/
spring-webmvc/.../servlet/mvc/method/annotation/CallableMethodReturnValueHandler.java
spring-webmvc/.../servlet/mvc/method/annotation/DeferredResultMethodReturnValueHandler.java
spring-webmvc/.../servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java
```

---

## 12.2 函数式端点（Functional Endpoints）

### 📌 WHAT

Spring 5+ 引入的**不使用注解**的路由方式，纯粹的 Java Lambda 风格。

### 🔧 HOW

```java
@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
        return RouterFunctions.route()
            .GET("/users", handler::listUsers)
            .GET("/users/{id}", handler::getUser)
            .POST("/users", handler::createUser)
            .build();
    }
}

@Component
public class UserHandler {
    private final UserService userService;
    
    public ServerResponse listUsers(ServerRequest request) {
        List<User> users = userService.findAll();
        return ServerResponse.ok().body(users);
    }
    
    public ServerResponse getUser(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        User user = userService.findById(id);
        return user != null ? 
            ServerResponse.ok().body(user) : 
            ServerResponse.notFound().build();
    }
    
    public ServerResponse createUser(ServerRequest request) throws Exception {
        User user = request.body(User.class);
        userService.save(user);
        return ServerResponse.created(URI.create("/users/" + user.getId())).build();
    }
}
```

### 关键类（均在 spring-webmvc）

| 类 | 角色 |
|----|------|
| `RouterFunction<T>` | 路由映射定义 |
| `RouterFunctions.route()` | 路由构建入口 |
| `HandlerFunction<T>` | 处理函数 |
| `ServerRequest` | 函数式请求封装 |
| `ServerResponse` | 函数式响应封装 |
| `RequestPredicate` | 请求匹配条件 |

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/function/
```

---

## 12.3 Spring 6 新特性

### 12.3.1 RestClient — RestTemplate 的现代化替代

Spring 6.1+ 引入的同步 HTTP 客户端，API 更流畅：

```java
// 旧：RestTemplate
String result = restTemplate.getForObject("/api/data", String.class);

// 新：RestClient
String result = restClient.get()
    .uri("/api/data")
    .retrieve()
    .body(String.class);
```

### 12.3.2 @HttpExchange — 声明式 HTTP 客户端

```java
// 定义接口
@HttpExchange("/api/users")
public interface UserClient {
    @GetExchange("/{id}")
    User getUser(@PathVariable Long id);
    
    @PostExchange
    User createUser(@RequestBody User user);
}

// 自动生成实现（类似 Feign）
UserClient client = HttpServiceProxyFactory
    .builder(WebClientAdapter.forClient(webClient))
    .build()
    .createClient(UserClient.class);

User user = client.getUser(1L);
```

### 12.3.3 ProblemDetail — 标准化错误响应

见 [第十阶段 10.9](10-exception-handling.md#109-problemdetailrfc-7807--spring-6-新特性)。

### 12.3.4 PathPattern 成为默认

Spring 6 中 URL 匹配默认使用 `PathPattern` 替代 `AntPathMatcher`，性能更好。

---

## 12.4 配置与定制 — WebMvcConfigurer

### 📌 WHAT

`org.springframework.web.servlet.config.annotation.WebMvcConfigurer` 是自定义 MVC 配置的**万能接口**。实现它的方法，不用写 XML，不用覆盖整个配置。

### 🔧 HOW

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 自定义静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/", "file:/external/static/")
                .setCachePeriod(3600);
    }

    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/admin/**");
    }

    // 注册自定义参数解析器
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    // 注册自定义返回值处理器
    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        // ...
    }

    // 注册自定义消息转换器
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }

    // CORS 全局配置
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000");
    }

    // 无逻辑的 URL → 视图 直接映射
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addRedirectViewController("/old", "/new");
    }
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/config/annotation/WebMvcConfigurer.java
spring-webmvc/src/main/java/org/springframework/web/servlet/config/annotation/WebMvcConfigurationSupport.java
spring-webmvc/src/main/java/org/springframework/web/servlet/config/annotation/EnableWebMvc.java
```

---

## 12.5 国际化（i18n）

### 📌 WHAT

根据用户的地区/语言自动切换界面文本。

### 🔧 HOW

```java
// 1. 准备资源文件
// messages_zh_CN.properties:   welcome=欢迎
// messages_en_US.properties:   welcome=Welcome

// 2. 配置
@Bean
public MessageSource messageSource() {
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasename("messages");
    ms.setDefaultEncoding("UTF-8");
    return ms;
}

@Bean
public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);  // 默认中文
    return resolver;
}

@Bean
public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");   // URL 参数: ?lang=en
    return interceptor;
}

// 3. 使用
@RestController
public class WelcomeController {
    @Autowired
    private MessageSource messageSource;
    
    @GetMapping("/welcome")
    public String welcome(Locale locale) {
        return messageSource.getMessage("welcome", null, locale);
    }
}
```

### 内置 LocaleResolver

| 实现 | 策略 |
|------|------|
| `AcceptHeaderLocaleResolver` | 根据 `Accept-Language` 请求头 |
| `CookieLocaleResolver` | 根据 Cookie 中的语言设置 |
| `SessionLocaleResolver` | 根据 Session 中的语言设置 |
| `FixedLocaleResolver` | 固定语言，不切换 |

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/i18n/
```

---

## 12.6 文件上传 — MultipartResolver

### 📌 WHAT

处理 `multipart/form-data` 请求（文件上传）。

### 🔧 HOW

```java
@PostMapping("/upload")
public String upload(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
        return "文件为空";
    }
    
    String filename = file.getOriginalFilename();
    long size = file.getSize();
    String contentType = file.getContentType();
    
    // 保存到磁盘
    Path dest = Path.of("/uploads", filename);
    file.transferTo(dest);
    
    return "上传成功：" + filename + " (" + size + " bytes)";
}

// 多文件上传
@PostMapping("/upload-multi")
public String uploadMulti(@RequestParam("files") MultipartFile[] files) {
    for (MultipartFile file : files) {
        file.transferTo(Path.of("/uploads", file.getOriginalFilename()));
    }
    return "上传了 " + files.length + " 个文件";
}
```

### ⚙️ 工作原理

`DispatcherServlet` 在处理请求时先检查是否是 multipart 请求：

```java
// DispatcherServlet.checkMultipart():
protected HttpServletRequest checkMultipart(HttpServletRequest request) {
    if (this.multipartResolver != null && 
        this.multipartResolver.isMultipart(request)) {
        // 包装为 MultipartHttpServletRequest
        return this.multipartResolver.resolveMultipart(request);
    }
    return request;
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/multipart/
spring-webmvc/.../servlet/multipart/support/StandardServletMultipartResolver.java
```

---

## 12.7 FlashMap — 重定向传参

### 📌 WHAT

POST → Redirect → GET 模式中，在重定向前后**传递参数**。数据存在 Session 中，GET 请求后自动移除。

### 🔧 HOW

```java
@PostMapping("/user")
public String createUser(User user, RedirectAttributes redirectAttrs) {
    userService.save(user);
    redirectAttrs.addFlashAttribute("message", "创建成功！");
    redirectAttrs.addFlashAttribute("newUserId", user.getId());
    return "redirect:/user/" + user.getId();  // GET 请求中可以取到 flash 属性
}

@GetMapping("/user/{id}")
public String showUser(@PathVariable Long id, Model model) {
    // message 和 newUserId 自动出现在 Model 中（一次有效）
    model.addAttribute("user", userService.findById(id));
    return "userDetail";
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/FlashMap.java
spring-webmvc/src/main/java/org/springframework/web/servlet/FlashMapManager.java
spring-webmvc/src/main/java/org/springframework/web/servlet/support/SessionFlashMapManager.java
```

---

## 12.8 静态资源处理

### 🔧 HOW

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(31556926);  // 一年缓存
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
                
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/external/images/");  // 外部目录
    }
}
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/resource/
spring-webmvc/src/main/java/org/springframework/web/servlet/config/annotation/ResourceHandlerRegistry.java
```

---

## 📊 全部阶段汇总

| 文档 | 阶段 | 知识点数 | 状态 |
|------|------|---------|------|
| [index.md](index.md) | 总览 | — | ✅ |
| [01-http-web-foundation.md](01-http-web-foundation.md) | HTTP 基础 | 9 | 🔄 子代理生成中 |
| [02-dispatcher-servlet.md](02-dispatcher-servlet.md) | 请求分发 | 6 | ✅ |
| [03-handler-mapping.md](03-handler-mapping.md) | 路由映射 | 14 | 🔄 子代理生成中 |
| [04-handler-adapter.md](04-handler-adapter.md) | 处理器适配 | 6 | ✅ |
| [05-argument-resolver.md](05-argument-resolver.md) | 参数解析 | 20 | ✅ |
| [06-return-value-handler.md](06-return-value-handler.md) | 返回值处理 | 15 | ✅ |
| [07-http-message-converter.md](07-http-message-converter.md) | 消息转换 | 9 | ✅ |
| [08-view-resolver.md](08-view-resolver.md) | 视图解析 | 19 | ✅ |
| [09-data-binding-validation.md](09-data-binding-validation.md) | 数据绑定校验 | 16 | ✅ |
| [10-exception-handling.md](10-exception-handling.md) | 异常处理 | 15 | ✅ |
| [11-interceptor-cors.md](11-interceptor-cors.md) | 拦截器 CORS | 9 | ✅ |
| [12-advanced-features.md](12-advanced-features.md) | 高级特性 | 24 | ✅ |
| **合计** | **14 阶段** | **106+** | |
