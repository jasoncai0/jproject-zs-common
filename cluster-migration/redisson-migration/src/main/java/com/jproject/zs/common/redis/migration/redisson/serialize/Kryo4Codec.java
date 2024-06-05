package com.jproject.zs.common.redis.migration.redisson.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.google.protobuf.GeneratedMessage;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import de.javakaffee.kryoserializers.protobuf.ProtobufSerializer;
import java.util.List;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.redisson.codec.KryoCodec;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/5/17
 */
public class Kryo4Codec extends KryoCodec {

    public static final Kryo4Codec INSTANCE = new Kryo4Codec();

    public Kryo4Codec() {
    }

    public Kryo4Codec(ClassLoader classLoader) {
        super(classLoader);
    }

    public Kryo4Codec(ClassLoader classLoader, Kryo4Codec codec) {
        super(classLoader, codec);
    }

    public Kryo4Codec(List<Class<?>> classes) {
        super(classes);
    }

    public Kryo4Codec(List<Class<?>> classes, ClassLoader classLoader) {
        super(classes, classLoader);
    }

    protected Kryo createInstance(List<Class<?>> classes, ClassLoader classLoader) {
        Kryo kryo = super.createInstance(classes, classLoader);
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        kryo.addDefaultSerializer(GeneratedMessage.class, ProtobufSerializer.class);
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);
        ImmutableListSerializer.registerSerializers(kryo);
        ImmutableSetSerializer.registerSerializers(kryo);
        ImmutableMapSerializer.registerSerializers(kryo);
        ImmutableMultimapSerializer.registerSerializers(kryo);
        ReverseListSerializer.registerSerializers(kryo);
        UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
        ArrayListMultimapSerializer.registerSerializers(kryo);
        HashMultimapSerializer.registerSerializers(kryo);
        LinkedHashMultimapSerializer.registerSerializers(kryo);
        LinkedListMultimapSerializer.registerSerializers(kryo);
        TreeMultimapSerializer.registerSerializers(kryo);
        ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(
                new StdInstantiatorStrategy());
        return kryo;
    }


}
