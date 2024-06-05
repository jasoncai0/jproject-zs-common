package com.jproject.zs.common.cache;

import io.vavr.control.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/20
 */

public interface LockService {


    /**
     * 对{@link #lock(Callable, String, long, TimeUnit, Error)}的Try封装
     */
    <T> Try<T> tryLock(Callable<T> task, String key,
                       long timeout, long leaseTime, TimeUnit unit,
                       int errCode, String errMsg);

    /**
     * 同上， ！！注意该方法不会抛出异常，因为返回的是Try！！
     */
    Try<Void> tryLock(ThrowableRunnable task, String key,
                      long timeout, long leaseTime, TimeUnit unit,
                      int errCode, String errMsg);

    /**
     * 封装了lock方法的一般使用，包括获取和释放锁、异常处理等操作；
     */
    <T> T lock(Callable<T> task, String key,
               long timeout, long leaseTime, TimeUnit unit, int errCode, String errMsg);


    /**
     * 同上
     */
    void lock(ThrowableRunnable task, String key,
              long timeout, long leaseTime, TimeUnit unit,
              int errCode, String errMsg);

    /**
     * try lock the module in timeout， 底层的锁实现；
     */
    boolean lock(String key, long timeout, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * release the lock of module
     */
    void unlock(String key);


    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Exception;
    }
}
