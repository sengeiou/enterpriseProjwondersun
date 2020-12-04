package com.proj.service;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.proj.dto.ProjStoreChainInfoDTO;

import java.util.List;
import java.util.Map;

public interface ProjStoreChainInfoService {

    APIResult<String> add(ProjStoreChainInfoDTO dto) throws Exception;

    APIResult<String> edit(ProjStoreChainInfoDTO dto) throws Exception;

    APIResult<String> setIsEnable(ProjStoreChainInfoDTO dto) throws Exception;

    APIResult<ProjStoreChainInfoDTO> getById(String id) throws Exception;

    APIResult<String> importExcel(List<Map<String, String>> rowList, SysUserDTO sysUserDTO) throws Exception;
}
