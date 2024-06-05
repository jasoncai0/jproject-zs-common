package com.jproject.zs.common.redis.migration.redis.spi;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/12
 */

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.migration.redis.dto.RedisOptAsyncParam;
import com.jproject.zs.common.redis.migration.redis.seralize.StringBytesCodec;
import com.google.protobuf.ByteString;
import io.vavr.Tuple;
import io.vavr.control.Try;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import devmodule.component.databus.sub.DatabusSub;


@Slf4j
@RequiredArgsConstructor
public abstract class BaseRedisMsgHandler {

    private static final Logger MQ_LOGGER = LoggerFactory.getLogger("mq");
    private static StringBytesCodec decoder = new StringBytesCodec();
    private final RedisOperations redisOperations;
    private final RedisAsyncConfig redisAsyncConfig;
    private final DatabusSub redisAsyncConsumer;

    @PostConstruct
    public void onMessage() {
        Thread consumeThread = new Thread(() -> {
            redisAsyncConsumer.sub(json -> {

                Try.run(() -> {

                    String msg = json.toString();

                    Long now = System.currentTimeMillis();
                    String businessId = UUID.randomUUID().toString();
                    MDC.put("request_id", businessId);
                    try {
                        asyncRedisCommand(msg);
                    } catch (Exception e) {
                        log.error("process message failed");
                        throw e;
                    } finally {
                        // 记录mq日志
                        Long cost = System.currentTimeMillis() - now;
                        MQ_LOGGER.info("{} | {}", msg, cost);
                        MDC.remove("request_id");
                    }

                }).onFailure(t -> {
                    log.error("Fail to handle the consumed databus msg, msg={}", json, t);
                });
            });


        }, "Redis-Async-Databus-Consume-Thread-");
        consumeThread.setDaemon(true);
        consumeThread.start();
    }

// 测试asyncRedisCommand

    public void asyncRedisCommand(String msg) {
        if (!redisAsyncConfig.isBroadcastEnabled()) {
            return;
        }
        RedisOptAsyncParam asyncParam;
        try {
            asyncParam = RedisOptAsyncParam.parseFrom(decoder.decode(msg));
        } catch (IOException e) {
            log.error("asyncRedisCommand parse error , msg:{}", msg, e);
            return;
        }
        // 非法action忽略
        if (!Objects.equals(AsyncConstants.ASYNC_REDIS_DATA_ACTION, asyncParam.getAction())) {
            return;
        }
        // 同源消息忽略
        if (Objects.equals(redisAsyncConfig.getZone(), asyncParam.getZone())) {
            return;
        }
        long expireSeconds = asyncParam.getExpireSeconds();
        switch (asyncParam.getOpt()) {
            case DEL:

                redisOperations.del(asyncParam.getRawKey().toByteArray());
                break;
            case SET:
                redisOperations.set(
                        asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawData().toByteArray(),
                        asyncParam.getExpireSeconds()
                );

                break;
            case SETNX:
                redisOperations.setNx(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawData().toByteArray(),
                        asyncParam.getExpireSeconds()
                );
                break;
            case INCR:

                redisOperations.incr(asyncParam.getRawKey().toByteArray(), asyncParam.getDelta());

                break;
            case SADD:
                redisOperations.sadd(
                        asyncParam.getRawKey().toByteArray(),
                        convert2ByteArrays(asyncParam.getRawDatasList()),
                        expireSeconds);

                break;
            case SREM:

                redisOperations.srem(
                        asyncParam.getRawKey().toByteArray(),
                        // FIXME 注意此处改为了批量
                        convert2ByteArrays(asyncParam.getRawDatasList()));

                break;
            case MSET:
                redisOperations.mset(asyncParam.getMultiRawDataMapMap()
                        .entrySet().stream()
                        .collect(Collectors.toMap(
                                t -> t.getKey().getBytes(StandardCharsets.UTF_8),
                                t -> t.getValue().getBytes(StandardCharsets.UTF_8),
                                (a, b) -> b)));

                break;
            case HSET:
                redisOperations.hset(
                        asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawHashKey().toByteArray(),
                        asyncParam.getRawData().toByteArray(),
                        expireSeconds
                );

                break;
            case HMSET:

                redisOperations.hmset(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getMultiRawDataMapMap().entrySet().stream()
                                .collect(Collectors.toMap(
                                        t -> t.getKey().getBytes(StandardCharsets.UTF_8),
                                        t -> t.getValue().getBytes(StandardCharsets.UTF_8),
                                        (a, b) -> b)),
                        expireSeconds
                );

                break;
            case ZSETADD:
                redisOperations.zadd(
                        asyncParam.getRawKey().toByteArray(),
                        asyncParam.getZSetRawValuesList().stream().map(t -> {
                            return Tuple.of(t.getKey().toByteArray(), t.getScore());
                        }).collect(Collectors.toList()),
                        expireSeconds
                );
                break;
            case HINCR:

                redisOperations.hincr(
                        asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawHashKey().toByteArray(),
                        asyncParam.getDelta()
                );

                break;
            case HREM:
                redisOperations.hdel(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawHashKey().toByteArray());

                break;
            case RPUSH:

                redisOperations.rpush(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawData().toByteArray());

                break;
            case RPUSH_ALL:

                redisOperations.rpushAll(asyncParam.getRawKey().toByteArray(),
                        convert2ByteArrays(asyncParam.getRawDatasList()),
                        expireSeconds
                );

                break;
            case LPUSH:

                redisOperations.lpush(asyncParam.getRawKey().toByteArray(),
                        convert2ByteArrays(asyncParam.getRawDatasList()));

                break;

            case LPUSH_TRIM:

                redisOperations.lpushTrim(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawData().toByteArray(),
                        asyncParam.getLimit());

                break;
            case EXPIRE:

                redisOperations.expire(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getExpireSeconds());

                break;
            case MEXPIRE:

                redisOperations.mexpire(asyncParam.getKeysList().stream()
                                .map(t -> t.toByteArray())
                                .collect(Collectors.toList()),
                        asyncParam.getExpireSeconds());
                break;
            case RPOP: {
                redisOperations.rpop(asyncParam.getRawKey().toByteArray());
                break;
            }
            case LPOP: {
                redisOperations.lpop(asyncParam.getRawKey().toByteArray());
                break;
            }
            case NUMBERSET: {
                redisOperations.numberSet(asyncParam.getRawKey().toByteArray(),
                        asyncParam.getRawData().toByteArray(),
                        asyncParam.getExpireSeconds());
                break;
            }
            case UNRECOGNIZED:
            default:
                break;
        }
    }


    private byte[][] convert2ByteArrays(List<ByteString> rawDatasList) {
        byte[][] data = new byte[rawDatasList.size()][];
        AtomicInteger i = new AtomicInteger(0);
        rawDatasList.forEach(t -> {
            data[i.getAndIncrement()] = t.toByteArray();
        });
        return data;
    }

}
