package com.rcloud.server.sealtalk.rongcloud;


import com.rcloud.server.sealtalk.exception.RCloudHttpException;
import com.rcloud.server.sealtalk.util.JacksonUtil;
import com.rcloud.server.sealtalk.util.ThrowSupplier;
import io.rong.models.Result;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class RCExecuteWrapper {


    public static <T extends Result> T executeSDK(ThrowSupplier<T> supplier) throws RCloudHttpException {
        try {
            T r = supplier.get();
            log.info("{}", JacksonUtil.toJson(r));
            return r;
        } catch (Throwable e) {
            log.error("", e);
            throw new RCloudHttpException("RCloud Error");
        }
    }


    public static <T> T executeHttp(ThrowSupplier<T> supplier) throws RCloudHttpException {
        try {
            T r = supplier.get();
            return r;
        } catch (Throwable e) {
            log.error("", e);
            throw new RCloudHttpException("RCloud Error");
        }
    }

}
