package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebt.appservice.impl.BillHeaderAppServiceImpl;
import com.arlen.ebt.entity.BillDetail;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.BillType;
import com.arlen.ebt.enums.BillStatus;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillTypeRepository;
import com.arlen.utils.common.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ehsure on 2019/12/30.
 *
 * @author arlenChen
 */
public class ProjBillHeaderAppServiceImpl extends BillHeaderAppServiceImpl {
    @Resource
    private BillHeaderRepository repo;
    @Resource
    private ProductionCodeRepository codeRepository;
    @Resource
    private BillTypeRepository billTypeRepository;

    /**
     * 作废单据
     *
     * @param id 单据ID
     * @return APIResult
     */
    @Override
    public APIResult<String> abandonBill(String id) {
        APIResult<String> result = new APIResult<>();
        BillHeader billHeader = repo.load(id);
        if (billHeader == null) {
            return result.fail(APIResultCode.NOT_FOUND, "BillHeader is not find");
        }
        if (billHeader.getBillStatus() != BillStatus.Confirm.index) {
            return result.fail(405, "Billheader.[Status] Is not Confirm");
        }
        billHeader.setBillStatus(BillStatus.Abandoned.index);
        //由于生成数据的runningBillId 没有索引，只能曲线救国
        //根据单据类型的出入库判断主体id,主体id和产品共同查询出对应明细的在途码
        BillType billType = billTypeRepository.load(billHeader.getBillTypeId());
        if (billType == null) {
            return result.fail(APIResultCode.NOT_FOUND, "BillType is not find");
        }
        String orgId = billType.getFromOrTo() == 1 ? billHeader.getSrcId() : billHeader.getDestId();
        List<ProductionCode> codeList = new ArrayList<>();
        for (BillDetail detail : billHeader.getDetailList()) {
            List<ProductionCode> list = codeRepository.getByOrgIdAndMaterialId(orgId, detail.getMaterial().getId());
            List<ProductionCode> runningList = list.stream().filter(x -> StringUtils.isNotEmpty(x.getRunningBillId()) && x.getRunningBillId().equals(billHeader.getId())).collect(Collectors.toList());
            codeList.addAll(runningList);
        }
        for (ProductionCode code : codeList) {
            code.setRunningBillId(null);
            code.setIsRunning(false);
        }
        codeRepository.save(codeList);
        repo.save(billHeader);
        return result.succeed();
    }
}
