package com.jproject.zs.common.redis.migration.redisson.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.google.protobuf.GeneratedMessage;
import de.javakaffee.kryoserializers.protobuf.ProtobufSerializer;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@Slf4j
@RequiredArgsConstructor
public class KryoSerializer {

    private static final Kryo kryo = new Kryo();


    static {
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        kryo.addDefaultSerializer(GeneratedMessage.class, ProtobufSerializer.class);
    }


    public byte[] serialize(Object obj) {
        byte[] bytes;
        try (Output ou = new Output(new ByteArrayOutputStream())) {
            kryo.writeClassAndObject(ou, obj);
            bytes = ou.toBytes();

            return bytes;
        }
    }

    public <V> byte[][] serializeList(List<V> values) {
        byte[][] bytesArray = new byte[values.size()][];

        for (int i = 0; i < values.size(); i++) {
            bytesArray[i] = serialize(values.get(i));
        }
        return bytesArray;
    }


    public Object deserialize(byte[] bytes) {
        try (Input input = new Input(bytes)) {
            return kryo.readClassAndObject(input);
        }
    }


    public List<Object> deserializeList(byte[][] bytesArray) {

        List<Object> result = new ArrayList<>();

        for (int i = 0; i < bytesArray.length; i++) {
            result.add(deserialize(bytesArray[i]));
        }
        return result;
    }
}
