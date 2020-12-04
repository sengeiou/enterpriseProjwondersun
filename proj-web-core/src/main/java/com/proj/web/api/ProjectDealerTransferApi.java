package com.proj.web.api;

import com.alibaba.druid.util.StringUtils;
import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebd.appservice.MaterialAppService;
import com.ebd.dto.MaterialDTO;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.ebt.appservice.BillHeaderAppService;
import com.ebt.dto.BillDetailDTO;
import com.ebt.dto.BillHeaderDTO;
import com.ebt.enums.BillAddFromType;
import com.ebt.enums.BillStatus;
import com.proj.appservice.DealerD2RelationAppService;
import com.proj.appservice.ProjRdcToDealerOutAppService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 总部
 * 经销商迁移单
 * Created  by arlenChen in 2020/4/13.
 *
 * @author arlenChen
 */
@Controller
@RequestMapping("/api/proj/dealertransfer")
public class ProjectDealerTransferApi extends BaseController {
    @Resource
    private BillHeaderAppService appService;
    @Resource
    private ProjRdcToDealerOutAppService projRdcToDealerOutAppService;
    @Resource
    private MaterialAppService materialAppService;
    @Resource
    private DealerD2RelationAppService dealerD2RelationAppService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<BillHeaderDTO> getById(@RequestParam(value = "id") String id) {
        return appService.getById(id);
    }


    @RequestMapping(value = "confirm", method = RequestMethod.POST)
    @ResponseBody
    public String confirm(@RequestParam(value = "ids") String[] ids) {
        int k = 0;
        if (ids == null) {
            return "ids is null";
        }
        String mes = "ok";
        String userName = getCurrentUser().getUserName();
        for (String id : ids) {
            APIResult<String> result = projRdcToDealerOutAppService.transferBillConfirm(id, userName);
            if (result.getCode() != 0) {
                mes = result.getErrMsg();
                k++;
            }
        }
        if (k == ids.length) {
            return mes;
        } else {
            return "ok";
        }
    }

    @RequestMapping(value = {"delete"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object delete(@RequestParam("ids") String[] ids) {
        APIResult<String> mes = new APIResult<>();
        int k = 0;
        if (ids == null) {
            return mes.fail(809, "ids not null");
        } else {
            for (String id : ids) {
                APIResult<String> result = this.appService.delete(id);
                if (result.getCode() != 0) {
                    mes = result;
                    k++;
                }
            }
            if (k == ids.length) {
                return mes;
            } else {
                return new APIResult<String>().succeed();
            }
        }
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> add(BillHeaderDTO billHeaderDTO) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(billHeaderDTO.getSrcId())) {
            return result.fail(APIResultCode.FORBIDDEN, "迁出方不能为空！");
        }
        if (StringUtils.isEmpty(billHeaderDTO.getDestId())) {
            return result.fail(APIResultCode.FORBIDDEN, "迁入方不能为空！");
        }
        if (billHeaderDTO.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据日期不能为空！");
        }
        if (billHeaderDTO.getDetailList() == null || billHeaderDTO.getDetailList().isEmpty()) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }
        if (billHeaderDTO.getDestId().equals(billHeaderDTO.getSrcId())) {
            return result.fail(403, "迁出方和迁入方不能相同！");
        }
        if (projRdcToDealerOutAppService.haveProcessingBill(billHeaderDTO.getSrcId())) {
            return result.fail(403, "该经销商有未完成单据，请结束单据后再做迁移！");
        }
        SysUserDTO currentUser = getCurrentUser();
        billHeaderDTO.setAddTime(new Date());
        billHeaderDTO.setEditTime(new Date());
        billHeaderDTO.setAddBy(currentUser.getUserName());
        billHeaderDTO.setEditBy(currentUser.getUserName());
        billHeaderDTO.setBillStatus(BillStatus.Confirm.index);
        billHeaderDTO.setAddFrom(BillAddFromType.WEB.getValue());
        //计算预计箱数
        BigDecimal expectQty = new BigDecimal(0);
        for (BillDetailDTO detailDto : billHeaderDTO.getDetailList()
                ) {
            if (StringUtils.isEmpty(detailDto.getMaterialId())) {
                return result.fail(APIResultCode.FORBIDDEN, "产品名称不能为空！");
            }
            int pcsQty = findMaterialPcsQty(detailDto);
            if (pcsQty == 0) {
                continue;
            }
            expectQty = expectQty.add(new BigDecimal(detailDto.getExpectQtyPcs()).divide(new BigDecimal(pcsQty), 2, BigDecimal.ROUND_HALF_UP));
        }
        billHeaderDTO.setExpectQty(expectQty);

        result = appService.create(billHeaderDTO);
        if (result.getCode() == 0) {
            dealerD2RelationAppService.changeOrganizationInUse(2, billHeaderDTO.getSrcId(), currentUser.getUserName());
        }
        return result;
    }

    private int findMaterialPcsQty(BillDetailDTO billDetailDTO) {
        APIResult<MaterialDTO> dtoapiResult = materialAppService.getById(billDetailDTO.getMaterialId());
        int pcsQty = 0;
        if (dtoapiResult.getCode() == 0) {
            pcsQty = dtoapiResult.getData().getPcsQty();
        }
        return pcsQty;
    }

    /**
     * 修改BillHeader
     *
     * @param dto BillHeaderDTO
     * @return
     */
    @RequestMapping(value = "edit", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> edit(BillHeaderDTO dto) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(dto.getSrcId())) {
            return result.fail(APIResultCode.FORBIDDEN, "迁出方不能为空！");
        }
        if (StringUtils.isEmpty(dto.getDestId())) {
            return result.fail(APIResultCode.FORBIDDEN, "迁入方不能为空！");
        }
        if (dto.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据日期不能为空！");
        }
        if (dto.getDetailList() == null || dto.getDetailList().isEmpty()) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }
        if (dto.getDestId().equals(dto.getSrcId())) {
            return result.fail(403, "迁出方和迁入方不能相同！");
        }
        //计算预计箱数
        BigDecimal expectQty = new BigDecimal(0);
        for (BillDetailDTO detailDto : dto.getDetailList()
                ) {
            if (StringUtils.isEmpty(detailDto.getMaterialId())) {
                return result.fail(APIResultCode.FORBIDDEN, "产品名称不能为空！");
            }
            int pcsQty = findMaterialPcsQty(detailDto);
            if (pcsQty == 0) {
                continue;
            }
            expectQty = expectQty.add(new BigDecimal(detailDto.getExpectQtyPcs()).divide(new BigDecimal(pcsQty), 2, BigDecimal.ROUND_HALF_UP));
        }
        dto.setExpectQty(expectQty);

        APIResult<BillHeaderDTO> billHeader = appService.getById(dto.getId());
        if (billHeader.getCode() == APIResultCode.OK) {
            if (billHeader.getData().getBillStatus() != BillStatus.Confirm.index) {
                return result.fail(APIResultCode.STATUS_CHANGED, "BillHeader.[Status] Is not Confirm");
            }
            SysUserDTO currentUser = getCurrentUser();
            dto.setEditTime(new Date());
            dto.setEditBy(currentUser.getUserName());
            result = appService.update(dto);
        } else {
            result.fail(billHeader.getCode(), billHeader.getErrMsg());
        }
        return result;
    }

    @RequestMapping(value = "getBillDetailList", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<List<BillDetailDTO>> getBillDetailList(String billHeaderId) {
        APIResult<List<BillDetailDTO>> result = appService.getBillDetailList(billHeaderId);

        if (result.getCode() == 0) {
            for (BillDetailDTO billDetailDTO : result.getData()) {
                int pcsQty = findMaterialPcsQty(billDetailDTO);
                if (pcsQty != 0) {
                    double expectQty = (double) billDetailDTO.getExpectQtyPcs() / (double) pcsQty;
                    double realQty = (double) billDetailDTO.getRealQtyPcs() / (double) pcsQty;
                    expectQty = new BigDecimal(expectQty).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    realQty = new BigDecimal(realQty).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    billDetailDTO.setExpectQty(expectQty);
                    billDetailDTO.setRealQty(realQty);
                } else {
                    billDetailDTO.setExpectQty(0);
                    billDetailDTO.setRealQty(0);
                }
            }
        }
        return result;
    }


    /**
     * 生成所有明细列表以及库存
     *
     * @param orgId 明细
     * @return APIResult
     */
    @RequestMapping(value = "getExpectQtyPcsDetailList", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<List<BillDetailDTO>> getExpectQtyPcsDetailList(@RequestParam String orgId) {
        return projRdcToDealerOutAppService.getExpectQtyPcsDetailList(orgId);
    }
}
