package com.jproject.zs.common.snowflake;


/**
 * @author caizhensheng
 * @desc
 * @date 2022/12/23
 */
class IdGeneratorTest {


    public static void main(String[] args) {
        System.out.println(1 << 8);
        System.out.println(-1L ^ (-1L << 8));

        System.out.println(1 << 1);
        System.out.println(-1L ^ (-1L << 1));

        IdGenerator id = new IdGenerator(1, 1, 8, 2);

        System.out.println(id.nextId());
        System.out.println(id.nextId());
        System.out.println(id.nextId());
        System.out.println(id.nextId());
        System.out.println(id.nextId());
    }
}