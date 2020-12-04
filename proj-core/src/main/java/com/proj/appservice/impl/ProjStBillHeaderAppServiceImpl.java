package com.proj.appservice.impl;

import com.alibaba.druid.util.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.ebd.common.CommonUtil;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.enums.MaterialType;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebt.dto.STBillHeaderDTO;
import com.arlen.ebt.entity.STBillDetail;
import com.arlen.ebt.entity.STBillHeader;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillTypeRepository;
import com.arlen.ebt.repository.STBillDetailRepository;
import com.arlen.ebt.repository.STBillHeaderRepository;
import com.proj.appservice.ProjStBillHeaderAppService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by arlenChen in 2020/4/24.
 * 自定义盘点单
 *
 * @author arlenChen
 */
@Service
public class ProjStBillHeaderAppServiceImpl implements ProjStBillHeaderAppService {

    @Resource
    private STBillHeaderRepository stBillHeaderRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private STBillDetailRepository stBillDetailRepository;
    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private BillTypeRepository billTypeRepository;

    /**
     * 通过经销商ID生成盘点单（经销商ID赋值在stBillHeaderDTO中）
     *
     * @param stBillHeaderDTO stBillHeaderDTO
     * @param newMaterial     是否动销
     * @return APIResult
     */
    @Override
    public APIResult<String> createStBillByDealerId(STBillHeaderDTO stBillHeaderDTO, Boolean newMaterial) {

        APIResult<String> result = new APIResult<>();
        //判断组织是否存在
        Organization organization = organizationRepository.load(stBillHeaderDTO.getOrgId());
        if (organization == null || !organization.getInUse()) {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "organization not found！");
        }
        //如果有未完成或未取消的盘点单不允许创建
        STBillHeader existedBill = stBillHeaderRepository.getNotFinishedOrCanceledBillByOrgId(stBillHeaderDTO.getOrgId());
        if (existedBill != null) {
            return result.fail(APIResultCode.FORBIDDEN, "存在未完成或未取消的盘点单(" + existedBill.getCode() + "),不允许创建");
        }
        STBillHeader stBillHeader = CommonUtil.map(stBillHeaderDTO, STBillHeader.class);
        //设置盘点单ID
        if (StringUtils.isEmpty(stBillHeader.getId())) {
            stBillHeader.setId(UUID.randomUUID().toString());
        }

        //获取盘点前库存>0明细列表
        List<STBillDetail> stBillDetailList = stBillDetailRepository.generateSTBillDetailList(stBillHeaderDTO.getOrgId(), newMaterial);
        Map<String, Integer> materialIdMap = new HashMap<>(6);
        for (STBillDetail detail : stBillDetailList
                ) {
            materialIdMap.put(detail.getMaterialId(), detail.getExpectQtyPcs());
        }
        //根据物料类型&是否新品获取产品
        List<Material> materialList = materialRepository.getAll(MaterialType.CHENGPIN.index, newMaterial);
        List<STBillDetail> opList = new ArrayList<>();
        //盘点前箱数
        BigDecimal expectQty = new BigDecimal(0);
        //生成所有物料对应明细
        if (materialList != null) {
            for (Material material : materialList
                    ) {
                if (!material.getInUse()) {
                    continue;
                }
                STBillDetail stBillDetail = new STBillDetail();
                stBillDetail.setMaterialId(material.getId());
                if (materialIdMap.containsKey(material.getId())) {
                    stBillDetail.setExpectQtyPcs(materialIdMap.get(material.getId()));
                    //计算盘点前箱数
                    expectQty = expectQty.add(new BigDecimal(stBillDetail.getExpectQtyPcs()).divide(new BigDecimal(material.getPcsQty()), 2, BigDecimal.ROUND_HALF_UP));
                } else {
                    stBillDetail.setExpectQtyPcs(0);
                }
                stBillDetail.setId(UUID.randomUUID().toString());
                stBillDetail.setBillHeader(stBillHeader);
                stBillDetail.setNewMaterial(newMaterial);
                opList.add(stBillDetail);
            }
        }
        //设置盘点前库存箱数
        stBillHeader.setExpectQty(expectQty);
        //设置明细
        stBillHeader.setDetailList(opList);
        stBillHeader.setNewMaterial(newMaterial);
        //设置盘点前库存单品数
        stBillHeader.setExpectQtyPcs(stBillHeader.getDetailList().stream().mapToInt(x -> x.getExpectQtyPcs()).sum());
        //新增数据
        stBillHeaderRepository.insert(stBillHeader);
        return result.succeed();
    }

    /**
     * 通过组织Code生成盘点单
     *
     * @param code            code
     * @param orgType         组织类型
     * @param stBillHeaderDTO dto
     * @param newMaterial     是否动销
     * @return APIResult
     */
    @Override
    public APIResult<String> createStBillByDealerCode(String code, OrgType orgType, STBillHeaderDTO stBillHeaderDTO, Boolean newMaterial) {
        Organization org = organizationRepository.getByCode(code, orgType.index);
        if (org == null) {
            return new APIResult<String>().fail(APIResultCode.NOT_FOUND, "Organization not found");
        }
        stBillHeaderDTO.setOrgId(org.getId());
        return createStBillByDealerId(stBillHeaderDTO, newMaterial);
    }

    /**
     * 通过考核城市创建盘点单
     *
     * @param stBillHeaderDTOList List
     * @param newMaterial         是否动销
     * @return APIResult
     */
    @Override
    public APIResult<String> createStBillBySingleCity(List<STBillHeaderDTO> stBillHeaderDTOList, Boolean newMaterial) {
        APIResult<String> result = new APIResult<>();
        for (STBillHeaderDTO stBillHeaderDTO : stBillHeaderDTOList) {
            //判断组织是否存在
            Organization organization = organizationRepository.load(stBillHeaderDTO.getOrgId());
            if (organization == null || !organization.getInUse()) {
                continue;
            }
            //如果有未完成或未取消的盘点单不允许创建
            STBillHeader existedBill = stBillHeaderRepository.getNotFinishedOrCanceledBillByOrgId(stBillHeaderDTO.getOrgId());
            if (existedBill != null) {
                continue;
            }
            STBillHeader stBillHeader = CommonUtil.map(stBillHeaderDTO, STBillHeader.class);
            //设置盘点单ID
            if (StringUtils.isEmpty(stBillHeader.getId())) {
                stBillHeader.setId(UUID.randomUUID().toString());
            }
            //获取盘点前库存>0明细列表
            List<STBillDetail> stBillDetailList = stBillDetailRepository.generateSTBillDetailList(stBillHeaderDTO.getOrgId(), newMaterial);
            Map<String, Integer> materialIdMap = new HashMap<>();
            for (STBillDetail detail : stBillDetailList
                    ) {
                materialIdMap.put(detail.getMaterialId(), detail.getExpectQtyPcs());
            }
            //根据物料类型&是否新品获取产品
            List<Material> materialList = materialRepository.getAll(MaterialType.CHENGPIN.index, newMaterial);
            List<STBillDetail> opList = new ArrayList<>();
            //盘点前箱数
            BigDecimal expectQty = new BigDecimal(0);
            //生成所有物料对应明细
            if (materialList != null) {
                for (Material material : materialList
                        ) {
                    if (!material.getInUse()) {
                        continue;
                    }
                    STBillDetail stBillDetail = new STBillDetail();
                    stBillDetail.setMaterialId(material.getId());
                    if (materialIdMap.containsKey(material.getId())) {
                        stBillDetail.setExpectQtyPcs(materialIdMap.get(material.getId()));
                        //计算盘点前箱数
                        expectQty = expectQty.add(new BigDecimal(stBillDetail.getExpectQtyPcs()).divide(new BigDecimal(material.getPcsQty()), 2, BigDecimal.ROUND_HALF_UP));
                    } else {
                        stBillDetail.setExpectQtyPcs(0);
                    }
                    stBillDetail.setId(UUID.randomUUID().toString());
                    stBillDetail.setBillHeader(stBillHeader);
                    stBillDetail.setNewMaterial(newMaterial);
                    opList.add(stBillDetail);
                }
            }
            //设置盘点前库存箱数
            stBillHeader.setExpectQty(expectQty);
            stBillHeader.setNewMaterial(newMaterial);
            //设置明细
            stBillHeader.setDetailList(opList);
            //设置盘点前库存单品数
            stBillHeader.setExpectQtyPcs(stBillHeader.getDetailList().stream().mapToInt(x -> x.getExpectQtyPcs()).sum());
            //插入
            stBillHeaderRepository.insert(stBillHeader);
        }
        return result.succeed();
    }
}
