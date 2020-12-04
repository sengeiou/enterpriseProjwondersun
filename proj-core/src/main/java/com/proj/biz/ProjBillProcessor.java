package com.proj.biz;

import com.eaf.core.utils.StringUtil;
import com.ebc.entity.ProductionCode;
import com.ebp.entity.Organization;
import com.ebp.entity.SysPara;
import com.ebp.enums.OrgType;
import com.ebp.repository.OrganizationRepository;
import com.ebp.repository.SysParaRepository;
import com.ebt.biz.billprocess.BillProcessContext;
import com.ebt.biz.billprocess.BillProcessor;
import com.ebt.biz.billprocess.ScanCodeInf;
import com.ebt.entity.*;
import com.ebt.repository.AdjustRecordRepository;
import com.ebt.repository.BillHeaderRepository;
import com.proj.repository.ProjExceptionDataRepository;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ehsure on 2018/3/12.
 * 重写
 */
public class ProjBillProcessor extends BillProcessor {

    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private ProjExceptionDataRepository exceptionDataRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private AdjustRecordRepository adjustRecordRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;

    @Override
    public Integer reckonSaleQtyPcs(BillProcessContext billProcessContext, List<ScanCodeInf> codeInfList) {
        Map<String, String> map = new HashMap<>();
        Map<String, String> adjustMap = new HashMap<>();
        //是否需要保存销售数量
        SysPara saveSaleQtyPcs = sysParaRepository.load("saveSaleQtyPcs");
        if (saveSaleQtyPcs != null) {
            if ("true".equalsIgnoreCase(saveSaleQtyPcs.getValue())) {
                BillType originBillType = billProcessContext.getBillInf().getBillType();
                //指定的单据类型来保存销售数量
                if ("DealerToD2Out".equals(originBillType.getId()) || "DealerToStoreOut".equals(originBillType.getId()) || "D2ToStoreOut".equals(originBillType.getId())) {
                    List<AdjustRecord> list = new ArrayList<>();
                    //本次是否有被调整过
                    List<ScanCodeInf> scanCodeInfList = codeInfList.stream().filter(x -> !x.isAdjusted()).collect(Collectors.toList());
                    //本次调整过的数据
                    List<ScanCodeInf> adjustCodeList = codeInfList.stream().filter(x -> x.isAdjusted()).collect(Collectors.toList());
                    for (ScanCodeInf scanCodeInf : adjustCodeList) {
                        //调整销量
                        adjustMap.put(scanCodeInf.getProductionCode().getId(), null);
                    }
                    String code = billProcessContext.getBillInf().getBillHeader().getCode();
                    String id = billProcessContext.getBillInf().getBillHeader().getId();
                    String orgId = billProcessContext.getBillInf().getBillHeader().getSrcId();
                    Date operateTime = billProcessContext.getBillInf().getBillHeader().getOperateTime();
                    for (ScanCodeInf scanCodeInf : scanCodeInfList) {
                        ProductionCode productionCode = scanCodeInf.getProductionCode();
                        if (productionCode != null) {
                            String route = productionCode.getRoute();
                            if (StringUtil.isNotEmpty(route)) {
                                String[] routeArr = route.split(";");
                                if (routeArr.length > 0) {
                                    String[] infoArray = routeArr[routeArr.length - 1].split(",");
                                    if (infoArray.length > 1 && !"SYS_ADJUST".equals(infoArray[1])) {
                                        //实际销量
                                        map.put(productionCode.getId(), null);
                                    } else {
                                        String msg = "";
                                        BillHeader adjustBillHeader = billHeaderRepository.load(infoArray[0]);
                                        Organization adJustDest = organizationRepository.load(adjustBillHeader.getDestId());
                                        Organization adJustSrc = organizationRepository.load(adjustBillHeader.getSrcId());
                                        if (adJustDest != null && adJustSrc != null) {
                                            if (StringUtil.isNotEmpty(adJustDest.getMainCode()) && StringUtil.isNotEmpty(adJustSrc.getMainCode()) && adJustDest.getMainCode().equals(adJustSrc.getMainCode())) {
                                                msg = "一家两户";
                                            } else if (StringUtil.isNotEmpty(adjustBillHeader.getSourceBillCode()) && adjustBillHeader.getSourceBillCode().contains("P")) {
                                                msg = "盘点异常";
                                            } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.DEALER.index && StringUtil.isNotEmpty(adJustDest.getMainCode()) && StringUtil.isNotEmpty(adJustSrc.getMainCode()) && !adJustDest.getMainCode().equals(adJustSrc.getMainCode())) {
                                                msg = "跨经销商异常";
                                            } else if (adJustDest.getOrgType() == OrgType.DISTRIBUTOR.index && adJustSrc.getOrgType() == OrgType.DISTRIBUTOR.index ) {
                                                msg = "跨分销商异常";
                                            } else if (adJustDest.getOrgType() == OrgType.DISTRIBUTOR.index && adJustSrc.getOrgType() == OrgType.STORE.index) {
                                                msg = "货在门店异常";
                                            } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.STORE.index) {
                                                msg = "货在门店异常";
                                            } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.RDC.index) {
                                                msg = "未入库操作";
                                            }  else if (adJustDest.getOrgType() == OrgType.DISTRIBUTOR.index && adJustSrc.getOrgType() == OrgType.DEALER.index) {
                                                msg = "未入库操作";
                                            } else {
                                                msg = "其他异常";
                                            }
                                        }
                                        AdjustRecord record = new AdjustRecord();
                                        record.setId(UUID.randomUUID().toString());
                                        record.setOperateTime(operateTime);
                                        record.setProductionCode(productionCode.getId());
                                        record.setOrgId(orgId);
                                        record.setAdjustId(infoArray[0]);
                                        record.setCode(code);
                                        record.setHeaderId(id);
                                        record.setAttr1(msg);
                                        list.add(record);
                                    }
                                }
                            }
                        }
                    }
                    //本次有需要调整的数据时，根据调整的单据类型获取上次处理的仓库出库单据是否有不存在的码。
                    //上次不存在而本次要调整，则算正常码
                    List<BillHeader> adjustedBillList = billProcessContext.getAdjustedBillList();
                    BillHeader sourceHeader = billProcessContext.getBillInf().getBillHeader();
                    BillHeader header = null;
                    boolean isAdjust = false;
                    for (BillHeader adjustBillHeader : adjustedBillList) {
                        Organization rdc = organizationRepository.load(adjustBillHeader.getSrcId());
                        int rdcType = -1;
                        if (rdc != null) {
                            rdcType = rdc.getOrgType();
                        }
                        if ("SYS_ADJUST".equals(adjustBillHeader.getBillTypeId()) && adjustBillHeader.getDestId().equals(sourceHeader.getSrcId()) && rdcType == OrgType.RDC.index) {
                            header = adjustBillHeader;
                            isAdjust = true;
                        }
                        if (isAdjust) {
                            List<ExceptionData> exceptionDataList = exceptionDataRepository.listByObjectIdAndSubjectId(header.getSrcId(), header.getDestId(), "编码不存在", "RDCToDealerOut");
                            if ("D2ToStoreOut".equals(originBillType.getId())) {
                                exceptionDataList = exceptionDataRepository.listByObjectIdAndSubjectId(header.getSrcId(), header.getDestId(), "编码不存在", "DealerToD2Out");
                            }
                            Map<String, String> exceptionDataMap = new HashMap<>();
                            for (ExceptionData exceptionData : exceptionDataList) {
                                if (StringUtil.isNotEmpty(exceptionData.getScanCode())) {
                                    exceptionDataMap.put(exceptionData.getScanCode(), null);
                                }
                            }
                            for (ScanCodeInf scanCodeInf : adjustCodeList) {
                                ProductionCode productionCode = scanCodeInf.getProductionCode();
                                boolean isAdjustScanCodeInf=productionCode != null && (exceptionDataMap.containsKey(productionCode.getId()) || exceptionDataMap.containsKey(productionCode.getParentCode()));
                                if (isAdjustScanCodeInf) {
                                    map.put(productionCode.getId(), null);
                                    adjustMap.remove(productionCode.getId());
                                }
                            }
                        }
                    }
                    for (Map.Entry<String, String> map1 : adjustMap.entrySet()) {
                        String msg = "";
                        String adjustCode = "";
                        String adjustId = "";
                        if (adjustedBillList.size() > 0) {
                            BillHeader adjustBillHeader = adjustedBillList.get(0);
                            if (adjustBillHeader != null) {
                                adjustCode = adjustBillHeader.getCode();
                                adjustId = adjustBillHeader.getId();
                                Organization adJustDest = organizationRepository.load(adjustBillHeader.getDestId());
                                Organization adJustSrc = organizationRepository.load(adjustBillHeader.getSrcId());
                                if (adJustDest != null && adJustSrc != null) {
                                    if (StringUtil.isNotEmpty(adJustDest.getMainCode()) && StringUtil.isNotEmpty(adJustSrc.getMainCode()) && adJustDest.getMainCode().equals(adJustSrc.getMainCode())) {
                                        msg = "一家两户";
                                    } else if (StringUtil.isNotEmpty(adjustBillHeader.getSourceBillCode()) && adjustBillHeader.getSourceBillCode().contains("P")) {
                                        msg = "盘点异常";
                                    } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.STORE.index) {
                                        msg = "货在门店异常";
                                    } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.STORE.index) {
                                        msg = "货在门店异常";
                                    } else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.RDC.index) {
                                        msg = "未入库操作";
                                    } else if (adJustDest.getOrgType() == OrgType.DISTRIBUTOR.index && adJustSrc.getOrgType() == OrgType.DEALER.index) {
                                        msg = "未入库操作";
                                    }else if (adJustDest.getOrgType() == OrgType.DEALER.index && adJustSrc.getOrgType() == OrgType.DEALER.index && StringUtil.isNotEmpty(adJustDest.getMainCode()) && StringUtil.isNotEmpty(adJustSrc.getMainCode()) && !adJustDest.getMainCode().equals(adJustSrc.getMainCode())) {
                                        msg = "跨经销商异常";
                                    }  else if (adJustDest.getOrgType() == OrgType.DISTRIBUTOR.index && adJustSrc.getOrgType() == OrgType.DISTRIBUTOR.index ) {
                                        msg = "跨分销商异常";
                                    } else {
                                        msg = "其他异常";
                                    }
                                }
                            }
                        }
                        AdjustRecord record = new AdjustRecord();
                        record.setId(UUID.randomUUID().toString());
                        record.setOperateTime(operateTime);
                        record.setOrgId(orgId);
                        record.setAdjustCode(adjustCode);
                        record.setAdjustId(adjustId);
                        record.setCode(code);
                        record.setHeaderId(id);
                        record.setProductionCode(map1.getKey());
                        record.setAttr1(msg);
                        list.add(record);
                    }
                    adjustRecordRepository.insert(list);
                }
            }
        }
        return map.size();
    }
}
