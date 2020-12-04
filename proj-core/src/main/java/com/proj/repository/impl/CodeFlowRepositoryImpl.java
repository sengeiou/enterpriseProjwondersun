package com.proj.repository.impl;

import com.eaf.core.repository.jpa.BaseHibernateRepository;
import com.eaf.core.utils.StringUtil;
import com.ebp.entity.Organization;
import com.ebt.entity.BillHeader;
import com.ebt.entity.BillType;
import com.proj.repository.CodeFlowRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条码流向
 */
@Repository
public class CodeFlowRepositoryImpl extends BaseHibernateRepository implements CodeFlowRepository {
    @Override
    public List<Organization> getAllOrg(){
        String hql = "select * from sysOrganization";
        return findAllResutlBeansBySql(hql,(Map)null,Organization.class);
    }
    @Override
    public List<BillHeader> getAllBillHeader(){
        String hql = "select * from ebtBillHeader";
        HashMap kvList = new HashMap();
        return findAllResutlBeansBySql(hql,(Map)null,BillHeader.class);
    }

    @Override
    public List<BillType> getAllBillType(){
        String hql = "select * from ebtBillType";
        HashMap kvList = new HashMap();
        return findAllResutlBeansBySql(hql,(Map)null,BillType.class);
    }

    @Override
    public List<Organization> getSrcOrgByBillIdList(List<String> billIdList) {
        if(billIdList==null || billIdList.size()==0){
            return null;
        }
        String sql = "select org.Id,org.ShortName,org.FullName,bill.Id as Attr1 from ebtBillHeader bill " +
                " left join sysOrganization org " +
                " on bill.SrcId = org.Id " +
                " where 1=1  ";
        HashMap kvList = new HashMap();
        String condition = null;
        for (String billId:billIdList
             ) {
            if(StringUtil.isNotEmpty(billId)){
                if(condition==null){
                    condition = " AND (bill.Id ='"+billId+"'";
                }else{
                    condition += " or bill.Id ='"+billId+"'";
                }
            }
        }
        if(condition!=null){
            sql += condition +")";
        }
        return findAllResutlBeansBySql(sql,kvList,Organization.class);
    }
}
