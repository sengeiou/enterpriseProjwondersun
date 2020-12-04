package com.proj.repository;

import com.arlen.ebc.entity.ImportCodeRecord;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebd.entity.Material;

import java.util.List;

/**
 * Created by ehsure on 2018/4/17.
 * 数据获取
 */
public interface SyncDataToLibodRepository {

    /**
     * 获取要同步的批次信息-力博数据
     *
     * @param num 数量
     * @return return
     */
    List<ProductionCode> getSyncProductionCode(int num);

    /**
     * 根据批号和产品获取生产数据-力博数据
     *
     * @param batchCode  批号
     * @param materialId 产品ID
     * @return return
     */
    List<ProductionCode> getListByBatchCodeAndMaterialId(String batchCode, String materialId);

    /**
     * 获取同批次，同产品的生产记录 -力博数据
     *
     * @param batchCode  批次
     * @param materialId 产品
     * @return return
     */
    List<ImportCodeRecord> getImportCodeRecordList(String batchCode, String materialId);

    /**
     * 获取力博的产品信息
     *
     * @return return
     */
    List<Material> getSyncList();
}
