package com.proj.appservice;

import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.CallBackFlow;

/**
 * 条码流向查询服务接口
 */
public interface CallBackFlowAppService {
    /**
     * 获得条码流向信息
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    APIResult<CallBackFlow> getSyncCodeData(String batchCode, String materialId);
    APIResult<CallBackFlow> getNewSyncCodeData(String batchCode, String materialId);

    /**
     * 经销商库存箱集合
     *
     * @param batchCode
     * @param sku
     * @param orgCode
     * @return
     */
    APIResult<JSONObject> getDealerInventoryList(String batchCode, String sku, String orgCode);

    APIResult<JSONObject> getCallBackFromDealerList(String batchCode, String sku, String orgCode);
    APIResult<JSONObject> getAllDealerList(String batchCode, String sku, String orgCode);

}
