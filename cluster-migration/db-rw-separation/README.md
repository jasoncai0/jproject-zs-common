# Quick Start

## maven依赖

```xml

<dependency>
  <groupId>com.jproject.zs</groupId>
  <artifactId>db-rw-separation</artifactId>
  <version>${version}</version>
</dependency>
```

## SpringMVC使用配置文件配置数据源的方式

### 配置多数据源



配置文件参考

```properties
db.switch.mobile.readDbKey=GZ
db.switch.mobile.writeDbKey=GZ
db.switch.mobile.isReadOnly=false
```

## SpringBoot方式

### 配置多数据源

```java

@Configuration
@Import(MybatisProperties.class)
public class DataSourceConfig {


    @Bean
    @ConfigurationProperties("spring.datasource.gz")
    public DataSource gzDataSource() {
        return new DruidDataSource();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.idc")
    public DataSource idcDataSource() {
        return new DruidDataSource();
    }

    @Bean
    public DataSource myRoutingDataSource(@Qualifier("gzDataSource") DataSource gzDataSource,
            @Qualifier("idcDataSource") DataSource idcDataSource, DataSourceSwitchConfig dataSourceSwitchConfig) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DBTypeEnum.GZ.name(), gzDataSource);
        targetDataSources.put(DBTypeEnum.IDC.name(), idcDataSource);
        MyRoutingDataSource myRoutingDataSource = new MyRoutingDataSource(dataSourceSwitchConfig);
        myRoutingDataSource.setDefaultTargetDataSource(gzDataSource);
        myRoutingDataSource.setTargetDataSources(targetDataSources);
        return myRoutingDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource myRoutingDataSource, MybatisProperties properties,
            DataSourceSwitchConfig dataSourceSwitchConfig) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(myRoutingDataSource);
        sqlSessionFactoryBean.setMapperLocations(properties.resolveMapperLocations());
        sqlSessionFactoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        sqlSessionFactoryBean.setConfiguration(properties.getConfiguration());
        sqlSessionFactoryBean.setPlugins(new DbSelectorInterceptor(dataSourceSwitchConfig), new CatMybatisPlugin());
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager(DataSource myRoutingDataSource) {
        return new DataSourceTransactionManager(myRoutingDataSource);
    }
}
```

并禁用数据源自动装配

```java

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

```

### 动态配置

```java

@Component
@Data
@WatchedProperties
@ConfigurationProperties(prefix = "db.switch")
public class DataSourceSwitchConfig implements DBSwitchConfig {

    @Autowired
    private SamplingRateService samplingRateService;

    private String readDbType = DBTypeEnum.GZ.name();

    private String writeDbType = DBTypeEnum.GZ.name();

    private boolean isReadOnly = false;

    @Override
    public String getReadDbKey() {
        return readDbType;
    }

    @Override
    public String getWriteDbKey() {
        return writeDbType;
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public boolean judgePrintLog() {
        return LoggingUtils.isTraceSampling(samplingRateService.getSamplingRate());
    }
}
```

配置文件参考

```properties
db.switch.readDbType=IDC
db.switch.writeDbType=GZ
db.switch.isReadOnly=false
```
