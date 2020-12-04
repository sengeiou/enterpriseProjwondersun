package com.proj.web.api;

import com.proj.service.PushRetailerToMcJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;

@Controller
@RequestMapping("/api/proj/invokeMc")
public class ProjInvokeMcApi {

    private static Logger logger = LoggerFactory.getLogger(ProjInvokeMcApi.class);

    @Resource
    private PushRetailerToMcJobService pushRetailerToMcJobService;


    @RequestMapping(value = "pushStore")
    @ResponseBody
    public Object pushStore() {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushStore();
        String msg = (String) resMap.get("msg");
        logger.info("门店-基础数据-接口对接(美驰)-PushStoreToMcJob-" + msg);
        return  resMap;
    }

    @RequestMapping(value = "pushDealer")
    @ResponseBody
    public Object pushDealer() {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushDealer();
        String msg = (String) resMap.get("msg");
        logger.info("经销商-基础数据-接口对接(美驰)-PushDealerToMcJob-" + msg);
        return  resMap;
    }

    @RequestMapping(value = "pushDistributor")
    @ResponseBody
    public Object pushDistributor() {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushDistributor();
        String msg = (String) resMap.get("msg");
        logger.info("分销商-基础数据-接口对接(美驰)-PushDistributorToMcJob-" + msg);
        return  resMap;
    }

    @RequestMapping(value = "pushWarehouse")
    @ResponseBody
    public Object pushWarehouse() {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushWarehouse();
        String msg = (String) resMap.get("msg");
        logger.info("仓库-基础数据-接口对接(美驰)-PushWarehouseToMcJob-" + msg);
        return  resMap;
    }

}
