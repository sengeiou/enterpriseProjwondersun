package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.CodeFlow;

/**
 * 条码流向查询服务接口
 */
public interface CodeFlowAppService {
    /**
     * 获得条码流向信息
     * @param code
     * @return
     */
    APIResult<CodeFlow> getSyncCodeData(String code);

    APIResult<CodeFlow> getOrgSyncCodeData(String code, String currentOrgId);
}
