package com.proj.appservice.impl;

import com.alibaba.druid.util.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.dto.PageResult;
import com.arlen.ebd.repository.SimpleTypeRepository;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.arlen.ebt.util.StringUtil;
import com.proj.appservice.DealerSignTypeAppService;
import com.proj.dto.DealerSignTypeDTO;
import com.proj.entity.DealerSignType;
import com.proj.repository.DealerSignTypeRepository;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.arlen.ebt.util.Utils.map;

/**
 * 经销商签收方式
 */
@Service
public class DealerSignTypeAppServiceImpl implements DealerSignTypeAppService {

    @Resource
    private DealerSignTypeRepository repository;

    @Resource
    private SimpleTypeRepository simpleTypeRepository;

    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private SupplyRelationRepository supplyRelationRepository;

    @Override
    public APIResult<DealerSignTypeDTO> getById(String id) {
        APIResult<DealerSignTypeDTO> result = new APIResult<>();
        DealerSignType bean = repository.load(id);
        if (bean == null) {
            return result.fail(APIResultCode.NOT_FOUND, "not found");
        }
        DealerSignTypeDTO dto = buildDealerSignTypeDTO(bean);
        return result.succeed().attachData(dto);
    }

    @Override
    public APIResult<String> create(DealerSignTypeDTO dto) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(dto.getId())) {
            dto.setId(UUID.randomUUID().toString());
        }
        if (StringUtils.isEmpty(dto.getDealerId())) {
           return result.fail(APIResultCode.ARGUMENT_INVALID,"经销商不能为空");
        }
        List<DealerSignType> list = repository.getListByDealerId(dto.getDealerId());
        if (list.size()>0){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"经销商已配置签收方式");
        }
        //如果是自动签收，则校验供货关系是否存在
        if ("3".equals(dto.getIsOneClickSignIn())){
            if (StringUtil.isNull(dto.getAutomaticDeliveryStore())){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"自动发货门店不存在");
            }
            SupplyRelation supplyRelation = supplyRelationRepository.getSupplyRelation(dto.getDealerId(),dto.getAutomaticDeliveryStore());
            if (supplyRelation == null){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"供货关系不存在");
            }
            if (!supplyRelation.getIsUse()){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"供货关系不存在");
            }
        }


        DozerBeanMapper mapper = new DozerBeanMapper();
        DealerSignType bean = mapper.map(dto, DealerSignType.class);
        repository.insert(bean);
        return result.succeed().attachData(dto.getId());
    }

    @Override
    public APIResult<String> update(DealerSignTypeDTO dto) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(dto.getId())) {
            return result.fail(APIResultCode.ARGUMENT_INVALID,"编码不能为空");
        }
        if (StringUtils.isEmpty(dto.getDealerId())) {
            return result.fail(APIResultCode.ARGUMENT_INVALID,"经销商不能为空");
        }
        DealerSignType bean = repository.load(dto.getId());
        if (bean == null) {
            return result.fail(APIResultCode.NOT_FOUND, "not find");
        }
        List<DealerSignType> list = repository.getListByDealerId(dto.getDealerId());
        if (list.size()>0){
            if (list.size()!=1){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"经销商已配置签收方式");
            }else{
                if (!bean.getDealerId().equals(list.get(0).getDealerId())){
                    return result.fail(APIResultCode.ARGUMENT_INVALID,"经销商已配置签收方式");
                }
            }
        }

        //如果是自动签收，则校验供货关系是否存在
        if ("3".equals(dto.getIsOneClickSignIn())){
            if (StringUtil.isNull(dto.getAutomaticDeliveryStore())){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"自动发货门店不存在");
            }
            SupplyRelation supplyRelation = supplyRelationRepository.getSupplyRelation(dto.getDealerId(),dto.getAutomaticDeliveryStore());
            if (supplyRelation == null){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"供货关系不存在");
            }
            if (!supplyRelation.getIsUse()){
                return result.fail(APIResultCode.ARGUMENT_INVALID,"供货关系不存在");
            }
        }
        bean.setDealerId(dto.getDealerId());
        bean.setIsOneClickSignIn(dto.getIsOneClickSignIn());
        bean.setAutomaticDeliveryStore(dto.getAutomaticDeliveryStore());
        bean.setInUse(dto.isInUse());
        bean.setAttr1(dto.getAttr1());
        bean.setAttr2(dto.getAttr2());
        bean.setAttr3(dto.getAttr3());
        bean.setAttr4(dto.getAttr4());
        bean.setAttr5(dto.getAttr5());
        bean.setAttr6(dto.getAttr6());
        bean.setEditBy(dto.getEditBy());
        bean.setEditTime(dto.getEditTime());
        bean.setRemark(dto.getRemark());
        repository.save(bean);
        return result.succeed().attachData(dto.getId());
    }

    @Override
    public APIResult<String> delete(String id) {
        APIResult<String> result = new APIResult<>();
        DealerSignType bean = repository.load(id);
        if (bean != null) {
            repository.delete(bean);
        }
        return result.succeed().attachData(id);
    }

    private DealerSignTypeDTO buildDealerSignTypeDTO(DealerSignType bean) {
        DozerBeanMapper mapper = new DozerBeanMapper();
        DealerSignTypeDTO dto = mapper.map(bean, DealerSignTypeDTO.class);
        return dto;
    }

    @Override
    public APIResult<List<DealerSignTypeDTO>> getChildList() {
        APIResult<List<DealerSignTypeDTO>> result = new APIResult<>();
        List<DealerSignType> enityList = repository.getList();
        Mapper mapper = new DozerBeanMapper();
        List<DealerSignTypeDTO> dtoList = map(mapper, enityList, DealerSignTypeDTO.class);
        return result.succeed().attachData(dtoList);
    }

    @Override
    public APIResult<PageResult<Map>> getPageListBySql(PageReq pageReq) {
        Page<Map> page = repository.getPageListBySql(pageReq);
        PageResult<Map> pageResult = new PageResult();
        pageResult.setTotal(page.getTotalElements());
        List<Map> list = page.getContent().stream().collect(Collectors.toList());
        for (Map map : list) {
            String signType = (String) map.get("SIGNTYPE");
            String[] split = signType.split(",");
            String signTypeName = "";
            for (String simpleTypeId : split) {
                signTypeName = signTypeName + simpleTypeRepository.load(simpleTypeId).getName() + ",";
            }
            signTypeName = signTypeName.substring(0, signTypeName.length() - 1);
            map.put("SIGNTYPE", signTypeName);
        }
        pageResult.setRows(list);
        APIResult<PageResult<Map>> result = new APIResult<>();
        result.succeed().attachData(pageResult);
        return result;
    }

    @Override
    public APIResult<Boolean> queryByDealerId(String dealerId) {
        APIResult<Boolean> result = new APIResult<>();
        List<DealerSignType> list =  repository.getListByDealerId(dealerId);
        if (list.size() > 1 ){
            return result.fail(APIResultCode.UNEXPECTED_ERROR,"数据异常");
        }else{
            if (list.isEmpty()){
                result.setData(false);
                result.setCode(0);
                return result;
            }
        }
        DealerSignType dealerSignType = list.get(0);
        if ("1".equals(dealerSignType.getIsOneClickSignIn())){
            result.setData(true);
            result.setCode(0);
        }else{
            result.setData(false);
            result.setCode(0);
        }
        return result;
    }

    @Override
    public APIResult<Boolean> isAutoReceive(String dealerId) {
        APIResult<Boolean> result = new APIResult<>();
        boolean isAutoReceive = false;
        List<DealerSignType> list = repository.getListByDealerId(dealerId);
        if (list != null) {
            String errMsg = "==========[" + dealerId + "]==========经销商签收方式，配置异常：";
            if (list.size() > 1) {
                throw new RuntimeException(errMsg + "数据重复");
            }
            if (list.size() == 1) {
                DealerSignType dealerSignType = list.get(0);
                if (StringUtils.isEmpty(dealerSignType.getIsOneClickSignIn())) {
                    throw new RuntimeException(errMsg + "签收方式为空");
                }
                if (dealerSignType.getIsOneClickSignIn().equals("2")) {
                    isAutoReceive = true;
                }
            }
        }
        return result.succeed().attachData(isAutoReceive);
    }

    @Override
    public APIResult<String> getAutoDeliveryStore(String dealerId) {
        APIResult<String> result = new APIResult<>();
        String storeId = "";
        List<DealerSignType> list = repository.getListByDealerId(dealerId);
        if (list != null) {
            String errMsg = "==========[" + dealerId + "]==========经销商签收方式，配置异常：";
            if (list.size() > 1) {
                throw new RuntimeException(errMsg + "数据重复");
            }
            if (list.size() == 1) {
                DealerSignType dealerSignType = list.get(0);
                if (StringUtils.isEmpty(dealerSignType.getIsOneClickSignIn())) {
                    throw new RuntimeException(errMsg + "签收方式为空");
                }
                if (dealerSignType.getIsOneClickSignIn().equals("2")) {
                    if (!StringUtils.isEmpty(dealerSignType.getAutomaticDeliveryStore())) {
                        String orgId = dealerSignType.getAutomaticDeliveryStore();
                        Organization org = organizationRepository.load(orgId);
                        if (org == null) {
                            throw new RuntimeException(errMsg + "收货方数据不存在，无法自动发货");
                        }
                        if (org.getOrgType() != OrgType.STORE.index) {
                            throw new RuntimeException(errMsg + "收货方组织类型错误，无法自动发货");
                        }
                        SupplyRelation supplyRelation = supplyRelationRepository.getSupplyRelation(dealerId, orgId);
                        if (supplyRelation == null) {
                            throw new RuntimeException(errMsg + "供货关系未维护，无法自动发货");
                        }
                        if (!supplyRelation.getIsUse()) {
                            throw new RuntimeException(errMsg + "供货关系已停用，无法自动发货");
                        }
                        storeId = orgId;
                    }
                }
            }
        }
        return result.succeed().attachData(storeId);
    }

}
