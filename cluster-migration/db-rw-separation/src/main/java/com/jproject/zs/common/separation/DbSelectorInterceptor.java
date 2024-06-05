package com.jproject.zs.common.separation;

import java.util.Objects;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p></p>
 *
 * @author : dinglei
 * @date : 2023/2/1
 **/
@Slf4j
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class,
                        BoundSql.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "close", args = {boolean.class})
})
public class DbSelectorInterceptor implements Interceptor {

    private final DBSwitchConfig dbSwitchConfig;

    public DbSelectorInterceptor(DBSwitchConfig dbSwitchConfig) {
        this.dbSwitchConfig = dbSwitchConfig;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String dbKey;
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        // 存在事务或关闭方法 使用读写库
        String closeMethodName = "close";
        if (synchronizationActive || closeMethodName.equals(invocation.getMethod().getName())) {
            dbKey = dbSwitchConfig.getWriteDbKey();
        } else {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            if (Objects.equals(SqlCommandType.SELECT, mappedStatement.getSqlCommandType())) {
                dbKey = dbSwitchConfig.getReadDbKey();
            } else {
                if (dbSwitchConfig.isReadOnly()) {
                    throw new RuntimeException("current status is readOnly !");
                }
                dbKey = dbSwitchConfig.getWriteDbKey();
            }

        }
        DynamicDBHolder.set(dbKey);
        if (dbSwitchConfig.judgePrintLog()) {
            log.info("switch db key : {}", dbKey);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {

    }

}
