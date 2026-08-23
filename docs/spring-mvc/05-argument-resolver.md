# 第五阶段：HandlerMethodArgumentResolver — 参数解析

> 所属模块：`spring-web/src/main/java/org/springframework/web/method/` + `spring-webmvc`
>
> 核心问题：Controller 方法的参数是怎么被**自动注入**的？为什么能写 `(@RequestParam String name, HttpSession session)` 而不用自己取？

---

## 5.1 HandlerMethodArgumentResolver 接口

### 📌 WHAT

`org.springframework.web.method.support.HandlerMethodArgumentResolver` 是参数解析器的**统一接口**。每个实现类负责解析**一种类型**的方法参数。

```java
// HandlerMethodArgumentResolver.java (spring-web)
public interface HandlerMethodArgumentResolver {
    // 判断这个解析器能不能处理这个参数
    boolean supportsParameter(MethodParameter parameter);

    // 从请求中解析出参数的值
    Object resolveArgument(MethodParameter parameter,
                           ModelAndViewContainer mavContainer,
                           NativeWebRequest webRequest,
                           WebDataBinderFactory binderFactory) throws Exception;
}
```

### 🎯 WHY

没有它，Controller 方法只能写成这样：

```java
// 如果没有参数解析器，你需要手动从 request 取参数
@RequestMapping("/user")
public String getUser(HttpServletRequest request, HttpServletResponse response) {
    String name = request.getParameter("name");    // 手动取
    String id = request.getParameter("id");        // 手动取
    HttpSession session = request.getSession();    // 手动取
    // 非常繁琐！
}
```

有了它，你可以直接写：

```java
@GetMapping("/user/{id}")
public String getUser(@PathVariable Long id, @RequestParam String name, HttpSession session) {
    // 参数自动注入！
}
```

### ⚙️ 工作原理

参数解析的完整链路在 `InvocableHandlerMethod.getMethodArgumentValues()` 中：

```java
// InvocableHandlerMethod.java (spring-web)
protected Object[] getMethodArgumentValues(NativeWebRequest request,
        ModelAndViewContainer mavContainer, Object... providedArgs) {

    MethodParameter[] parameters = getMethodParameters();  // 获取方法所有参数

    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
        MethodParameter parameter = parameters[i];
        parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);

        // ① 先检查有没有 directly provided 的值
        args[i] = findProvidedArgument(parameter, providedArgs);
        if (args[i] != null) continue;

        // ② 没有直接值，遍历所有解析器
        if (!this.resolvers.supportsParameter(parameter)) {
            throw new IllegalStateException("No suitable resolver");
        }

        // ③ 找到支持的解析器，调用它解析
        args[i] = this.resolvers.resolveArgument(
            parameter, mavContainer, request, this.dataBinderFactory);
    }
    return args;
}
```

`HandlerMethodArgumentResolverComposite` 内部维护一个解析器列表，按顺序遍历——**第一个 `supportsParameter()` 返回 `true` 的就被选中**。

```java
// HandlerMethodArgumentResolverComposite.java
public Object resolveArgument(MethodParameter parameter, ...) {
    HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
    return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
}

private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
    for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
        if (resolver.supportsParameter(parameter)) {
            return resolver;  // 返回第一个匹配的
        }
    }
    return null;
}
```

**这就是为什么解析器顺序很重要！** 比如 `@RequestParam` 的解析器在 `@ModelAttribute` 之前——否则参数可能被错误绑定。

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/support/HandlerMethodArgumentResolver.java
```

---

## 5.2 @RequestParam — 从 URL 查询参数取值

### 📌 WHAT

`@RequestParam` 标注的参数，从 URL 的查询字符串（`?key=value`）或表单 POST 数据中取值。

### 🎯 WHY

HTTP GET 请求最常见的情况：`/user?name=张三&age=18`，服务端需要拿到 `name` 和 `age`。

### 🔧 HOW

```java
@RestController
public class UserController {

    // 必填参数
    @GetMapping("/user")
    public String getUser(@RequestParam("name") String name,
                          @RequestParam("age") int age) {
        return "姓名：" + name + "，年龄：" + age;
    }

    // 可选参数（设置默认值）
    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size) {
        return "第" + page + "页，每页" + size + "条";
    }

    // 参数名和方法参数名一致时可省略 value
    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return "Hello " + name;
    }

    // 可选参数（required = false）
    @GetMapping("/filter")
    public String filter(@RequestParam(required = false) String keyword) {
        return keyword != null ? "搜索：" + keyword : "无关键词";
    }

    // 多值参数：?ids=1&ids=2&ids=3
    @GetMapping("/batch")
    public String batch(@RequestParam List<Long> ids) {
        return "IDs: " + ids;
    }
}
```

### ⚙️ 工作原理

对应的解析器是 `RequestParamMethodArgumentResolver`，它实现了 `AbstractNamedValueMethodArgumentResolver` 的模板方法：

```java
// RequestParamMethodArgumentResolver.supportsParameter():
// 判断条件：参数上有 @RequestParam 注解，或参数是简单类型（无注解时也作为默认处理）
public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(RequestParam.class)) {
        if (Map.class.isAssignableFrom(parameter.nestedIfOptional()
                .getNestedParameterType())) {
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            return (requestParam != null && StringUtils.hasText(requestParam.name()));
        } else {
            return true;
        }
    }
    // 无注解的简单类型参数，默认也当 @RequestParam 处理
    if (parameter.hasParameterAnnotation(RequestPart.class)) {
        return false;
    }
    parameter = parameter.nestedIfOptional();
    if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
        return true;
    } else if (this.useDefaultResolution) {
        return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
    } else {
        return false;
    }
}
```

resolveArgument() → resolveName() 实际从 request 取值：

```java
// RequestParamMethodArgumentResolver.resolveName():
@Override
protected Object resolveName(String name, MethodParameter parameter, 
        NativeWebRequest request) {
    // 处理文件上传
    HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
    MultipartHttpServletRequest multipartRequest = 
        WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);
    
    if (multipartRequest != null) {
        List<MultipartFile> files = multipartRequest.getFiles(name);
        if (!files.isEmpty()) {
            return (files.size() == 1 ? files.get(0) : files);
        }
    }
    
    // 从请求参数中获取值
    String[] paramValues = request.getParameterValues(name);
    if (paramValues != null) {
        return (paramValues.length == 1 ? paramValues[0] : paramValues);
    }
    return null;
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/annotation/RequestParamMethodArgumentResolver.java
```
```
解析器注册位置：
spring-webmvc/.../servlet/mvc/method/annotation/RequestMappingHandlerAdapter.getDefaultArgumentResolvers()
```

---

## 5.3 @PathVariable — 从 URL 路径取参数

### 📌 WHAT

从 RESTful URL 的路径段中取值，如 `/user/123` 中的 `123`。

### 🔧 HOW

```java
@RestController
public class UserController {

    // 单个变量
    @GetMapping("/user/{id}")
    public String getUser(@PathVariable Long id) {
        return "查询用户ID：" + id;
    }

    // 多个变量
    @GetMapping("/user/{userId}/order/{orderId}")
    public String getOrder(@PathVariable Long userId,
                           @PathVariable Long orderId) {
        return "用户" + userId + "的订单" + orderId;
    }

    // 变量名不同时指定
    @GetMapping("/user/{userId}")
    public String getUser(@PathVariable("userId") Long id) {
        return "用户ID：" + id;
    }
}
```

### ⚙️ 工作原理

`PathVariableMethodArgumentResolver`，同样继承 `AbstractNamedValueMethodArgumentResolver`：

```java
// PathVariableMethodArgumentResolver.resolveName():
@Override
protected Object resolveName(String name, MethodParameter parameter, 
        NativeWebRequest request) {
    // 从请求的 URI 模板变量中获取值
    Map<String, String> uriTemplateVars = (Map<String, String>) 
        request.getAttribute(
            HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
            RequestAttributes.SCOPE_REQUEST);
    return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
}
```

注意：URL 模板变量是 `HandlerMapping`（如 `RequestMappingHandlerMapping`）在匹配 URL 时解析出来的，存储在 request attribute 中。

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/PathVariableMethodArgumentResolver.java
```

---

## 5.4 @RequestBody — 把 HTTP 请求体转成 Java 对象

### 📌 WHAT

把 HTTP 请求体（JSON、XML 等）**反序列化**成 Java 对象。

### 🔧 HOW

```java
@RestController
public class UserController {

    // JSON → 对象
    @PostMapping("/user")
    public String createUser(@RequestBody User user) {
        return "创建用户：" + user.getName();
    }

    // String 类型
    @PostMapping("/raw")
    public String raw(@RequestBody String body) {
        return "收到原始内容：" + body;
    }
}
```

### ⚙️ 工作原理

`RequestResponseBodyMethodProcessor` 实现了参数解析 + 返回值处理两个角色：

```java
// RequestResponseBodyMethodProcessor.supportsParameter():
@Override
public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(RequestBody.class);
}

// RequestResponseBodyMethodProcessor.resolveArgument():
@Override
public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    
    parameter = parameter.nestedIfOptional();
    // ① 使用 HttpMessageConverter 读取请求体
    Object arg = readWithMessageConverters(webRequest, parameter, 
                parameter.getNestedGenericParameterType());
    
    String name = Conventions.getVariableNameForParameter(parameter);
    
    // ② 如果需要校验（@Valid）
    if (binderFactory != null) {
        WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
        if (arg != null) {
            validateIfApplicable(binder, parameter);
            if (binder.getBindingResult().hasErrors() && 
                isBindExceptionRequired(binder, parameter)) {
                throw new MethodArgumentNotValidException(parameter, 
                    binder.getBindingResult());
            }
        }
        if (arg != null) {
            mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, 
                binder.getBindingResult());
        }
    }
    
    return adaptArgumentIfNecessary(arg, parameter);
}
```

`readWithMessageConverters()` 遍历所有 `HttpMessageConverter`，找到能处理 `Content-Type` 和 Java 类型的那个来读：

```
遍历 HttpMessageConverters：
  ├→ MappingJackson2HttpMessageConverter.canRead(User.class, application/json) → true
  ├→ 调用 read() → Jackon ObjectMapper.readValue(inputStream, User.class)
  └→ 返回 User 对象
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/RequestResponseBodyMethodProcessor.java
```

---

## 5.5 @ModelAttribute — 请求参数自动绑定到对象

### 📌 WHAT

把请求参数（URL 参数或表单数据）**自动绑定**到一个 Java 对象上。

### 🔧 HOW

```java
// 表单：name=张三&age=18&email=zhangsan@example.com
// 自动映射到 User 对象

@PostMapping("/user")
public String createUser(@ModelAttribute User user) {
    // user.getName() → "张三"
    // user.getAge() → 18
    return "创建成功：" + user.getName();
}
```

### ⚙️ 工作原理

`ModelAttributeMethodProcessor` 通过 `WebDataBinder` 把请求参数绑定到对象：

```java
// ModelAttributeMethodProcessor.resolveArgument():
@Override
public final Object resolveArgument(MethodParameter parameter, ...) {
    String name = ModelFactory.getNameForParameter(parameter);
    
    // 创建目标对象
    Object attribute = (mavContainer.containsAttribute(name) ? 
        mavContainer.getModel().get(name) : 
        createAttribute(name, parameter, binderFactory, webRequest));
    
    // ★ 通过 WebDataBinder 把请求参数绑定到对象上
    WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
    if (binder.getTarget() != null) {
        if (!mavContainer.isBindingDisabled(name)) {
            bindRequestParameters(binder, webRequest);  // ← 核心：绑定
        }
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors() && ...) {
            throw new BindException(binder.getBindingResult());
        }
    }
    
    // 放入 Model
    bindingResult = binder.getBindingResult();
    Map<String, Object> bindingResultModel = bindingResult.getModel();
    mavContainer.removeAttributes(bindingResultModel);
    mavContainer.addAllAttributes(bindingResultModel);
    
    return binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType());
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ModelAttributeMethodProcessor.java
```

---

## 5.6 @RequestHeader — 获取请求头

### 📌 WHAT

从 HTTP 请求头中取值。

### 🔧 HOW

```java
@GetMapping("/info")
public String info(@RequestHeader("User-Agent") String userAgent,
                   @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
    return "浏览器：" + userAgent + "，请求ID：" + requestId;
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/annotation/RequestHeaderMethodArgumentResolver.java
```

---

## 5.7 @CookieValue — 获取 Cookie

### 📌 WHAT

从 HTTP Cookie 中取值。

### 🔧 HOW

```java
@GetMapping("/track")
public String track(@CookieValue("JSESSIONID") String sessionId) {
    return "Session ID：" + sessionId;
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ServletCookieValueMethodArgumentResolver.java
```

---

## 5.8 @SessionAttribute — 从 Session 中获取属性

### 📌 WHAT

从 `HttpSession` 中获取之前存入的属性值。

### 🔧 HOW

```java
@GetMapping("/profile")
public String profile(@SessionAttribute("currentUser") User user) {
    return "当前用户：" + user.getName();
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/SessionAttributeMethodArgumentResolver.java
```

---

## 5.9 @RequestAttribute — 从 Request 属性获取

### 📌 WHAT

获取 `HttpServletRequest` 的 attribute（通常是 Filter 或 Interceptor 设置的值）。

### 🔧 HOW

```java
@GetMapping("/data")
public String data(@RequestAttribute("startTime") Long startTime) {
    return "请求开始时间：" + startTime;
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/RequestAttributeMethodArgumentResolver.java
```

---

## 5.10 @MatrixVariable — 矩阵变量

### 📌 WHAT

从 URL 的矩阵变量（`/user;name=zhangsan;age=18`）中取值。

### 🔧 HOW

```java
// URL: /user;name=zhangsan;age=18
@GetMapping("/user")
public String user(@MatrixVariable String name,
                   @MatrixVariable int age) {
    return "姓名：" + name + "，年龄：" + age;
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/MatrixVariableMethodArgumentResolver.java
```

---

## 5.11 HttpServletRequest / HttpServletResponse — 原生 Servlet 对象

### 📌 WHAT

直接注入 Servlet API 的原生对象。

### 🔧 HOW

```java
@GetMapping("/raw")
public void raw(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String ip = request.getRemoteAddr();
    response.setContentType("text/plain");
    response.getWriter().write("Your IP: " + ip);
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ServletRequestMethodArgumentResolver.java
spring-webmvc/.../servlet/mvc/method/annotation/ServletResponseMethodArgumentResolver.java
```

---

## 5.12 HttpSession — 注入 Session

### 🔧 HOW

```java
@GetMapping("/login")
public String login(@RequestParam String username, HttpSession session) {
    session.setAttribute("user", username);
    return "登录成功：" + username;
}
```

### ⚙️ 工作原理

`ServletRequestMethodArgumentResolver` 统一处理。如果参数类型是 `HttpSession.class`，调用 `request.getSession()`。

---

## 5.13 Principal — 当前用户身份

### 🔧 HOW

```java
@GetMapping("/whoami")
public String whoami(Principal principal) {
    return "当前用户：" + principal.getName();
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/PrincipalMethodArgumentResolver.java
```

---

## 5.14 InputStream / Reader — 原始输入流

### 📌 WHAT

获取原始的 HTTP 请求体输入流（不经过任何转换）。

### 🔧 HOW

```java
@PostMapping("/upload")
public String upload(InputStream inputStream) throws IOException {
    byte[] data = inputStream.readAllBytes();
    return "收到 " + data.length + " 字节";
}
```

> ⚠️ 注意：用了 `InputStream` 就不能再用 `@RequestBody`——流只能读一次。

---

## 5.15 OutputStream / Writer — 原始输出流

### 🔧 HOW

```java
@GetMapping("/download")
public void download(OutputStream outputStream) throws IOException {
    outputStream.write("Hello World".getBytes());
}
```

---

## 5.16 Map / Model / ModelMap — 注入 Model

### 📌 WHAT

注入 Model 容器，用于向视图传递数据。

### 🔧 HOW

```java
@GetMapping("/page")
public String page(Map<String, Object> model) {
    model.put("title", "首页");
    model.put("message", "欢迎");
    return "index";  // 视图名
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/annotation/MapMethodProcessor.java
```

---

## 5.17 Errors / BindingResult — 绑定/校验结果

### 📌 WHAT

接收数据绑定或校验的错误信息。

### 🔧 HOW

```java
@PostMapping("/user")
public String createUser(@Valid @ModelAttribute User user, BindingResult result) {
    if (result.hasErrors()) {
        return "error";  // 返回错误页面
    }
    // 保存用户...
    return "success";
}
```

> ⚠️ `BindingResult` 必须紧跟在被校验的参数后面，否则 Spring 会直接抛异常。

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/method/annotation/ErrorsMethodArgumentResolver.java
```

---

## 5.18 @RequestPart — 文件上传 + JSON 混合

### 📌 WHAT

处理 `multipart/form-data` 中的混合内容：一部分是文件，一部分是 JSON。

### 🔧 HOW

```java
@PostMapping("/upload")
public String upload(@RequestPart("file") MultipartFile file,
                     @RequestPart("metadata") UserMetadata metadata) {
    // file 是上传的文件
    // metadata 是 JSON 部分，自动反序列化
    return "上传成功";
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/RequestPartMethodArgumentResolver.java
```

---

## 5.19 HttpEntity / RequestEntity — 完整 HTTP 实体

### 📌 WHAT

获取完整的 HTTP 请求实体，包括请求头和请求体。

### 🔧 HOW

```java
@PostMapping("/echo")
public String echo(HttpEntity<String> entity) {
    HttpHeaders headers = entity.getHeaders();
    String body = entity.getBody();
    return "Headers: " + headers + "\nBody: " + body;
}

@PostMapping("/echo2")
public String echo2(RequestEntity<User> requestEntity) {
    User user = requestEntity.getBody();
    HttpMethod method = requestEntity.getMethod();
    URI url = requestEntity.getUrl();
    return "Method: " + method + ", URL: " + url + ", User: " + user.getName();
}
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/HttpEntityMethodProcessor.java
```

---

## 5.20 自定义参数解析器

### 📌 WHAT

当内置 30+ 个参数解析器都不满足需求时，实现自己的。

### 🔧 HOW

```java
// 1. 定义注解
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

// 2. 实现解析器
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = request.getHeader("Authorization");
        // 从 token 中解析用户信息
        return userService.getUserFromToken(token);
    }
}

// 3. 注册解析器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}

// 4. 使用
@GetMapping("/me")
public User me(@CurrentUser User user) {
    return user;  // 自动注入当前登录用户
}
```

---

## 🔗 参数解析完整链路总结

```
Controller 方法被调用（InvocableHandlerMethod.invokeForRequest）
    └→ getMethodArgumentValues()
         └→ for each parameter:
              ├→ ① findProvidedArgument() — 检查是否有直接提供的参数
              │    (如异常处理时提供的 Exception 参数)
              │
              ├→ ② 遍历所有 HandlerMethodArgumentResolver：
              │    ┌────────────────────────────────────────────────┐
              │    │ 1. RequestParamMethodArgumentResolver        │
              │    │ 2. RequestParamMapMethodArgumentResolver     │
              │    │ 3. PathVariableMethodArgumentResolver        │
              │    │ 4. MatrixVariableMethodArgumentResolver      │
              │    │ 5. ServletModelAttributeMethodProcessor      │
              │    │ 6. RequestResponseBodyMethodProcessor        │
              │    │ 7. RequestPartMethodArgumentResolver         │
              │    │ 8. RequestHeaderMethodArgumentResolver       │
              │    │ ... (共 30+ 个解析器)                         │
              │    │ 30. 用户自定义解析器                           │
              │    └────────────────────────────────────────────────┘
              │    匹配规则：第一个 supportsParameter() 返回 true 的
              │
              ├→ ③ resolveArgument() — 从请求中取出参数值
              │
              └→ ④ 收集所有参数值到 args[] 数组
    
    └→ doInvoke(args[])  ← java.lang.reflect.Method.invoke(bean, args)
```

**关键设计原则**：

1. **职责单一**：每个 Resolver 只处理一种类型，互不干扰
2. **策略模式**：supportsParameter() 决定由谁处理，resolveArgument() 执行处理
3. **顺序敏感**：排在前面先匹配——比如 `RequestResponseBodyMethodProcessor` 必须排在能匹配 `String` 的通用解析器之前

---

> **下一阶段**：[第六阶段：返回值处理 HandlerMethodReturnValueHandler](06-return-value-handler.md)
