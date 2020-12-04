package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.appservice.ProductionCodeAppService;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebd.appservice.MaterialAppService;
import com.arlen.ebd.dto.MaterialDTO;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebt.appservice.impl.BillBizAppServiceImpl;
import com.arlen.ebt.dto.VerifiedCodeDTO;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.util.ComUtils;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.repository.ProjBillHeaderRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ProjBillBizAppServiceImpl extends BillBizAppServiceImpl {
    private static Logger log = LoggerFactory.getLogger(ProjBillBizAppServiceImpl.class);
    @Resource
    private ProductionCodeAppService productionCodeAppService;
    @Resource
    private ProductionCodeRepository productionCodeRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private MaterialAppService materialAppService;
    @Resource
    private ProjBillHeaderRepository projBillHeaderRepository;
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private SupplyRelationRepository supplyRelationRepository;

    /**
     * RDC编码回收验证
     * 只验证编码存不存在
     *
     * @param code       扫描码
     * @param orgId      组织ID
     * @param isVirtural 是否虚拟
     * @return APIResult
     */
    @Override
    public APIResult<VerifiedCodeDTO> getVerifiedCodeForRDCRecycle(String code, String orgId, boolean isVirtural, String billTypeId) {
        //返回对象
        APIResult<VerifiedCodeDTO> result = new APIResult<>();
        //回收单校验
        //增加新品产品校验
        if (ComUtils.DEALER_FROM_STORE_IN.equals(billTypeId) || ComUtils.D2_FROM_STORE_IN.equals(billTypeId)) {

            ProductionCode productionCode = productionCodeAppService.findProductionCode(code);
            if (productionCode == null) {
                return result.fail(APIResultCode.NOT_FOUND, "编码不存在");
            }
            //如果是虚拟编码，则代表扫描的是上级编码
            if (isVirtural) {
                productionCode = productionCodeRepository.load(productionCode.getRootCode());
                if (productionCode == null) {
                    return result.fail(APIResultCode.NOT_FOUND, "编码不存在");
                }
            }
            //校验产品
            APIResult<MaterialDTO> materialDtoApiResult = materialAppService.getById(productionCode.getMaterialId());
            if (materialDtoApiResult.getCode() != APIResultCode.OK) {
                return result.fail(1404, "没有找到对应的产品");
            }
            MaterialDTO materialDTO = materialDtoApiResult.getData();
            VerifiedCodeDTO verifiedCodeDTO = buildVerifiedCodeDTO(productionCode);
            //是否必须整比例在库
            verifiedCodeDTO.setIsMustFull(false);
            //新品产品码,配置的单据类型使用强校验
            //配置强校验&&产品为新品
            boolean checkByNewMaterial = checkByNewMaterial(billTypeId, materialDTO);
            if (productionCode.isMinSaleUnit()) {
                //检测退货门店是否在盘点
                APIResult<Boolean> isStResult = checkOrgIsSt(productionCode.getShouldOrgId());
                if (isStResult.getCode() != 0) {
                    return result.fail(isStResult.getCode(), isStResult.getErrMsg());
                }
                //检测退货门店是否在盘点
                boolean isSt = isStResult.getData();
                if (isSt) {
                    return result.fail(45001, "门店正在盘点中，无法退货");
                }
                if (productionCode.getShouldOrgId().equals(orgId) && !productionCode.getIsRunning()) {
                    return result.fail(45003, "编码在库");
                }
                //新品产品码,配置的单据类型使用强校验
                //动销码,位置在门店，则经销商和分销商不可以扫码退货
                if (checkByNewMaterial) {
                    //return result.fail(45004, productionCode.getShouldOrgType() == OrgType.STORE.index ? "门店动销产品，无法退货" : "编码在途");
                    return result.fail(45004, "编码在途");
                }
                APIResult<Boolean> booleanApiResult = checkCodeIsSale(productionCode.getId());
                if (booleanApiResult.getCode() != 0) {
                    return result.fail(booleanApiResult.getCode(), booleanApiResult.getErrMsg());
                }
                //是否已销售
                boolean isSale = booleanApiResult.getData();
                if (isSale) {
                    return result.fail(45001, "商品已售出，禁止回收");
                }
                if (productionCode.getIsRunning()) {
                    return result.fail(45003, "编码在途");
                }
                List<BillHeader> headerList = projBillHeaderRepository.getListByOrgAndScanCode(orgId, productionCode.getShouldOrgId(), productionCode.getId());
                if (headerList.isEmpty()) {
                    //经销商编码回收,根据迁移单和供货关系判断是否可发
                    if (billTypeId.equals(ComUtils.DEALER_FROM_STORE_IN)) {
                        boolean isNormal = checkCodeByTransfer(productionCode, orgId);
                        if (!isNormal) {
                            return result.fail(45002, "非本主体货物，禁止回收");
                        }
                    } else {
                        return result.fail(45002, "非本主体货物，禁止回收");
                    }
                }
                return result.succeed().attachData(verifiedCodeDTO);
            }
            //获取下级编码集合
            List<ProductionCode> codeList = productionCodeRepository.getChildren(productionCode.getId());
            if (CollectionUtils.isEmpty(codeList)) {
                return result.fail(APIResultCode.NOT_FOUND, "下级编码未找到");
            }
            //是否是托盘码
            if (StringUtil.isNotEmpty(productionCode.getParentCode())) {
                //查询箱下编码是否都在库
                Boolean existNotInStore = codeList.stream().anyMatch(x -> !x.getShouldOrgId().equals(orgId) || x.getIsRunning());
                if (!existNotInStore) {
                    return result.fail(45005, "编码都在库");
                }
                //新品产品码,配置的单据类型使用强校验
                //配置强校验&&产品为新品
                if (checkByNewMaterial) {
                    //ProductionCode childCode = codeList.stream().filter(x -> x.getShouldOrgType() == OrgType.STORE.index).findFirst().orElse(null);
                    //if (childCode != null) {
                    //    return result.fail(45001, "动销产品，箱内有单品在门店，请逐一扫描箱内单品码退货！");
                    //}
                    //codeList = codeList.stream().filter(x -> !x.getIsRunning() && x.getShouldOrgType() != OrgType.STORE.index).collect(Collectors.toList());
                    codeList = codeList.stream().filter(x -> !x.getIsRunning()).collect(Collectors.toList());
                }
                //门店为销售的码
                for (ProductionCode childCode : codeList) {
                    //校验扫描编码是否正常
                    checkChildCodeNormal(result, childCode, orgId, billTypeId);
                    if (result.getCode() != 0) {
                        return result;
                    }
                }
                //关联子编码，并计算对应的数量
                setVerifiedCodeChildren(verifiedCodeDTO, codeList, materialDTO, false);
            } else {
                //获取整托单品码
                List<ProductionCode> singlesCodeList = productionCodeRepository.getSingleCodes(productionCode.getId());
                //是否存在编码不在库
                Boolean existNotInStore = singlesCodeList.stream().anyMatch(x -> !orgId.equals(x.getShouldOrgId()) || x.getIsRunning());
                if (!existNotInStore) {
                    return result.fail(1, "编码都在库");
                }
                for (ProductionCode childCode : singlesCodeList) {
                    //校验扫描编码是否正常
                    checkChildCodeNormal(result, childCode, orgId, billTypeId);
                    if (result.getCode() != 0) {
                        return result;
                    }
                }
                //扫码为跟编码时
                //新品产品码,配置的单据类型使用强校验
                //配置强校验&&产品为新品
                if (checkByNewMaterial) {
                    //ProductionCode childCode = codeList.stream().filter(x -> x.getShouldOrgType() == OrgType.STORE.index).findFirst().orElse(null);
                    //if (childCode != null) {
                    //    return result.fail(45001, "动销产品，箱内有单品在门店，请逐一扫描箱内单品码退货！");
                    //}
                    //返回所有的回收编码
                    //List<ProductionCode> normalSinglesCodeList = singlesCodeList.stream().filter(x -> !x.getIsRunning() && x.getShouldOrgType() != OrgType.STORE.index).collect(Collectors.toList());
                    List<ProductionCode> normalSinglesCodeList = singlesCodeList.stream().filter(x -> !x.getIsRunning()).collect(Collectors.toList());
                    if (normalSinglesCodeList.size() < normalSinglesCodeList.size()) {
                        verifiedCodeDTO.setIsFull(false);
                        verifiedCodeDTO.setQty(normalSinglesCodeList.size());
                        verifiedCodeDTO.setChildren(normalSinglesCodeList);
                    } else {
                        //关联子编码，并计算对应的数量
                        setVerifiedCodeChildren(verifiedCodeDTO, codeList, materialDTO, false);
                    }
                }
            }
            return result.succeed().attachData(verifiedCodeDTO);
        }
        return super.getVerifiedCodeForRDCRecycle(code, orgId, isVirtural, billTypeId);
    }

    private void checkChildCodeNormal(APIResult<VerifiedCodeDTO> result, ProductionCode childCode, String orgId, String billTypeId) {
        //检测退货门店是否在盘点
        APIResult<Boolean> isStResult = checkOrgIsSt(childCode.getShouldOrgId());
        if (isStResult.getCode() != 0) {
            result.fail(isStResult.getCode(), isStResult.getErrMsg());
        }
        //检测退货门店是否在盘点
        boolean isSt = isStResult.getData();
        if (isSt) {
            result.fail(45001, "箱内有单品在门店，门店正在盘点中，请逐一扫描箱内单品码退货！");
            return;
        }
        APIResult<Boolean> booleanApiResult = checkCodeIsSale(childCode.getId());
        if (booleanApiResult.getCode() != 0) {
            result.fail(booleanApiResult.getCode(), booleanApiResult.getErrMsg());
        }
        //是否已销售
        boolean isSale = booleanApiResult.getData();
        if (isSale) {
            result.fail(45001, "箱内有单品已售出，请逐一扫描箱内单品码退货！");
            return;
        }
        //是否在途
        if (childCode.getIsRunning()) {
            result.fail(45001, "箱内有单品在途，请逐一扫描箱内单品码退货！");
            return;
        }
        //是否是本经销商和分销上发货的码
        List<BillHeader> headerList = projBillHeaderRepository.getListByOrgAndScanCode(orgId, childCode.getShouldOrgId(), childCode.getId());
        if (headerList.isEmpty()) {
            //经销商编码回收,根据迁移单和供货关系判断是否可发
            if (billTypeId.equals(ComUtils.DEALER_FROM_STORE_IN)) {
                boolean isNormal = checkCodeByTransfer(childCode, orgId);
                if (!isNormal) {
                    result.fail(45001, "箱内有单品非本主体货物，请逐一扫描箱内单品码退货！");
                    return;
                }
            } else {
                result.fail(45001, "箱内有单品非本主体货物，请逐一扫描箱内单品码退货！");
                return;
            }
        }
    }

    /**
     * 根据迁移单据判断扫码是否正常
     *
     * @param code  扫码
     * @param orgId 扫码组织
     * @return boolean
     */
    private boolean checkCodeByTransfer(ProductionCode code, String orgId) {
        //确认最后一次经销商发货单
        BillHeader lastDealerOutHeader = billHeaderRepository.getByCode(code.getDealerOutId(), ComUtils.DEALER_TO_STORE_OUT);
        if (lastDealerOutHeader == null) {
            return false;
        }
        //查询最后一次发货经销商和本次扫码经销商是否存在迁移单据 DealerTransfer
        List<BillHeader> dealerTransferHeaderList = projBillHeaderRepository.getListByOrg(lastDealerOutHeader.getSrcId(), orgId, "DealerTransfer");
        if (dealerTransferHeaderList == null || dealerTransferHeaderList.isEmpty()) {
            return false;
        }
        //查询本次扫码经销商和门店是否有供货关系
        SupplyRelation relation = supplyRelationRepository.getSupplyRelation(orgId, code.getShouldOrgId());
        return relation != null;
    }

    /**
     * 入库编码验证
     *
     * @param code         编码
     * @param isVirtural   是否整托
     * @param checkBoxFull 是否验证整箱
     * @param billId       单据id
     * @return 验证
     */
    @Override
    public APIResult<VerifiedCodeDTO> getVerifiedCodeForRdcIn(String code, boolean isVirtural, boolean checkBoxFull, String orgId, String billTypeId, String billId) {
        if ("DealerFromStoreInMeiChi".equals(billTypeId) || "D2FromStoreInMeiChi".equals(billTypeId) || ComUtils.RDC_FROM_DEALER_IN.equals(billTypeId)) {
            APIResult<VerifiedCodeDTO> result = new APIResult<>();
            List<ProductionCode> codeList = productionCodeAppService.getSelfOrSamePackageCodesInTopPackage(code, isVirtural);
            if (codeList.size() <= 0) {
                ProductionCode productionCode = productionCodeAppService.findProductionCode(code);
                if (productionCode == null) {
                    return result.fail(APIResultCode.NOT_FOUND, "编码不存在");
                }
                if (isVirtural) {
                    productionCode = productionCodeRepository.load(productionCode.getRootCode());
                    if (productionCode == null) {
                        return result.fail(APIResultCode.NOT_FOUND, "编码不存在");
                    }
                }
                return result.fail(APIResultCode.ALREADY_EXSISTED, "already existed");
            }
            ProductionCode oneCode = codeList.stream().filter(x -> x.getId().equals(code) || x.getEptCode().equals(code)).findFirst().orElse(null);
            if (oneCode != null && oneCode.isMinSaleUnit()) {
                if (StringUtils.isEmpty(oneCode.getRunningBillId()) || !oneCode.getRunningBillId().equals(billId)) {
                    return result.fail(45006, "非本单产品");
                }
            } else {
                oneCode = codeList.get(0);
                List<ProductionCode> childList = productionCodeRepository.getChildren(oneCode.getId());
                List<ProductionCode> thisBillCodeList = childList.stream().filter(x -> StringUtils.isNotEmpty(x.getRunningBillId()) && x.getRunningBillId().equals(billId)).collect(Collectors.toList());
                if (thisBillCodeList.size() != childList.size()) {
                    return result.fail(45006, "箱内有非本单产品，请逐一扫描箱内单品码！");
                }
            }
        }
        return super.getVerifiedCodeForRdcIn(code, isVirtural, checkBoxFull, orgId, billTypeId, billId);
    }

    /**
     * 是否使用新码强校验
     *
     * @return boolean
     */
    private boolean checkByNewMaterial(String billTypeId, MaterialDTO materialDTO) {
        SysPara splitByNewMaterial = sysParaRepository.load("splitByNewMaterial");
        return splitByNewMaterial != null
                //是否根据新旧品拆分单据
                && StringUtils.isNotEmpty(splitByNewMaterial.getValue()) && ComUtils.TRUE.equals(splitByNewMaterial.getValue())
                //根据备注中配置的单据进行拆单
                && StringUtils.isNotEmpty(splitByNewMaterial.getRemark()) && splitByNewMaterial.getRemark().contains(billTypeId + ";")
                //产品是新品(强校验产品)
                && materialDTO.getNewMaterial() != null && materialDTO.getNewMaterial();
    }

    /**
     * 检测单品是否销售
     *
     * @param id 单品码
     */
    private APIResult<Boolean> checkCodeIsSale(String id) {
        String url = sysProperties.getProperty("thisProject.meiChi.view.url");
        String user = sysProperties.getProperty("thisProject.meiChi.view.user");
        String password = sysProperties.getProperty("thisProject.meiChi.view.password");
        String driverName = sysProperties.getProperty("thisProject.meiChi.view.driverClass");
        boolean isSetParam = StringUtils.isEmpty(url) || StringUtils.isEmpty(user) || StringUtils.isEmpty(password) || StringUtils.isEmpty(driverName);
        if (isSetParam) {
            return new APIResult<Boolean>().fail(403, "美驰中间库连接未设置");
        }
        Connection conn = excelUtilAppService.getConnection(url, user, password, driverName);
        if (conn == null) {
            return new APIResult<Boolean>().fail(403, "美驰中间库连接失败");
        }
        String sql = "SELECT * FROM YS_InventoryCodeLib where PieceCode='" + id + "'";
        String code = "";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                code = resultSet.getString(1);
            }
            return new APIResult<Boolean>().succeed().attachData(StringUtils.isNotEmpty(code));
        } catch (SQLException e) {
            // 回滚
            try {
                conn.rollback();
            } catch (SQLException ex) {
                log.error("检测单品是否销售,数据查询失败：\"{}\"", ex);
            }
            return new APIResult<Boolean>().fail(403, "美驰中间库查询单品是否销售失败");
        }
    }

    /**
     * 检测退货门店是否在盘点
     *
     * @param orgId 退货门店
     */
    private APIResult<Boolean> checkOrgIsSt(String orgId) {
        String url = sysProperties.getProperty("thisProject.meiChi.view.url");
        String user = sysProperties.getProperty("thisProject.meiChi.view.user");
        String password = sysProperties.getProperty("thisProject.meiChi.view.password");
        String driverName = sysProperties.getProperty("thisProject.meiChi.view.driverClass");
        boolean isSetParam = StringUtils.isEmpty(url) || StringUtils.isEmpty(user) || StringUtils.isEmpty(password) || StringUtils.isEmpty(driverName);
        if (isSetParam) {
            return new APIResult<Boolean>().fail(403, "美驰中间库连接未设置");
        }
        Connection conn = excelUtilAppService.getConnection(url, user, password, driverName);
        if (conn == null) {
            return new APIResult<Boolean>().fail(403, "美驰中间库连接失败");
        }
        String sql = "SELECT * FROM YS_CheckClientInventory where YSID='" + orgId + "'";
        String code = "";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                code = resultSet.getString(1);
            }
            return new APIResult<Boolean>().succeed().attachData(StringUtils.isNotEmpty(code));
        } catch (SQLException e) {
            // 回滚
            try {
                conn.rollback();
            } catch (SQLException ex) {
                log.error("退货门店是否在盘点,数据查询失败：\"{}\"", ex);
            }
            return new APIResult<Boolean>().fail(403, "美驰中间库查询退货门店是否在盘点失败");
        }
    }
}
