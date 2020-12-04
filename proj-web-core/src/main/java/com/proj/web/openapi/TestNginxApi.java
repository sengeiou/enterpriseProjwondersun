package com.proj.web.openapi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestNginxApi {


    @RequestMapping(value = "/openapi/WechatWeb/Activity/Point")
    @ResponseBody
    public String testUrl(String code){
        return code;
    }
}
