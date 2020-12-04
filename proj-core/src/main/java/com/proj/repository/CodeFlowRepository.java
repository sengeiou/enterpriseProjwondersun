package com.proj.repository;

import com.arlen.ebp.entity.Organization;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.BillType;

import java.util.List;

/**
 * 条码流向
 */
public interface CodeFlowRepository {
    /**
     * 获得所有组织Organization集合
     * @return
     */
    List<Organization> getAllOrg();

    /**、
     * 获得所有单据BillHeader集合
     * @return
     */
    List<BillHeader> getAllBillHeader();

    /**
     * 获得所有单据类型集合
     * @return
     */
    List<BillType> getAllBillType();

    /**
     * 根据单据ID集合查询所有组织
     * @param billIdList
     * @return
     */
    List<Organization> getSrcOrgByBillIdList(List<String> billIdList);
}
