package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebc.entity.ProductionCode;
import com.proj.repository.ProjProductionCodeRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * Created by arlenChen on 2020/4/14.
 * 自定义生产数据
 *
 * @author arlenChen
 */
@Repository
public class ProjProductionCodeRepositoryImpl extends BaseHibernateRepository<ProductionCode> implements ProjProductionCodeRepository {
    /**
     * 根据物料和当前所在地获取生产数据
     *
     * @param materialId 物料
     * @param orgId      当前所在地
     * @return List
     */
    @Override
    public List<ProductionCode> listByMaterialIdAndOrgId(String materialId, String orgId) {
        String sql = "select * from ebcProductionCode where ShouldOrgId =:orgId  and materialId =:materialId and MinSaleUnit = 1 and IsRunning = 0 ";
        HashMap<String, Object> kvList = new HashMap<>(1);
        kvList.put("materialId", materialId);
        kvList.put("orgId", orgId);
        return this.findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
    }
}
