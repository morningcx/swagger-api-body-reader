package com.morningcx.swagger.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiListingBuilder;
import springfox.documentation.builders.ModelBuilder;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.schema.Model;
import springfox.documentation.schema.ModelProperty;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.Types;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.Operation;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ApiJsonBodyReader
 *
 * @author MorningStar
 * @date 2020/8/16
 */
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1)
@ConditionalOnProperty(prefix = "swagger", name = "enable", havingValue = "true")
public class ApiBodyReader implements ApiListingBuilderPlugin {

    private static final Logger log = LoggerFactory.getLogger(ApiBodyReader.class);

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }

    @Override
    public void apply(ApiListingContext context) {
        try {
            if (context.getResourceGroup().getControllerClass().isPresent()) {
                Class<?> clazz = context.getResourceGroup().getControllerClass().get();
                String reqPrefix = join(ApiRequestBody.class.getSimpleName(), clazz.getSimpleName());
                String respPrefix = join(ApiResponseBody.class.getSimpleName(), clazz.getSimpleName());
                ApiListingBuilder apiListingBuilder = context.apiListingBuilder();
                Map<String, Model> models = getDeclaredField(apiListingBuilder, "models");
                List<ApiDescription> apis = getDeclaredField(apiListingBuilder, "apis");
                Map<String, ModelRef> modelRefs = new HashMap<>(apis.size());
                for (Method method : clazz.getDeclaredMethods()) {
                    ApiRequestBody reqBody = method.getAnnotation(ApiRequestBody.class);
                    ApiResponseBody respBody = method.getAnnotation(ApiResponseBody.class);
                    if (reqBody != null) {
                        String name = join(reqPrefix, method.getName());
                        modelRefs.put(name, generateModel(models, name, JSON.parse(reqBody.value())).getValue());
                    }
                    if (respBody != null) {
                        String name = join(respPrefix, method.getName());
                        modelRefs.put(name, generateModel(models, name, JSON.parse(respBody.value())).getValue());
                    }
                }
                for (ApiDescription api : apis) {
                    ModelRef reqModelRef = modelRefs.get(join(reqPrefix, api.getDescription()));
                    ModelRef respModelRef = modelRefs.get(join(respPrefix, api.getDescription()));
                    if (reqModelRef != null) {
                        for (Operation operation : api.getOperations()) {
                            for (Parameter parameter : operation.getParameters()) {
                                if ("body".equals(parameter.getParamType())) {
                                    setDeclaredField(parameter, "modelRef", reqModelRef);
                                }
                            }
                        }
                    }
                    if (respModelRef != null) {
                        for (Operation operation : api.getOperations()) {
                            for (ResponseMessage respMsg : operation.getResponseMessages()) {
                                // getResponseModel不为null则说明returnType不为void
                                if (respMsg.getCode() == HttpStatus.OK.value() && respMsg.getResponseModel() != null) {
                                    setDeclaredField(respMsg, "responseModel", respModelRef);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("swagger接口解析失败：" + e.getMessage(), e);
        }
    }

    private Pair<ResolvedType, ModelRef> generateModel(Map<String, Model> models, String name, Object obj) {
        Pair<ResolvedType, ModelRef> typeRefPair;
        TypeResolver resolver = new TypeResolver();
        if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            Map<String, ModelProperty> properties = new HashMap<>(jsonObject.size());
            jsonObject.forEach((key, value) -> {
                String[] keys = key.split("//");
                key = keys[0].trim();
                String refName = join(name, key);
                Pair<ResolvedType, ModelRef> pair = generateModel(models, refName, value);
                properties.put(key, new ModelPropertyBuilder()
                        .name(key)
                        .type(pair.getKey())
                        .example(models.containsKey(refName) ? null : value)
                        .description(keys.length > 1 ? keys[1].trim() : null)
                        .build()
                        .updateModelRef(t -> pair.getValue()));
            });
            ResolvedType resolvedType = resolver.resolve(Object.class);
            models.put(name, new ModelBuilder()
                    .name(name)
                    .type(resolvedType)
                    .properties(properties)
                    .build());
            typeRefPair = new Pair<>(resolvedType, new ModelRef(name));
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            Object element = jsonArray.isEmpty() ? null : jsonArray.get(0);
            Pair<ResolvedType, ModelRef> pair = generateModel(models, name, element);
            ResolvedType resolvedType = resolver.resolve(List.class, pair.getKey());
            typeRefPair = new Pair<>(resolvedType, new ModelRef(name, pair.getValue()));
        } else {
            // resolvedType为Void时会被忽略
            Type type = obj == null ? Void.class : obj.getClass();
            ResolvedType resolvedType = resolver.resolve(type);
            ModelRef modelRef = new ModelRef(Types.typeNameFor(type));
            typeRefPair = new Pair<>(resolvedType, modelRef);
        }
        return typeRefPair;
    }

    private String join(CharSequence... elements) {
        return String.join("_", elements);
    }

    @SuppressWarnings("unchecked")
    private <T> T getDeclaredField(Object object, String field) throws Exception {
        Field declaredField = object.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return (T) declaredField.get(object);
    }

    private void setDeclaredField(Object object, String field, Object value) throws Exception {
        Field declaredField = object.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(object, value);
    }
}
