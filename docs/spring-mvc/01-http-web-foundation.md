# Spring MVC 第一阶段：HTTP 与 Web 基础抽象

> **Spring Framework 版本**: 6.0.0  
> **源代码模块**: `spring-web`  
> **核心包路径**: `org.springframework.http` / `org.springframework.web.util` / `org.springframework.web.client`

---

## 目录

1. [HttpMessage — HTTP 消息的顶层抽象](#1-httpmessage--http-消息的顶层抽象)
2. [HttpInputMessage / HttpOutputMessage — 输入输出消息接口](#2-httpinputmessage--httpoutputmessage--输入输出消息接口)
3. [HttpHeaders — HTTP 头部封装](#3-httpheaders--http-头部封装)
4. [MediaType — HTTP 媒体类型](#4-mediatype--http-媒体类型)
5. [HttpMethod — HTTP 请求方法](#5-httpmethod--http-请求方法)
6. [HttpStatus / HttpStatusCode — HTTP 状态码](#6-httpstatus--httpstatuscode--http-状态码)
7. [HttpEntity / ResponseEntity — HTTP 实体封装](#7-httpentity--responseentity--http-实体封装)
8. [RequestEntity — HTTP 请求实体](#8-requestentity--http-请求实体)
9. [UriComponentsBuilder — URI 构建器](#9-uricomponentsbuilder--uri-构建器)
10. [RestTemplate / RestClient — HTTP 客户端](#10-resttemplate--restclient--http-客户端)

---

## 1. HttpMessage — HTTP 消息的顶层抽象

### WHAT

`HttpMessage` 是 Spring HTTP 模块的**顶层接口**，定义了任何 HTTP 消息（请求或响应）都必须具备的基础能力——获取 HTTP 头部。它是 `HttpInputMessage` 和 `HttpOutputMessage` 的共同父接口。

### WHY

HTTP 协议的核心概念是"消息"（Message），无论是客户端发出的请求（Request）还是服务端返回的响应（Response），都由**头部（Headers）** 和**消息体（Body）** 两部分组成。Spring 将这种共性提取为 `HttpMessage` 接口，统一管理 HTTP 消息的头部访问，为后续的输入/输出消息抽象奠定基础。

### HOW

```java
package org.springframework.http;

/**
 * Represents the base interface for HTTP request and response messages.
 * Consists of HttpHeaders, retrievable via getHeaders().
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpMessage {

    /**
     * Return the headers of this message.
     * @return a corresponding HttpHeaders object (never null)
     */
    HttpHeaders getHeaders();
}
```

### 工作原理（源码分析）

`HttpMessage` 本身只有 `getHeaders()` 一个方法，但它构建了 Spring HTTP 模块的**继承体系根基**：

```
HttpMessage                         // 顶层：头部能力
├── HttpInputMessage                // 扩展：可读的消息体（InputStream）
│   └── ServerHttpRequest           // 服务端请求（ServerHttpRequest extends HttpInputMessage）
│   └── ClientHttpResponse          // 客户端响应（ClientHttpResponse extends HttpInputMessage）
└── HttpOutputMessage               // 扩展：可写的消息体（OutputStream）
    └── ServerHttpResponse          // 服务端响应（ServerHttpResponse extends HttpOutputMessage）
    └── ClientHttpRequest           // 客户端请求（ClientHttpRequest extends HttpOutputMessage）
```

这种设计的巧妙之处在于对称性：
- **服务端视角**：接收请求（`HttpInputMessage`），发送响应（`HttpOutputMessage`）
- **客户端视角**：发送请求（`HttpOutputMessage`），接收响应（`HttpInputMessage`）

`HttpMessage.getHeaders()` 返回的 `HttpHeaders` 是 **never null** 的——因为无论是空消息头还是实际有内容的头部，都必须有对应的返回对象，这个约束将 null 检查下沉到了框架内部。

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpMessage.java
```

---

## 2. HttpInputMessage / HttpOutputMessage — 输入输出消息接口

### WHAT

- **`HttpInputMessage`**：表示**可读的 HTTP 消息**，在 `HttpMessage` 的基础上增加了 `getBody()` 方法，返回一个 `InputStream`，用于读取消息体内容。
- **`HttpOutputMessage`**：表示**可写的 HTTP 消息**，在 `HttpMessage` 的基础上增加了 `getBody()` 方法，返回一个 `OutputStream`，用于写入消息体内容。

这两个接口在服务端和客户端语义中角色互换：
- 服务端：`HttpInputMessage` = 客户端发来的请求（读），`HttpOutputMessage` = 要返回给客户端的响应（写）
- 客户端：`HttpOutputMessage` = 发送给服务端的请求（写），`HttpInputMessage` = 服务端返回的响应（读）

### WHY

将 HTTP 消息的通路方向抽象为 `Input` 和 `Output` 两个正交接口，使得框架能够统一处理**消息体的读写**，而不必关心这个操作发生在客户端还是服务端。这种对称设计也是 HTTP 协议"请求-响应"模型的直接体现。

### HOW

```java
// === HttpInputMessage ===
package org.springframework.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpInputMessage extends HttpMessage {

    /**
     * Return the body of the message as an input stream.
     * @return the input stream body (never null)
     * @throws IOException in case of I/O errors
     */
    InputStream getBody() throws IOException;
}
```

```java
// === HttpOutputMessage ===
package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpOutputMessage extends HttpMessage {

    /**
     * Return the body of the message as an output stream.
     * @return the output stream body (never null)
     * @throws IOException in case of I/O errors
     */
    OutputStream getBody() throws IOException;
}
```

### 工作原理（源码分析）

1. **继承关系**：两个接口都继承自 `HttpMessage`，因此它们都具备 `getHeaders()` 获取 HTTP 头部的能力，并各自增加了对消息体的读写能力。

2. **返回值的 never-null 约束**：两个 `getBody()` 方法的 Javadoc 中都明确标注 `never null`，这是 Spring 框架的设计原则——框架内部保证不返回 null，使用者无需进行 null 检查。

3. **典型实现路径**：

```
服务端请求处理链（以 Tomcat 为例）:
   HttpServletRequest (Servlet API)
     → ServletServerHttpRequest (implements HttpInputMessage)
        → getBody() → request.getInputStream()

客户端 HTTP 调用链:
   RestTemplate.doExecute()
     → ClientHttpRequest (implements HttpOutputMessage)
        → getBody() → 底层 HTTP 连接的 OutputStream
     → ClientHttpResponse (implements HttpInputMessage)
        → getBody() → 底层 HTTP 连接的 InputStream
```

4. **代码示例 — 自定义实现 HttpInputMessage 来解析请求体**：

```java
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SimpleHttpInputMessage implements HttpInputMessage {

    private final HttpHeaders headers;
    private final byte[] body;

    public SimpleHttpInputMessage(String bodyContent, MediaType contentType) {
        this.body = bodyContent.getBytes(StandardCharsets.UTF_8);
        this.headers = new HttpHeaders();
        this.headers.setContentType(contentType);
        this.headers.setContentLength(this.body.length);
    }

    @Override
    public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(this.body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }
}

// 使用示例
public class Demo {
    public static void main(String[] args) throws IOException {
        HttpInputMessage message = new SimpleHttpInputMessage(
            "{\"name\": \"Spring\"}", 
            MediaType.APPLICATION_JSON
        );

        // 读取消息体
        byte[] buffer = new byte[1024];
        int len = message.getBody().read(buffer);
        String bodyStr = new String(buffer, 0, len, StandardCharsets.UTF_8);
        System.out.println("Body: " + bodyStr);                    // {"name": "Spring"}
        System.out.println("Content-Type: " + message.getHeaders().getContentType());  // application/json
    }
}
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpInputMessage.java
spring-web/src/main/java/org/springframework/http/HttpOutputMessage.java
```

---

## 3. HttpHeaders — HTTP 头部封装

### WHAT

`HttpHeaders` 是 Spring 对 HTTP 请求/响应头部的**数据结构表示**，本质是一个 `Map<String, List<String>>`，将头部名称映射到值列表。它同时实现了 `MultiValueMap<String, String>` 和 `Serializable` 接口，提供了大量针对常见 HTTP 头部（Content-Type、Accept、Cache-Control、Authorization 等）的类型安全的访问方法。

### WHY

直接操作原始的 `Map<String, String>` 来处理 HTTP 头部有三个问题：
1. **一个头部名可能对应多个值**（如 `Accept: text/html, application/json`）
2. **大小写不敏感**：HTTP 规范要求头部名大小写不敏感（如 `content-type` 和 `Content-Type` 等效）
3. **类型不安全**：Date、Content-Length、ETag 等头部有特定的类型语义，原始字符串操作容易出错

`HttpHeaders` 通过类型安全的 getter/setter 方法（如 `getContentType()` 返回 `MediaType`、`getContentLength()` 返回 `long`、`getIfModifiedSince()` 返回 `long` 类型的毫秒时间戳）解决了这些问题。

### HOW

```java
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 1. 基本 CRUD 操作
HttpHeaders headers = new HttpHeaders();

// 添加/设置头部值
headers.add("X-Custom-Header", "value1");
headers.add("X-Custom-Header", "value2");  // 同名字段允许多个值
headers.set("X-Single-Value", "only-one");   // 覆盖设置

// 获取头部值
String firstValue = headers.getFirst("X-Custom-Header");  // "value1"
List<String> allValues = headers.get("X-Custom-Header");   // ["value1", "value2"]

// 2. 类型安全的常用头部方法
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setContentLength(1024);
headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
headers.setBasicAuth("admin", "secret");  // 自动 Base64 编码 Authorization 头
headers.setBearerAuth("jwt-token-here");  // 设置 Bearer token

// 3. 读取常用头部
MediaType contentType = headers.getContentType();              // application/json
long contentLength = headers.getContentLength();               // 1024
List<MediaType> acceptTypes = headers.getAccept();             // [application/json, text/plain]
String origin = headers.getOrigin();                           // null（未设置）
headers.setOrigin("https://example.com");                      // Origin: https://example.com
```

### 工作原理（源码分析）

`HttpHeaders` 的核心数据结构是声明在类中的：

```java
// 第 424 行
@SuppressWarnings("serial")
final MultiValueMap<String, String> headers;
```

默认构造函数使用 `LinkedCaseInsensitiveMap` 实现大小写不敏感：

```java
// 第 431-433 行
public HttpHeaders() {
    this(CollectionUtils.toMultiValueMap(
        new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH)));
}
```

这意味着 `headers.get("content-type")` 和 `headers.get("Content-Type")` 会返回相同的结果。

#### 类型安全方法的工作原理

以 `getContentType()` 为例：

```java
// HttpHeaders.java 中类似这样的方法（略简化）
public MediaType getContentType() {
    String value = getFirst(CONTENT_TYPE);  // CONTENT_TYPE = "Content-Type"
    return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
}
```

框架内部将字符串自动解析为目标类型（`MediaType`、`long`、`long（毫秒时间戳）`、`List<MediaType>` 等），这比手动处理字符串要健壮得多。

#### 内置常量

`HttpHeaders` 中定义了几乎所有常用 HTTP 头部的字符串常量（第 86-387 行），如 `ACCEPT`、`AUTHORIZATION`、`CONTENT_TYPE`、`CACHE_CONTROL`、`COOKIE`、`SET_COOKIE`、`ETAG`、`HOST`、`LOCATION`、`ORIGIN` 等几十个常量，避免了代码中散布硬编码的字符串字面量。

### 完整代码示例

```java
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HttpHeadersDemo {
    public static void main(String[] args) {
        HttpHeaders headers = new HttpHeaders();

        // 设置请求头
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("user", "pass");
        headers.set("X-Request-Id", "req-12345");

        // 模拟服务端设置响应头
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        responseHeaders.setContentLength(256);
        responseHeaders.setLocation(URI.create("https://api.example.com/resources/1"));
        responseHeaders.setCacheControl("max-age=3600");
        responseHeaders.setETag("\"v1.0\"");
        responseHeaders.set("X-Trace-Id", "trace-abc");

        // 检查 header 是否存在
        if (responseHeaders.containsKey("X-Trace-Id")) {
            System.out.println("Trace ID: " + responseHeaders.getFirst("X-Trace-Id"));
        }

        // 遍历所有头部
        System.out.println("\n=== All Response Headers ===");
        responseHeaders.forEach((name, values) -> {
            System.out.println(name + ": " + String.join(", ", values));
        });
    }
}
```

**输出示例**：

```
Trace ID: trace-abc

=== All Response Headers ===
Content-Type: application/json
Content-Length: 256
Location: https://api.example.com/resources/1
Cache-Control: max-age=3600
ETag: "v1.0"
X-Trace-Id: trace-abc
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpHeaders.java
```

---

## 4. MediaType — HTTP 媒体类型

### WHAT

`MediaType` 继承自 `org.springframework.util.MimeType`，是 Spring 中表示 **HTTP 媒体类型**（MIME type）的类。它由一个主类型（type）、一个子类型（subtype）和一组可选的参数（parameters）组成，格式为 `type/subtype;param1=value1;param2=value2`。

例如：`application/json`、`text/html;charset=UTF-8`、`multipart/form-data;boundary=----FormBoundary`。

### WHY

HTTP 协议通过 `Content-Type` 和 `Accept` 头部来协商客户端和服务端之间传输数据的格式。直接使用字符串（如 `"application/json"`）存在以下问题：
1. 拼写错误难以在编译期发现
2. 无法利用 IDE 的代码补全和重构能力
3. 解析 `charset`、`q`（quality factor，质量因子）等参数需要手动处理
4. 内容协商（Content Negotiation）——判断一个 MediaType 是否兼容另一个——需要复杂的字符串比较逻辑

`MediaType` 通过提供**类型安全的常量**、**参数解析能力**和**兼容性检查**方法解决了以上问题。

### HOW

```java
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

// 1. 使用预定义常量（最常用）
MediaType json = MediaType.APPLICATION_JSON;          // "application/json"
MediaType xml = MediaType.APPLICATION_XML;            // "application/xml"
MediaType html = MediaType.TEXT_HTML;                 // "text/html"
MediaType plain = MediaType.TEXT_PLAIN;               // "text/plain"
MediaType form = MediaType.APPLICATION_FORM_URLENCODED; // "application/x-www-form-urlencoded"
MediaType multipart = MediaType.MULTIPART_FORM_DATA;   // "multipart/form-data"

// 2. 字符串解析
MediaType parsed = MediaType.parseMediaType("application/json;charset=UTF-8");
System.out.println(parsed.getType());       // "application"
System.out.println(parsed.getSubtype());     // "json"
System.out.println(parsed.getCharset());     // UTF-8（Charset 对象）

// 3. 手动创建
MediaType custom = new MediaType("application", "vnd.myapp-v1+json");

// 4. 兼容性检查（isCompatibleWith）
MediaType jsonWildcard = MediaType.APPLICATION_JSON;
MediaType all = MediaType.ALL;  // "*/*"
System.out.println(jsonWildcard.isCompatibleWith(all));           // true
System.out.println(all.isCompatibleWith(jsonWildcard));           // true
System.out.println(jsonWildcard.isCompatibleWith(MediaType.APPLICATION_XML)); // false
```

### 工作原理（源码分析）

#### 类定义与静态常量初始化

```java
// MediaType.java 第 52 行
public class MediaType extends MimeType implements Serializable {

    // 静态代码块中初始化所有常量（第 418-449 行）
    static {
        ALL = new MediaType("*", "*");
        APPLICATION_JSON = new MediaType("application", "json");
        APPLICATION_XML = new MediaType("application", "xml");
        TEXT_PLAIN = new MediaType("text", "plain");
        // ... 等 20+ 种常用类型
    }
}
```

每个预定义常量的值在类加载时就被确定，因此使用 `MediaType.APPLICATION_JSON` 不会产生额外的对象创建开销。

#### 质量因子（Quality Factor / q）

MediaType 支持 HTTP 内容协商中的质量因子参数：

```java
// 构造函数（第 491-493 行）
public MediaType(String type, String subtype, double qualityValue) {
    this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, 
        Double.toString(qualityValue)));
}
```

`q` 参数的范围是 0.0 到 1.0，表示客户端对某种媒体类型的偏好程度。

#### 兼容性判断（isCompatibleWith）

兼容性判断的核心逻辑在父类 `MimeType` 中：

```
isCompatibleWith(other) 算法：
  if (this.type.equals("*") || other.type.equals("*")) → true
  if (!this.type.equals(other.type))                   → false
  if (this.subtype.equals("*") || other.subtype.equals("*")) → true
  if (!this.subtype.equals(other.subtype))             → false
  → true (参数不参与兼容性判断)
```

这意味着 `text/*` 与 `text/html` 兼容，`*/*` 与任何类型兼容。

### 完整代码示例

```java
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MediaTypeDemo {
    public static void main(String[] args) {
        // === 常用预定义常量 ===
        System.out.println("=== 预定义常量 ===");
        System.out.println(MediaType.APPLICATION_JSON_VALUE);    // application/json
        System.out.println(MediaType.TEXT_HTML_VALUE);           // text/html
        System.out.println(MediaType.IMAGE_PNG_VALUE);           // image/png
        System.out.println(MediaType.MULTIPART_FORM_DATA_VALUE); // multipart/form-data
        System.out.println(MediaType.TEXT_EVENT_STREAM_VALUE);   // text/event-stream (SSE)

        // === 解析媒体类型字符串 ===
        MediaType parsed = MediaType.parseMediaType(
            "application/json;charset=UTF-8");
        System.out.println("\n=== 解析结果 ===");
        System.out.println("Type: " + parsed.getType());           // application
        System.out.println("Subtype: " + parsed.getSubtype());     // json
        System.out.println("Charset: " + parsed.getCharset());     // UTF-8
        System.out.println("toString: " + parsed.toString());      // application/json;charset=UTF-8

        // === 内容协商（Content Negotiation）===
        List<MediaType> clientAccept = List.of(
            new MediaType("application", "json", 1.0),   // 最优先
            new MediaType("text", "html", 0.8),           // 次优先
            new MediaType("text", "plain", 0.5)           // 最低优先
        );

        MediaType serverProduces = MediaType.APPLICATION_JSON;

        // 检查是否兼容
        for (MediaType accept : clientAccept) {
            boolean compatible = serverProduces.isCompatibleWith(accept);
            System.out.printf("%s is compatible with %s ? %s%n",
                serverProduces, accept, compatible);
        }

        // === 检查特定类型 ===
        MediaType json = MediaType.APPLICATION_JSON;
        System.out.println("\n=== 类型检查 ===");
        System.out.println("isConcrete (非通配符): " + json.isConcrete());  // true
        System.out.println("isConcrete (ALL): " + MediaType.ALL.isConcrete()); // false
    }
}
```

**输出示例**：

```
=== 预定义常量 ===
application/json
text/html
image/png
multipart/form-data
text/event-stream

=== 解析结果 ===
Type: application
Subtype: json
Charset: UTF-8
toString: application/json;charset=UTF-8

application/json is compatible with application/json ? true
application/json is compatible with text/html ? false
application/json is compatible with text/plain ? false

=== 类型检查 ===
isConcrete (非通配符): true
isConcrete (ALL): false
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/MediaType.java
```

---

## 5. HttpMethod — HTTP 请求方法

### WHAT

`HttpMethod` 是 Spring 对 HTTP 请求方法（GET、POST、PUT、DELETE 等）的**不可变对象表示**。它是一个 `final` 类（不是 enum），提供了预定义的静态常量（`HttpMethod.GET`、`HttpMethod.POST` 等），同时允许动态创建非标准 HTTP 方法（如 WebDAV 的 `PROPFIND`、`LOCK` 等）。

### WHY

将 HTTP 方法建模为封装的对象而非字符串或枚举，有以下几个重要原因：
1. **类型安全**：`HttpMethod.GET` 比 `"GET"` 或枚举更不容易被误用
2. **可比性**：实现 `Comparable<HttpMethod>` 允许排序
3. **可扩展性**：不像枚举那样封闭——可以通过 `valueOf()` 动态创建非标准方法
4. **线程安全**：所有预定义实例是 `final` 不可变的，可以在多线程环境中安全共享

### HOW

```java
import org.springframework.http.HttpMethod;
import java.util.Arrays;

public class HttpMethodDemo {
    public static void main(String[] args) {
        // 1. 使用预定义常量
        HttpMethod get = HttpMethod.GET;
        HttpMethod post = HttpMethod.POST;
        HttpMethod put = HttpMethod.PUT;
        HttpMethod delete = HttpMethod.DELETE;
        HttpMethod patch = HttpMethod.PATCH;
        HttpMethod head = HttpMethod.HEAD;
        HttpMethod options = HttpMethod.OPTIONS;
        HttpMethod trace = HttpMethod.TRACE;

        // 2. 字符串转换
        HttpMethod fromString = HttpMethod.valueOf("POST");
        System.out.println(fromString == post); // true（返回同一个常量对象）

        // 3. 支持非标准 HTTP 方法（如 WebDAV）
        HttpMethod propfind = HttpMethod.valueOf("PROPFIND");
        System.out.println(propfind.name());          // "PROPFIND"

        // 4. 名称比较
        System.out.println(post.matches("POST"));      // true
        System.out.println(post.matches("GET"));       // false

        // 5. 列出所有标准方法
        Arrays.stream(HttpMethod.values())
            .forEach(m -> System.out.print(m + " "));
        // GET HEAD POST PUT PATCH DELETE OPTIONS TRACE
    }
}
```

### 工作原理（源码分析）

#### 核心数据结构

```java
// HttpMethod.java 第 37 行
public final class HttpMethod implements Comparable<HttpMethod>, Serializable {

    // 预定义的标准方法常量（第 45-87 行）
    public static final HttpMethod GET     = new HttpMethod("GET");
    public static final HttpMethod HEAD    = new HttpMethod("HEAD");
    public static final HttpMethod POST    = new HttpMethod("POST");
    public static final HttpMethod PUT     = new HttpMethod("PUT");
    public static final HttpMethod PATCH   = new HttpMethod("PATCH");
    public static final HttpMethod DELETE  = new HttpMethod("DELETE");
    public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");
    public static final HttpMethod TRACE   = new HttpMethod("TRACE");

    // 内部缓存，用于加速 valueOf() 查询
    private static final Map<String, HttpMethod> mappings = Arrays.stream(values)
        .collect(Collectors.toUnmodifiableMap(HttpMethod::name, Function.identity()));

    // 唯一的字段：方法名字符串
    private final String name;
}
```

#### valueOf() — 标准方法缓存 + 动态创建

```java
// HttpMethod.java 第 122-131 行
public static HttpMethod valueOf(String method) {
    Assert.notNull(method, "Method must not be null");
    HttpMethod result = mappings.get(method);  // 先在缓存中查找
    if (result != null) {
        return result;   // 命中缓存，返回预定义常量实例（引用相等）
    }
    else {
        return new HttpMethod(method);  // 缓存未命中，创建新实例（如 WebDAV 方法）
    }
}
```

**关键设计**：预定义方法（GET/POST 等）在 `valueOf()` 中始终返回**同一个实例**（因为私有构造函数，外部无法创建这些方法的其他实例），因此可以使用 `==` 进行引用比较。

#### 构造函数是 private

```java
// HttpMethod.java 第 98-100 行
private HttpMethod(String name) {
    this.name = name;
}
```

这确保了外部代码只能通过 `valueOf()` 或直接使用预定义常量来获取 `HttpMethod` 实例，保证了实例的受控性。

#### equals() 和 compareTo()

```java
// 第 176-184 行 — equals
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    else if (o instanceof HttpMethod otherMethod) {
        return this.name.equals(otherMethod.name);
    }
    return false;
}

// 第 166-168 行 — compareTo
@Override
public int compareTo(HttpMethod other) {
    return this.name.compareTo(other.name);
}
```

`equals()` 基于方法名字符串比较，因此 `HttpMethod.valueOf("GET") == HttpMethod.GET` 为 `true`（引用相等），而 `new HttpMethod("PROPFIND").equals(HttpMethod.valueOf("PROPFIND"))` 也为 `true`（值相等）。

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpMethod.java
```

---

## 6. HttpStatus / HttpStatusCode — HTTP 状态码

### WHAT

- **`HttpStatusCode`**：Spring 6.0 引入的**密封接口（sealed interface）**，定义了 HTTP 状态码的通用行为（取值、类别判断）。它被 `HttpStatus` 和 `DefaultHttpStatusCode` 实现。
- **`HttpStatus`**：传统的枚举类型，自 Spring 3.0 起存在，定义了所有标准 HTTP 状态码常量（如 `200 OK`、`404 Not Found`、`500 Internal Server Error`）。

### WHY

1. **枚举 + 接口双轨制**：`HttpStatus` 是枚举，只能表示标准状态码；`HttpStatusCode` 是接口，可以表示任意 3 位数字状态码（如自定义的 4xx/5xx 或非标准码），两者通过 `HttpStatus implements HttpStatusCode` 统一。
2. **类别判断**：`is1xxInformational()`、`is2xxSuccessful()`、`is4xxClientError()`、`is5xxServerError()`、`isError()` 等便捷方法，无需手写 `statusCode >= 400 && statusCode < 500` 之类容易出错的逻辑。
3. **语义化常量**：`HttpStatus.NOT_FOUND` 比魔术数字 `404` 更可读，在 REST API 的 `ResponseEntity` 构建中使用时尤为清晰。

### HOW

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class HttpStatusDemo {
    public static void main(String[] args) {
        // 1. 使用预定义常量
        HttpStatus ok = HttpStatus.OK;                          // 200
        HttpStatus created = HttpStatus.CREATED;                // 201
        HttpStatus notFound = HttpStatus.NOT_FOUND;             // 404
        HttpStatus serverError = HttpStatus.INTERNAL_SERVER_ERROR; // 500

        // 2. 获取状态码值和原因短语
        System.out.println(ok.value());                    // 200
        System.out.println(ok.getReasonPhrase());          // "OK"
        System.out.println(notFound.value());              // 404
        System.out.println(notFound.getReasonPhrase());    // "Not Found"

        // 3. 类别判断（HttpStatusCode 接口方法）
        System.out.println(ok.is2xxSuccessful());          // true
        System.out.println(notFound.is4xxClientError());  // true
        System.out.println(serverError.is5xxServerError());// true
        System.out.println(notFound.isError());            // true（4xx 或 5xx）

        // 4. 通过状态码值解析
        HttpStatus resolved = HttpStatus.resolve(200);
        System.out.println(resolved == HttpStatus.OK);     // true

        // 5. Series 分类
        HttpStatus.Series series = notFound.series();
        System.out.println(series);                        // "CLIENT_ERROR"
        System.out.println(series.value());                // 4
    }
}
```

### 工作原理（源码分析）

#### HttpStatusCode 接口（Spring 6.0 新增）

```java
// HttpStatusCode.java 第 32 行
public sealed interface HttpStatusCode extends Serializable 
    permits DefaultHttpStatusCode, HttpStatus {

    int value();
    boolean is1xxInformational();
    boolean is2xxSuccessful();
    boolean is3xxRedirection();
    boolean is4xxClientError();
    boolean is5xxServerError();
    boolean isError();  // 4xx 或 5xx

    // 工厂方法：根据整数值创建 HttpStatusCode
    static HttpStatusCode valueOf(int code) {
        Assert.isTrue(code >= 100 && code <= 999, 
            () -> "Code '" + code + "' should be a three-digit positive integer");
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null) {
            return status;  // 标准码：返回枚举实例
        } else {
            return new DefaultHttpStatusCode(code);  // 非标准码：返回默认实现
        }
    }
}
```

#### HttpStatus 枚举结构

```java
// HttpStatus.java 第 34 行
public enum HttpStatus implements HttpStatusCode {

    // 1xx Informational
    CONTINUE(100, Series.INFORMATIONAL, "Continue"),
    SWITCHING_PROTOCOLS(101, Series.INFORMATIONAL, "Switching Protocols"),
    // ...

    // 2xx Success
    OK(200, Series.SUCCESSFUL, "OK"),
    CREATED(201, Series.SUCCESSFUL, "Created"),
    // ...

    // 4xx Client Error
    BAD_REQUEST(400, Series.CLIENT_ERROR, "Bad Request"),
    NOT_FOUND(404, Series.CLIENT_ERROR, "Not Found"),
    // ...

    // 5xx Server Error
    INTERNAL_SERVER_ERROR(500, Series.SERVER_ERROR, "Internal Server Error"),
    // ...
}
```

每个枚举常量包含三个属性：`value`（整数状态码）、`series`（分类枚举）、`reasonPhrase`（原因短语）。

#### Series 内部枚举

`HttpStatus.Series` 是一个内部枚举，将状态码按百位分为 5 类：

| Series 枚举值 | 数值范围 | 含义 |
|---|---|---|
| `INFORMATIONAL` | 1xx | 信息响应 |
| `SUCCESSFUL` | 2xx | 成功 |
| `REDIRECTION` | 3xx | 重定向 |
| `CLIENT_ERROR` | 4xx | 客户端错误 |
| `SERVER_ERROR` | 5xx | 服务端错误 |

#### resolve() 方法

```java
// HttpStatus.java 中的 resolve(int statusCode)
public static HttpStatus resolve(int statusCode) {
    // 在内部数组中查找匹配的状态码
    for (HttpStatus status : values()) {
        if (status.value == statusCode) {
            return status;
        }
    }
    return null;  // 非标准码返回 null
}
```

### 完整代码示例

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class HttpStatusCodeDemo {
    public static void main(String[] args) {
        // === HttpStatus 枚举常用值 ===
        System.out.println("=== 常用状态码 ===");
        printStatus(HttpStatus.OK);                    // 200 OK
        printStatus(HttpStatus.CREATED);               // 201 Created
        printStatus(HttpStatus.ACCEPTED);              // 202 Accepted
        printStatus(HttpStatus.NO_CONTENT);            // 204 No Content
        printStatus(HttpStatus.MOVED_PERMANENTLY);     // 301 Moved Permanently
        printStatus(HttpStatus.FOUND);                 // 302 Found
        printStatus(HttpStatus.NOT_MODIFIED);          // 304 Not Modified
        printStatus(HttpStatus.BAD_REQUEST);           // 400 Bad Request
        printStatus(HttpStatus.UNAUTHORIZED);          // 401 Unauthorized
        printStatus(HttpStatus.FORBIDDEN);             // 403 Forbidden
        printStatus(HttpStatus.NOT_FOUND);             // 404 Not Found
        printStatus(HttpStatus.TOO_MANY_REQUESTS);     // 429 Too Many Requests
        printStatus(HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        printStatus(HttpStatus.SERVICE_UNAVAILABLE);   // 503 Service Unavailable

        // === HttpStatusCode.valueOf() — 支持非标准码 ===
        System.out.println("\n=== 非标准状态码 ===");
        HttpStatusCode custom429 = HttpStatusCode.valueOf(429);
        System.out.println(custom429.value() + " → isError: " + custom429.isError());
        // 429 → isError: true

        // 自定义非标准码（如 AWS 的 460）
        HttpStatusCode aws460 = HttpStatusCode.valueOf(460);
        System.out.println(aws460.value() + " → isError: " + aws460.isError());
        // 460 → isError: true
    }

    private static void printStatus(HttpStatus status) {
        System.out.printf("%3d %-25s → is2xx=%s, is4xx=%s, isError=%s%n",
            status.value(),
            status.getReasonPhrase(),
            status.is2xxSuccessful(),
            status.is4xxClientError(),
            status.isError());
    }
}
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpStatusCode.java
spring-web/src/main/java/org/springframework/http/HttpStatus.java
```

---

## 7. HttpEntity / ResponseEntity — HTTP 实体封装

### WHAT

- **`HttpEntity<T>`**：封装 HTTP 请求或响应的**消息体（body）**和**消息头（headers）**。泛型 `<T>` 表示 body 的类型。
- **`ResponseEntity<T>`**：`HttpEntity<T>` 的子类，在 body + headers 的基础上**增加了 HTTP 状态码**（`HttpStatusCode`），专门用于表示 HTTP 响应。

这两个类在 Spring MVC 控制器方法中既可以作为**参数**（用于获取请求信息），也可以作为**返回值**（用于精确控制响应）。

### WHY

1. **统一的请求/响应模型**：通过泛型 `<T>` 将 body 类型参数化，避免了 `Object` 类型的强制转换
2. **响应控制粒度**：`ResponseEntity` 允许控制器方法精确设置返回的 HTTP 状态码、响应头和响应体——这对于 REST API 开发至关重要
3. **Builder 模式**：`ResponseEntity` 提供了一组流畅的静态工厂方法和 Builder（如 `ok()`、`created(location)`、`badRequest()` 等），简化响应构建

### HOW

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

// === HttpEntity 基本用法 ===

// 1. 只有 body
HttpEntity<String> entity1 = new HttpEntity<>("Hello World");

// 2. body + headers
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.TEXT_PLAIN);
HttpEntity<String> entity2 = new HttpEntity<>("Hello World", headers);

// 3. 获取内容
String body = entity2.getBody();        // "Hello World"
HttpHeaders h = entity2.getHeaders();   // HttpHeaders 对象
boolean hasBody = entity2.hasBody();    // true

// === ResponseEntity 基本用法 ===

// 1. 状态码 + body + headers
ResponseEntity<String> response1 = new ResponseEntity<>(
    "Created", headers, HttpStatus.CREATED);

// 2. Builder 模式（推荐）
ResponseEntity<String> response2 = ResponseEntity
    .status(HttpStatus.CREATED)
    .header("X-Custom", "value")
    .contentType(MediaType.APPLICATION_JSON)
    .body("{\"id\": 1}");

// 3. 便捷静态方法
ResponseEntity<String> ok = ResponseEntity.ok("Success");
ResponseEntity<Void> noContent = ResponseEntity.noContent().build();
ResponseEntity<Void> notFound = ResponseEntity.notFound().build();
ResponseEntity<Object> badRequest = ResponseEntity.badRequest().body("Invalid input");
```

### 工作原理（源码分析）

#### HttpEntity 源码结构

```java
// HttpEntity.java 第 58 行
public class HttpEntity<T> {

    public static final HttpEntity<?> EMPTY = new HttpEntity<>();  // 空实体

    private final HttpHeaders headers;   // 不可变的 headers
    @Nullable
    private final T body;               // body 可为 null

    // 核心构造函数（第 100-103 行）
    public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
        this.body = body;
        // 关键：headers 被包装为只读的 HttpHeaders
        this.headers = HttpHeaders.readOnlyHttpHeaders(
            headers != null ? headers : new HttpHeaders());
    }
}
```

**关键设计**：构造函数中使用 `HttpHeaders.readOnlyHttpHeaders()` 将传入的 headers 包装为只读视图，防止外部修改影响实体内部状态。这体现了"不可变性优先"的设计原则。

#### ResponseEntity 源码结构

```java
// ResponseEntity.java 第 79 行
public class ResponseEntity<T> extends HttpEntity<T> {

    private final Object status;  // HttpStatusCode 或 Integer

    // 核心构造函数（第 134-138 行）
    private ResponseEntity(@Nullable T body, 
                           @Nullable MultiValueMap<String, String> headers, 
                           Object status) {
        super(body, headers);
        Assert.notNull(status, "HttpStatusCode must not be null");
        this.status = status;
    }

    // getStatusCode() 根据内部类型适配
    public HttpStatusCode getStatusCode() {
        if (this.status instanceof HttpStatusCode statusCode) {
            return statusCode;
        } else {
            return HttpStatusCode.valueOf((Integer) this.status);  // int → HttpStatusCode
        }
    }
}
```

**关键设计**：`status` 字段类型为 `Object`，同时支持 `HttpStatusCode` 和 `int` 两种输入。这在 `getStatusCode()` 中通过 `instanceof` 进行适配——既保持了向后的 `int` 兼容性，又为 `HttpStatusCode` 新接口提供了支持。

#### Builder 模式

`ResponseEntity` 的 Builder 是一个典型的**分层 Builder 模式**：

```java
ResponseEntity.ok()                          // → BodyBuilder
    .header("X-Custom", "value")             // → BodyBuilder
    .contentType(MediaType.APPLICATION_JSON)  // → BodyBuilder
    .body("result");                          // → ResponseEntity<T>
```

Builder 有两个接口层级：
- `HeadersBuilder<B>`：提供 `header()`、`headers()`、`allow()`、`cacheControl()` 等方法
- `BodyBuilder extends HeadersBuilder<BodyBuilder>`：额外提供 `contentLength()`、`contentType()` 和 `body()`

### 完整代码示例

```java
import org.springframework.http.*;

import java.net.URI;

/**
 * 模拟 Spring MVC Controller 中使用 HttpEntity/ResponseEntity
 */
public class HttpEntityDemo {

    // === 作为方法参数（模拟 @Controller 的请求映射） ===
    public void handleRequest(HttpEntity<String> requestEntity) {
        // 读取请求头和请求体
        HttpHeaders requestHeaders = requestEntity.getHeaders();
        String requestBody = requestEntity.getBody();

        System.out.println("Received Content-Type: " + requestHeaders.getContentType());
        System.out.println("Received body: " + requestBody);
    }

    // === 作为方法返回值（模拟 @Controller 的响应） ===
    public ResponseEntity<User> getUser(Long id) {
        User user = findUserById(id);  // 模拟数据库查询

        if (user != null) {
            return ResponseEntity.ok(user);  // 200 OK
        } else {
            return ResponseEntity.notFound().build();  // 404 Not Found
        }
    }

    public ResponseEntity<User> createUser(User user) {
        // 201 Created + Location header
        URI location = URI.create("/users/" + user.getId());
        return ResponseEntity
            .created(location)
            .body(user);
    }

    public ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponse("BAD_REQUEST", message));
    }

    // === 演示 ===
    public static void main(String[] args) {
        HttpEntityDemo demo = new HttpEntityDemo();

        // 1. 模拟发送一个请求实体
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"name\": \"test\"}", headers);
        demo.handleRequest(request);

        // 2. 模拟获取用户（存在）
        ResponseEntity<User> response = demo.getUser(1L);
        System.out.println("\n=== 获取用户 ===");
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        System.out.println("Headers: " + response.getHeaders());

        // 3. 模拟创建用户
        User newUser = new User(2L, "Alice");
        ResponseEntity<User> created = demo.createUser(newUser);
        System.out.println("\n=== 创建用户 ===");
        System.out.println("Status: " + created.getStatusCode().value());
        System.out.println("Location: " + created.getHeaders().getLocation());
        System.out.println("Body: " + created.getBody());
    }

    private User findUserById(Long id) {
        // 模拟：id=1 存在，其他不存在
        return id == 1L ? new User(1L, "Bob") : null;
    }

    // === 内部模型类 ===
    record User(Long id, String name) {}
    record ErrorResponse(String code, String message) {}
}
```

**输出示例**：

```
Received Content-Type: application/json
Received body: {"name": "test"}

=== 获取用户 ===
Status: 200 OK
Body: User[id=1, name=Bob]
Headers: {}

=== 创建用户 ===
Status: 201
Location: /users/2
Body: User[id=2, name=Alice]
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/HttpEntity.java
spring-web/src/main/java/org/springframework/http/ResponseEntity.java
```

---

## 8. RequestEntity — HTTP 请求实体

### WHAT

`RequestEntity<T>` 是 `HttpEntity<T>` 的子类，除了 body 和 headers 之外，还封装了**HTTP 方法（HttpMethod）**和**目标 URL（URI）**。它在 `RestTemplate.exchange()` 方法中作为请求的完整表示，也可以在 `@Controller` 方法中声明为参数来获取完整的请求信息。

### WHY

在 `RestTemplate` 中使用时，`RequestEntity` 替代了原来需要分别传递 `url`、`method`、`headers`、`body` 的多参数调用方式，提供了一个单一的、自包含的请求对象：

```java
// 不使用 RequestEntity（参数分散）
template.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

// 使用 RequestEntity（参数集中）
RequestEntity<MyBody> request = RequestEntity.post(url).body(body);
template.exchange(request, String.class);
```

`RequestEntity` 还提供了流畅的 Builder API，如 `.accept()`、`.acceptCharset()`、`.ifModifiedSince()` 等方法链式设置请求头。

### HOW

```java
import org.springframework.http.RequestEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.net.URI;

// 1. 基本构造
RequestEntity<String> request1 = new RequestEntity<>(
    "body", HttpMethod.POST, URI.create("https://api.example.com/items"));

// 2. 静态工厂方法 + Builder 模式
RequestEntity<String> request2 = RequestEntity
    .post(URI.create("https://api.example.com/items"))
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .header("Authorization", "Bearer token123")
    .body("{\"name\": \"Spring\"}");

// 3. 读取请求信息
System.out.println(request2.getMethod());   // POST
System.out.println(request2.getUrl());      // https://api.example.com/items
System.out.println(request2.getBody());     // {"name": "Spring"}
```

### 工作原理（源码分析）

#### 核心属性

```java
// RequestEntity.java 第 67 行
public class RequestEntity<T> extends HttpEntity<T> {

    @Nullable
    private final HttpMethod method;   // HTTP 方法

    @Nullable
    private final URI url;             // 目标 URL

    @Nullable
    private final Type type;           // body 的泛型类型（用于类型解析）
}
```

与 `HttpEntity<T>` 相比，`RequestEntity<T>` 新增的三个字段完整描述了一个 HTTP 请求：

| 字段 | 类型 | 作用 |
|---|---|---|
| `method` | `HttpMethod` | GET, POST, PUT, DELETE 等 |
| `url` | `URI` | 目标 URL（已编码） |
| `type` | `Type` | 泛型类型信息（用于 `RestTemplate` 内部的反序列化） |

#### URI 模板支持

`RequestEntity` 支持两种创建方式：
1. **直接使用 URI 实例**：`new RequestEntity<>(body, method, URI.create("..."))` —— URL 已完全确定
2. **使用 URI 模板**：通过静态工厂方法的模板重载（如 `RequestEntity.post("/users/{id}", userId)`）—— URL 在 `RestTemplate` 调用时由 `UriTemplateHandler` 展开

当使用模板方式创建时，`this.url` 为 `null`，调用 `getUrl()` 会抛出 `UnsupportedOperationException`：

```java
// RequestEntity.java 第 171-180 行
public URI getUrl() {
    if (this.url == null) {
        throw new UnsupportedOperationException(
            "The RequestEntity was created with a URI template and variables, " +
            "and there is not enough information on how to correctly expand and " +
            "encode the URI template. This will be done by the RestTemplate instead " +
            "with help from the UriTemplateHandler it is configured with.");
    }
    return this.url;
}
```

#### 静态工厂方法

`RequestEntity` 提供了大量静态工厂方法，对应每种 HTTP 方法：

| 方法 | 说明 |
|---|---|
| `RequestEntity.get(URI)` | 创建 GET 请求 |
| `RequestEntity.post(URI)` | 创建 POST 请求 |
| `RequestEntity.put(URI)` | 创建 PUT 请求 |
| `RequestEntity.patch(URI)` | 创建 PATCH 请求 |
| `RequestEntity.delete(URI)` | 创建 DELETE 请求 |
| `RequestEntity.options(URI)` | 创建 OPTIONS 请求 |
| `RequestEntity.head(URI)` | 创建 HEAD 请求 |

每个方法都有多个重载：接受 `URI`、接受 `String` 模板 + `Object...` 变量、接受 `URI` + 泛型 `Type`。

### 完整代码示例

```java
import org.springframework.http.*;
import java.net.URI;
import java.time.Instant;

public class RequestEntityDemo {

    public static void main(String[] args) {
        // === 方式 1：基本构造器（全部手动设置） ===
        URI url = URI.create("https://api.example.com/v1/users");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        RequestEntity<String> request1 = new RequestEntity<>(
            "{\"name\":\"Alice\"}",
            headers,
            HttpMethod.POST,
            url
        );

        printRequest(request1);

        // === 方式 2：静态工厂 + Builder 链式调用 ===
        RequestEntity<String> request2 = RequestEntity
            .post(URI.create("https://api.example.com/v1/users"))
            .accept(MediaType.APPLICATION_JSON)
            .acceptCharset(java.nio.charset.StandardCharsets.UTF_8)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Request-Id", "req-001")
            .ifModifiedSince(Instant.now().toEpochMilli())
            .body("{\"name\":\"Bob\"}");

        printRequest(request2);

        // === 方式 3：URI 模板（由 RestTemplate 展开） ===
        RequestEntity<Void> request3 = RequestEntity
            .get("https://api.example.com/v1/users/{id}", 42L)
            .accept(MediaType.APPLICATION_JSON)
            .build();

        System.out.println("Method: " + request3.getMethod());
        // URL 模板未展开时 getUrl() 会抛异常，实际使用中由 RestTemplate 展开
    }

    private static void printRequest(RequestEntity<?> request) {
        System.out.println("=== Request ===");
        System.out.println("Method: " + request.getMethod());
        System.out.println("URL: " + request.getUrl());
        System.out.println("Headers:");
        request.getHeaders().forEach((name, values) -> {
            System.out.println("  " + name + ": " + String.join(", ", values));
        });
        System.out.println("Body: " + request.getBody());
        System.out.println();
    }
}
```

**输出示例**：

```
=== Request ===
Method: POST
URL: https://api.example.com/v1/users
Headers:
  Content-Type: application/json
  Accept: application/json
Body: {"name":"Alice"}

=== Request ===
Method: POST
URL: https://api.example.com/v1/users
Headers:
  Accept: application/json
  Accept-Charset: UTF-8
  Content-Type: application/json
  X-Request-Id: req-001
  If-Modified-Since: Wed, 02 Aug 2026 12:00:00 GMT
Body: {"name":"Bob"}
```

### 源码位置

```
spring-web/src/main/java/org/springframework/http/RequestEntity.java
```

---

## 9. UriComponentsBuilder — URI 构建器

### WHAT

`UriComponentsBuilder` 是 Spring Web 模块中的 URI 构建工具类，实现了 `UriBuilder` 和 `Cloneable` 接口。它提供了一种**类型安全、可读性强**的方式来构造 URI，支持设置 scheme、host、port、path、query 参数、fragment 等所有 URI 组件。

### WHY

手动拼接 URI 字符串存在以下问题：
1. **特殊字符需要手动编码**：如空格 → `%20`、中文 → UTF-8 百分号编码
2. **路径变量替换容易出错**：`"/users/" + id + "/orders/" + orderId` 可读性差
3. **查询参数拼接繁琐**：需要处理 `?`、`&`、`=` 的边界情况
4. **编码策略不统一**：不同组件（path vs query）的编码规则不同

`UriComponentsBuilder` 通过组件化构建和自动编码解决了这些问题。

### HOW

```java
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

// 1. 从头构建完整 URI
URI uri = UriComponentsBuilder.newInstance()
    .scheme("https")
    .host("api.example.com")
    .port(443)
    .path("/v1/users/{id}")          // 路径变量占位符
    .queryParam("fields", "name,email")
    .queryParam("expand", "orders")
    .buildAndExpand(42);              // 展开路径变量 {id} → 42

System.out.println(uri);
// https://api.example.com:443/v1/users/42?fields=name,email&expand=orders

// 2. 从已有 URI 修改
URI updated = UriComponentsBuilder
    .fromUriString("https://api.example.com/v1/users")
    .pathSegment("42")               // 追加路径段
    .queryParam("version", "2")
    .build()
    .toUri();

System.out.println(updated);
// https://api.example.com/v1/users/42?version=2

// 3. 仅在本地路径上构建
String path = UriComponentsBuilder
    .fromPath("/api/users")
    .queryParam("page", 1)
    .queryParam("size", 20)
    .toUriString();

System.out.println(path);
// /api/users?page=1&size=20
```

### 工作原理（源码分析）

#### 核心数据结构

```java
// UriComponentsBuilder.java 第 72 行
public class UriComponentsBuilder implements UriBuilder, Cloneable {

    @Nullable private String scheme;       // http, https, ftp...
    @Nullable private String ssp;          // scheme-specific-part（不常用）
    @Nullable private String userInfo;     // user:password@
    @Nullable private String host;         // 主机名或 IP
    @Nullable private String port;         // 端口号
    private CompositePathComponentBuilder pathBuilder;  // 路径组件（分段存储）
    private final MultiValueMap<String, String> queryParams;  // 查询参数（多值 Map）
    @Nullable private String fragment;     // #fragment
    private final Map<String, Object> uriVariables;  // URI 模板变量
    private boolean encodeTemplate;         // 是否编码模板
    private Charset charset = StandardCharsets.UTF_8;  // 编码字符集
}
```

#### URI 模板展开机制

`UriComponentsBuilder` 支持 URI 模板（RFC 6570 Level 3），使用 `{variableName}` 作为占位符：

```java
UriComponentsBuilder.fromUriString("/users/{userId}/orders/{orderId}")
    .buildAndExpand(Map.of("userId", 42, "orderId", 100));
// → /users/42/orders/100
```

`buildAndExpand()` 内部委托给 `UriComponents.expand()` 方法，将 Map 中的变量值替换到模板中，并自动进行 URI 编码。

#### 查询参数处理

查询参数通过 `queryParam(name, values...)` 方法添加。内部使用 `LinkedMultiValueMap<String, String>` 存储，支持同一参数名的多个值：

```java
builder.queryParam("filter", "active")
       .queryParam("filter", "verified")
       .toUriString();
// → ?filter=active&filter=verified
```

#### 构建过程中的 URI 编码

`build()` 方法返回 `UriComponents` 对象，后者在 `toUri()` / `toUriString()` 时进行 URI 编码，确保特殊字符被正确转义：

```java
UriComponentsBuilder.fromPath("/search")
    .queryParam("q", "hello world")      // 空格需要编码
    .queryParam("lang", "中文")           // 中文需要 UTF-8 编码
    .build()
    .toUriString();
// → /search?q=hello%20world&lang=%E4%B8%AD%E6%96%87
```

### 完整代码示例

```java
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Map;

public class UriComponentsBuilderDemo {

    public static void main(String[] args) {
        // === 示例 1：完整 URI 构建 ===
        URI apiUri = UriComponentsBuilder.newInstance()
            .scheme("https")
            .host("api.example.com")
            .path("/v2/users")
            .queryParam("role", "admin")
            .queryParam("status", "active")
            .queryParam("page", 1)
            .queryParam("size", 50)
            .build()
            .toUri();

        System.out.println("完整 URI: " + apiUri);
        // https://api.example.com/v2/users?role=admin&status=active&page=1&size=50

        // === 示例 2：URI 模板展开 ===
        URI templateUri = UriComponentsBuilder
            .fromUriString("https://api.example.com/users/{userId}/posts/{postId}")
            .queryParam("format", "detailed")
            .buildAndExpand(Map.of("userId", "1001", "postId", "500"))
            .toUri();

        System.out.println("模板 URI: " + templateUri);
        // https://api.example.com/users/1001/posts/500?format=detailed

        // === 示例 3：在已有 URI 上添加路径段 ===
        URI extended = UriComponentsBuilder
            .fromUri(URI.create("https://api.example.com/v1"))
            .pathSegment("users", "1001", "orders")  // 安全拼接路径
            .queryParam("sort", "desc")
            .fragment("summary")                      // 添加锚点
            .build()
            .toUri();

        System.out.println("扩展 URI: " + extended);
        // https://api.example.com/v1/users/1001/orders?sort=desc#summary

        // === 示例 4：构建 OAuth 回调 URL ===
        URI oauthUri = UriComponentsBuilder
            .fromUriString("https://auth.example.com/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", "my-client-app")
            .queryParam("redirect_uri", "https://myapp.example.com/callback")
            .queryParam("scope", "read write")
            .queryParam("state", "random-state-123")
            .build()
            .toUri();

        System.out.println("OAuth URI: " + oauthUri);
        // https://auth.example.com/authorize?response_type=code&client_id=my-client-app
        //   &redirect_uri=https://myapp.example.com/callback&scope=read%20write&state=random-state-123

        // === 示例 5：REST 分页查询字符串 ===
        UriComponentsBuilder pageBuilder = UriComponentsBuilder
            .fromPath("/api/products")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .queryParam("sort", "price,desc")
            .queryParam("filter[category]", "electronics");

        // 第一次渲染第 1 页
        URI page1 = pageBuilder.build().toUri();
        System.out.println("第 1 页: " + page1);

        // 替换 page 参数翻到第 2 页
        URI page2 = pageBuilder.replaceQueryParam("page", 1).build().toUri();
        System.out.println("第 2 页: " + page2);
    }
}

/*
=== 输出 ===
完整 URI: https://api.example.com/v2/users?role=admin&status=active&page=1&size=50
模板 URI: https://api.example.com/users/1001/posts/500?format=detailed
扩展 URI: https://api.example.com/v1/users/1001/orders?sort=desc#summary
OAuth URI: https://auth.example.com/authorize?response_type=code&client_id=my-client-app&redirect_uri=https://myapp.example.com/callback&scope=read%20write&state=random-state-123
第 1 页: /api/products?page=0&size=20&sort=price,desc&filter[category]=electronics
第 2 页: /api/products?page=1&size=20&sort=price,desc&filter[category]=electronics
*/
```

### 源码位置

```
spring-web/src/main/java/org/springframework/web/util/UriComponentsBuilder.java
```

---

## 10. RestTemplate / RestClient — HTTP 客户端

### WHAT

- **`RestTemplate`**：Spring 提供的一个**同步 HTTP 客户端模板类**，通过简单的模板方法 API 封装了底层的 HTTP 客户端库（JDK `HttpURLConnection`、Apache HttpComponents、OkHttp 等），支持常见 HTTP 方法（GET、POST、PUT、DELETE 等），并自动处理请求/响应的序列化和反序列化。
- **`RestClient`**（Spring 6.1+）：新一代的 Fluent API HTTP 客户端（在当前 Spring 6.0.0 源码中尚未引入，此处仅做知识性介绍）。

### WHY

在没有 `RestTemplate` 之前，通过 Java 发起 HTTP 请求需要编写大量样板代码：

```java
// 原生 JDK HTTP 调用（繁琐）
URL url = new URL("https://api.example.com/users/1");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setRequestProperty("Accept", "application/json");
int status = conn.getResponseCode();
InputStream is = conn.getInputStream();
// 手动读取 InputStream、解析 JSON...
```

`RestTemplate` 通过以下方式消除样板代码：
1. **自动序列化/反序列化**：内置 `HttpMessageConverter` 链，自动在 Java 对象和 JSON/XML 之间转换
2. **统一异常处理**：HTTP 错误状态码自动转换为 `HttpClientErrorException` / `HttpServerErrorException`
3. **URI 模板支持**：使用 `{variable}` 占位符简化 URL 拼接
4. **可插拔的底层 HTTP 库**：换用不同的 `ClientHttpRequestFactory` 实现即可切换连接池、超时策略等

### HOW

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import java.util.Map;

RestTemplate restTemplate = new RestTemplate();

// 1. GET 请求
String result = restTemplate.getForObject(
    "https://jsonplaceholder.typicode.com/posts/1", String.class);

// 2. GET 请求（带 URI 模板变量）
Post post = restTemplate.getForObject(
    "https://jsonplaceholder.typicode.com/posts/{id}", 
    Post.class, 1);

// 3. GET 请求（返回包含完整响应信息的 ResponseEntity）
ResponseEntity<Post> response = restTemplate.getForEntity(
    "https://jsonplaceholder.typicode.com/posts/{id}", 
    Post.class, 1);
HttpStatus status = response.getStatusCode();  // 200
Post body = response.getBody();

// 4. POST 请求
Post newPost = new Post("Spring REST", "Content...");
Post created = restTemplate.postForObject(
    "https://jsonplaceholder.typicode.com/posts", 
    newPost, Post.class);

// 5. PUT 请求
restTemplate.put("https://jsonplaceholder.typicode.com/posts/{id}", 
    updatedPost, 1);

// 6. DELETE 请求
restTemplate.delete("https://jsonplaceholder.typicode.com/posts/{id}", 1);

// 7. 通用 exchange 方法（最大灵活度）
ResponseEntity<Post> exchangeResp = restTemplate.exchange(
    "https://jsonplaceholder.typicode.com/posts/{id}",
    HttpMethod.GET,
    null,              // HttpEntity（可设置请求头）
    Post.class,
    1);
```

### 工作原理（源码分析）

#### 类继承关系

```java
// RestTemplate.java 第 110 行
public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {

// 继承关系：
// RestTemplate
//   └── InterceptingHttpAccessor
//         └── HttpAccessor
//               └── ClientHttpRequestFactory  // 底层 HTTP 连接工厂
//   └── RestOperations  // 定义了所有高层 API 接口
```

#### 核心执行流程

`RestTemplate` 中所有高层方法（`getForObject()`、`postForEntity()` 等）最终都汇聚到 `doExecute()` 方法：

```java
// RestTemplate.java 第 849-891 行（简化）
protected <T> T doExecute(URI url, @Nullable String uriTemplate, 
        @Nullable HttpMethod method, 
        @Nullable RequestCallback requestCallback,
        @Nullable ResponseExtractor<T> responseExtractor) {
    
    Assert.notNull(url, "url is required");
    Assert.notNull(method, "HttpMethod is required");
    
    // 1. 通过 ClientHttpRequestFactory 创建 HTTP 请求
    ClientHttpRequest request = createRequest(url, method);
    
    // 2. 执行请求回调（设置请求头、写入请求体）
    if (requestCallback != null) {
        requestCallback.doWithRequest(request);
    }
    
    // 3. 执行 HTTP 调用，获取响应
    ClientHttpResponse response = request.execute();
    
    // 4. 检查响应状态码（抛出异常或继续）
    handleResponse(url, method, response);
    
    // 5. 提取响应结果（反序列化等）
    return (responseExtractor != null ? responseExtractor.extractData(response) : null);
}
```

**关键组件**：

| 组件 | 作用 |
|---|---|
| `ClientHttpRequestFactory` | 创建底层 HTTP 连接（JDK/HttpComponents/OkHttp） |
| `RequestCallback` | 将请求体写入 `ClientHttpRequest`（通过 `HttpMessageConverter`） |
| `ResponseExtractor` | 从 `ClientHttpResponse` 中提取并反序列化响应体 |
| `HttpMessageConverter` | 在 Java 对象和 HTTP 消息体之间双向转换 |
| `ResponseErrorHandler` | 判断响应是否为错误并抛出相应异常 |
| `UriTemplateHandler` | 展开 URI 模板变量 |
| `ClientHttpRequestInterceptor` | 请求拦截器链（日志、认证等） |

#### 内置 MessageConverter

`RestTemplate` 默认构造函数会注册一系列 `HttpMessageConverter`（第 126-160 行附近）：

| Converter | 支持的类型 |
|---|---|
| `StringHttpMessageConverter` | `text/*`, `String` |
| `FormHttpMessageConverter` | `application/x-www-form-urlencoded`, `MultiValueMap` |
| `MappingJackson2HttpMessageConverter` | `application/json`, Java 对象 |
| `MappingJackson2XmlHttpMessageConverter` | `application/xml`, Java 对象 |
| `Jaxb2RootElementHttpMessageConverter` | `application/xml`, JAXB 注解对象 |
| `SourceHttpMessageConverter` | `text/xml`, `application/xml`, `javax.xml.transform.Source` |
| `ByteArrayHttpMessageConverter` | `*/*`, `byte[]` |
| `ResourceHttpMessageConverter` | `*/*`, `Resource` |

> **注意**：Spring 6.0 中 `RestTemplate` 已处于维护模式（maintenance mode），官方推荐在新项目中使用 `org.springframework.web.reactive.client.WebClient`（支持同步、异步、响应式流三种模式）。

### 完整代码示例

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * RestTemplate 完整示例
 * 展示 GET、POST、PUT、DELETE、exchange 和错误处理
 */
public class RestTemplateDemo {

    private final RestTemplate restTemplate;

    public RestTemplateDemo() {
        this.restTemplate = new RestTemplate();

        // 自定义错误处理器（可选）
        // restTemplate.setErrorHandler(new MyResponseErrorHandler());

        // 添加请求拦截器（可选）
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            System.out.println(">>> " + request.getMethod() + " " + request.getURI());
            ClientHttpResponse response = execution.execute(request, body);
            System.out.println("<<< " + response.getRawStatusCode());
            return response;
        }));
    }

    // GET — 返回反序列化的对象
    public Post getPost(Long id) {
        return restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/posts/{id}",
            Post.class, id);
    }

    // GET — 返回完整 ResponseEntity（含状态码和响应头）
    public ResponseEntity<Post> getPostEntity(Long id) {
        return restTemplate.getForEntity(
            "https://jsonplaceholder.typicode.com/posts/{id}",
            Post.class, id);
    }

    // POST — 创建资源
    public Post createPost(Post post) {
        return restTemplate.postForObject(
            "https://jsonplaceholder.typicode.com/posts",
            post, Post.class);
    }

    // POST — 使用 HttpEntity 设置自定义请求头
    public ResponseEntity<Post> createPostWithHeaders(Post post) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Custom-Header", "my-value");

        HttpEntity<Post> request = new HttpEntity<>(post, headers);

        return restTemplate.exchange(
            "https://jsonplaceholder.typicode.com/posts",
            HttpMethod.POST,
            request,
            Post.class);
    }

    // PUT — 更新资源
    public void updatePost(Long id, Post post) {
        restTemplate.put(
            "https://jsonplaceholder.typicode.com/posts/{id}",
            post, id);
    }

    // DELETE — 删除资源
    public void deletePost(Long id) {
        restTemplate.delete(
            "https://jsonplaceholder.typicode.com/posts/{id}", id);
    }

    // exchange — 最灵活的调用方式
    public ResponseEntity<List<Post>> getAllPosts() {
        // 注意：ParameterizedTypeReference 用于保留泛型类型信息
        return restTemplate.exchange(
            "https://jsonplaceholder.typicode.com/posts",
            HttpMethod.GET,
            null,
            new org.springframework.core.ParameterizedTypeReference<List<Post>>() {});
    }

    // === 演示 ===
    public static void main(String[] args) {
        RestTemplateDemo demo = new RestTemplateDemo();

        try {
            // 1. GET 单个对象
            System.out.println("=== GET /posts/1 ===");
            Post post = demo.getPost(1L);
            System.out.println("Title: " + post.title());
            System.out.println("Body:  " + post.body());

            // 2. GET 带状态码
            System.out.println("\n=== GET /posts/99 (可能不存在) ===");
            try {
                ResponseEntity<Post> entity = demo.getPostEntity(99L);
                System.out.println("Status: " + entity.getStatusCode());
            } catch (Exception e) {
                System.out.println("请求失败: " + e.getMessage());
            }

            // 3. POST 创建
            System.out.println("\n=== POST /posts ===");
            Post newPost = new Post("Learn Spring MVC", "A comprehensive guide");
            Post created = demo.createPost(newPost);
            System.out.println("Created ID: " + created.id());

            // 4. PUT 更新
            System.out.println("\n=== PUT /posts/1 ===");
            Post updated = new Post("Updated Title", "Updated Body");
            demo.updatePost(1L, updated);
            System.out.println("Update completed");

            // 5. DELETE 删除
            System.out.println("\n=== DELETE /posts/1 ===");
            demo.deletePost(1L);
            System.out.println("Delete completed");

        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("网络连接失败（请确保能访问 jsonplaceholder.typicode.com）: " + e.getMessage());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("客户端错误: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.err.println("服务端错误: " + e.getStatusCode());
        }
    }

    // 记录类型：用于 JSON 反序列化
    record Post(Long userId, Long id, String title, String body) {
        // 用于创建新 Post（不包含 id）
        public Post(String title, String body) {
            this(null, null, title, body);  // 或者 userId 设为固定值
        }
    }
}
```

### 源码位置

```
spring-web/src/main/java/org/springframework/web/client/RestTemplate.java
```

---

## 总结：HTTP 抽象层架构全景

下面展示了整个 HTTP 基础抽象层的类图概览：

```
┌─────────────────────────────────────────────────────────────┐
│                    HttpMessage（顶层接口）                     │
│                    getHeaders() : HttpHeaders                │
├──────────────────┬──────────────────────────────────────────┤
│  HttpInputMessage │           HttpOutputMessage              │
│  getBody()→InputStream  │     getBody()→OutputStream        │
├─────────────────────────┼───────────────────────────────────┤
│ （服务端请求/客户端响应）  │  （服务端响应/客户端请求）          │
└─────────────────────────┴───────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│            HttpHeaders（MultiValueMap<String,String>）        │
│  • 大小写不敏感的头部名      • 类型安全访问器                   │
│  • 50+ 头部名称常量          • 只读包装                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────┐ ┌──────────────┐ ┌────────────────────────────┐
│  MediaType   │ │  HttpMethod   │ │ HttpStatusCode (sealed)     │
│  type/subtype │ │  GET/POST/..  │ │   ├── HttpStatus (enum)     │
│  + 参数      │ │  final class  │ │   └── DefaultHttpStatusCode │
└─────────────┘ └──────────────┘ └────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      HttpEntity<T>                           │
│                   body: T + headers: HttpHeaders              │
├──────────────────────┬──────────────────────────────────────┤
│   ResponseEntity<T>   │        RequestEntity<T>               │
│   + status: HttpStatus │        + method: HttpMethod           │
│   响应端专用            │        + url: URI                     │
│                        │        请求端专用                      │
└────────────────────────┴──────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 UriComponentsBuilder                         │
│  scheme → host → port → path → query → fragment → build()   │
│  支持 URI 模板展开  支持自动编码                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      RestTemplate                           │
│  execute() → doExecute()                                     │
│    ├── ClientHttpRequestFactory（底层连接）                    │
│    ├── HttpMessageConverter 链（序列化/反序列化）               │
│    ├── ResponseErrorHandler（错误处理）                        │
│    └── UriTemplateHandler（URI 模板展开）                      │
└─────────────────────────────────────────────────────────────┘
```

这些抽象共同构成了 Spring MVC 处理 HTTP 请求的基石。理解它们的关系和使用方式，是深入学习 DispatcherServlet、HandlerMapping、HandlerAdapter 和 MessageConverter 等后续核心组件的必要前提。

---

> **下一阶段**：[Spring MVC 第二阶段：核心组件与请求处理流程](02-dispatcher-servlet.md)（待完成）
