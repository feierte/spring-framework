# 第九阶段：数据绑定 & 类型转换 & 校验

> 所属模块：`spring-web/src/main/java/org/springframework/web/bind/` + `spring-beans` + `spring-core`
>
> 核心问题：`?name=张三&age=18` 这串字符串是怎么变成一个 `User` 对象的？`@Valid` 是怎么触发校验的？

---

## 9.1 DataBinder — 数据绑定器

### 📌 WHAT

`org.springframework.validation.DataBinder` 是数据绑定的核心类。它把属性值（通常是字符串）**设置到目标对象**的属性上。

### 🔧 HOW

```java
// DataBinder 底层做的事：
User user = new User();
DataBinder binder = new DataBinder(user);
// 相当于：
// user.setName("张三");
// user.setAge(18);
```

### ⚙️ 工作原理（源码）

```java
// DataBinder.bind():
public void bind(PropertyValues pvs) {
    MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ? 
        (MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
    doBind(mpvs);
}

protected void doBind(MutablePropertyValues mpvs) {
    checkAllowedFields(mpvs);   // 检查是否允许绑定的字段
    checkRequiredFields(mpvs);  // 检查必填字段
    applyPropertyValues(mpvs);  // 实际设置属性值
}
```

`applyPropertyValues()` 最终调用 JavaBeans 的 `setXxx()` 方法。

### 📂 源码位置

```
spring-beans/src/main/java/org/springframework/validation/DataBinder.java
spring-web/src/main/java/org/springframework/web/bind/WebDataBinder.java
```

---

## 9.2 ServletRequestDataBinder — Servlet 专用绑定器

### 📌 WHAT

`ServletRequestDataBinder` 专门从 `HttpServletRequest` 的参数中提取值来绑定。

### ⚙️ 工作原理

```java
// ServletRequestDataBinder.bind():
public void bind(ServletRequest request) {
    // ① 从 request 提取所有参数到 MutablePropertyValues
    MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
    
    // ② 处理文件上传
    MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
    if (multipartRequest != null) {
        bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
    }
    
    // ③ 调用父类的 bind() 执行绑定
    doBind(mpvs);
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/bind/ServletRequestDataBinder.java
```

---

## 9.3 Converter — 类型转换器

### 📌 WHAT

`org.springframework.core.convert.converter.Converter<S, T>` 负责把一种类型**转换**成另一种类型。

```java
public interface Converter<S, T> {
    T convert(S source);
}
```

### 🔧 HOW

```java
// 内置 Converter 示例（String → Integer）
// StringToNumberConverterFactory 产生 Converter<String, Integer>
// 当 DataBinder 发现目标属性是 Integer 但源值是字符串时，自动调用

// 自定义 Converter
@Component
public class StringToLocalDateConverter implements Converter<String, LocalDate> {
    @Override
    public LocalDate convert(String source) {
        return LocalDate.parse(source, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
```

### ⚙️ 工作原理

Spring 内置了大量 Converter：

| 转换类型 | 实现类 |
|----------|--------|
| `String → Integer/Long/Double...` | `StringToNumberConverterFactory` |
| `String → Enum` | `StringToEnumConverterFactory` |
| `String → Boolean` | `StringToBooleanConverter` |
| `String → File` | `StringToFileConverter` |
| `Object → String` | `ObjectToStringConverter` |
| `Collection → Collection` | `CollectionToCollectionConverter` |

### 📂 源码位置

```
spring-core/src/main/java/org/springframework/core/convert/converter/Converter.java
spring-core/src/main/java/org/springframework/core/convert/support/
```

---

## 9.4 ConversionService — 类型转换服务

### 📌 WHAT

`ConversionService` 统一管理所有 `Converter`，提供类型转换的统一入口。

### 🔧 HOW

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalDateConverter());
    }
}
```

### 📂 源码位置

```
spring-core/src/main/java/org/springframework/core/convert/ConversionService.java
spring-core/src/main/java/org/springframework/core/convert/support/GenericConversionService.java
```

---

## 9.5 Formatter — 格式化器

### 📌 WHAT

`org.springframework.format.Formatter<T>` 在 Converter 基础之上增加了**本地化**支持和 `String ↔ Object` 的双向转换。

```java
public interface Formatter<T> extends Printer<T>, Parser<T> {
}

public interface Printer<T> {
    String print(T object, Locale locale);  // Object → String
}

public interface Parser<T> {
    T parse(String text, Locale locale) throws ParseException;  // String → Object
}
```

### 🔧 HOW

```java
// 自定义日期格式化器
public class DateFormatter implements Formatter<Date> {
    @Override
    public Date parse(String text, Locale locale) {
        return new SimpleDateFormat("yyyy-MM-dd").parse(text);
    }
    
    @Override
    public String print(Date date, Locale locale) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
```

### 📂 源码位置

```
spring-context/src/main/java/org/springframework/format/Formatter.java
spring-context/src/main/java/org/springframework/format/support/FormattingConversionService.java
```

---

## 9.6 @DateTimeFormat / @NumberFormat — 注解驱动格式化

### 🔧 HOW

```java
public class UserForm {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createTime;
    
    @NumberFormat(pattern = "#,##0.00")
    private BigDecimal salary;
}
```

### 📂 源码位置

```
spring-context/src/main/java/org/springframework/format/annotation/DateTimeFormat.java
spring-context/src/main/java/org/springframework/format/annotation/NumberFormat.java
```

---

## 9.7 @InitBinder — 自定义绑定器配置

### 📌 WHAT

在 Controller 中自定义 `DataBinder` 的行为——注册属性编辑器、设置允许/禁止绑定的字段。

### 🔧 HOW

```java
@Controller
public class UserController {
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 禁止直接绑定 id（防止恶意修改）
        binder.setDisallowedFields("id");
        
        // 注册自定义 PropertyEditor
        binder.registerCustomEditor(Date.class, 
            new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }
}
```

### 📂 源码位置

```
spring-web/src/main/java/org/springframework/web/bind/annotation/InitBinder.java
```

---

## 9.8 Validator 接口（Spring 原生校验）

### 📌 WHAT

Spring 自己的校验接口，执行校验逻辑，把错误放入 `Errors` 对象。

```java
public interface Validator {
    boolean supports(Class<?> clazz);  // 能校验这种类型吗？
    void validate(Object target, Errors errors);  // 执行校验
}
```

### 🔧 HOW

```java
public class UserValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }
    
    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;
        if (user.getName() == null || user.getName().isBlank()) {
            errors.rejectValue("name", "required", "姓名不能为空");
        }
        if (user.getAge() < 0 || user.getAge() > 150) {
            errors.rejectValue("age", "range", "年龄范围不合法");
        }
    }
}
```

### 📂 源码位置

```
spring-context/src/main/java/org/springframework/validation/Validator.java
```

---

## 9.9 Bean Validation（JSR-380 / Hibernate Validator）

### 📌 WHAT

使用注解声明校验规则，由 Hibernate Validator（最常用的实现）在运行时执行。

### 🔧 HOW

```java
public class User {
    @NotNull(message = "ID不能为空")
    private Long id;
    
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度2-20字符")
    private String name;
    
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过150")
    private int age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

@PostMapping("/user")
public String createUser(@Valid @RequestBody User user, BindingResult result) {
    if (result.hasErrors()) {
        // 校验失败
        return "error";
    }
    userService.save(user);
    return "success";
}
```

### ⚙️ 工作原理

触发链路：

```
@Valid 注解
    ↓
MethodValidationPostProcessor（AOP 代理）
    ↓
LocalValidatorFactoryBean（Spring 的 Validator 包装）
    ↓
Hibernate Validator（实际执行校验）
    ↓
把错误放入 BindingResult
```

### 📂 源码位置

校验框架本身不在 Spring 源码中，Spring 的适配层在：
```
spring-context/src/main/java/org/springframework/validation/beanvalidation/
```

---

## 9.10 常用 Bean Validation 注解一览

| 注解 | 校验规则 |
|------|---------|
| `@NotNull` | 不能为 null |
| `@NotEmpty` | 不能为 null 且不能为空（集合/字符串） |
| `@NotBlank` | 不能为 null 且 trim 后不能为空 |
| `@Size(min, max)` | 长度范围 |
| `@Min` / `@Max` | 数值范围 |
| `@Pattern(regexp)` | 正则匹配 |
| `@Email` | 邮箱格式 |
| `@Positive` / `@Negative` | 正数/负数 |
| `@Past` / `@Future` | 过去/未来日期 |
| `@AssertTrue` / `@AssertFalse` | 布尔校验 |

---

## 🔗 数据绑定完整流程

```
HTTP 请求 → @ModelAttribute(User user) / @RequestBody(User user)

① 创建目标对象
   user = new User()

② 创建 WebDataBinder
   binder = new ServletRequestDataBinder(user, "user")

③ 应用 @InitBinder 配置
   binder.setDisallowedFields(...)

④ 提取请求参数
   ServletRequestParameterPropertyValues(request)
   → {name: "张三", age: "18", email: "zhangsan@example.com"}

⑤ 类型转换
   "张三" → "张三" (String → String, 无需转换)
   "18"   → 18     (String → int, StringToNumberConverter)
   
⑥ 属性绑定（反射调用 setter）
   user.setName("张三")
   user.setAge(18)

⑦ 校验（如果有 @Valid 注解）
   LocalValidatorFactoryBean.validate(user)
   → 错误信息放入 BindingResult

⑧ 返回绑定好的对象
```

---

> **下一阶段**：[第十阶段：异常处理](10-exception-handling.md)
