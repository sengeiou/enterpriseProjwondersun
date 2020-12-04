package com.proj.service;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.OrganizationDTO;
import com.arlen.ebp.dto.SysForeignPushRecordDTO;
import com.arlen.ebp.entity.SysForeignPushRecord;
import com.proj.dto.ClientJsonForMC;
import org.dom4j.DocumentException;

import java.util.Map;

public interface PushRetailerToMcJobService {

    /**
     * 执行推送
     *
     * @param dateType 3-仓库 4-经销商 5-分销商 6-门店 enum OrgType.java getIndex
     * @param clientType 1-公司仓库 2-经销商 3-终端门店 4-分销商 10-其他
     * @return
     */
    public Map<String,Object> doPush(String dateType,String clientType);

    /**
     * 门店
     *
     * dateType=6
     * clientType=3
     * @return
     */
    public Map<String,Object> pushStore();

    /**
     * 经销商
     *
     * dateType=4
     * clientType=2
     * @return
     */
    public Map<String,Object> pushDealer();

    /**
     * 分销商
     *
     * dateType=5
     * clientType=4
     * @return
     */
    public Map<String,Object> pushDistributor();

    /**
     * 仓库
     *
     * dateType=3
     * clientType=1
     * @return
     */
    public Map<String,Object> pushWarehouse();

    /**
     * 执行推送
     * @param sysForeignPushRecordDTO
     * @return
     */
    public String pushToMC(SysForeignPushRecordDTO sysForeignPushRecordDTO);

    /**
     * 获取序列号
     * @return
     */
    public int getSequence();

    /**
     * 接口登陆获取AuthKey
     * @return
     * @throws DocumentException
     */
    public String getAuthKey() throws DocumentException;

    /**
     * 创建要推送的DTO
     * @param sysForeignPushRecord
     * @param clientType
     * @return
     * @throws DocumentException
     */
    public APIResult<SysForeignPushRecordDTO> createDto(SysForeignPushRecord sysForeignPushRecord, String clientType) throws DocumentException;

    /**
     * 封装请求参数
     * @param org
     * @param clientType
     * @return
     */
    public ClientJsonForMC createReqParam(OrganizationDTO org, String clientType) ;

}
