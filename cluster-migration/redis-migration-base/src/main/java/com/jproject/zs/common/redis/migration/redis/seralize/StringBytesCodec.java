package com.jproject.zs.common.redis.migration.redis.seralize;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/19
 */
@NoArgsConstructor
public class StringBytesCodec {

    private static BASE64Encoder encoder = new BASE64Encoder();

    private static BASE64Decoder decoder = new BASE64Decoder();


    public String encode(byte[] bytes) {
        return encode(bytes, true);
    }

    public byte[] decode(String str) throws IOException {
        return decode(str, true);
    }

    public String encode(byte[] bytes, boolean wrapInJson) {
        String str = encoder.encode(bytes);
        if (wrapInJson) {
            str = JSON.toJSONString(new WrappedStringInJson().setV(str));
        }
        return str;
    }

    public byte[] decode(String str, boolean wrapInJson) throws IOException {

        String strVal = str;

        if (wrapInJson) {
            WrappedStringInJson json = JSON.parseObject(str, WrappedStringInJson.class);
            strVal = json.getV();
        }

        return decoder.decodeBuffer(strVal);
    }


    @Data
    @Accessors(chain = true)
    public static class WrappedStringInJson {

        private String v;

    }


}
