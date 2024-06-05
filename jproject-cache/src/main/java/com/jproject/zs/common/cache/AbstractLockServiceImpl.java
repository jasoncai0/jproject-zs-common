package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.exception.JProjectCommonException;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/20
 */

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLockServiceImpl implements LockService {


    @Override
    public <T> Try<T> tryLock(Callable<T> task, String key, long timeout, long leaseTime, TimeUnit unit,
                              int errCode, String errMsg) {
        return Try.of(() -> lock(task, key, timeout, leaseTime, unit, errCode, errMsg));
    }

    @Override
    public Try<Void> tryLock(ThrowableRunnable task, String key, long timeout, long leaseTime, TimeUnit unit, int errCode, String errMsg) {
        return Try.run(() -> lock(task, key, timeout, leaseTime, unit, errCode, errMsg));
    }

    @Override
    public void lock(ThrowableRunnable task, String key, long timeout, long leaseTime, TimeUnit unit, int errCode, String errMsg) {
        this.lock((Callable<Void>) () -> {
            task.run();
            return null;
        }, key, timeout, leaseTime, unit, errCode, errMsg);
    }

    @Override
    public <T> T lock(Callable<T> task, String key, long timeout, long leaseTime, TimeUnit unit, int errCode, String errMsg) {
        try {
            if (!this.lock(key, timeout, leaseTime, unit)) {
                log.error("Fail to acquire the lock key={}, timeout", key);
                throw new JProjectCommonException(errCode, errMsg);
            }
        } catch (InterruptedException e) {
            log.error("Interrupt acquire the lock key={}, interrupt", key, e);
            throw new JProjectCommonException(errCode, errMsg);
        }
        try {
            return task.call();
        } catch (Exception e) {
            if (e instanceof JProjectCommonException) {
                log.warn("Fail to handle the locked task, key={}", key, e);
                throw (JProjectCommonException) e;
            }
            log.error("Fail to handle the locked task, key={}", key, e);
            throw new JProjectCommonException(e.getMessage(), e, errCode, errMsg);
        } finally {
            this.unlock(key);
        }
    }

}
