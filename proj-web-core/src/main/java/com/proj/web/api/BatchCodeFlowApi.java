package com.proj.web.api;

import com.alibaba.fastjson.JSONObject;
import com.eaf.core.dto.APIResult;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.BatchCodeFlowAppService;
import com.proj.dto.CodeFlow;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2016/6/13.
 */
@Controller
@RequestMapping({"api/proj/batchcodeflow"})
public class BatchCodeFlowApi extends BaseController {
    @Resource
    BatchCodeFlowAppService appService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<CodeFlow> getByCode(@RequestParam(value = "code") String code, @RequestParam(value = "sku") String sku) {
        return appService.getSyncCodeData(code, sku);
    }

    @RequestMapping(value = "buildTree", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<JSONObject> buildTree(String[] codeStr,String billCode) {
        return appService.buildTree(codeStr,billCode);
    }
}
