package com.proj.service;

import com.arlen.eaf.core.dto.APIResult;
import com.ebp.dto.SysUserDTO;

import java.util.List;
import java.util.Map;

public interface ImportStoreEditService {

    APIResult<String> importExcel(List<Map<String, String>> rowList, SysUserDTO sysUserDTO) throws Exception;
}
