# 第十一阶段：拦截器 & CORS & 过滤器

> 核心问题：怎么在请求处理前/后加"切面"逻辑？怎么处理跨域请求？

---

## 11.1 HandlerInterceptor — Spring MVC 拦截器

### 📌 WHAT

`org.springframework.web.servlet.HandlerInterceptor` 定义了三个切入时机：

```java
// HandlerInterceptor.java
public interface HandlerInterceptor {
    
    // ① Controller 方法执行前
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) throws Exception {
        return true;  // false = 拦截，不继续执行
    }

    // ② Controller 方法执行后、视图渲染前
    default void postHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler, @Nullable ModelAndView modelAndView) throws Exception {
    }

    // ③ 视图渲染完成后（一定会执行，即使有异常）
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, @Nullable Exception ex) throws Exception {
    }
}
```

### 🎯 WHY

经典场景：
- **登录检查**：`preHandle` 检查 Session，未登录返回 false
- **请求日志**：`preHandle` 记录开始时间，`afterCompletion` 算耗时
- **权限校验**：检查用户角色
- **统一响应头**：给所有响应加自定义 Header

### 🔧 HOW

```java
// 1. 定义拦截器
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) {
        Object user = request.getSession().getAttribute("user");
        if (user == null) {
            response.setStatus(401);
            response.getWriter().write("请先登录");
            return false;  // 阻止继续
        }
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(request.getRequestURI() + " 耗时 " + duration + "ms");
    }
}

// 2. 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")           // 拦截所有
                .excludePathPatterns("/login", "/register");  // 排除登录/注册
    }
}
```

### ⚙️ 工作原理

`DispatcherServlet.doDispatch()` 中拦截器被调用的位置：

```
doDispatch()
  ├→ mappedHandler = getHandler(request)
  │    ↓ HandlerExecutionChain 中包含 Handler + 拦截器列表
  │
  ├→ mappedHandler.applyPreHandle(request, response)    ← ① preHandle
  │    遍历所有拦截器的 preHandle()，任何一个返回 false 就停止
  │
  ├→ ha.handle(request, response, handler)              ← Controller 执行
  │
  ├→ mappedHandler.applyPostHandle(request, response, mv) ← ② postHandle
  │    逆序遍历
  │
  └→ processDispatchResult()
       └→ mappedHandler.triggerAfterCompletion(request, response, ex) ← ③ afterCompletion
           逆序遍历，一定执行（finally 块中）
```

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerInterceptor.java
spring-webmvc/src/main/java/org/springframework/web/servlet/HandlerExecutionChain.java
```

---

## 11.2 拦截器 vs Filter

| 维度 | Filter（Servlet 标准） | HandlerInterceptor（Spring MVC） |
|------|----------------------|--------------------------------|
| 规范 | Jakarta Servlet 规范 | Spring MVC 专有 |
| 作用范围 | 所有请求（包括静态资源） | 只拦截进入 DispatcherServlet 的请求 |
| 能访问的上下文 | 只有 request/response | 可以知道是哪个 Handler（哪个 Controller 方法） |
| 控制粒度 | 只能按 URL 模式过滤 | 可以精确到 Controller 方法 |
| 能否获取 ModelAndView | ❌ | ✅（postHandle 中） |

**一句话**：Filter 是看门的，Interceptor 是进门的。先用 Filter 干通用的活（编码、安全头），再用 Interceptor 干业务相关的活（登录检查、权限）。

---

## 11.3 CORS 跨域

### 📌 WHAT

CORS（Cross-Origin Resource Sharing）是浏览器的一种安全机制——当前端 `http://localhost:3000` 调用后端 `http://localhost:8080` 时，浏览器会先发一个"预检请求"（OPTIONS），确认后端允许跨域后才发真正的请求。

### 🔧 HOW

```java
// 方式一：@CrossOrigin 注解
@RestController
@CrossOrigin(origins = "http://localhost:3000")  // 类级别
public class UserController {

    @CrossOrigin(origins = "*", maxAge = 3600)    // 方法级别（覆盖类级别）
    @GetMapping("/users")
    public List<User> list() {
        return userService.findAll();
    }
}

// 方式二：全局配置
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")         // 对哪些路径生效
                .allowedOrigins("http://localhost:3000")  // 允许的来源
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // 允许的方法
                .allowedHeaders("*")           // 允许的请求头
                .allowCredentials(true)        // 是否允许 Cookie
                .maxAge(3600);                 // 预检请求缓存时间（秒）
    }
}
```

### ⚙️ 工作原理

`AbstractHandlerMapping.getHandler()` 在处理请求时会检查 CORS 配置：

```java
// AbstractHandlerMapping.getHandler():
public final HandlerExecutionChain getHandler(HttpServletRequest request) {
    Object handler = getHandlerInternal(request);
    // ...
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
    
    // ★ CORS 处理
    if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
        CorsConfiguration config = getCorsConfiguration(handler, request);
        executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
    }
    
    return executionChain;
}
```

如果是 OPTIONS 预检请求且配置了 CORS，`DispatcherServlet` 会直接处理不进入 Controller。

### 📂 源码位置

```
spring-webmvc/src/main/java/org/springframework/web/servlet/handler/AbstractHandlerMapping.java
spring-web/src/main/java/org/springframework/web/cors/CorsConfiguration.java
spring-web/src/main/java/org/springframework/web/filter/CorsFilter.java
```

---

> **下一阶段**：[第十二阶段：高级特性合集](12-advanced-features.md)
