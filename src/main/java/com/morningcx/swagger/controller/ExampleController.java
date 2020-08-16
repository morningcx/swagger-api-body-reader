package com.morningcx.swagger.controller;

import com.alibaba.fastjson.JSONObject;
import com.morningcx.swagger.plugins.ApiRequestBody;
import com.morningcx.swagger.plugins.ApiResponseBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ExampleController
 *
 * @author MorningStar
 * @date 2020/8/16
 */
@RestController
@Api(tags = "api-body-reader示例")
public class ExampleController {

    @PostMapping("example")
    @ApiOperation("接口示例")
    @ApiRequestBody("{" +
            "	'string // 字符串':'string'," +
            "	'int // 整型':123456," +
            "	'double // 浮点型':123.456," +
            "	'boolean // 布朗型':true," +
            "	'intArray // 整型数组':[" +
            "		1," +
            "		2," +
            "		3" +
            "	]," +
            "	'stringArray // 字符串数组':[" +
            "		'string1'," +
            "		'string2'," +
            "		'string3'" +
            "	]," +
            "	'object // 对象类型':{" +
            "		'string // 字符串':'string'," +
            "		'int // 整型':123456," +
            "		'double // 浮点型':123.456," +
            "		'boolean // 布朗型':true," +
            "		'intArray // 整型数组':[" +
            "			1," +
            "			2," +
            "			3" +
            "		]," +
            "		'stringArray // 字符串数组':[" +
            "			'string1'," +
            "			'string2'," +
            "			'string3'" +
            "		]," +
            "		'object // 对象类型':{" +
            "			'key':'value'" +
            "		}" +
            "	}," +
            "	'objectArray // 对象数组':[" +
            "		{" +
            "			'string // 字符串':'string'," +
            "			'int // 整型':123456," +
            "			'double // 浮点型':123.456," +
            "			'boolean // 布朗型':true," +
            "			'intArray // 整型数组':[" +
            "				1," +
            "				2," +
            "				3" +
            "			]," +
            "			'stringArray // 字符串数组':[" +
            "				'string1'," +
            "				'string2'," +
            "				'string3'" +
            "			]," +
            "			'objectArray // 对象数组':[" +
            "				{" +
            "					'key':'value'" +
            "				}" +
            "			]" +
            "		}" +
            "	]," +
            "	'specialFields // 特殊字段':{" +
            "		'noDescription':'不加//则该字段没有注释'," +
            "		'nullField // 字段为null，则忽略，则忽略':null," +
            "		'nullArray // 数组为空或第一个元素为null，则忽略':[]," +
            "		'diffArray // 各元素都不同的数组只取第一个进行解析，如果为基础类型则直接返回example':[" +
            "			1," +
            "			'1'," +
            "			true," +
            "			{}," +
            "			[]" +
            "		]" +
            "	}" +
            "}")
    @ApiResponseBody("{" +
            "	'code // 响应代码':200," +
            "	'error // 接口调用失败后返回的错误信息':'操作失败：xxxxxx！'," +
            "	'data // 数据结果':{" +
            "		'key':'value'" +
            "	}," +
            "	'msg // 接口调用成功后返回的信息':'操作成功！'," +
            "	'success // 是否成功':true" +
            "}")
    public JSONObject example(@RequestBody JSONObject jsonObject) {
        return jsonObject;
    }
}
