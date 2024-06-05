package com.jproject.zs.common.redis.migration.redis.broadcast;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redis.constants.AsyncConstants;
import com.jproject.zs.common.migration.redis.dto.RedisOptAsyncParam;
import com.jproject.zs.common.migration.redis.dto.RedisOptAsyncParam.Builder;
import com.jproject.zs.common.migration.redis.dto.RedisOptEnum;
import com.jproject.zs.common.migration.redis.dto.Tuple;
import com.jproject.zs.common.redis.migration.redis.seralize.StringBytesCodec;
import com.jproject.zs.common.redis.migration.redis.spi.RedisOperations;
import com.google.protobuf.ByteString;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/12
 */
@Slf4j
@Setter
@NoArgsConstructor
public class RedisOperationsBroadcast implements RedisOperations {

    private static StringBytesCodec encoder = new StringBytesCodec();
    private DatabusPub redisDatabusProducer;
    private RedisAsyncConfig redisAsyncConfig;
    private ExecutorService publishExecutor;

    public RedisOperationsBroadcast(DatabusPub redisDatabusProducer, RedisAsyncConfig redisAsyncConfig) {
        this.redisDatabusProducer = redisDatabusProducer;
        this.redisAsyncConfig = redisAsyncConfig;
        this.publishExecutor = Executors.newFixedThreadPool(redisAsyncConfig.getBroadcastAsyncThreadNum());
    }

    @Override
    public void del(byte[] keyBytes) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.DEL);
        });
    }

    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.SET)
                    .setRawData(ByteString.copyFrom(valueBytes));
        }, expireSeconds);
    }

    @Override
    public void setNx(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.SETNX)
                    .setRawData(ByteString.copyFrom(valueBytes));
        }, expireSeconds);
    }

    @Override
    public void incr(byte[] keyBytes, long delta) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.INCR)
                    .setDelta(delta);
        });
    }

    @Override
    public void sadd(byte[] keyBytes, byte[][] data, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.SADD)
                    .addAllRawDatas(Arrays.stream(data)
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()));
        });

    }

    // TODO 应该算bug？ sadd是批量的而srem不是
    @Override
    public void srem(byte[] keyBytes, byte[][] data) {

        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.SREM)
                    .addAllRawDatas(Arrays.stream(data)
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()));
        });
    }


    // TODO 是否要指定消息key
    @Override
    public void mset(Map<byte[], byte[]> dataMap) {
        Try.run(() -> {
            Builder paramBuilder = RedisOptAsyncParam.newBuilder()
                    .setZone(redisAsyncConfig.getZone())
                    .setOpt(RedisOptEnum.MSET)
                    .setAction(AsyncConstants.ASYNC_REDIS_DATA_ACTION)
                    .putAllMultiRawDataMap(dataMap.entrySet().stream()
                            .collect(Collectors.toMap(
                                    t -> new String(t.getKey(), StandardCharsets.UTF_8),
                                    t -> new String(t.getValue(),
                                            StandardCharsets.UTF_8),
                                    (a, b) -> b)));
            doBroadcast(paramBuilder.build(), null);
        }).onFailure(e -> log.warn("redis async command error ! key:{}", null, e));
    }

    @Override
    public void hset(byte[] keyBytes, byte[] hashKeyBytes, byte[] dataBytes, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.HSET)
                    .setRawHashKey(ByteString.copyFrom(hashKeyBytes))
                    .setRawData(ByteString.copyFrom(dataBytes));
        }, expireSeconds);
    }

    @Override
    public void hmset(byte[] keyBytes, Map<byte[], byte[]> dataMap, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.HMSET)
                    .putAllMultiRawDataMap(dataMap.entrySet().stream()
                            .collect(Collectors.toMap(
                                    t -> new String(t.getKey(), StandardCharsets.UTF_8),
                                    t -> new String(t.getValue(),
                                            StandardCharsets.UTF_8),
                                    (a, b) -> b)));
        }, expireSeconds);
    }

    @Override
    public void zadd(byte[] keyBytes, List<Tuple2<byte[], Double>> values, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.ZSETADD)
                    .addAllZSetRawValues(values
                            .stream()
                            .map(v -> Tuple.newBuilder().setScore(v._2)
                                    .setKey(ByteString.copyFrom(v._1)).build()).collect(Collectors.toSet()));
        }, expireSeconds);


    }

    @Override
    public void hincr(byte[] keyBytes, byte[] hashKeyBytes, long delta) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.HINCR)
                    .setRawHashKey(ByteString.copyFrom(hashKeyBytes))
                    .setDelta(delta);
        });
    }

    @Override
    public void hdel(byte[] keyBytes, byte[] hashKeyBytes) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.HREM)
                    .setRawHashKey(ByteString.copyFrom(hashKeyBytes));
        });
    }

    @Override
    public void rpush(byte[] keyBytes, byte[] data) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.RPUSH)
                    .addRawDatas(ByteString.copyFrom(data));
        });
    }

    @Override
    public void rpushAll(byte[] keyBytes, byte[][] data, long expireSeconds) {

        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.RPUSH_ALL)
                    .addAllRawDatas(Arrays.stream(data)
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()));
        }, expireSeconds);
    }

    @Override
    public void lpush(byte[] keyBytes, byte[][] data) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.LPUSH).addAllRawDatas(
                    Arrays.stream(data)
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()));
        });
    }

    @Override
    public void rpop(byte[] keyBytes) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.RPOP);
        });
    }

    @Override
    public void lpop(byte[] keyBytes) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.LPOP);
        });
    }

    @Override
    public void lpushTrim(byte[] keyBytes, byte[] data, int limit) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.LPUSH_TRIM)
                    .addRawDatas(ByteString.copyFrom(data))
                    .setLimit(limit);
        });
    }

    @Override
    public void expire(byte[] keyBytes, long expireSeconds) {
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.EXPIRE)
                    .setExpireSeconds(expireSeconds);
        }, expireSeconds);
    }


    // TODO 广播时候的msgKey不允许自定义
    @Override
    public void mexpire(List<byte[]> keys, long expireSeconds) {

        Try.run(() -> {
            Builder paramBuilder = RedisOptAsyncParam.newBuilder()
                    .setZone(redisAsyncConfig.getZone())
                    .setOpt(RedisOptEnum.MEXPIRE)
                    .setAction(AsyncConstants.ASYNC_REDIS_DATA_ACTION)
                    .addAllKeys(keys.stream()
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()))
                    .setExpireSeconds(expireSeconds);
            doBroadcast(paramBuilder.build(), null);
        }).onFailure(e -> log.warn("redis async command error ! key:{}", keys, e));
    }

    @Override
    public void numberSet(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        // number set
        broadcastCommand(keyBytes, builder -> {
            builder.setOpt(RedisOptEnum.NUMBERSET)
                    .setRawData(ByteString.copyFrom(valueBytes))
                    .setExpireSeconds(expireSeconds);
        }, expireSeconds);
    }

    private void broadcastCommand(byte[] key, Consumer<Builder> valueParamSetter) {
        broadcastCommand(key, valueParamSetter, null, key);
    }

    // TODO rename broadcastCommand
    private void broadcastCommand(byte[] key, Consumer<Builder> valueParamSetter, Long expireSeconds) {
        broadcastCommand(key, valueParamSetter, expireSeconds, key);
    }

    private void broadcastCommand(byte[] key, Consumer<Builder> valueParamSetter, Long expireExpireSeconds,
            byte[] msgKey) {
        if (redisAsyncConfig.isBroadcastEnabled()) {
            Try.run(() -> {
                Builder paramBuilder = RedisOptAsyncParam.newBuilder()
                        .setZone(redisAsyncConfig.getZone())
                        .setAction(AsyncConstants.ASYNC_REDIS_DATA_ACTION)
                        .setRawKey(ByteString.copyFrom(key));
                valueParamSetter.accept(paramBuilder);
                if (Objects.nonNull(expireExpireSeconds)) {
                    paramBuilder.setExpireSeconds(expireExpireSeconds);
                }
                doBroadcast(paramBuilder.build(), msgKey);
            }).onFailure(e -> log.warn("redis async command error ! key:{}", key, e));
        }
    }

    private void doBroadcast(RedisOptAsyncParam redisOptAsyncDTO, byte[] key) {
        if (log.isDebugEnabled()) {
            log.debug("sendAsync data:{}", redisOptAsyncDTO.toString());
        }

        Runnable publish = () -> {
            redisDatabusProducer.pub(
                    Optional.ofNullable(key).map(keyData -> new String(keyData, StandardCharsets.UTF_8)).orElse(null),
                    encoder.encode(redisOptAsyncDTO.toByteArray()));
        };

        if (redisAsyncConfig.isBroadcastAsync()) {
            publishExecutor.submit(publish);
        } else {
            publish.run();
        }
    }



}
