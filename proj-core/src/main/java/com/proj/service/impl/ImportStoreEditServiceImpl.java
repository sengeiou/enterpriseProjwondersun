package com.proj.service.impl;

import cn.jiguang.common.utils.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.util.CommonUtil;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SimpleData;
import com.arlen.ebp.entity.SysGeoCity;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SimpleDataRepository;
import com.arlen.ebp.repository.SysGeoCityRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebp.util.DateTimeTool;
import com.arlen.ebp.util.EbpConstantUtils;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.entity.ProjImportErrLog;
import com.proj.enums.ImportErrType;
import com.proj.repository.ProjImportErrLogRepository;
import com.proj.service.ImportStoreEditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ImportStoreEditServiceImpl implements ImportStoreEditService {

    private static Logger logger = LoggerFactory.getLogger(ImportStoreInUseServiceImpl.class);

    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private SysGeoCityRepository sysGeoCityRepository;

    @Resource
    private SimpleDataRepository simpleDataRepository;

    @Resource
    private ProjImportErrLogRepository projImportErrLogRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private SysParaRepository sysParaRepository;


    @Override
    public APIResult<String> importExcel(List<Map<String, String>> rowList, SysUserDTO sysUserDTO) throws Exception {
        String unique = CommonUtil.getBillNo("STORE_IMPORT_EDIT");
        Date logStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店修改信息--> SERVICE开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        APIResult<String> result = new APIResult<>();
        List<Organization> okList = new ArrayList<>();
        List<Map<String,Object>> errList = new ArrayList<>();

        //1.文件内容是否为空
        if (rowList.isEmpty()){
            logger.info("文件中没有内容，直接返回到API"+"，唯一值："+unique);
            return result.fail(APIResultCode.NOT_FOUND,"文件中必须有内容");
        }

        Date checkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店修改信息---> 校验开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        for (Map<String,String> map:rowList) {
            String errMsg = "";
            Organization organization = new Organization();
            Date checkRowStartTime = DateTimeTool.getCurDatetime();
            logger.info("excel内容校验，第"+map.get("index")+"行，开始时间："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
            if (CommonUtil.isNull(map.get("storeCode"))){
                errMsg += " | 门店代码不能为空";
            }
            //门店代码
            String storeCode = map.get("storeCode");






            if (StringUtil.isEmpty(errMsg)){
                //-------------------------------------------校验门店代码是否存在 ----------------------------------------------
                organization = organizationRepository.getByCode(storeCode,OrgType.STORE);
                if (organization == null){
                    errMsg += " | 门店代码不能为空";
                }

                //-------------------------------------------校验省市区 ----------------------------------------------
                boolean isEmptyProvince = false;
                boolean isEmptyCity = false;
                boolean isEmptyDistrict = false;
                if (StringUtil.isEmpty(map.get("province"))){
                    isEmptyProvince = true;
                }
                if (StringUtil.isEmpty(map.get("city"))){
                    isEmptyCity = true;
                }
                if (StringUtil.isEmpty(map.get("district"))){
                    isEmptyDistrict = true;
                }

                if (isEmptyProvince&&isEmptyCity&&isEmptyDistrict){
                    logger.info("excel内容校验，第"+map.get("index")+"行，省市区校验都为空，不进行校验，唯一值："+unique);
                }else{
                    if (isEmptyProvince){
                        errMsg += " | 省份不能为空";
                    }
                    if (isEmptyCity){
                        errMsg += " | 城市不能为空";
                    }
                    if (isEmptyDistrict){
                        errMsg += " | 区县不能为空";
                    }
                }
                if (StringUtil.isEmpty(errMsg)&&StringUtil.isNotEmpty(map.get("province"))&&StringUtil.isNotEmpty(map.get("city"))&&StringUtil.isNotEmpty(map.get("district"))){
                    //省份
                    String province = map.get("province");
                    //城市
                    String city = map.get("city");
                    //区县
                    String district = map.get("district");
                    String provinceId = "";
                    String cityId = "";
                    String districtId = "";
                    //查询省份
                    SysGeoCity provinceGeo = sysGeoCityRepository.getIdByName(province,1,"0");
                    if (CommonUtil.isNull(provinceGeo)){
                        errMsg += " | 省份在系统中不存在";
                    } else {
                        provinceId = provinceGeo.getId();
                        //查询城市
                        SysGeoCity cityGeo = sysGeoCityRepository.getIdByName(city,2,provinceGeo.getId());
                        if (CommonUtil.isNull(cityGeo)){
                            errMsg += " | 城市在系统中不存在";
                        } else {
                            cityId = cityGeo.getId();
                            //查询区县
                            SysGeoCity districtGeo = sysGeoCityRepository.getIdByName(district,3,cityGeo.getId());
                            if (CommonUtil.isNull(districtGeo)){
                                errMsg += " | 区县在系统中不存在";
                            } else {
                                districtId = districtGeo.getId();
                            }
                        }
                    }
                    //-------------------------------------------赋值 ----------------------------------------------
                    if (StringUtil.isEmpty(errMsg)){
                        //省份
                        organization.setProvinceId(provinceId);
                        //城市
                        organization.setCityId(cityId);
                        //区县
                        organization.setDistrictId(districtId);
                        //乡镇
                        if (StringUtil.isNotEmpty(map.get("township"))){
                            organization.setExt10(map.get("township"));
                        }
                        //详细地址
                        if (StringUtil.isNotEmpty(map.get("address"))){
                            organization.setAddress(map.get("address"));
                        }
                    }
                }
                //-------------------------------------------渠道类型校验 ----------------------------------------------
                if (StringUtil.isEmpty(errMsg)&&StringUtil.isNotEmpty(map.get("channelType"))){
                    //渠道类型
                    String channelType = map.get("channelType");
                    //根据名称查询启用的渠道类型
                    List<SimpleData> channelTypeList = simpleDataRepository.getListByNameByCategoryIdByInUse(channelType,"STORECHANNELTYPE",true);
                    String channelTypeId = new String();
                    if (channelTypeList.size() > 0){
                        channelTypeId = channelTypeList.get(0).getId();
                    }
                    //如果excel中的渠道类型名称，在系统中渠道类型集合中不存在，记录为错误信息
                    if (StringUtil.isEmpty(channelTypeId)){
                        errMsg += " | 渠道类型在系统中不存在";
                    }
                    //-------------------------------------------赋值 ----------------------------------------------
                    if (StringUtil.isEmpty(errMsg)){
                        //渠道类型
                        organization.setAttr1(channelTypeId);
                    }
                }
                //-------------------------------------------次渠道校验 ----------------------------------------------
                if (StringUtil.isEmpty(errMsg)&&StringUtil.isNotEmpty(map.get("secondaryChannel"))){
                    //次渠道
                    String secondaryChannel = map.get("secondaryChannel");
                    List<SimpleData> secondaryChannelList = simpleDataRepository.getListByNameByCategoryIdByInUse(secondaryChannel,"STORESUBCHANNELTYPE",true);
                    String secondaryChannelId = new String();
                    if (secondaryChannelList.size() > 0){
                        secondaryChannelId = secondaryChannelList.get(0).getId();
                    }
                    //如果excel中的渠道类型名称，在系统中渠道类型集合中不存在，记录为错误信息
                    if (StringUtil.isEmpty(secondaryChannelId)){
                        errMsg += " | 次渠道在系统中不存在";
                    }
                    //-------------------------------------------赋值 ----------------------------------------------
                    if (StringUtil.isEmpty(errMsg)){
                        //次渠道
                        organization.setAttr3(secondaryChannelId);
                    }
                }
                //-------------------------------------------所属KA系统校验 ----------------------------------------------
                if (StringUtil.isEmpty(errMsg)&&StringUtil.isNotEmpty(map.get("ka"))){
                    //所属KA系统
                    String ka = map.get("ka");
                    List<SimpleData> kaList = simpleDataRepository.getListByNameByCategoryIdByInUse(ka,"STOREKATYPE",true);
                    String kaId = new String();
                    if (kaList.size() > 0){
                        kaId = kaList.get(0).getId();
                    }
                    //如果excel中的渠道类型名称，在系统中渠道类型集合中不存在，记录为错误信息
                    if (StringUtil.isEmpty(kaId)){
                        errMsg += " | 所属KA系统在系统中不存在";
                    }
                    //-------------------------------------------赋值 ----------------------------------------------
                    if (StringUtil.isEmpty(errMsg)){
                        //所属KA系统
                        organization.setAttr2(kaId);
                    }
                }
            }






            if (StringUtil.isNotEmpty(errMsg)){
                errMsg += "，错误行号："+map.get("index");
                Map<String,Object> errMap = new HashMap<>();
                errMap.put("errMsg",errMsg);
                errMap.put("errData",map);
                errList.add(errMap);
            }else{
                //门店全称
                if (StringUtil.isNotEmpty(map.get("storeFullName"))){
                    organization.setFullName(map.get("storeFullName"));
                }
                //简称
                if (StringUtil.isNotEmpty(map.get("storeShortName"))){
                    organization.setShortName(map.get("storeShortName"));
                }
                //联系人
                if (StringUtil.isNotEmpty(map.get("contact"))){
                    organization.setContact(map.get("contact"));
                }
                //联系电话
                if (StringUtil.isNotEmpty(map.get("phone"))){
                    organization.setPhone(map.get("phone"));
                }
                //营业面积
                if (StringUtil.isNotEmpty(map.get("businessArea"))){
                    organization.setExt1(map.get("businessArea"));
                }
                //负责KA专员联系人
                if (StringUtil.isNotEmpty(map.get("kaContact"))){
                    organization.setExt7(map.get("kaContact"));
                }
                //负责KA专员联系方式
                if (StringUtil.isNotEmpty(map.get("kaPhone"))){
                    organization.setExt8(map.get("kaPhone"));
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
        logger.info("导入门店修改信息---> 校验 ，消耗[{"+checkInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店修改信息---> 校验结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);


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
        logger.info("导入门店修改信息---> SERVICE ，消耗[{"+interval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店修改信息---> SERVICE ，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importOkList(List<Organization> okList,String unique, SysUserDTO sysUserDTO) throws Exception{
        APIResult<String> result = new APIResult<>();
        Date logOkStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店修改信息---> 正确集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        organizationRepository.save(okList);
        result.setCode(APIResultCode.OK);
        result.setData("添加正确信息，共"+okList.size()+"条信息");
        //计算耗时
        Date logOkEndTime = DateTimeTool.getCurDatetime();
        long okInterval = (logOkEndTime.getTime() - logOkStartTime.getTime()) / 1000;
        logger.info("导入门店修改信息---> 正确集合添加，消耗[{"+okInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店修改信息---> 正确集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }

    private APIResult<String> importErrList(List<Map<String,Object>> errList,String unique, SysUserDTO sysUserDTO){
        APIResult<String> result = new APIResult<>();
        Date logErrStartTime = DateTimeTool.getCurDatetime();
        logger.info("导入门店修改信息---> 错误集合添加，开始:"+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
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
            importErrLog.setType(ImportErrType.STORE_IMPORT_EDIT.index);
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
        logger.info("导入门店修改信息---> 错误集合添加，消耗[{"+errInterval+"}]秒 ，唯一值："+unique);
        logger.info("导入门店修改信息---> 错误集合添加，结束："+DateTimeTool.getCurDatetimeSSSFormatStr()+"，唯一值："+unique);
        return result;
    }
}
