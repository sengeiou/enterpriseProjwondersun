package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebt.dto.STBillHeaderDTO;
import com.arlen.ebt.entity.STBillHeader;

import java.util.List;

/**
 * Created by arlenChen on 2020/4/14
 * 自定义单据
 *
 * @author arlenChen
 */
public interface ProjStBillHeaderRepository extends BaseCrudRepository<STBillHeader> {
    /**
     * 根据主体Id获取对应的盘点单
     *
     * @param orgId 主体Id
     * @return List
     */
    List<STBillHeaderDTO> listByOrgId(String orgId);
}
