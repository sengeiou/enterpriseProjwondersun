package com.proj.web.api;

import com.alibaba.druid.util.StringUtils;
import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebd.appservice.MaterialAppService;
import com.ebd.dto.MaterialDTO;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.ebt.appservice.BillHeaderAppService;
import com.ebt.appservice.BillHeaderUploadAppService;
import com.ebt.dto.BillDetailDTO;
import com.ebt.dto.BillHeaderDTO;
import com.ebt.enums.BillAddFromType;
import com.ebt.enums.BillStatus;
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
 * 经销商 - 退货仓库单
 *
 * @author arlenChen
 */
@Controller
@RequestMapping("/api/proj/org_dealertordc")
public class ProjOrg_DealerToRDCApi extends BaseController {
    @Resource
    private BillHeaderAppService appService;
    @Resource
    private BillHeaderUploadAppService uploadAppService;
    @Resource
    private MaterialAppService materialAppService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<BillHeaderDTO> getById(@RequestParam(value = "id") String id) {
        return appService.getById(id);
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> add(BillHeaderDTO billHeaderDTO) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(billHeaderDTO.getSrcId())) {
            return result.fail(APIResultCode.FORBIDDEN, "退货方不能为空！");
        }
        if (billHeaderDTO.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据时间不能为空！");
        }
        if (billHeaderDTO.getDetailList() == null || billHeaderDTO.getDetailList().size() == 0) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }
        SysUserDTO currentUser = getCurrentUser();
        billHeaderDTO.setDestId(currentUser.getSysOrganizationId());
        billHeaderDTO.setAddTime(new Date());
        billHeaderDTO.setEditTime(new Date());
        billHeaderDTO.setAddBy(currentUser.getUserName());
        billHeaderDTO.setEditBy(currentUser.getUserName());
        billHeaderDTO.setBillStatus(BillStatus.Created.index);
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
        return result;
    }

    /**
     * 修改BillHeader
     *
     * @param dto BillHeaderDTO
     * @return APIResult
     */
    @RequestMapping(value = "edit", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> edit(BillHeaderDTO dto) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(dto.getSrcId())) {
            return result.fail(APIResultCode.FORBIDDEN, "退货方不能为空！");
        }
        if (dto.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据时间不能为空！");
        }
        if (dto.getDetailList() == null || dto.getDetailList().size() == 0) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }
        //计算预计箱数
        BigDecimal expectQty = new BigDecimal(0);
        for (BillDetailDTO detailDto : dto.getDetailList()) {
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

        APIResult<BillHeaderDTO> billheader = appService.getById(dto.getId());
        if (billheader.getCode() == APIResultCode.OK) {
            if (billheader.getData().getBillStatus() != BillStatus.Created.index) {
                return result.fail(APIResultCode.STATUS_CHANGED, "BillHeader.[Status] Is not Created");
            }
            SysUserDTO currentUser = getCurrentUser();
            dto.setDestId(currentUser.getSysOrganizationId());
            dto.setEditTime(new Date());
            dto.setEditBy(currentUser.getUserName());
            result = appService.update(dto);
        } else {
            result.fail(billheader.getCode(), billheader.getErrMsg());
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

    private int findMaterialPcsQty(BillDetailDTO billDetailDTO) {
        APIResult<MaterialDTO> dtoapiResult = materialAppService.getById(billDetailDTO.getMaterialId());
        int pcsQty = 0;
        if (dtoapiResult.getCode() == 0) {
            pcsQty = dtoapiResult.getData().getPcsQty();
        }
        return pcsQty;
    }


    @RequestMapping(value = "confirm", method = RequestMethod.POST)
    @ResponseBody
    public String confirm(@RequestParam(value = "ids") String[] ids) {
        int k = 0;
        if (ids == null) {
            return "ids is null";
        }
        String mes = "ok";
        for (String id : ids) {
            APIResult<String> result = appService.Confirm(id);
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

    @RequestMapping(value = "scancancel", method = RequestMethod.POST)
    @ResponseBody
    public String scancancel(@RequestParam(value = "ids") String[] ids) {
        int k = 0;
        if (ids == null) {
            return "ids is null";
        }
        String mes = "ok";
        for (String id : ids) {
            APIResult<String> result = appService.CancelScan(id);
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

    @RequestMapping(value = "confirmcancle", method = RequestMethod.POST)
    @ResponseBody
    public String confirmcancle(@RequestParam(value = "ids") String[] ids) {
        int k = 0;
        if (ids == null) {
            return "ids is null";
        }
        String mes = "ok";
        for (String id : ids) {
            APIResult<String> result = appService.ReverseConfirm(id);
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

    @RequestMapping(value = "upload")
    @ResponseBody
    public APIResult<BillHeaderDTO> upload(String code, String billTypeId) {
        return uploadAppService.Upload(code, billTypeId);
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
}
