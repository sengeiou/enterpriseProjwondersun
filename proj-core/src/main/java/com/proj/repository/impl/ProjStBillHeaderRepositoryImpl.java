package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebt.dto.STBillHeaderDTO;
import com.arlen.ebt.entity.STBillHeader;
import com.proj.repository.ProjStBillHeaderRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * Created by arlenChen on 2019/07/19
 * 自定义单据
 *
 * @author arlenChen
 */
@Repository
public class ProjStBillHeaderRepositoryImpl extends BaseHibernateRepository<STBillHeader> implements ProjStBillHeaderRepository {
    /**
     * 根据主体Id获取对应的盘点单
     *
     * @param orgId 主体Id
     * @return List
     */
    @Override
    public List<STBillHeaderDTO> listByOrgId(String orgId) {
        String sql = "select * from ebtSTBillHeader where  orgId =:orgId  and  billStatus  not in (7,8) order by operateTime desc  ";
        HashMap<String, Object> kvList = new HashMap<>(1);
        kvList.put("orgId", orgId);
        return this.findAllResutlBeansBySql(sql, kvList, STBillHeaderDTO.class);
    }
}
