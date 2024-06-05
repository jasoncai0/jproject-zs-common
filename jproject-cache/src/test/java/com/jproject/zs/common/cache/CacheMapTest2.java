package com.jproject.zs.common.cache;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/11/15
 */
public class CacheMapTest2 {


    private Set<String> dummyIds = Sets.newHashSet("dummy0", "dummy1", "dummy2", "dummy3");

    private CacheMapMemoryImpl cache = new CacheMapMemoryImpl(false, "_dummy");

    private Function<List<String>, Map<String, String>> valueGetter = ids -> {
        System.out.println("load from db, ids=" + ids);
        return ids.stream().filter(id -> !dummyIds.contains(id))
                .collect(Collectors.toMap(id -> id, id -> id));
    };


    @Test
    public void test() {
        System.out.println("----------");
        System.out.println("result:" + cache.getCacheHelper().readValueByIds(Lists.newArrayList("1", "2", "dummy1"), valueGetter));


        System.out.println("----------");

        System.out.println("result:" + cache.getCacheHelper().readValueByIds(Lists.newArrayList("1", "2", "dummy1"), valueGetter));

        System.out.println("----------");

        System.out.println("result:" + cache.getCacheHelper().readValueByIds(Lists.newArrayList("1", "2", "dummy1"), valueGetter));


        System.out.println("----------");

        System.out.println("result:" + cache.getCacheHelper().readValue("1", () -> {
            System.out.println("load from db 1");
            return Optional.of("1");
        }));

        System.out.println("result:" + cache.getCacheHelper().readValue("1", () -> {
            System.out.println("load from db 1");
            return Optional.of("1");
        }));
        System.out.println("result:" + cache.getCacheHelper().readValue("dummy1", () -> {
            System.out.println("load from db dummy1");
            return Optional.empty();
        }));

        System.out.println("result:" + cache.getCacheHelper().readValue("dummy1", () -> {
            System.out.println("load from db dummy1");
            return Optional.empty();
        }));

        System.out.println("--------");


        System.out.println("result:" + cache.getCacheHelper().readValue("dummy2", () -> {
            System.out.println("load from db dummy2");
            return Optional.empty();
        }));

        System.out.println("result:" + cache.getCacheHelper().readValue("dummy2", () -> {
            System.out.println("load from db dummy2");
            return Optional.empty();
        }));


    }
}
