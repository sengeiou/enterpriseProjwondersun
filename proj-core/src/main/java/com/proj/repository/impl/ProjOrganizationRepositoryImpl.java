package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebp.entity.Organization;
import com.proj.repository.ProjOrganizationRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2017/12/20
 * 组织repo.
 */
@Repository
public class ProjOrganizationRepositoryImpl extends BaseHibernateRepository<Organization> implements ProjOrganizationRepository {
    @Override
    public List<Organization> getListByCodeAndWithOutId(String code, String id) {
        String sql = "select * from sysOrganization where code=:code ";
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        if (StringUtil.isNotEmpty(id)) {
            sql += " and id !=:id ";
            map.put("id", id);
        }
        return findEntityObjectsBySql(sql, map);
    }
}
