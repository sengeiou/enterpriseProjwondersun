package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;

/**
 * Created by ehsure on 2018/4/17.
 * 同步数据到力博
 */
public interface SyncDataTOLibodAppService {

    /**
     * 同步单品码信息
     *
     * @return 处理结果
     */
    APIResult<String> syncProductionCode() throws Exception;

    /**
     * 同步系列信息
     *
     * @return 处理结果
     */
    APIResult<String> syncSeries() throws Exception;

    /**
     * 同步产品信息信息
     *
     * @return 处理结果
     */
    APIResult<String> syncMaterial() throws Exception;
}
