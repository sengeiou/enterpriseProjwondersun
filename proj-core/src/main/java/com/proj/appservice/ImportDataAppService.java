package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;

import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2016/8/18.
 */
public interface ImportDataAppService {

    APIResult<Map<String,Object>> importDistributor(List<Map<String,String>> rowList, SysUserDTO userDTO,Integer OrgType);
}
