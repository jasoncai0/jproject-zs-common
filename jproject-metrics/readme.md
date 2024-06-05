## step1 注册自定义的 /metrics

替换MetricServlet

```shell

webapp/WEB-INF/web.xml 

<servlet>
    <servlet-name>prometheus</servlet-name>
    <servlet-class>com.sample.project.SmapleMetricServlet</servlet-class>
    <load-on-startup>2</load-on-startup>
</servlet>
```

## step2 使用注解进行埋点

注意如果当前的注解如果无法满足需要， 可以（1）联系组件开发者， （2） 业务自行定义， 参考目前的实现即可； 
