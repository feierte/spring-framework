# 第十阶段：异常处理

> 所属模块：`spring-webmvc/src/main/java/org/springframework/web/servlet/`
>
> 核心问题：Controller 抛出异常后，怎么变成友好的错误响应而不是 Tomcat 默认的 500 白页？

---

## 10.1 DispatcherServlet 中异常处理的位置

### ⚙️ 工作原理

```java
// DispatcherServlet.doDispatch() — 异常捕获位置：
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    try {
        // ... handler 执行 ...
        mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        // ...
        processDispatchResult(processedRequest, response, mappedHandler, mv, null);
    }
    catch (Exception ex) {
        // ★ 异常统一在这里捕获！
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        processDispatchResult(processedRequest, response, mappedHandler, null, ex);
    }
}

// processDispatchResult() — 异常分发给 HandlerExceptionResolver：
private void processDispatchResult(..., Exception exception) {
    if (exception != null) {
        if (exception instanceof ModelAndViewDefiningException) {
            // 直接返回 ModelAndView
            mv = ((ModelAndViewDefiningException) exception).getModelAndView();
        } else {
            // ★ 遍历所有 HandlerExceptionResolver 处理异常
            Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
            mv = processHandlerException(request, response, handler, exception);
        }
    }
    // 渲染视图...
}

protected ModelAndView processHandlerException(HttpServletRequest request,
        HttpServletResponse response, Object handler, Exception ex) {
    
    ModelAndView exMv = null;
    if (this.handlerExceptionResolvers != null) {
        for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
            exMv = resolver.resolveException(request, response, handler, ex);
            if (exMv != null) {
                break;  // 第一个返回非 null 的胜出！
            }
        }
    }
    if (exMv != null) {
        // 渲染异常视图
        return exMv;
    }
    // 没人处理就重新抛出 → Tomcat 的 500 页面
    throw ex;
}
```

---

## 10.2 HandlerExceptionResolver 接口

### 📌 WHAT

`org.springframework.web.servlet.HandlerExceptionResolver` 是异常解析器的统一接口：

```java
// HandlerExceptionResolver.java
public interface HandlerExceptionResolver {
    @Nullable
    ModelAndView resolveException(HttpServletRequest request, 
        HttpServletResponse response, @Nullable Object handler, Exception ex);
}
```

### ⚙️ 默认注册的 4 个 Resolver（优先级从高到低）：

| 顺序 | Resolver | 功能 |
|------|----------|------|
| 1 | `ExceptionHandlerExceptionResolver` | 处理 `@ExceptionHandler` 方法 |
| 2 | `ResponseStatusExceptionResolver` | 处理 `@ResponseStatus` 注解的异常 |
| 3 | `DefaultHandlerExceptionResolver` | 标准 Spring MVC 异常 → HTTP 状态码 |
| 4 | `SimpleMappingExceptionResolver`（可选） | 异常 → 错误页面映射 |

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerExceptionResolver.java
```

---

## 10.3 @ExceptionHandler — Controller 级异常处理

### 📌 WHAT

在 Controller 中用 `@ExceptionHandler` 定义专门处理某个异常的方法。

### 🔧 HOW

```java
@RestController
public class UserController {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("参数错误：" + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception e) {
        return ResponseEntity.status(500).body("服务器内部错误");
    }
}
```

### ⚙️ 工作原理

`ExceptionHandlerExceptionResolver` 在启动时扫描所有 Controller 的 `@ExceptionHandler` 方法，建立异常类型 → 处理方法的映射。异常发生时，找到最匹配的处理方法，用参数解析器+返回值处理器执行它。

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/ExceptionHandlerExceptionResolver.java
```

---

## 10.4 @ControllerAdvice / @RestControllerAdvice — 全局异常处理

### 📌 WHAT

把 `@ExceptionHandler` 提升到全局，所有 Controller 的异常都能被它处理。

### 🔧 HOW

```java
@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // 处理参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // 处理所有未捕获异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("系统错误：" + e.getMessage());
    }
}
```

### ⚙️ 工作原理

`@ControllerAdvice` 的 Bean 会被 `ExceptionHandlerExceptionResolver` 的 `initExceptionHandlerAdviceCache()` 方法扫描并缓存。异常发生时，优先匹配 Controller 内的 `@ExceptionHandler`，然后匹配全局 `@ControllerAdvice`。

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/bind/annotation/ControllerAdvice.java
```

---

## 10.5 @ResponseStatus — 异常自动转 HTTP 状态码

### 📌 WHAT

把 Java 异常标记为特定的 HTTP 状态码。

### 🔧 HOW

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Controller 中直接抛出
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    throw new ResourceNotFoundException("用户 " + id + " 不存在");
    // → HTTP 404
}
```

### ⚙️ 工作原理

`ResponseStatusExceptionResolver.resolveException()` 检查异常上是否有 `@ResponseStatus`，有则调用 `response.sendError(statusCode, reason)`。

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/annotation/ResponseStatusExceptionResolver.java
```

---

## 10.6 DefaultHandlerExceptionResolver — 标准异常映射

### 📌 WHAT

把 Spring MVC 自己的异常自动转换为 HTTP 状态码。

### ⚙️ 内置映射

| 异常 | HTTP 状态码 |
|------|------------|
| `HttpRequestMethodNotSupportedException` | 405 Method Not Allowed |
| `HttpMediaTypeNotSupportedException` | 415 Unsupported Media Type |
| `HttpMediaTypeNotAcceptableException` | 406 Not Acceptable |
| `MissingPathVariableException` | 500 Internal Server Error |
| `MissingServletRequestParameterException` | 400 Bad Request |
| `ServletRequestBindingException` | 400 Bad Request |
| `ConversionNotSupportedException` | 500 Internal Server Error |
| `TypeMismatchException` | 400 Bad Request |
| `HttpMessageNotReadableException` | 400 Bad Request |
| `HttpMessageNotWritableException` | 500 Internal Server Error |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `NoHandlerFoundException` | 404 Not Found |
| `AsyncRequestTimeoutException` | 503 Service Unavailable |

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/support/DefaultHandlerExceptionResolver.java
```

---

## 10.7 ResponseStatusException — 手动抛出带状态码的异常

### 📌 WHAT

一种便捷方式：直接 new 一个带状态码的运行时异常，不用自己定义异常类。

### 🔧 HOW

```java
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    if (user == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在: " + id);
    }
    return user;
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/server/ResponseStatusException.java
```

---

## 10.8 SimpleMappingExceptionResolver — 异常映射到错误页面

### 📌 WHAT

在 XML 或 Java Config 中配置异常类型 → 错误视图的映射。**在现代前后端分离场景中基本不用。**

### 📂 源码位置

```
spring-webmvc/.../servlet/handler/SimpleMappingExceptionResolver.java
```

---

## 10.9 ProblemDetail（RFC 7807）— Spring 6 新特性

### 📌 WHAT

Spring 6 / Spring Boot 3 引入的标准化错误响应格式（RFC 7807）。

### 🔧 HOW

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ProblemDetail handleNotFound(ResourceNotFoundException e) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND, e.getMessage());
    problem.setTitle("资源未找到");
    problem.setProperty("timestamp", Instant.now());
    return problem;  // 返回值自动序列化为 JSON
}
```

**响应格式**：

```json
{
    "type": "about:blank",
    "title": "资源未找到",
    "status": 404,
    "detail": "用户 123 不存在",
    "instance": "/user/123",
    "timestamp": "2025-01-01T12:00:00Z"
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/ErrorResponse.java
spring-web/src/main/java/org/springframework/http/ProblemDetail.java
```

---

## 10.10 异常处理完整链路

```
Controller 方法抛出异常
    │
    ▼
DispatcherServlet.doDispatch() → catch (Exception ex)
    │
    ▼
processDispatchResult(request, response, handler, exception)
    │
    ▼
processHandlerException(request, response, handler, exception)
    │
    └→ 遍历 handlerExceptionResolvers（按 Order 排序）：
        
        ┌───────────────────────────────────────────────────┐
        │ 1. ExceptionHandlerExceptionResolver             │
        │    检查 @ExceptionHandler 方法                     │
        │    ├→ Controller 内的 @ExceptionHandler → 执行     │
        │    └→ @ControllerAdvice 的 @ExceptionHandler → 执行│
        │    返回 ModelAndView                              │
        ├───────────────────────────────────────────────────┤
        │ 2. ResponseStatusExceptionResolver               │
        │    检查 @ResponseStatus 注解                        │
        │    → response.sendError(statusCode)               │
        ├───────────────────────────────────────────────────┤
        │ 3. DefaultHandlerExceptionResolver               │
        │    标准异常 → HTTP 状态码                          │
        │    → response.sendError(statusCode)               │
        └───────────────────────────────────────────────────┘
        
        任意一个返回非 null → 停止 → 渲染错误视图/响应
        全部返回 null → 重新抛出 → Tomcat 默认 500 页面
```

---

> **下一阶段**：[第十一阶段：拦截器 & 过滤器 & CORS](11-interceptor-cors.md)
