# Spring MVC 6.0 完全知识手册

> 基于 `spring-webmvc` 和 `spring-web` 源码（Spring Framework 6.0.0）
>
> 共 14 个阶段，106 个知识点，每个知识点包含 **WHAT（是什么）/ WHY（为什么）/ HOW（怎么用）** + **源码级工作原理** + **代码示例**

---

## 📖 文档目录

| 阶段 | 文档 | 知识点数 | 核心内容 |
|------|------|---------|---------|
| 1 | [HTTP 与 Web 基础抽象](01-http-web-foundation.md) | 9 | HttpInputMessage、HttpHeaders、MediaType、RestTemplate 等 |
| 2 | [DispatcherServlet 与请求分发](02-dispatcher-servlet.md) | 6 | DispatcherServlet、doDispatch()、WebApplicationContext |
| 3 | [HandlerMapping 路由映射](03-handler-mapping.md) | 14 | RequestMappingHandlerMapping、@RequestMapping、请求条件匹配 |
| 4 | [HandlerAdapter 处理器适配](04-handler-adapter.md) | 6 | RequestMappingHandlerAdapter、适配器模式 |
| 5 | [参数解析 HandlerMethodArgumentResolver](05-argument-resolver.md) | 20 | @RequestParam、@RequestBody、@PathVariable 等全部参数注解 |
| 6 | [返回值处理 HandlerMethodReturnValueHandler](06-return-value-handler.md) | 15 | @ResponseBody、ResponseEntity、ModelAndView 等全部返回值类型 |
| 7 | [HttpMessageConverter 消息转换](07-http-message-converter.md) | 9 | JSON ↔ 对象转换、内容协商选择算法 |
| 8 | [ViewResolver 视图解析](08-view-resolver.md) | 19 | JSP、Thymeleaf、JSON、PDF、Excel 等全部视图类型 |
| 9 | [数据绑定 & 类型转换 & 校验](09-data-binding-validation.md) | 16 | DataBinder、Converter、Formatter、@Valid、Bean Validation |
| 10 | [异常处理](10-exception-handling.md) | 15 | @ExceptionHandler、@ControllerAdvice、ProblemDetail（RFC 7807） |
| 11 | [拦截器 & 过滤器 & CORS](11-interceptor-cors.md) | 9 | HandlerInterceptor、Filter、CORS 跨域 |
| 12 | [高级特性：异步/函数式/新特性/配置/周边](12-advanced-features.md) | 24 | 异步请求、RouterFunction、RestClient、i18n、文件上传、FlashMap |

### 📘 特别推荐

| 文档 | 说明 |
|------|------|
| [**完整请求处理流程（10步逐行拆解）**](workflow-detailed.md) | 基于 `doDispatch()` 源码，逐步详解每个方法干了什么，适合精读 |

---

## 🗺️ Spring MVC 请求处理总流程

```
                    ┌─────────────────────────────────────┐
                    │        HTTP Request（HTTP 请求）       │
                    └──────────────┬──────────────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │     DispatcherServlet        │  ← 总调度员（第二阶段）
                    │     doDispatch()              │
                    └──────────────┬───────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
   ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
   │  HandlerMapping   │ │  HandlerAdapter  │ │  HandlerException│
   │  (找谁处理)        │ │  (怎么调用)      │ │  Resolver(异常)   │
   │  第三阶段          │ │  第四阶段         │ │  第十阶段         │
   └────────┬─────────┘ └────────┬─────────┘ └──────────────────┘
            │                    │
            │     ┌──────────────┴──────────────┐
            │     │                             │
            │     ▼                             ▼
            │  ┌────────────────────┐  ┌────────────────────┐
            │  │ ArgumentResolver   │  │ ReturnValueHandler │
            │  │ (参数解析)          │  │ (返回值处理)         │
            │  │ 第五阶段            │  │ 第六阶段             │
            │  └────────┬───────────┘  └────────┬───────────┘
            │           │                       │
            │           ▼                       ▼
            │  ┌─────────────────────────────────────────┐
            │  │       HttpMessageConverter              │
            │  │       (消息转换，JSON↔对象)              │
            │  │       第七阶段                            │
            │  └─────────────────────────────────────────┘
            │
            ▼
   ┌──────────────────┐
   │   ViewResolver    │
   │   (视图解析)       │
   │   第八阶段          │
   └──────────────────┘
```

---

## 🎯 文档使用说明

每篇文档的结构统一为：

```
## 知识点名称

### 📌 WHAT（是什么）
一句话说清楚这个类/接口/注解是干什么的

### 🎯 WHY（为什么需要它）
解决什么问题？没有它会怎样？

### 🔧 HOW（怎么用）
代码示例 + 配置方式

### ⚙️ 工作原理
深入源码级别，解释它到底是怎么实现的，
包括关键方法调用链、类关系、执行流程

### 📂 源码位置
在 spring-webmvc 中的具体路径
```

---

## 🏷️ 版本信息

- **Spring Framework**: 6.0.0
- **Java 版本要求**: Java 17+
- **Servlet API**: Jakarta Servlet 6.0
- **包名前缀**: `jakarta.servlet`（非 `javax.servlet`）
