[TOC]

# jproject-http组件使用教程

jproject-httpclient组件是游戏业务基于目前业务中混乱使用现状，而进行梳理重构优化后的组件；

由于jproject-http为了和通用http组件更好的集成，同时也是为了更快速的迭代，jproject-httpclient组件是基于devmodule-httpclient的再封装；
扩展了通用http组件：
* 无法自定义拦截器
* 无法自定义序列化converter等缺陷
* 无法支持POST（application/json）格式的请求
* 不支持hystrix的熔断和<b>降级</b>的组件；
* 内置的日志打印拦截器不适应游戏业务的打印规范和现状；

## 快速使用

### jar包依赖

```xml

<dependency>
    <groupId>com.jproject.zs</groupId>
    <artifactId>jproject-httpclient</artifactId>
    <version>${jproject-httpclient.version}</version>
</dependency>
```

注意依赖的devmodule的版本为1.0.32, 注意版本冲突；

同时需要注意当前版本下，不支持使用@RESTClient自动注入，未来规划中会进行支持；

### bean构造

```java


class Demo {
    @Bean
    public MainSiteApi mainSiteApi() {

        log.info("Start to init the bean mainSiteApi by config={},{},{},{}", hosts,
                standByHosts, appKey, appSecret);

        return HttpClient.builder()
                .converterFactory(GsonConverterFactory.create(new GsonBuilder()
                        .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()))
                .hystrixProperties(new HystrixProperties()
                        .setStandByHosts(Try.of(() -> Splitter.on(",")
                                .splitToList(standByHosts))
                                .getOrNull()))
                .hystrixBreakPredicate(rsp -> {
                    return !rsp.isSuccessful() && rsp.code() != 412;
                })
                .options(new HttpClient.Options(10_000, 10_000, 10_000))
                .addRequestInterceptor(new ApiSignUrlInterceptor(
                        new AppSignatureProperties(appKey, appSecret)))
                .target(MainSiteApi.class, hosts);


    }

    @LogLevel(Level.BODY)
    public interface MainSiteApi extends GeneralHttpApi {


        @GET("/x/internal/v3/account/profile")
        Call<Response<AccountProfile>> accountProfile(@Query("mid") String mid);


        @GET("/x/internal/account/info")
        Call<Response<OtherAccountData>> accountInfo(@Query("mid") String mid);

        // .....
    }
}
```

其他header、body等retrofit相关注解可参考官方文档：https://square.github.io/retrofit/

以上是一个典型的主站服务的http客户端的构造方法， 我们注意到是基于devmodule的实现，

* 自定义了下划线方式的序列化方式，
* 支持hystrix的熔断降级策略，
* 并且自定义的请求签名拦截器；

## 内置组件

所有devmodule内置组件均支持，包括
* 日志， （开启）
* trace （ 开启） 
* metric （开启） 
* 内置签名拦截器（ 按需，包括GET请求，POST-form请求， 使用RequestInterceptor注册）
* request拦截器（ 按需，使用RequestInterceptor注册）
* devmodule内置熔断器，（按需， 不支持降级）
* discovery( 按需，将 baseUrl，配置为discovery:// 即可开启)

## 自定义组件

jproject-httpclient组件在啊devmodule的基础上， 支持了以下功能；

* 支持整个请求的拦截器， 包括对请求前、请求后的切面拦截，使用addInterceptor注册
* 支持hystrix熔断器，能够支持http请求的熔断和降级（使用添加hystrixProperties，进行配置；
* 开放配置请求体返回体的converterFactory（也支持调用POST-json请求）
* 内置了符合游戏业务日志打印规范的日志拦截器；
* 内置了游戏业务通用请求头拦截器
* 内置了游戏sdk通用签名拦截器
* 内置调用主站内部服务、鉴权认证服务的示例

### 游戏业务通用日志打印组件

devmodule内置的打印组件，由于以下原因，游戏业务的一部分历史服务无法使用该日志打印组件； （1）答应日志的topic为http-client-access，不适用游戏业务中历史的告警等等规则；
（2）同时由于游戏业务历史的古老项目中，日志组件可能不支持org.slf4j.Marker,导致日志打印的关键信息丢失；

所以我们也提供了适用于游戏业务的日志打印组件，LoggingInterceptor，当提供该组件时devmodule默认的日志打印组件将被关闭；

```
//...
.addInterceptor(new LoggingInterceptor())
//...
```

如果服务中不支持org.slf4j.Marker， 可以通过重载log方法实现自定义的日志打印，包括重定义定义日志的打印topic

```

.addInterceptor(new LoggingInterceptor(){
    @Override
    public void log(String method,String query,String url,int status,String addr,float ts,String requestHeader,String requestBody,String responseHeader,String responseMessage,Throwable ex){
        log.info(LogHelper.getLogString(
        ControllerContext.getContext().getRealIp(),url+"?"+query,requestHeader,
        requestBody,responseMessage,(long)ts*1000
        ));
    }
})
```

最后，如果你既不想开启devmodule的日志打印，也不想使用游戏通用日志打印，那么你可以使用LogLevel注解关闭日志的devmodule日志注解， 然后加入自己的日志拦截器；

### 游戏业务通用请求头拦截器

```java
 .addInterceptor(new SpecialHeaderInterceptor()
        .setCaller("test-module")
        .setUserAgent("Mozalia")
        .setClientIpGetter(()->ControllerContext.getRealIp())
        .setRequestIdGetter(()->ControllerContext.getRequestId())
        )
```

需要注意的是由于在弃用hystrix的时候存在跨线程，如果你提供的clientIp和requestId获取方法是通过ThreadLocal，那么需要继承现成本地变量；

集成方式参考如下：

```java

public class Demo {
    static {
        HystrixPlugins.getInstance().registerConcurrencyStrategy(new HystrixConcurrencyStrategy() {
            @Override
            public <T> Callable<T> wrapCallable(Callable<T> callable) {
                return new Callable<T>() {

                    String localValue = local.get();

                    @Override
                    public T call() throws Exception {

                        local.set(localValue);
                        return callable.call();
                    }

                };
            }
        });
    }
}

```

另外如果服务内部已经注册该并发策略，注意统一hystrix的注册逻辑，将该逻辑合并入以后的逻辑中

### hystrix熔断器

hystrix熔断器目前进支持配置standByHosts参数， 支持备机降级 （ 通常是为使用公网地址进行降级） ，

使用方式上文已经讲述, 这里再另外讲述下hystrixBreakPredicate，该参数是针对http返回的熔断判断条件， 当判断条件返回为true时，虽然http返回了，但是还是认为是失败，会触发熔断器；
当判断条件返回为false是，则表示正常返回； 系统内置的熔断判断条件是，所有http返回都不触发熔断；

当我们需要自定义熔断判断条件时，我们可以主动设置进行自定义，通常的熔断条件包括

所有的http为code非2XX的，都认为失败

```java

.hystrixBreakPredicate(rsp->{
    return !rsp.isSuccessful()
        })
```

或者所有httpcode 为非2XX，且http code不为412时都认为失败，412是因为主站的网关拦截判断的错误码， 不希望该错误码命中熔断条件时，可以这么设置熔断判断条件；

```java

.hystrixBreakPredicate(rsp->{
        return!rsp.isSuccessful()&&rsp.code()!=412;
        })
```

## 其他特性

为了进一步简化调用的流程，我们在GeneralHttpApi中封装了call、callWithDataResponse的工具方法， 封装了一般的调用流程，简化http组件的使用；

实例如下：

```java
class Demo {
    /**
     * 获取up主投稿列表
     *
     */
    public List<ArchiveDTO> getArchive(Long mid, Integer pn, Integer ps) {

        return Try.of(() -> {
            return mainSiteApi.callWithDataResponse(mainSiteApi.pageArchivesByUp(String.valueOf(mid), String.
                            valueOf(pn), String.valueOf(ps)))
                    .getArchives();
        }).getOrElseGet(t -> {
            LOGGER.error("Get archive failed", t);
            return Collections.emptyList();
        });
    }
}
```

重构前(里面涉及其他方面的优化， 仅做参考)

```java

```

## 最佳实践

为了方便同学更易于接入使用jproject-http组件，我们在ample-api项目中进行了internalHttpClient的重构打样，通过前后的重构，能够优化整个http流程的调用；








