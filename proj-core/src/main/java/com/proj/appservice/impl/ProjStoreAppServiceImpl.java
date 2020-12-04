package com.proj.appservice.impl;

import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.proj.appservice.ProjStoreAppService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by ehsure on 2017/8/25.
 * 门店
 */
@Service
public class ProjStoreAppServiceImpl implements ProjStoreAppService {
    @Resource
    private SupplyRelationRepository supplyRelationRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private ProductionCodeRepository codeRepository;

    @Override
    public String checkName(String shortName, String fullName, String id) {
        String msg = "";
        List<SupplyRelation> relations = supplyRelationRepository.getParentList(id);
        for (SupplyRelation supplyRelation : relations) {
            List<SupplyRelation> relationList = supplyRelationRepository.getChildLst(supplyRelation.getParentId());
            for (SupplyRelation relation : relationList) {
                Organization organization = organizationRepository.load(relation.getChildId());
                if (organization != null && !organization.getId().equals(id)) {
                    if (StringUtil.isNotEmpty(organization.getShortName()) && organization.getShortName().equals(shortName)) {
                        msg = "已存在相同名称的门店，不允许保存";
                        break;
                    } else if (StringUtil.isNotEmpty(organization.getFullName()) && organization.getFullName().equals(fullName)) {
                        msg = "已存在相同名称的门店，不允许保存";
                        break;
                    }
                }
            }
        }
        return msg;
    }

    @Override
    public String check(String id) {
        ProductionCode code = codeRepository.load(id);
        if (code != null) {
            return "true";
        } else {
            return "false";
        }
    }
}
