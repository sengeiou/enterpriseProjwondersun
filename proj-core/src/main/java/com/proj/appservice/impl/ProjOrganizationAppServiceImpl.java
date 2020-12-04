package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.ebp.appservice.impl.OrganizationAppServiceImpl;
import com.arlen.ebp.dto.OrganizationDTO;
import com.arlen.ebp.repository.OrganizationRepository;
import com.proj.repository.ProjOrganizationRepository;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2017/12/20.
 * 组织服务
 */
public class ProjOrganizationAppServiceImpl extends OrganizationAppServiceImpl {

    @Resource
    private OrganizationRepository repository;

    @Resource
    private ProjOrganizationRepository projOrganizationRepository;

    @Override
    public APIResult<String> create(OrganizationDTO organizationDTO) {
        APIResult<String> result = new APIResult<>();
        if (repository.getByCode(organizationDTO.getCode()) != null) {
            return result.fail(APIResultCode.ALREADY_EXSISTED, "代码已存在");
        } else {
            return super.create(organizationDTO);
        }
    }

    @Override
    public APIResult<String> update(OrganizationDTO organizationDTO) {
        APIResult<String> result = new APIResult<>();
        if (projOrganizationRepository.getListByCodeAndWithOutId(organizationDTO.getCode(), organizationDTO.getId()).size() > 0) {
            return result.fail(APIResultCode.ALREADY_EXSISTED, "代码已存在");
        } else {
            return super.update(organizationDTO);
        }
    }
}
