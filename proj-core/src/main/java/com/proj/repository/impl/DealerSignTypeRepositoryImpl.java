package com.proj.repository.impl;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.entity.DealerSignType;
import com.proj.repository.DealerSignTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DealerSignTypeRepositoryImpl extends BaseHibernateRepository<DealerSignType> implements DealerSignTypeRepository {

    @Override
    public List<DealerSignType> getList() {
        String hql = "from DealerSignType";
        HashMap<String, Object> kvList = new HashMap<>();
        return super.findEntityObjects(hql, kvList);
    }

    @Override
    public Page<Map> getPageListBySql(PageReq pageReq) {
        Pageable pageable = new PageRequest(pageReq.getPage() - 1, pageReq.getRows());
        HashMap<String, Object> kvList = new HashMap<>();
        String hql =
                "SELECT\n" +
                        "signType.Id as ID,\n" +
                        "signType.DealerId as DEALERID,\n" +
                        "dealer.ShortName as DEALERNAME,\n" +
                        "dealer.Code as DEALERCODE,\n" +
                        "signType.SignType as SIGNTYPE,\n" +
                        "signType.AddBy as ADDBY,\n" +
                        "signType.AddTime as ADDTIME,\n" +
                        "signType.EditBy as EDITBY,\n" +
                        "CASE signType.InUse WHEN 0 THEN '禁用' ELSE '启用' END AS INUSENAME,\n" +
                        "signType.EditTime as EDITTIME,\n" +
                        "signType.Remark as REMARK\n" +
                        "FROM projDealerSignType signType\n" +
                        "LEFT JOIN sysOrganization dealer ON signType.DealerId = dealer.Id\n" +
                        "WHERE 1=1\n";
        hql = buildSql(kvList, pageReq.getConditionItems(), hql);
        hql += " ORDER BY signType.DealerId ";
        return super.findResultMapsSql(hql, kvList, pageable);
    }

    @Override
    public List<DealerSignType> getListByDealerId(String dealerId) {
        String hql = "from DealerSignType where inUse=1 and  dealerId=:dealerId";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("dealerId", dealerId);
        return super.findEntityObjects(hql, kvList);
    }

}
