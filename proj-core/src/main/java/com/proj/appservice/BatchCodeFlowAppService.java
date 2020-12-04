package com.proj.appservice;

import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.CodeFlow;

/**
 * 条码流向查询服务接口
 */
public interface BatchCodeFlowAppService {
    /**
     * 获得条码流向信息
     * @param code
     * @param materialId
     * @return
     */
    APIResult<CodeFlow> getSyncCodeData(String code,String materialId);
    APIResult<JSONObject> buildTree(String[] codeStr,String billCode);
}
