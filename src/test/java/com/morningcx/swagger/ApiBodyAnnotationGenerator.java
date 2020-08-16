package com.morningcx.swagger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.morningcx.swagger.plugins.ApiRequestBody;
import com.morningcx.swagger.plugins.ApiResponseBody;

import java.io.InputStream;

/**
 * ApiBodyAnnotationGenerator
 *
 * @author MorningStar
 * @date 2020/8/16
 */
public class ApiBodyAnnotationGenerator {

    public static void main(String[] args) throws Exception {
        String reqName = "@" + ApiRequestBody.class.getSimpleName();
        String respName = "@" + ApiResponseBody.class.getSimpleName();
        InputStream in = ApiBodyAnnotationGenerator.class.getClassLoader().getResourceAsStream("ApiBody.json");
        JSONObject jsonObject = JSON.parseObject(in, JSONObject.class, Feature.OrderedField);
        String reqJson = JSON.toJSONString(jsonObject.get(reqName), true);
        String respJson = JSON.toJSONString(jsonObject.get(respName), true);
        System.out.println(reqName + "(\"" + reqJson.replaceAll("\"", "'").replaceAll("\n", "\" + \n\"") + "\")");
        System.out.println(respName + "(\"" + respJson.replaceAll("\"", "'").replaceAll("\n", "\" + \n\"") + "\")");
    }
}
