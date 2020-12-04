package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.entity.ProjStoreChainInfo;
import com.proj.repository.ProjStoreChainInfoRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * 门店连锁信息DAO层实现类
 * NEIL
 */
@Repository
public class ProjStoreChainInfoRepositoryImpl extends BaseHibernateRepository<ProjStoreChainInfo> implements ProjStoreChainInfoRepository {

    @Override
    public List<ProjStoreChainInfo> getByCode(String code, int isEnable) throws Exception {
        String hql = "FROM ProjStoreChainInfo WHERE Code =:code AND IsEnable =:isEnable";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        kvList.put("isEnable", isEnable);
        return super.findEntityObjects(hql,kvList);
    }

    @Override
    public List<ProjStoreChainInfo> getByCode(String code) throws Exception {
        String hql = "FROM ProjStoreChainInfo WHERE Code =:code";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        return super.findEntityObjects(hql,kvList);
    }

    @Override
    public List<ProjStoreChainInfo> getByName(String name) throws Exception {
        String hql = "FROM ProjStoreChainInfo WHERE Name =:name";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("name", name);
        return super.findEntityObjects(hql,kvList);
    }

    @Override
    public List<ProjStoreChainInfo> getByCodeNotId(String code, int isEnable, String id) throws Exception {
        String hql = "FROM ProjStoreChainInfo WHERE Code =:code AND IsEnable =:isEnable AND Id != :id";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        kvList.put("isEnable", isEnable);
        kvList.put("id", id);
        return super.findEntityObjects(hql,kvList);
    }

    @Override
    public List<ProjStoreChainInfo> getByCodeNotId(String code, String id) throws Exception {
        String hql = "FROM ProjStoreChainInfo WHERE Code =:code  AND Id != :id";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("code", code);
        kvList.put("id", id);
        return super.findEntityObjects(hql,kvList);
    }

    @Override
    public List<ProjStoreChainInfo> getAll() throws Exception {
        String hql = "FROM ProjStoreChainInfo";
        HashMap<String, Object> kvList = new HashMap<>();
        return super.findEntityObjects(hql,kvList);
    }
}
