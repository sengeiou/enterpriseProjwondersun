package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.utils.StringUtil;
import com.ebp.appservice.OrganizationAppService;
import com.ebp.dto.OrganizationDTO;
import com.ebp.dto.SysUserDTO;
import com.ebp.enums.OrgType;
import com.ebp.web.comm.BaseController;
import com.ebt.dto.STBillHeaderDTO;
import com.ebt.enums.STBillStatus;
import com.ebu.appservice.IdKeyAppService;
import com.proj.appservice.ProjStBillHeaderAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by arlenChen on 2016/7/5.
 *
 * @author arlenChen
 */
@Controller
@RequestMapping("/api/proj/stbillheader")
public class ProjStBillHeaderApi extends BaseController {
    @Resource
    private ProjStBillHeaderAppService projStBillHeaderAppService;

    @Resource
    private IdKeyAppService idKeyAppService;
    @Resource
    private OrganizationAppService organizationAppService;

    /**
     * 批量添加
     *
     * @param orgId orgId
     * @return APIResult
     */
    @RequestMapping(value = "createstbillbydealerid", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> createSTBillByDealerId(@RequestParam String orgId, @RequestParam Boolean newMaterial) {
        APIResult<String> result = new APIResult<>();
        SysUserDTO currentUser = getCurrentUser();
        STBillHeaderDTO stBillHeaderDTO = new STBillHeaderDTO();
        APIResult<String> idKeyResult = idKeyAppService.generateOne("STBillHeader");
        if (idKeyResult.getCode() == 0) {
            stBillHeaderDTO.setCode(idKeyResult.getData());
        } else {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "生成Code失败！");
        }
        stBillHeaderDTO.setOrgId(orgId);
        stBillHeaderDTO.setOperateTime(new Date());
        stBillHeaderDTO.setAddTime(new Date());
        stBillHeaderDTO.setEditTime(new Date());
        stBillHeaderDTO.setAddBy(currentUser.getUserName());
        stBillHeaderDTO.setEditBy(currentUser.getUserName());
        stBillHeaderDTO.setBillStatus(STBillStatus.CONFIRMED.index);
        result = projStBillHeaderAppService.createStBillByDealerId(stBillHeaderDTO, newMaterial);
        return result;
    }

    @RequestMapping(value = "createstbillbydealercode", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> createSTBillByDealerCode(@RequestParam String orgCode, @RequestParam(required = false) Boolean newMaterial) {
        return createSTBillByCode(orgCode, OrgType.DEALER, newMaterial);
    }

    @RequestMapping(value = "createstbillbyd2code", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> createSTBillByD2Code(@RequestParam String orgCode, @RequestParam(required = false) Boolean newMaterial) {
        return createSTBillByCode(orgCode, OrgType.DISTRIBUTOR, newMaterial);
    }

    @RequestMapping(value = "createstbillbysinglecity", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> createSTBillBySingleCity(@RequestParam String checkCityId, @RequestParam(required = false) String dealerTypes, @RequestParam(required = false) Boolean newMaterial) {
        APIResult<String> result = new APIResult<>();
        SysUserDTO currentUser = getCurrentUser();
        APIResult<List<OrganizationDTO>> organizationDtoResult = organizationAppService.getBySingleCity(checkCityId);
        List<OrganizationDTO> organizationDTOList = new ArrayList<>();
        if (organizationDtoResult.getData().isEmpty()) {
            return result.fail(APIResultCode.NOT_FOUND, "该考核城市下没有关联的组织！");
        }
        for (OrganizationDTO organizationDTO : organizationDtoResult.getData()) {
            boolean isTrue = 4 == organizationDTO.getOrgType() && organizationDTO.getInUse() && (StringUtil.isEmpty(organizationDTO.getAttr4()) || organizationDTO.getAttr4().equals("否")) && StringUtil.isEmpty(organizationDTO.getMainCode()) || organizationDTO.getCode().equals(organizationDTO.getMainCode());
            if (isTrue) {
                if (StringUtil.isNotEmpty(dealerTypes)) {
                    String[] dealerType = dealerTypes.split(",");
                    for (String s : dealerType) {
                        if (StringUtil.isEmpty(organizationDTO.getAttr5()) || s.equals(organizationDTO.getAttr5())) {
                            organizationDTOList.add(organizationDTO);
                        }
                    }
                } else {
                    organizationDTOList.add(organizationDTO);
                }
            }
        }
        List<STBillHeaderDTO> stBillHeaderDTOList = new ArrayList<>();
        for (OrganizationDTO organizationDTO : organizationDTOList) {
            STBillHeaderDTO stBillHeaderDTO = new STBillHeaderDTO();
            APIResult<String> idKeyResult = idKeyAppService.generateOne("STBillHeader");
            if (idKeyResult.getCode() == 0) {
                stBillHeaderDTO.setCode(idKeyResult.getData());
            } else {
                return result.fail(APIResultCode.UNEXPECTED_ERROR, "生成Code失败！");
            }
            stBillHeaderDTO.setOrgId(organizationDTO.getId());
            stBillHeaderDTO.setOperateTime(new Date());
            stBillHeaderDTO.setAddTime(new Date());
            stBillHeaderDTO.setEditTime(new Date());
            stBillHeaderDTO.setAddBy(currentUser.getUserName());
            stBillHeaderDTO.setEditBy(currentUser.getUserName());
            stBillHeaderDTO.setBillStatus(STBillStatus.CONFIRMED.index);
            stBillHeaderDTOList.add(stBillHeaderDTO);
        }
        result = projStBillHeaderAppService.createStBillBySingleCity(stBillHeaderDTOList, newMaterial);
        return result;
    }

    private APIResult<String> createSTBillByCode(String orgCode, OrgType orgType, Boolean newMaterial) {
        APIResult<String> result = new APIResult<>();
        SysUserDTO currentUser = getCurrentUser();
        STBillHeaderDTO stBillHeaderDTO = new STBillHeaderDTO();
        APIResult<String> idKeyResult = idKeyAppService.generateOne("STBillHeader");
        if (idKeyResult.getCode() == 0) {
            stBillHeaderDTO.setCode(idKeyResult.getData());
        } else {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "生成Code失败！");
        }
        stBillHeaderDTO.setOperateTime(new Date());
        stBillHeaderDTO.setAddTime(new Date());
        stBillHeaderDTO.setEditTime(new Date());
        stBillHeaderDTO.setAddBy(currentUser.getUserName());
        stBillHeaderDTO.setEditBy(currentUser.getUserName());
        stBillHeaderDTO.setBillStatus(STBillStatus.CONFIRMED.index);
        result = projStBillHeaderAppService.createStBillByDealerCode(orgCode, orgType, stBillHeaderDTO, newMaterial);
        return result;
    }
}
