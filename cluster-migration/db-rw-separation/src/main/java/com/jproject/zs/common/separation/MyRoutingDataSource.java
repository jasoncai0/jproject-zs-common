package com.jproject.zs.common.separation;

import java.util.Objects;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * <p></p>
 *
 * @author : dinglei
 * @date : 2023/1/31
 **/
public class MyRoutingDataSource extends AbstractRoutingDataSource {

    private DBSwitchConfig dbSwitchConfig;

    public MyRoutingDataSource(DBSwitchConfig dbSwitchConfig) {
        super();
        this.dbSwitchConfig = dbSwitchConfig;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String dbKey = DynamicDBHolder.get();
        if (Objects.isNull(dbKey)) {
            dbKey = dbSwitchConfig.getWriteDbKey();
            DynamicDBHolder.set(dbKey);
        }
        return dbKey;
    }
}
