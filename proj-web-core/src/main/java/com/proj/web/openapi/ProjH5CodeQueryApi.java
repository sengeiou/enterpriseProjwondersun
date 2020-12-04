package com.proj.web.openapi;

import com.proj.appservice.H5CodeQueryAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 编码信息查询接口
 */
@Controller
@RequestMapping({"openapi/proj/h5CodeQuery"})
public class ProjH5CodeQueryApi {
    @Resource
    private H5CodeQueryAppService appService;

    @RequestMapping(value = "codeQuery")
    @ResponseBody
    public Object query(String code) {
        return appService.queryByCode(code);
    }
}
