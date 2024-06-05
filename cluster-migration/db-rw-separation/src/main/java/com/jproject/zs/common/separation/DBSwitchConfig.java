package com.jproject.zs.common.separation;

/**
 * <p></p>
 *
 * @author : dinglei
 * @date : 2023/2/1
 **/
public interface DBSwitchConfig {

    String getReadDbKey();

    String getWriteDbKey();

    boolean isReadOnly();

    /**
     * 用于判断是否打印数据源切换日志，开启会打印 switch db key : {dbKey}
     *
     * @return
     */
    boolean judgePrintLog();

}
