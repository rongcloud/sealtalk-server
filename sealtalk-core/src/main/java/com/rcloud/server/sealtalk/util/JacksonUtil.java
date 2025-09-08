package com.rcloud.server.sealtalk.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JacksonUtil {

    private final static ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
    }

    public static ObjectMapper getInstance() {
        return objectMapper;
    }

    /**
     * bean、array、List、Map --> json
     */
    public static String toJson(Object object) throws ServiceException {
        try {
            return getInstance().writeValueAsString(object);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new ServiceException(ErrorCode.UNKNOWN_ERROR.getErrorCode(), "JSON ERROR");
        }
    }

    /**
     * string --> bean、Map、List(array)
     */
    public static <T> T fromJson(String jsonStr, Class<T> clazz) throws ServiceException {
        try {
            return getInstance().readValue(jsonStr, clazz);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new ServiceException(ErrorCode.UNKNOWN_ERROR.getErrorCode(), "JSON ERROR");
        }
    }


    public static <T> T fromJson(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return getInstance().readValue(json, typeReference);
    }


    /**
     * string --> List<Bean>
     */
    public static <T> T fromJson(String jsonStr, Class<?> parametrized, Class<?>... parameterClasses) throws ServiceException {

        try {
            JavaType javaType = getInstance().getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return getInstance().readValue(jsonStr, javaType);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new ServiceException(ErrorCode.UNKNOWN_ERROR.getErrorCode(), "JSON ERROR");
        }
    }

    /**
     * 将json串转成 JsonNode
     */
    public static JsonNode getJsonNode(String json) throws ServiceException {

        try {
            json = json.replaceAll("\r|\n|\t", "");
            return getInstance().reader().readTree(json);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new ServiceException(ErrorCode.UNKNOWN_ERROR.getErrorCode(), "JSON ERROR");
        }
    }

}
