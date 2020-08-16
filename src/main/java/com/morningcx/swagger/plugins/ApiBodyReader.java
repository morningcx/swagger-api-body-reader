package com.morningcx.swagger.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiListingBuilder;
import springfox.documentation.builders.ModelBuilder;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.schema.*;
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
import java.util.Collection;
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

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApiBodyReader.class);

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }

    @Override
    public void apply(ApiListingContext context) {
        try {
            if (context.getResourceGroup().getControllerClass().isPresent()) {
                String reqPrefix = ApiRequestBody.class.getSimpleName();
                String respPrefix = ApiResponseBody.class.getSimpleName();
                Class<?> controllerClass = context.getResourceGroup().getControllerClass().get();
                String controllerName = controllerClass.getSimpleName();
                ApiListingBuilder apiListingBuilder = context.apiListingBuilder();
                Map<String, Model> models = getDeclaredField(apiListingBuilder, "models");
                List<ApiDescription> apis = getDeclaredField(apiListingBuilder, "apis");
                for (Method method : controllerClass.getDeclaredMethods()) {
                    String suffix = join(controllerName, method.getName());
                    ApiRequestBody reqBody = method.getAnnotation(ApiRequestBody.class);
                    ApiResponseBody respBody = method.getAnnotation(ApiResponseBody.class);
                    if (reqBody != null) {
                        generateModel(models, join(reqPrefix, suffix), JSON.parse(reqBody.value()));
                    }
                    if (respBody != null) {
                        generateModel(models, join(respPrefix, suffix), JSON.parse(respBody.value()));
                    }
                }
                for (ApiDescription api : apis) {
                    String suffix = join(controllerName, api.getDescription());
                    Model reqModel = models.get(join(reqPrefix, suffix));
                    Model respModel = models.get(join(respPrefix, suffix));
                    if (reqModel != null) {
                        for (Operation operation : api.getOperations()) {
                            for (Parameter parameter : operation.getParameters()) {
                                if ("body".equals(parameter.getParamType())) {
                                    ModelReference modelRef = parameter.getModelRef();
                                    setDeclaredField(parameter, "modelRef", new ModelRef(reqModel.getName(),
                                            modelRef.isCollection() ? new ModelRef(reqModel.getName()) : null));
                                }
                            }
                        }
                    }
                    if (respModel != null) {
                        for (Operation operation : api.getOperations()) {
                            for (ResponseMessage respMsg : operation.getResponseMessages()) {
                                // responseModel不为null则说明returnType不为void
                                ModelReference responseModel = respMsg.getResponseModel();
                                if (respMsg.getCode() == HttpStatus.OK.value() && responseModel != null) {
                                    setDeclaredField(respMsg, "responseModel", new ModelRef(respModel.getName(),
                                            responseModel.isCollection() ? new ModelRef(respModel.getName()) : null));
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

    private ResolvedType generateModel(Map<String, Model> models, String name, Object obj) {
        ResolvedType resolvedType;
        TypeResolver resolver = new TypeResolver();
        if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            Map<String, ModelProperty> properties = new HashMap<>(jsonObject.size());
            jsonObject.forEach((key, value) -> {
                String[] keys = key.split("//");
                key = keys[0].trim();
                String refName = join(name, key);
                ResolvedType type = generateModel(models, refName, value);
                boolean isRef = models.containsKey(refName);
                properties.put(key, new ModelPropertyBuilder()
                        .name(key)
                        .type(type)
                        .example(isRef ? null : value)
                        .description(keys.length > 1 ? keys[1].trim() : null)
                        .build()
                        .updateModelRef(t -> Types.isBaseType(type)
                                ? new ModelRef(Types.typeNameFor(type.getErasedType()))
                                : new ModelRef(refName, type.isInstanceOf(Collection.class)
                                ? new ModelRef(isRef ? refName : Types.typeNameFor(type.getTypeParameters().get(0).getErasedType()))
                                : null)));
            });
            models.put(name, new ModelBuilder()
                    .name(name)
                    // 这里resolvedType不能为baseType，否则返回给上级递归的updateModelRef将会识别为基础类型
                    .type(resolvedType = resolver.resolve(this.getClass()))
                    .properties(properties)
                    .build());
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            resolvedType = resolver.resolve(Collection.class,
                    generateModel(models, name, jsonArray.isEmpty() ? null : jsonArray.get(0)));
        } else {
            // resolvedType为Void时会被忽略
            resolvedType = resolver.resolve(obj == null ? Void.class : obj.getClass());
        }
        return resolvedType;
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
