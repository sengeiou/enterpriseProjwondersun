package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebt.dto.BillDetailDTO;
import com.arlen.ebt.dto.BillHeaderDTO;
import com.arlen.ebt.entity.BillHeader;
import com.proj.repository.ProjBillHeaderRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by arlenChen on 2019/07/19
 * 自定义单据
 *
 * @author arlenChen
 */
@Repository
public class ProjBillHeaderRepositoryImpl extends BaseHibernateRepository<BillHeader> implements ProjBillHeaderRepository {

    /**
     * 获取编码不存在异常的单据
     *
     * @return 单据
     */
    @Override
    public List<BillHeader> noCodeBillList() {
        String sql = " select * from ebtBillHeader  with(noLock)  where billStatus=5 and  exceptionQtyPcs>0  and attr1 is null  and billTypeId in ('RDCToDealerOut','RDCToRDCOut') ";
        Map<String, Object> map = new HashMap<>(0);
        return findEntityObjectsBySql(sql, map);
    }

    /**
     * 根据双方组织和单号获取数据
     *
     * @param srcCode  发货方
     * @param destCode 收货方
     * @param code     单号
     * @return 单据
     */
    @Override
    public List<BillHeader> getListByOrgAndCode(String srcCode, String destCode, String code) {
        String sql = " select * from ebtBillHeader  with(noLock)  " +
                " where code=:code " +
                " and srcId in (select id from sysOrganization with(noLock) where code=:srcCode) " +
                " and destId in (select id from sysOrganization with(noLock) where code=:destCode) ";
        Map<String, Object> map = new HashMap<>(3);
        map.put("srcCode", srcCode);
        map.put("destCode", destCode);
        map.put("code", code);
        return findEntityObjectsBySql(sql, map);
    }

    /**
     * 根据双方组织和扫码获取数据
     *
     * @param srcId    发货方
     * @param destId   收货方
     * @param scanCode 扫码
     * @return 单据
     */
    @Override
    public List<BillHeader> getListByOrgAndScanCode(String srcId, String destId, String scanCode) {
        String sql = " select billHeader.* from " +
                " ebtBillHeader   billHeader with(noLock)" +
                " left join  ebtBillData billData with(noLock) on billData.headerId=billHeader.Id " +
                " where billData.productionCodeId=:scanCode " +
                " and srcId =:srcId " +
                " and destId=:destId ";
        Map<String, Object> map = new HashMap<>(3);
        map.put("srcId", srcId);
        map.put("destId", destId);
        map.put("scanCode", scanCode);
        return findEntityObjectsBySql(sql, map);
    }

    /**
     * 生成明细
     *
     * @param orgId 经销商
     * @return BillDetail
     */
    @Override
    public List<BillDetailDTO> generateBillDetailList(String orgId) {
        String sql = "select count(1) as expectQtyPcs,materialId " +
                " from ebcProductionCode  " +
                " where ShouldOrgId =:orgId and MinSaleUnit = 1 and IsRunning = 0  group by MaterialId ";
        HashMap<String, Object> kvList = new HashMap<>(1);
        kvList.put("orgId", orgId);
        return this.findAllResutlBeansBySql(sql, kvList, BillDetailDTO.class);
    }

    /**
     * 根据主体Id获取对应的出库单
     *
     * @param orgId 主体Id
     * @return List
     */
    @Override
    public List<BillHeaderDTO> listByOrgId(String orgId) {
        String sql = "select * from ebtBillHeader where (srcId =:orgId or destId =:orgId) and  billStatus in (1,2,3,4) order by operateTime desc  ";
        HashMap<String, Object> kvList = new HashMap<>(1);
        kvList.put("orgId", orgId);
        return this.findAllResutlBeansBySql(sql, kvList, BillHeaderDTO.class);
    }

    /**
     * 根据双方组织和单据类型获取数据
     *
     * @param srcId      发货方
     * @param destId     收货方
     * @param billTypeId 单据类型
     * @return List
     */
    @Override
    public List<BillHeader> getListByOrg(String srcId, String destId, String billTypeId) {
        String sql = "select * from ebtBillHeader where srcId =:srcId  and  destId =:destId and  billStatus =5 and billTypeId=:billTypeId  order by operateTime desc  ";
        HashMap<String, Object> kvList = new HashMap<>(1);
        kvList.put("srcId", srcId);
        kvList.put("destId", destId);
        kvList.put("billTypeId", billTypeId);
        return this.findAllResutlBeansBySql(sql, kvList, BillHeader.class);
    }
}
