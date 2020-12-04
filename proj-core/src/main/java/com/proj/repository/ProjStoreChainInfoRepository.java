package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.proj.entity.ProjStoreChainInfo;

import java.util.List;

/**
 * 门店连锁信息DAO层
 * NEIL
 */
public interface ProjStoreChainInfoRepository extends BaseCrudRepository<ProjStoreChainInfo> {

    List<ProjStoreChainInfo> getByCode(String code, int isEnable) throws Exception;

    List<ProjStoreChainInfo> getByCode(String code) throws Exception;

    List<ProjStoreChainInfo> getByCodeNotId(String code, int isEnable, String id) throws Exception;

    List<ProjStoreChainInfo> getByCodeNotId(String code, String id) throws Exception;

    List<ProjStoreChainInfo> getAll() throws Exception;

    List<ProjStoreChainInfo> getByName(String name) throws Exception;
}
