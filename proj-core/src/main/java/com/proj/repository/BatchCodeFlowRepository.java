package com.proj.repository;

import com.arlen.ebc.entity.ProductionCode;
import com.proj.dto.CallBackDTO;

import java.util.List;

/**
 * 条码流向
 */
public interface BatchCodeFlowRepository {

    /**
     * 根据批号和产品id查询所有编码
     *
     * @param code       批号
     * @param materialId 产品id
     * @return
     */
    List<ProductionCode> getListByBatchCodeAndMaterialId(String code, String materialId);

    /**
     * 根据批号查询去重复的当前所在地
     *
     * @param code 批号
     * @return
     */
    List<String> getShouldOrdIdByBatchCode(String code);

    /**
     * 根据批号和产品id查询编码当前所在地在经销商的编码
     *
     * @param code       批号
     * @param materialId 产品id
     * @return
     */
    List<ProductionCode> getInDealerByBatchAndMaterialId(String code, String materialId);

    /**
     * 根据批号和产品编码获取该批次和该产品的生产数量
     *
     * @param code       批号
     * @param materialId 产品id
     * @return 结果
     */
    int getCountByBatchCodeAndMaterialId(String code, String materialId);

    /**
     * 根据单据类型获取发货码箱数
     *
     * @param billTypeId 单据类型
     * @return 结果
     */
    int getCodeDataCountByBillTypeId(String billTypeId, String materialId);

    /**
     * 根据单据类型查询单据编码
     *
     * @param billTypeId 批号
     * @return
     */
    List<CallBackDTO> getBillDataByBillTypeId(String billTypeId, String batchCode, String materialId);

    List<String> getDealerInventoryList(String batchCode, String sku, String orgCode);

    List<String> getCallBackFromDealerList(String batchCode, String sku, String orgCode);

    /**
     * 批次总库存数
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    int getTotalProductQty(String batchCode, String materialId);

    /**
     * 获取在仓库的数量
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    int getRDCInQty(String batchCode, String materialId);

    /**
     * 根据单据算经销商的召回数量
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    List<ProductionCode> getDealerCallBackQty(String batchCode, String materialId);

    /**
     * 经销商库存数量(未召回的)
     *
     * @param batchCode
     * @param materialId
     * @return getDealerCallBackQty
     */
    List<ProductionCode> getDealerNotCallBackQty(String batchCode, String materialId);
}
