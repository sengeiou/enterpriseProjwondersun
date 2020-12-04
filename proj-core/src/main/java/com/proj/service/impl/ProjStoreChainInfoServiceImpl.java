package com.proj.service.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.util.CommonUtil;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.util.DateTimeTool;
import com.arlen.ebu.appservice.IdKeyAppService;
import com.proj.dto.ProjStoreChainInfoDTO;
import com.proj.entity.ProjImportErrLog;
import com.proj.entity.ProjStoreChainInfo;
import com.proj.enums.ImportErrType;
import com.proj.repository.ProjImportErrLogRepository;
import com.proj.repository.ProjStoreChainInfoRepository;
import com.proj.service.ProjStoreChainInfoService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 连锁信息service接口实现类
 * NEIL
 */
@Service
public class ProjStoreChainInfoServiceImpl implements ProjStoreChainInfoService {

    private static Logger logger = LoggerFactory.getLogger(ProjStoreChainInfoServiceImpl.class);

    @Resource
    private ProjStoreChainInfoRepository projStoreChainInfoRepository;

    @Resource
    private ProjImportErrLogRepository projImportErrLogRepository;

    @Resource
    private IdKeyAppService idKeyAppService;


    @Override
    public APIResult<String> add(ProjStoreChainInfoDTO dto) throws Exception {
        //定义返回值
        APIResult<String> result = new APIResult<>();
        //校验必填项
        if (StringUtil.isEmpty(dto.getCode())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁编码不能为空");
        }
        if (StringUtil.isEmpty(dto.getName())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁名称不能为空");
        }


        //校验code是否在数据库中存在
        List<ProjStoreChainInfo> list = projStoreChainInfoRepository.getByCode(dto.getCode());
        if (list.size()> 0 ){
            return result.fail(APIResultCode.ALREADY_EXSISTED,"编码已存在");
        }

        //校验name是否在数据库中存在
        List<ProjStoreChainInfo> list1 = projStoreChainInfoRepository.getByName(dto.getName());
        if (list1.size()> 0 ){
            return result.fail(APIResultCode.ALREADY_EXSISTED,"连锁名称已存在");
        }

        ProjStoreChainInfo storeChainInfo = new ProjStoreChainInfo();
        BeanUtils.copyProperties(storeChainInfo,dto);
        projStoreChainInfoRepository.insert(storeChainInfo);
        result.setData(String.valueOf(storeChainInfo.getId()));
        return result.succeed();
    }

    @Override
    public APIResult<String> edit(ProjStoreChainInfoDTO dto) throws Exception {
        //定义返回值
        APIResult<String> result = new APIResult<>();

        ProjStoreChainInfo storeChainInfo = projStoreChainInfoRepository.load(dto.getId());

        if (storeChainInfo == null){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁不存在");
        }
        //校验必填项
        if (StringUtil.isEmpty(dto.getCode())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁编码不能为空");
        }
        if (StringUtil.isEmpty(dto.getName())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁名称不能为空");
        }

        //校验code是否在数据库中存在
        List<ProjStoreChainInfo> list = projStoreChainInfoRepository.getByCodeNotId(dto.getCode(),dto.getId());
        if (list.size()> 0 ){
            return result.fail(APIResultCode.ALREADY_EXSISTED,"编码已存在");
        }

        //校验name是否在数据库中存在
        if(!storeChainInfo.getName().equals(dto.getName())){
            List<ProjStoreChainInfo> list1 = projStoreChainInfoRepository.getByName(dto.getName());
            if (list1.size()> 0 ){
                return result.fail(APIResultCode.ALREADY_EXSISTED,"连锁名称已存在");
            }
        }

        storeChainInfo.setName(dto.getName());
        storeChainInfo.setIsEnable(dto.getIsEnable());
        storeChainInfo.setEditBy(dto.getEditBy());
        storeChainInfo.setEditTime(dto.getEditTime());
        storeChainInfo.setRemark(dto.getRemark());
        projStoreChainInfoRepository.save(storeChainInfo);
        result.setData(String.valueOf(storeChainInfo.getId()));
        return result.succeed();
    }

    @Override
    public APIResult<String> setIsEnable(ProjStoreChainInfoDTO dto) throws Exception {
        //定义返回值
        APIResult<String> result = new APIResult<>();
        if (StringUtil.isEmpty(dto.getId())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"请选择连锁信息");
        }

        if (CommonUtil.isNull(dto.getIsEnable())){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁是否启用信息不能为空");
        }

        ProjStoreChainInfo storeChainInfo = projStoreChainInfoRepository.load(dto.getId());

        if (storeChainInfo == null){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁不存在");
        }

        storeChainInfo.setIsEnable(dto.getIsEnable());
        storeChainInfo.setEditBy(dto.getEditBy());
        storeChainInfo.setEditTime(dto.getEditTime());
        storeChainInfo.setRemark(dto.getRemark());
        projStoreChainInfoRepository.save(storeChainInfo);
        result.setData(String.valueOf(storeChainInfo.getId()));
        return result.succeed();
    }

    @Override
    public APIResult<ProjStoreChainInfoDTO> getById(String id) throws Exception {
        //定义返回值
        APIResult<ProjStoreChainInfoDTO> result = new APIResult<>();
        ProjStoreChainInfo storeChainInfo = projStoreChainInfoRepository.load(id);
        if (storeChainInfo == null){
            return result.fail(APIResultCode.ARGUMENT_INVALID,"连锁不存在");
        }
        ProjStoreChainInfoDTO dto = new ProjStoreChainInfoDTO();
        BeanUtils.copyProperties(dto,storeChainInfo);

        result.setData(dto);
        return result.succeed();
    }

    @Override
    public APIResult<String> importExcel(List<Map<String, String>> rowList, SysUserDTO sysUserDTO) throws Exception {
        String unique = CommonUtil.getBillNo("STORE_CHAIN_IMPORT");
        Date logStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入连锁信息--> SERVICE开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        APIResult<String> result = new APIResult<>();
        List<Map<String,String>> okList = new ArrayList<>();
        List<Map<String,Object>> errList = new ArrayList<>();

        //1.文件内容是否为空
        if (rowList.isEmpty()){
            logger.info("文件中没有内容，直接返回到API"+"，唯一值："+unique);
            return result.fail(APIResultCode.NOT_FOUND,"文件中必须有内容");
        }

        HashSet<String> names = new HashSet<String>();

        Date checkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入连锁信息---> 校验开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        for (Map<String,String> map:rowList){
            String errMsg = "";
            Date checkRowStartTime = DateTimeTool.getCurDatetime();
            logger.info("excel内容校验，第"+map.get("index")+"行，开始时间："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
            if (CommonUtil.isNull(map.get("name"))){
                errMsg += " | 连锁名称不能为空";
            }
            if(names.contains(map.get("name"))){
                errMsg += " | 连锁名称已在当前文件中存在";
            }
            List<ProjStoreChainInfo> list1 = projStoreChainInfoRepository.getByName(map.get("name"));
            if (list1.size()> 0 ){
                errMsg += " | 连锁名称已存在";
            }
            if (StringUtil.isNotEmpty(errMsg)){
                errMsg += "，错误行号："+map.get("index");
                Map<String,Object> errMap = new HashMap<>();
                errMap.put("errMsg",errMsg);
                errMap.put("errData",map);
                errList.add(errMap);
            }else{
                names.add(map.get("name"));
                okList.add(map);
            }
            Date checkRowEndTime = DateTimeTool.getCurDatetime();
            long checkRowInterval = (checkRowEndTime.getTime() - checkRowStartTime.getTime()) / 1000;
            logger.info("excel内容校验，第"+map.get("index")+"行 ，消耗[{"+checkRowInterval+"}]秒 ，唯一值："+unique);
            logger.info("excel内容校验，第"+map.get("index")+"行，结束时间："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        }
        //计算耗时
        Date checkEndTime = DateTimeTool.getCurDatetime();
        long checkInterval = (checkEndTime.getTime() - checkStartTime.getTime()) / 1000;
        logger.info("导入连锁信息---> 校验 ，消耗[{"+checkInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入连锁信息---> 校验结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);

        APIResult<String> okResult = importOkList(okList,unique,sysUserDTO);
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
        logger.info("导入连锁信息---> SERVICE ，消耗[{"+interval+"}]秒 ，唯一值："+unique);
        logger.info("导入连锁信息---> SERVICE ，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importOkList(List<Map<String,String>> okList,String unique, SysUserDTO sysUserDTO) throws Exception{
        APIResult<String> result = new APIResult<>();
        Date logOkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入连锁信息---> 正确集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        for (Map<String,String> map:okList){
            APIResult<String> codeResult = idKeyAppService.generateOne("StoreChainInfo");
            if (codeResult.getCode() != 0){
                throw new RuntimeException("获取连锁信息编码【CODE】出现异常，异常信息："+codeResult.getErrMsg());
            }
            String code = codeResult.getData();
            //校验code是否在数据库中存在
            List<ProjStoreChainInfo> list = projStoreChainInfoRepository.getByCode(code);
            if (list.size()> 0 ){
                throw new RuntimeException("编码已存在,编码："+code);
            }
            ProjStoreChainInfo storeChainInfo = new ProjStoreChainInfo();
            Date date = new Date();
            storeChainInfo.setCode(code);
            storeChainInfo.setName(map.get("name"));
            storeChainInfo.setContactName(map.get("contactName"));
            storeChainInfo.setContactPhone(map.get("contactPhone"));
            storeChainInfo.setIsEnable(0);
            storeChainInfo.setAddBy(sysUserDTO.getUserName()+"_import");
            storeChainInfo.setAddTime(date);
            storeChainInfo.setEditBy(sysUserDTO.getUserName()+"_import");
            storeChainInfo.setEditTime(date);
            projStoreChainInfoRepository.insert(storeChainInfo);
        }
        result.setCode(APIResultCode.OK);
        result.setData("添加正确信息，共"+okList.size()+"条信息");
        //计算耗时
        Date logOkEndTime = DateTimeTool.getCurDatetime();
        long okInterval = (logOkEndTime.getTime() - logOkStartTime.getTime()) / 1000;
        logger.info("导入连锁信息---> 正确集合添加，消耗[{"+okInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入连锁信息---> 正确集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importErrList(List<Map<String,Object>> errList,String unique, SysUserDTO sysUserDTO){
        APIResult<String> result = new APIResult<>();
        Date logErrStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入连锁信息---> 错误集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
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
            importErrLog.setType(ImportErrType.STORE_CHAIN_INFO.index);
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
        logger.info("导入连锁信息---> 错误集合添加，消耗[{"+errInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入连锁信息---> 错误集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }
}
