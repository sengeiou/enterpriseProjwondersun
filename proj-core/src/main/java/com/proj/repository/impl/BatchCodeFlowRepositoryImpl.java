package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebc.entity.ProductionCode;
import com.proj.dto.CallBackDTO;
import com.proj.repository.BatchCodeFlowRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 条码流向
 */
@Repository
public class BatchCodeFlowRepositoryImpl extends BaseHibernateRepository implements BatchCodeFlowRepository {
    /**
     * 根据批号和产品sku查询所有编码
     *
     * @param code       批号
     * @param materialId 产品sku
     * @return
     */
    @Override
    public List<ProductionCode> getListByBatchCodeAndMaterialId(String code, String materialId) {
        String hql = "select p.id,p.ParentCode,p.BatchCode,p.materialId,p.RootCode,p.ShouldOrgId,p.ShouldOrgType,p.MinSaleUnit,p.ValidDate,p.PackDate  from ebcProductionCode p with(nolock)  where p.batchCode=:batchCode " +
                " and p.materialId=:materialId  ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", code);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(hql, kvList, ProductionCode.class);
    }

    /**
     * 根据批号查询去重复的当前所在地
     *
     * @param code 批号
     * @return
     */
    @Override
    public List<String> getShouldOrdIdByBatchCode(String code) {
        List<String> list = new ArrayList<>();
        String sql = "select distinct p.shouldOrgId from ebcProductionCode p where p.batchCode=:batchCode and  minSaleUnit=:minSaleUnit ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", code);
        kvList.put("minSaleUnit", Boolean.valueOf(true));
        List<ProductionCode> codeList = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        for (ProductionCode productionCode : codeList) {
            list.add(productionCode.getShouldOrgId());
        }
        return list;
    }

    /**
     * 根据批号和产品id查询编码当前所在地在经销商的编码
     *
     * @param code       批号
     * @param materialId 产品id
     * @return
     */
    @Override
    public List<ProductionCode> getInDealerByBatchAndMaterialId(String code, String materialId) {
        List<ProductionCode> list = new ArrayList<>();
        String sql = "select p.BatchCode,p.MaterialId,p.ValidDate,p.ParentCode,p.PackDate,p.ShouldOrgId,org.OrgType " +
                " from ebcProductionCode p with(nolock)  " +
                " left join  sysOrganization org with(nolock) on org.id=p.ShouldOrgId  " +
                " where p.BatchCode=:code and p.MaterialId=:materialId and p.minSaleUnit=:minSaleUnit and org.OrgType=4 " +
                " group BY p.BatchCode,p.MaterialId,p.ValidDate,p.ParentCode,p.ShouldOrgId,p.PackDate,org.OrgType ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        kvList.put("materialId", materialId);
        kvList.put("minSaleUnit", Boolean.valueOf(true));
        return findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
    }

    @Override
    public int getCountByBatchCodeAndMaterialId(String code, String materialId) {
        String sql = "select p.BatchCode,p.MaterialId,p.ValidDate,p.ParentCode,p.PackDate  " +
                "from ebcProductionCode p with (nolock) " +
                "where p.BatchCode=:code and p.MaterialId=:materialId and p.minSaleUnit=:minSaleUnit  and ParentCode is not null " +
                "group BY p.BatchCode,p.MaterialId,p.ValidDate,p.ParentCode,p.PackDate ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        kvList.put("materialId", materialId);
        kvList.put("minSaleUnit", Boolean.valueOf(true));
        List<ProductionCode> list = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        return list.size();
    }

    /**
     * 根据单据类型获取发货码箱数
     *
     * @param billTypeId 单据类型
     * @return 结果
     */
    @Override
    public int getCodeDataCountByBillTypeId(String billTypeId, String materialId) {
        String sql = " select code.ParentCode " +
                " from  ebtBillData billData with(nolock) " +
                " left join ebtBillHeader billHeader with(nolock) on billData.HeaderId=billHeader.Id  " +
                " left join sysOrganization org with(nolock) on org.id= billHeader.SrcId  " +
                " left join ebtBillDetail detail with(nolock) on detail.headerId=billHeader.id " +
                " left join ebcProductionCode code with(nolock) on billData.productionCodeId=code.id " +
                " where billHeader.BillTypeId=:billTypeId and org.OrgType=4   and detail.materialId=:materialId  " +
                " group by code.ParentCode ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("billTypeId", billTypeId);
        kvList.put("materialId", materialId);
        List<ProductionCode> list = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        return list.size();
    }

    /**
     * 根据单据类型查询在经销商处单据编码
     *
     * @param billTypeId 批号
     * @return
     */
    @Override
    public List<CallBackDTO> getBillDataByBillTypeId(String billTypeId, String batchCode, String materialId) {
        String sql = "select billData.productionCodeId as productionCodeId," +
                "org.Id as orgId   " +
                "from  ebtBillHeader billHeader with(nolock)  " +
                "left join ebtBillDetail detail   with(nolock) on billHeader.id= detail.HeaderId  " +
                "left join ebtBillData billData   with(nolock)  on billData.HeaderId=billHeader.Id " +
                " left join  ebtBillSubDetail subDetail with(nolock)   on subDetail.parentiD=detail.ID " +
                "left join sysOrganization org with(nolock) on org.id= billheader.SrcId  " +
                "where billHeader.BillTypeId=:billTypeId and org.OrgType=4 and subDetail.batchCode=:batchCode and detail.materialId=:materialId  " +
                "group by billData.productionCodeId,org.Id";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("billTypeId", billTypeId);
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(sql, kvList, CallBackDTO.class);
    }

    @Override
    public List<String> getDealerInventoryList(String batchCode, String sku, String orgCode) {
        List<String> list = new ArrayList<>();
        String sql = "select code.ParentCode from ebcProductionCode  code " +
                " left join sysOrganization org on code.ShouldOrgId=org.id " +
                " left join ebdMaterial material on material.id=code.materialId " +
                " where code.batchCode=:batchCode and material.id=:sku and code.minSaleUnit=1 and org.Code=:orgCode  " +
                " group by code.ParentCode";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("sku", sku);
        kvList.put("orgCode", orgCode);
        List<ProductionCode> codeList = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        for (ProductionCode productionCode : codeList) {
            list.add(productionCode.getParentCode());
        }
        return list;
    }

    @Override
    public List<String> getCallBackFromDealerList(String batchCode, String sku, String orgCode) {
        List<String> list = new ArrayList<>();
        String sql = " select code.ParentCode " +
                " from  ebtBillData billData with(nolock)  " +
                " left join ebtBillHeader billHeader with(nolock)  on billData.HeaderId=billHeader.Id " +
                " left join sysOrganization srcOrg with(nolock) on srcOrg.id= billheader.SrcId  " +
                " left join ebtBillDetail detail   with(nolock) on detail.id= billData.detailId  " +
                " left join ebdMaterial material on material.id=detail.materialId " +
                " left join ebcProductionCode code on code.id=billData.productionCodeId " +
                " where billHeader.BillTypeId='RDCCallBack' and srcOrg.code=:orgCode and billData.batchCode=:batchCode and material.id=:sku  " +
                " group by code.ParentCode ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("sku", sku);
        kvList.put("orgCode", orgCode);
        List<ProductionCode> codeList = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        for (ProductionCode productionCode : codeList) {
            list.add(productionCode.getParentCode());
        }
        return list;
    }

    /**
     * 批次总库存数
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    @Override
    public int getTotalProductQty(String batchCode, String materialId) {
        String sql = "select count(distinct ParentCode) as COUNT  from ebcProductionCode  code with(nolock)  where code.MinSaleUnit=1 and batchCode=:batchCode and MaterialId=:materialId and ParentCode is not null ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        List<ProductionCode> dataList = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        return dataList.get(0).getCount() ;
    }

    /**
     * 获取在仓库的数量
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    @Override
    public int getRDCInQty(String batchCode, String materialId) {
        String sql = "select count(distinct ParentCode) as COUNT  from ebcProductionCode  code with(nolock)  " +
                "where code.MinSaleUnit=1 and batchCode=:batchCode  and ShouldOrgType=3 and MaterialId=:materialId and ParentCode is not null ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        List<ProductionCode> dataList = findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
        return dataList.get(0).getCount() ;
    }

    /**
     * 经销商库存数量(未召回的)
     *
     * @param batchCode
     * @param materialId
     * @return getDealerCallBackQty
     */
    @Override
    public List<ProductionCode> getDealerNotCallBackQty(String batchCode, String materialId) {
        String sql = " select ShouldOrgId as ShouldOrgId,count(distinct parentCode) as COUNT from ebcProductionCode  code " +
                " where code.ShouldOrgType=4 and code.MinSaleUnit=1 and batchCode=:batchCode and MaterialId=:materialId  " +
                " group by ShouldOrgId ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
    }

    /**
     * 根据单据算经销商的召回数量
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    @Override
    public List<ProductionCode> getDealerCallBackQty(String batchCode, String materialId) {
        String sql = " select  " +
                " aa.Id as ShouldOrgId,count(1) AS COUNT " +
                " from ( " +
                " select   org.Id ,code.parentCode " +
                " from  ebtBillHeader billHeader with(noLock)   " +
                " inner join ebtBillData billData   with(noLock)  on billData.HeaderId=billHeader.Id  " +
                " inner join sysOrganization org with(noLock) on org.id= billHeader.SrcId   " +
                " inner join ebcProductionCode code with(noLock) on code.id=billData.productionCodeId " +
                " where billHeader.BillTypeId='RDCCallBack' and org.OrgType=4 and code.batchCode=:batchCode and code.materialId=:materialId   " +
                " group by org.Id,code.parentCode " +
                " )aa " +
                " group by aa.Id ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findAllResutlBeansBySql(sql, kvList, ProductionCode.class);
    }
}
