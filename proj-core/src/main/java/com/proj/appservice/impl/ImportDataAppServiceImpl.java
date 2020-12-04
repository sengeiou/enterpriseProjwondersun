package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.entity.SysStructure;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.arlen.ebp.repository.SysStructureRepository;
import com.proj.appservice.ImportDataAppService;
import com.proj.repository.ImportDataRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by ehsure on 2016/8/18.
 */
@Service
public class ImportDataAppServiceImpl implements ImportDataAppService {

    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private SupplyRelationRepository supplyRelationRepository;
    @Resource
    private SysStructureRepository sysStructureRepository;
    @Resource
    private ImportDataRepository importDataRepository;
    @Override
    public APIResult<Map<String, Object>> importDistributor(List<Map<String, String>> rowList, SysUserDTO userDTO,Integer orgType) {
        APIResult<Map<String,Object>> result = new APIResult<>();
        List<Organization> newOrgList = new ArrayList<>();
        List<SupplyRelation> newRelationList = new ArrayList<>();
        List<String> checkRepeatList = new ArrayList<>();
        for (Map<String,String> rowMap:rowList
             ) {
            String fullName = rowMap.get("fullName");
            if(StringUtil.isEmpty(fullName)){
                continue;
            }
            String code = rowMap.get("code");
            String shortName = rowMap.get("shortName");
            String index = rowMap.get("index");
            String inUse = rowMap.get("inUse");
            String parentCode = rowMap.get("parentCode");
            String area = rowMap.get("area");
            String bigArea = rowMap.get("bigArea");
            String checkCity = rowMap.get("checkCity");
            if(checkRepeatList.contains(code+","+fullName+","+shortName+","+checkCity)){
                return result.fail(500,"组织重复(第"+index+"行)");
            }else{
                checkRepeatList.add(code+","+fullName+","+shortName+","+checkCity);
            }
            Organization org = new Organization();
            org.setCode(rowMap.get("code"));
            org.setOrgType(orgType);
            org.setFullName(fullName);
            if(StringUtil.isEmpty(shortName)){
                org.setShortName(fullName);
            }else{
                org.setShortName(rowMap.get("shortName"));
            }

            org.setInUse(true);
            org.setAddTime(new Date());
            org.setEditTime(new Date());
            org.setAttr5(rowMap.get("checkCity"));
            org.setAttr6("excel");
            if(StringUtil.isNotEmpty(inUse)){
                if("否".equals(inUse)){
                    org.setInUse(false);
                }
                if("无效".equals(inUse)){
                    org.setInUse(false);
                }
                if("无".equals(inUse)){
                    org.setInUse(false);
                }
            }
            SysStructure sysStructure = importDataRepository.getSysStructureByName(area);
            if(sysStructure==null){
                return result.fail(500,"组织结构不存在(第"+index+"行)");
            }
//            SysGeoCity geoCity = importDataRepository.getSysGeoCityByName(checkCity);
//            if(geoCity==null){
//                return result.fail(500,"地理城市不存在(第"+index+"行)");
//            }
            org.setCheckCityId(sysStructure.getId());
//            org.setCityId(geoCity.getId());
            if(StringUtil.isNotEmpty(parentCode)){
               Organization parentOrg =  organizationRepository.getByCode(parentCode,OrgType.DEALER.index);
                if(parentOrg==null&&orgType==OrgType.STORE.index){
                    parentOrg = organizationRepository.getByCode(parentCode,OrgType.DISTRIBUTOR.index);
                }
                if(parentOrg==null){
                    return result.fail(500,"所属经销商或者分销商不存在(第"+index+"行)");
                }
                SupplyRelation supplyRelation = new SupplyRelation();
                organizationRepository.insert(org);
                supplyRelation.setParentId(parentOrg.getId());
                supplyRelation.setChildId(org.getId());
                supplyRelation.setIsUse(true);
                supplyRelation.setAddTime(new Date());
                supplyRelation.setEditTime(new Date());
                newRelationList.add(supplyRelation);
            }else{
                organizationRepository.insert(org);
            }
        }
//        if(newOrgList.size()>0){
//            organizationRepository.insert(newOrgList);
//        }
        if(newRelationList.size()>0){
            supplyRelationRepository.insert(newRelationList);
        }
        return result.succeed();
    }
}
