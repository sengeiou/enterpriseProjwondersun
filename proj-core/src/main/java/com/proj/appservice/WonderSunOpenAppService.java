package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.proj.dto.ProductionCodeQueryDTO;

/**
 * Created by arlenChen on 2016/12/23.
 * 对外数据
 *
 * @author arlenChen
 */
public interface WonderSunOpenAppService {
    /**
     * 根据单品码查询基础信息
     *
     * @param qrId 单品码
     * @return 返回结果
     */
    APIResult<ProductionCodeQueryDTO> getProductionCodeByQRId(String qrId);

    /**
     * 新增单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    APIResult<String> createBill(String billData);
    /**
     * 新增单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    APIResult<Object> createRecycleBill(String billData);
    /**
     * 新增门店调拨单单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    APIResult<Object> createStoreAdjustBill(String billData);

    /**
     * 取消单据
     *
     * @param billData 单据
     * @return 取消结果
     */
    APIResult<String> cancelBill(String billData);

    /**
     * 同步产品到美驰中间表
     */
    APIResult<String> syncMaterialToMidTable();

    /**
     * 同步地理城市到美驰中间表
     */
    APIResult<String> syncGeoCityToMidTable();

    /**
     * 同步考核城市到美驰中间表
     */
    APIResult<String> syncCheckCityToMidTable();
}
