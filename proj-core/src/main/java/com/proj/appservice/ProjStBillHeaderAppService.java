package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebt.dto.STBillHeaderDTO;

import java.util.List;

/**
 * Created by arlenChen in 2020/4/24.
 * 自定义盘点单
 *
 * @author arlenChen
 */
public interface ProjStBillHeaderAppService {

    /**
     * 通过经销商ID生成盘点单（经销商ID赋值在stBillHeaderDTO中）
     *
     * @param stBillHeaderDTO stBillHeaderDTO
     * @param newMaterial     是否动销
     * @return APIResult
     */
    APIResult<String> createStBillByDealerId(STBillHeaderDTO stBillHeaderDTO, Boolean newMaterial);

    /**
     * 通过组织Code生成盘点单
     *
     * @param code            code
     * @param orgType         组织类型
     * @param stBillHeaderDTO dto
     * @param newMaterial     是否动销
     * @return APIResult
     */
    APIResult<String> createStBillByDealerCode(String code, OrgType orgType, STBillHeaderDTO stBillHeaderDTO, Boolean newMaterial);

    /**
     * 通过考核城市创建盘点单
     *
     * @param stBillHeaderDTOList List
     * @param newMaterial         是否动销
     * @return APIResult
     */
    APIResult<String> createStBillBySingleCity(List<STBillHeaderDTO> stBillHeaderDTOList, Boolean newMaterial);
}
