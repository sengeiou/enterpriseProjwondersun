package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebc.entity.ImportCodeRecord;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebd.entity.Material;
import com.proj.repository.SyncDataToLibodRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2018/4/17.
 * 数据获取
 */
@Repository
public class SyncDataToLibodRepositoryImpl extends BaseHibernateRepository implements SyncDataToLibodRepository {
    /**
     * 获取要同步的批次信息-力博数据
     *
     * @return return
     */
    @Override
    public List<ProductionCode> getSyncProductionCode(int num) {
        String sql =  "select top "+num+" code.* from ebcProductionCode code with(noLock) " +
                " left join ebdMaterial  material with(noLock) on (material.id=code.MaterialId and material.materialType=3)  " +
                " where  (code.attr2 is null or code.attr2 like '%failed;%')  and material.EXT11='1'  and batchCode not in (select  batchCode from ebcImportCodeRecord record with(noLock)  " +
                " inner join ebdMaterial material with(noLock) on (material.sku=record.MaterialId and material.materialType=3 )  " +
                " where  recordStatus=2 and (record.attr2 is null or record.attr2 like '%failed;%') and material.EXT11='1'  " +
                " group by  batchCode )  " +
                " order by code.addTime  asc ";
        return findAllResutlBeansBySql(sql, (Map) null, ProductionCode.class);
    }


    /**
     * 根据批号和产品获取生产数据-力博数据
     *
     * @param batchCode  批号
     * @param materialId 产品ID
     * @return return
     */
    @Override
    public List<ProductionCode> getListByBatchCodeAndMaterialId(String batchCode, String materialId) {
        String hql = "select * from ebcProductionCode  with(noLock)  where batchCode=:batchCode " +
                " and materialId=:materialId  ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(hql, kvList, ProductionCode.class);
    }

    /**
     * 获取同批次，同产品的生产记录-力博数据
     *
     * @param batchCode  批次
     * @param materialId 产品
     * @return return
     */
    @Override
    public List<ImportCodeRecord> getImportCodeRecordList(String batchCode, String materialId) {
        String sql = " from ebcImportCodeRecord with(noLock)  where batchCode=:batchCode and materialId=:materialId";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
    }

    /**
     * 获取力博的产品信息
     *
     * @return return
     */
    @Override
    public List<Material> getSyncList() {
        String sql = "select * from ebdMaterial  with(noLock) where EXT11='1'  and ( EXT12 is null or EXT12  like '%failed;%')";
        return findAllResutlBeansBySql(sql, new HashMap(), Material.class);
    }
}
