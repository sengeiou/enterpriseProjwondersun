package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebp.entity.Organization;

import java.util.List;

/**
 * Created by ehsure on 2017/12/20.
 * 组织repo
 */
public interface ProjOrganizationRepository extends BaseCrudRepository<Organization> {
    List<Organization> getListByCodeAndWithOutId(String code,String id);
}
