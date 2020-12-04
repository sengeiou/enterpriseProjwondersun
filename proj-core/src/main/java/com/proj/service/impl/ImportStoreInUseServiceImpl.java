package com.proj.service.impl;

import cn.jiguang.common.utils.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.util.CommonUtil;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebp.util.DateTimeTool;
import com.arlen.ebp.util.EbpConstantUtils;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.entity.ProjImportErrLog;
import com.proj.enums.ImportErrType;
import com.proj.repository.ProjImportErrLogRepository;
import com.proj.service.ImportStoreInUseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ImportStoreInUseServiceImpl implements ImportStoreInUseService {

    private static Logger logger = LoggerFactory.getLogger(ImportStoreInUseServiceImpl.class);

    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private ProjImportErrLogRepository projImportErrLogRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private SysParaRepository sysParaRepository;

    @Override
    public APIResult<String> importExcel(List<Map<String, String>> rowList, SysUserDTO sysUserDTO) throws Exception {
        String unique = CommonUtil.getBillNo("STORE_IMPORT_INUSE");
        Date logStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店启用信息--> SERVICE开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        APIResult<String> result = new APIResult<>();
        List<Organization> okList = new ArrayList<>();
        List<Map<String,Object>> errList = new ArrayList<>();


        //1.文件内容是否为空
        if (rowList.isEmpty()){
            logger.info("文件中没有内容，直接返回到API"+"，唯一值："+unique);
            return result.fail(APIResultCode.NOT_FOUND,"文件中必须有内容");
        }

        //查询门店代码
        List<Organization> storeList = organizationRepository.getByOrgType(OrgType.STORE.index);
        Map<String,Organization> organizationMap = new HashMap<>();
        for (Organization organization :storeList){
            organizationMap.put(organization.getCode(),organization);
        }

        Date checkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店启用信息---> 校验开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        for (Map<String,String> map:rowList){
            String errMsg = "";
            Date checkRowStartTime = DateTimeTool.getCurDatetime();
            logger.info("excel内容校验，第"+map.get("index")+"行，开始时间："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
            if (CommonUtil.isNull(map.get("storeCode"))){
                errMsg += " | 门店代码不能为空";
            }
            if (CommonUtil.isNull(map.get("inUse"))){
                errMsg += " | 启用状态不能为空";
            }
            //校验门店代码是否存在
            Organization organization = organizationMap.get(map.get("storeCode"));
            if (organization == null){
                errMsg += " | 门店代码不能为空";
            }


            if (StringUtil.isNotEmpty(errMsg)){
                errMsg += "，错误行号："+map.get("index");
                Map<String,Object> errMap = new HashMap<>();
                errMap.put("errMsg",errMsg);
                errMap.put("errData",map);
                errList.add(errMap);
            }else{
                String inUser = map.get("inUse");
                if ("启用".equals(inUser)){
                    organization.setInUse(true);
                }else{
                    organization.setInUse(false);
                }
                okList.add(organization);
            }
            Date checkRowEndTime = DateTimeTool.getCurDatetime();
            long checkRowInterval = (checkRowEndTime.getTime() - checkRowStartTime.getTime()) / 1000;
            logger.info("excel内容校验，第"+map.get("index")+"行 ，消耗[{"+checkRowInterval+"}]秒 ，唯一值："+unique);
            logger.info("excel内容校验，第"+map.get("index")+"行，结束时间："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        }
        //计算耗时
        Date checkEndTime = DateTimeTool.getCurDatetime();
        long checkInterval = (checkEndTime.getTime() - checkStartTime.getTime()) / 1000;
        logger.info("导入门店启用信息---> 校验 ，消耗[{"+checkInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店启用信息---> 校验结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);

        APIResult<String> okResult = importOkList(okList,unique,sysUserDTO);
        SysPara sysPara = sysParaRepository.load("createPushRecord");
        for(Organization organization:okList){
            boolean isCreatePushRecord = sysPara != null && EbpConstantUtils.TRUE.equals(sysPara.getValue()) &&
                    StringUtils.isNotEmpty(sysPara.getRemark()) && sysPara.getRemark().contains(organization.getOrgType() + EbpConstantUtils.COMMA);
            if (isCreatePushRecord) {
                excelUtilAppService.createPushRecord(organization,false);
            }
        }
        APIResult<String> errResult = importErrList(errList,unique,sysUserDTO);

        result.setCode(APIResultCode.OK);
        if (errList.size()>0){
            result.setData(okResult.getData()+","+errResult.getData());
        }else{
            result.setData(okResult.getData());
        }


        //计算耗时
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("导入门店启用信息---> SERVICE ，消耗[{"+interval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店启用信息---> SERVICE ，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importOkList(List<Organization> okList,String unique, SysUserDTO sysUserDTO) throws Exception{
        APIResult<String> result = new APIResult<>();
        Date logOkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店启用信息---> 正确集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        organizationRepository.save(okList);
        result.setCode(APIResultCode.OK);
        result.setData("添加正确信息，共"+okList.size()+"条信息");
        //计算耗时
        Date logOkEndTime = DateTimeTool.getCurDatetime();
        long okInterval = (logOkEndTime.getTime() - logOkStartTime.getTime()) / 1000;
        logger.info("导入门店启用信息---> 正确集合添加，消耗[{"+okInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店启用信息---> 正确集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importErrList(List<Map<String,Object>> errList,String unique, SysUserDTO sysUserDTO){
        APIResult<String> result = new APIResult<>();
        Date logErrStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店启用信息---> 错误集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        String errCode = CommonUtil.getBillNo("IMPORT_ERR_CODE");
        //1.添加导入文件错误日志表
       /* errMap.put("errMsg",errMsg);
        errMap.put("errData",map);*/
        for (Map<String,Object> map:errList){
            Date date = new Date();
            ProjImportErrLog importErrLog = new ProjImportErrLog();
            importErrLog.setErrCode(errCode);
            importErrLog.setErrData(map.get("errData").toString());
            importErrLog.setErrMsg(map.get("errMsg").toString());
            importErrLog.setType(ImportErrType.STORE_IMPORT_INUSE.index);
            importErrLog.setAddBy(sysUserDTO.getUserName());
            importErrLog.setAddTime(date);
            importErrLog.setEditBy(sysUserDTO.getUserName());
            importErrLog.setEditTime(date);
            projImportErrLogRepository.insert(importErrLog);
        }
        result.setData(errCode);
        result.setCode(APIResultCode.OK);
        result.setData("添加错误信息，共"+errList.size()+"条信息，请根据错误码：【"+errCode+"】查看错误信息");
        //计算耗时
        Date logErrEndTime = DateTimeTool.getCurDatetime();
        long errInterval = (logErrEndTime.getTime() - logErrStartTime.getTime()) / 1000;
        logger.info("导入门店启用信息---> 错误集合添加，消耗[{"+errInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店启用信息---> 错误集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }
}
