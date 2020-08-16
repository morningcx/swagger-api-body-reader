package com.morningcx.swagger.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ApiResponseBody
 *
 * @author MorningStar
 * @date 2020/8/16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResponseBody {
    /**
     * json字符串
     *
     * @return
     */
    String value();
}
