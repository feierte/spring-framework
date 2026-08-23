# 第七阶段：HttpMessageConverter — HTTP 消息转换

> 所属模块：`spring-web/src/main/java/org/springframework/http/converter/`
>
> 核心问题：JSON ↔ 对象、XML ↔ 对象，谁在干这个翻译活？怎么选翻译官？

---

## 7.1 HttpMessageConverter 接口

### 📌 WHAT

`org.springframework.http.converter.HttpMessageConverter<T>` 是 HTTP 消息转换器的统一接口：

```java
// HttpMessageConverter.java (spring-web)
public interface HttpMessageConverter<T> {

    // 能读取这种 Content-Type + 这种 Java 类型吗？
    boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

    // 能写出这种 Java 类型为这种 Content-Type 吗？
    boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

    // 这个转换器支持哪些 MediaType？
    List<MediaType> getSupportedMediaTypes();

    // 读：HTTP 请求体 → Java 对象
    T read(Class<? extends T> clazz, HttpInputMessage inputMessage) 
        throws IOException, HttpMessageNotReadableException;

    // 写：Java 对象 → HTTP 响应体
    void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException;
}
```

**大白话**：HttpMessageConverter 就是"翻译官"——一头对着 HTTP 报文（JSON/XML/表单），另一头对着 Java 对象，来回互译。

### 🎯 WHY

没有它，你只能这样处理 JSON：

```java
// 没有消息转换器的话
@PostMapping("/user")
public String createUser(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    String line;
    BufferedReader reader = request.getReader();
    while ((line = reader.readLine()) != null) { sb.append(line); }
    String json = sb.toString();
    // 手动解析 JSON...
    User user = new ObjectMapper().readValue(json, User.class);
    // ...
}
```

有了它，`@RequestBody User user` 一行搞定。

### ⚙️ 工作原理

`AbstractMessageConverterMethodArgumentResolver.writeWithMessageConverters()` 是内容协商的核心：

```
接收返回值
    │
    ├── ① 确定目标 Content-Type：
    │     客户端 Accept 头想要什么？
    │     方法上有 @RequestMapping(produces="application/json") 吗？
    │     能找到能处理的 Converter 吗？
    │
    ├── ② 遍历所有 HttpMessageConverter：
    │     ┌──────────────────────────────────────────────────┐
    │     │ 1. ByteArrayHttpMessageConverter — byte[]        │
    │     │ 2. StringHttpMessageConverter      — String      │
    │     │ 3. ResourceHttpMessageConverter    — Resource     │
    │     │ 4. SourceHttpMessageConverter      — Source       │
    │     │ 5. AllEncompassingFormHttpMessageConverter — 表单│
    │     │ 6. MappingJackson2HttpMessageConverter — JSON    │
    │     │ 7. Jaxb2RootElementHttpMessageConverter — XML    │
    │     └──────────────────────────────────────────────────┘
    │     找到 canWrite(returnValueType, targetMediaType) 为 true 的
    │
    └── ③ 调用 converter.write() 写入响应
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/HttpMessageConverter.java
```

---

## 7.2 MappingJackson2HttpMessageConverter — JSON 转换器

### 📌 WHAT

使用 **Jackson** 库进行 JSON ↔ Java 对象的序列化和反序列化。这是 **最常用的消息转换器**。

### 🔧 HOW

```java
// Controller 中自动使用（Spring Boot 默认注册了它）
@PostMapping("/user")
public User createUser(@RequestBody User user) {   // JSON → User（read）
    return userService.save(user);                 // User → JSON（write）
}
```

**等价的手动操作**：

```java
// Spring 底层相当于干了这些事：
ObjectMapper mapper = new ObjectMapper();

// 读（@RequestBody）
User user = mapper.readValue(request.getInputStream(), User.class);

// 写（@ResponseBody）
mapper.writeValue(response.getOutputStream(), user);
```

### ⚙️ 工作原理

```java
// MappingJackson2HttpMessageConverter.canRead():
@Override
public boolean canRead(Class<?> clazz, MediaType mediaType) {
    return canRead(mediaType) && 
           this.objectMapper.canDeserialize(this.objectMapper.constructType(clazz));
}

// MappingJackson2HttpMessageConverter.read():
@Override
public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) {
    JavaType javaType = getJavaType(type, contextClass);
    return this.objectMapper.readValue(inputMessage.getBody(), javaType);
}

// MappingJackson2HttpMessageConverter.write():
@Override
protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) {
    // ...
    this.objectMapper.writeValue(generator, object);
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.java
```

---

## 7.3 StringHttpMessageConverter — 文本转换器

### 📌 WHAT

处理 `text/plain`、`text/html` 等文本内容和 `String` 类型之间的转换。

### 🔧 HOW

```java
// 读：请求体 → String
@PostMapping("/raw")
public String echo(@RequestBody String body) {
    return "收到：" + body;  // 返回 String
}
```

### ⚙️ 工作原理

```java
// StringHttpMessageConverter.readInternal():
@Override
protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) {
    Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
    return StreamUtils.copyToString(inputMessage.getBody(), charset);
}
```

> ⚠️ 注意：`StringHttpMessageConverter` 的默认 Content-Type 是 `text/plain`，`canWrite` 时如果没匹配的 converter 可能回退到它——这就是为什么有时返回的 JSON 变成了 `text/plain`。

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/StringHttpMessageConverter.java
```

---

## 7.4 FormHttpMessageConverter — 表单转换器

### 📌 WHAT

处理 `application/x-www-form-urlencoded` 表单数据的转换（MultiValueMap ↔ 表单字符串）。

### ⚙️ 工作原理

```java
// FormHttpMessageConverter.read():
// "name=张三&age=18" → MultiValueMap<String, String>

// FormHttpMessageConverter.write():
// MultiValueMap → 序列化为 URL 编码字符串
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/FormHttpMessageConverter.java
```

---

## 7.5 ByteArrayHttpMessageConverter — 字节数组转换器

### 📌 WHAT

处理 `byte[]` 类型：读时原样读取传输内容，写时原样写入。

### 🔧 HOW

```java
@PostMapping("/upload")
public String upload(@RequestBody byte[] data) {
    // 读取原始字节
    return "收到 " + data.length + " 字节";
}

@GetMapping("/image")
public ResponseEntity<byte[]> image() {
    byte[] imageData = loadImage();
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(imageData);
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/ByteArrayHttpMessageConverter.java
```

---

## 7.6 ResourceHttpMessageConverter — 资源转换器

### 📌 WHAT

处理 `org.springframework.core.io.Resource` 类型，用于文件下载。

### 🔧 HOW

```java
@GetMapping("/download/{filename}")
public ResponseEntity<Resource> download(@PathVariable String filename) {
    Resource resource = new FileSystemResource("/uploads/" + filename);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=" + filename)
        .body(resource);
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/ResourceHttpMessageConverter.java
```

---

## 7.7 Jaxb2RootElementHttpMessageConverter — XML 转换器

### 📌 WHAT

使用 JAXB 实现 XML ↔ Java 对象的转换。

### 🔧 HOW

```java
@XmlRootElement
public class User {
    private Long id;
    private String name;
    // getters/setters...
}

@GetMapping(value = "/user/{id}", produces = MediaType.APPLICATION_XML_VALUE)
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
    // → <user><id>1</id><name>张三</name></user>
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/xml/Jaxb2RootElementHttpMessageConverter.java
```

---

## 7.8 AllEncompassingFormHttpMessageConverter — 综合表单

### 📌 WHAT

扩展 `FormHttpMessageConverter`，增加对 **multipart/form-data**（文件上传）的支持。

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/http/converter/support/AllEncompassingFormHttpMessageConverter.java
```

---

## 7.9 消息转换器的协商流程（重点）

### ⚙️ 选择算法

`AbstractMessageConverterMethodProcessor.writeWithMessageConverters()` — **最复杂的方法之一**：

```java
// 简化版协商流程
protected void writeWithMessageConverters(Object value, MethodParameter returnType,
        ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage) {

    // ① 确定目标 Content-Type：
    //    优先级：方法 @RequestMapping(produces) > 请求 Accept 头 > 默认 */*（都能接受）
    List<MediaType> acceptableTypes = getAcceptableMediaTypes(request);
    List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);
    
    // ② 找出可协商的 Content-Type（取交集）
    List<MediaType> mediaTypesToUse = new ArrayList<>();
    for (MediaType requestedType : acceptableTypes) {
        for (MediaType producibleType : producibleTypes) {
            if (requestedType.isCompatibleWith(producibleType)) {
                mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
            }
        }
    }
    
    // ③ 对每个候选 MediaType，找能处理的 Converter
    for (MediaType mediaType : mediaTypesToUse) {
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            if (converter.canWrite(valueType, mediaType)) {
                // ④ 找到了！调用 write() 写入响应
                ((HttpMessageConverter<Object>) converter).write(body, mediaType, outputMessage);
                return;
            }
        }
    }
    
    throw new HttpMediaTypeNotAcceptableException("Could not find acceptable representation");
}
```

**内容协商示例**：

```
请求 Accept: application/xml

返回值类型: User

流程：
  ① acceptableTypes = [application/xml]
  ② producibleTypes  = [application/json, application/xml]  (取决于注册的 Converter)
  ③ mediaTypesToUse  = [application/xml]  (取交集)
  ④ 遍历 Converter：
       MappingJackson2HttpMessageConverter.canWrite(User, application/xml) → false
       Jaxb2RootElementHttpMessageConverter.canWrite(User, application/xml) → true ✓
  ⑤ 返回 XML: <user><id>1</id><name>张三</name></user>
```

### 📂 源码位置

```
spring-webmvc/.../servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java
```

---

## 🔗 HttpMessageConverter 在 MVC 流程中的位置

```
@RequestBody → read
┌─────────────────────────────────────┐
│  RequestResponseBodyMethodProcessor  │
│  resolveArgument():                  │
│    readWithMessageConverters()       │──→ HttpMessageConverter.read()
│    → 遍历 Converter，找 canRead()     │
└─────────────────────────────────────┘

@ResponseBody → write
┌─────────────────────────────────────┐
│  RequestResponseBodyMethodProcessor  │
│  handleReturnValue():                │
│    writeWithMessageConverters()      │──→ HttpMessageConverter.write()
│    → 内容协商 + 找 canWrite()         │
└─────────────────────────────────────┘
```

---

> **下一阶段**：[第八阶段：ViewResolver 视图解析](08-view-resolver.md)
