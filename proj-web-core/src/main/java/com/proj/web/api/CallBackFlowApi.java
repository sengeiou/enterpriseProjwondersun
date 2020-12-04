package com.proj.web.api;

import com.alibaba.fastjson.JSONObject;
import com.eaf.core.dto.APIResult;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.CallBackFlowAppService;
import com.proj.appservice.impl.CallBackFlowAppServiceImpl;
import com.proj.dto.CallBackFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping({"api/proj/callbackflow"})
public class CallBackFlowApi extends BaseController {
    @Resource
    CallBackFlowAppService appService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<CallBackFlow> getByCode(@RequestParam(value = "code") String code, @RequestParam(value = "sku") String sku) {
        Logger logger = LoggerFactory.getLogger(CallBackFlowAppServiceImpl.class);
        logger.info("召回查询--api开始调用，code" + code + "sku" + sku);
        APIResult<CallBackFlow> result = appService.getNewSyncCodeData(code, sku);
        logger.info("召回查询--api开始调用，code" + code + "sku" + sku);
        return result;
    }

    @RequestMapping(value = "buildTree", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<JSONObject> buildTree(String batchCode, String materialId, String orgCode, int type) {
        if (type == 1) {//库存
            return appService.getDealerInventoryList(batchCode, materialId, orgCode);
        } else if (type == 2) {
            return appService.getCallBackFromDealerList(batchCode, materialId, orgCode);
        } else {
            return appService.getAllDealerList(batchCode, materialId, orgCode);
        }
    }
}
