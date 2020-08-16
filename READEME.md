# Swagger Api Body Reader

很多开发者都会喜欢传递动态的Map或者JSONObject作为入参接收对象，出参作为返回对象，这样能以JSON的方式对入参出参进行扩展，方便业务功能的扩展开发。

但是，如果使用以上的方式，我们用Swagger框架来生成文档时，Swagger并不知道我们需要传递那些字段属性，因为它是声明式的。

只有在定义了相关类或者参数注释说明后，Swagger才可以帮助我们生成文档。

为了解决以上动态类型字段没有注释的问题，ApiBodyReader实现了Swagger的ApiListingBuilderPlugin接口，利用自定义注解@ApiRequestBody、@ApiResponseBody获取接口参数信息，以帮助我们完成对动态类型字段的注释说明。

## 使用

com.morningcx.swagger.plugins包下的所有类拷贝至项目即可使用

接口示例详见com.morningcx.swagger.controller.ExampleController

注解生成器详见com.morningcx.swagger.ApiBodyAnnotationGenerator

## 注意事项

1. 需要FastJson依赖
2. 仅支持Swagger2.x，最新Swagger3.x某些类存在变动，暂不兼容




待完善...





