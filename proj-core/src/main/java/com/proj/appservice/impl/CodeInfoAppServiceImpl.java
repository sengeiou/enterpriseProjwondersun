package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.BillType;
import com.arlen.ebt.enums.BillTypeDirection;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillTypeRepository;
import com.arlen.ecipher.service.DESService;
import com.proj.appservice.CodeInfoAppService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by ehsure on 2016/9/13.
 */
@Service
public class CodeInfoAppServiceImpl implements CodeInfoAppService {
    @Resource
    private ProductionCodeRepository productionCodeRepository;
    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private BillTypeRepository billTypeRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private DESService desService;

    /**
     * 根据箱码查询下级编码集合
     *
     * @param boxCode 箱码
     * @return
     */
    @Override
    public APIResult<List<Map<String, Object>>> getByBoxCode(String boxCode) {
        APIResult<List<Map<String, Object>>> result = new APIResult<>();
        List<Map<String, Object>> getProductionCodeDTOList = new ArrayList<>();
        //根据箱码获取下级编码集合
        List<ProductionCode> productionCodeList = productionCodeRepository.getCodeByParentCode(boxCode);
        for (ProductionCode productionCode : productionCodeList) {
            //构造DTO
            String eptCode = "";
            try {
                eptCode = desService.equalsEncrypt(productionCode.getId());
            } catch (Exception e) {
                e.getMessage();
            }
            APIResult<Map<String, Object>> result1 = buildGetProductionCodeDTO(productionCode,eptCode);
            getProductionCodeDTOList.add(result1.getData());
        }
        return result.succeed().attachData(getProductionCodeDTOList);
    }

    /**
     * 根据积分码或者单品码查询数据
     *
     * @param eptCode 积分码
     * @param code    单品码
     * @return
     */
    @Override
    public APIResult<Map<String, Object>> getByCode(String code, String eptCode) {
        ProductionCode productionCode = new ProductionCode();
        //如果单品码不为空，积分码为空
        if (StringUtil.isNotEmpty(code) && StringUtil.isEmpty(eptCode)) {
            productionCode = productionCodeRepository.load(code);
            try {
                eptCode = desService.equalsEncrypt(code);
            } catch (Exception e) {
                e.getMessage();
            }
        }
        //如果单品码为空，积分码不为空
        if (StringUtil.isNotEmpty(eptCode) && StringUtil.isEmpty(code)) {
            //先解密成单品码再查询
            APIResult<String> decryptResult = desService.decrypt(eptCode);
            if (decryptResult.getCode() != 0) {
                //解密失败
                return null;
            }
            String decryptedCode = decryptResult.getData();
            productionCode = productionCodeRepository.load(decryptedCode);
        }
        if (productionCode == null) {
            return null;
        }
        APIResult<Map<String, Object>> result = buildGetProductionCodeDTO(productionCode, eptCode);
        return result.succeed();
    }

    /**
     * 构建DTO
     */
    public APIResult<Map<String, Object>> buildGetProductionCodeDTO(ProductionCode productionCode, String eptCode) {
        APIResult<Map<String, Object>> mapAPIResult = new APIResult<>();
        Map<String, Object> map = new HashMap<>();
        map.put("Code", productionCode.getId());
        map.put("EptCode", eptCode);
        map.put("BoxCode", productionCode.getParentCode());
        map.put("PalletCode", productionCode.getRootCode());
        map.put("BatchCode", productionCode.getBatchCode());
        map.put("PackDate", productionCode.getPackDate());
        map.put("ValidDate", productionCode.getValidDate());
        map.put("PrintDate", productionCode.getPrintDate());
        map.put("CurrentLocation", productionCode.getShouldOrgId());
        map.put("AddTime", productionCode.getAddTime());
        map.put("AddBy", productionCode.getAddBy());
        //判断对应的产品存不存在
        Material material = materialRepository.load(productionCode.getMaterialId());
        if (material == null) {
            map.put("ProductCode", null);
            map.put("ProductSKU", null);
            map.put("ProductName", null);
            map.put("ProductId", null);
        } else {
            map.put("ProductCode", material.getShortCode());
            map.put("ProductSKU", material.getSku());
            map.put("ProductName", material.getShortName());
            map.put("ProductId", material.getId());
        }


        //所属经销商
        String sendToCode = null;
        if (StringUtil.isNotEmpty(productionCode.getDealerInId())) {
            BillHeader billHeader = billHeaderRepository.getByCode(productionCode.getDealerInId(), "DealerFromRDCIn");
            if (billHeader != null) {
                sendToCode = billHeader.getDestId();
            }
        }


        //所属门店
        String sendToPos = null;
        if (StringUtil.isNotEmpty(productionCode.getDealerOutId())) {
            BillHeader billHeader1 = billHeaderRepository.getByCode(productionCode.getDealerOutId(), "DealerToStoreOut");
            if (billHeader1 != null) {
                sendToPos = billHeader1.getDestId();
                //所属经销商
                if (StringUtil.isEmpty(sendToCode)) {
                    sendToCode = billHeader1.getSrcId();
                }
            }
        }

        //所属经销商
        if (StringUtil.isEmpty(sendToCode)) {
            BillHeader billHeader1 = billHeaderRepository.getByCode(productionCode.getStoreOutId(), "RDCToDealerOut");
            if (billHeader1 != null) {
                sendToCode = billHeader1.getDestId();
            }
        }

        //所属分销商
        String sendToD2Code = this.getCodeOfD2Id(productionCode);

        //所属门店
        if (StringUtil.isNotEmpty(sendToD2Code)) {
            //根据单据类型集合，获得编码最后单据Id（流向逆推）
            String billTypeIds = "DealerToStoreOut,DealerWantToStoreOut,D2ToStoreOut,D2WantToStoreOut,StoreToStoreOut";
            String lastBillId = this.getCodeOfLastBillId(productionCode, billTypeIds);
            if (StringUtil.isNotEmpty(lastBillId)) {
                BillHeader billHeader1 = billHeaderRepository.load(lastBillId);
                if (billHeader1 != null) {
                    sendToPos = billHeader1.getDestId();
                }
            }
        }

        map.put("SendToCode", sendToCode);
        map.put("SendToPOS", sendToPos);
        map.put("SendToD2Code", sendToD2Code);

        return mapAPIResult.succeed().attachData(map);
    }

    /**
     * 获得编码所属分销商（流向逆推）
     *
     * @param productionCode
     * @return
     */
    public String getCodeOfD2Id(ProductionCode productionCode) {
        String d2Id = null;
        if (productionCode != null) {
            //获取编码路由
            String route = productionCode.getRoute();
            if (StringUtil.isNotEmpty(route)) {
                //流向倒序循环
                String[] routeArry = route.split(";");
                for (int i = routeArry.length - 1; i >= 0; i--) {
                    String[] billArry = routeArry[i].split(",");
                    String billId = billArry[0];
                    String billTypeId = billArry[1];
                    BillHeader billHeader = billHeaderRepository.load(billId);//获取单据信息
                    BillType billType = billTypeRepository.load(billTypeId);//获取单据类型信息
                    if (billHeader != null && billType != null) {
                        if ("SYS_ADJUST".equals(billType.getId())) {
                            //系统调整单：判断收货方
                            Organization destOrg = organizationRepository.load(billHeader.getDestId());//获取收货方
                            if (destOrg != null) {
                                if (destOrg.getOrgType() == OrgType.DISTRIBUTOR.index) {
                                    //正常情况：收货方==分销商，停止查找
                                    //d2Id=destId
                                    d2Id = billHeader.getDestId();
                                    break;
                                } else if (destOrg.getOrgType() == OrgType.STORE.index) {
                                    //正常情况：收货方==门店，继续查找
                                    continue;
                                } else {
                                    //正常情况：收货方!=分销商和门店，停止查找
                                    break;
                                }
                            } else {
                                //异常情况：判断不了分销商，停止查找
                                break;
                            }
                        } else {
                            //非系统调整单：判断主体
                            if (billType.getSubjectType() == OrgType.DISTRIBUTOR.index) {
                                //正常情况：主体==分销商，停止查找
                                //单据方向出库：fromOrTo==1--->d2Id=srcId
                                //单据方向入库：fromOrTo==-1-->d2Id=destId
                                d2Id=billType.getFromOrTo() == BillTypeDirection.OUT.index ? billHeader.getSrcId() : billHeader.getDestId();
                                break;
                            } else if (billType.getSubjectType() == OrgType.STORE.index) {
                                //正常情况：主体==门店，继续查找
                                continue;
                            } else {
                                //正常情况：主体!=分销商和门店，停止查找
                                break;
                            }
                        }
                    } else {
                        //异常情况：判断不了分销商，停止查找
                        break;
                    }
                }
            }
        }
        return d2Id;
    }

    /**
     * 根据单据类型集合，获得编码最后单据Id（流向逆推）
     *
     * @param productionCode
     * @param billTypeIds
     * @return
     */
    public String getCodeOfLastBillId(ProductionCode productionCode, String billTypeIds) {
        String billId = null;
        if (productionCode != null && StringUtil.isNotEmpty(billTypeIds)) {
            String[] billTypeIdArry = billTypeIds.split(",");
            if (billTypeIdArry.length > 0) {
                //获取编码路由
                String route = productionCode.getRoute();
                if (StringUtil.isNotEmpty(route)) {
                    //流向倒序循环
                    String[] routeArry = route.split(";");
                    for (int i = routeArry.length - 1; i >= 0; i--) {
                        String[] billArry = routeArry[i].split(",");
                        String routeBillId = billArry[0];
                        String routeBillTypeId = billArry[1];
                        //根据单据类型进行匹配
                        if (Arrays.asList(billTypeIdArry).contains(routeBillTypeId)) {
                            billId = routeBillId;
                            break;
                        }
                    }
                }
            }
        }
        return billId;
    }

}
