package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebc.entity.ProductionCode;

import java.util.List;

/**
 * Created by arlenChen on 2020/4/14.
 * 自定义生产数据
 *
 * @author arlenChen
 */
public interface ProjProductionCodeRepository extends BaseCrudRepository<ProductionCode> {
    /**
     * 根据物料和当前所在地获取生产数据
     *
     * @param materialId 物料
     * @param orgId      当前所在地
     * @return List
     */
    List<ProductionCode> listByMaterialIdAndOrgId(String materialId, String orgId);
}
