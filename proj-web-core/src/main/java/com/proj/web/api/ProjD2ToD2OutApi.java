package com.proj.web.api;

import com.alibaba.druid.util.StringUtils;
import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebd.appservice.MaterialAppService;
import com.ebd.common.CommonUtil;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 总部
 * 分销商 - 采购出库单
 */
@Controller
@RequestMapping("/api/proj/d2tod2out")
public class ProjD2ToD2OutApi extends BaseController {
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
            return result.fail(APIResultCode.FORBIDDEN, "调出方不能为空！");
        }
        if (StringUtils.isEmpty(billHeaderDTO.getDestId())) {
            return result.fail(APIResultCode.FORBIDDEN, "调入方不能为空！");
        }
        if (billHeaderDTO.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据时间不能为空！");
        }
        if (billHeaderDTO.getDetailList() == null || billHeaderDTO.getDetailList().size() == 0) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }

        SysUserDTO currentUser = getCurrentUser();
        billHeaderDTO.setAddTime(new Date());
        billHeaderDTO.setEditTime(new Date());
        billHeaderDTO.setAddBy(currentUser.getUserName());
        billHeaderDTO.setEditBy(currentUser.getUserName());
        billHeaderDTO.setBillStatus(BillStatus.Created.index);
        billHeaderDTO.setAddFrom(BillAddFromType.WEB.getValue());
        //计算预计箱数
        BigDecimal billExpectQty = new BigDecimal(0);
        for (BillDetailDTO billDetailDTO : billHeaderDTO.getDetailList()) {
            if (StringUtils.isEmpty(billDetailDTO.getMaterialId())) {
                return result.fail(APIResultCode.FORBIDDEN, "产品名称不能为空！");
            }
        }
        List<BillHeaderDTO> list = splitBill(billHeaderDTO);
        for (BillHeaderDTO dto : list) {
            for (BillDetailDTO billDetailDTO : dto.getDetailList()) {
                if (billDetailDTO.getExpectQtyPcs() > 0) {
                    int pcsQty = findMaterialPcsQty(billDetailDTO);
                    if (pcsQty > 0) {
                        billExpectQty = billExpectQty.add(new BigDecimal(billDetailDTO.getExpectQtyPcs()).divide(new BigDecimal(pcsQty), 2, BigDecimal.ROUND_HALF_UP));
                    }
                } else {
                    billExpectQty = billExpectQty.add(new BigDecimal(billDetailDTO.getExpectQty()));
                }
                int expectQtyPcs;
                if (billDetailDTO.getExpectQtyPcs() > 0) {
                    expectQtyPcs = billDetailDTO.getExpectQtyPcs();
                } else {
                    expectQtyPcs = (int) billDetailDTO.getExpectQty() * findMaterialPcsQty(billDetailDTO);
                }
                billDetailDTO.setExpectQtyPcs(expectQtyPcs);
            }
            dto.setExpectQty(billExpectQty);
            dto.setRealQty(new BigDecimal(0));
            result = appService.create(dto);
        }
        return result;
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
            return result.fail(APIResultCode.FORBIDDEN, "调出方不能为空！");
        }
        if (StringUtils.isEmpty(dto.getDestId())) {
            return result.fail(APIResultCode.FORBIDDEN, "调入方不能为空！");
        }
        if (dto.getOperateTime() == null) {
            return result.fail(APIResultCode.FORBIDDEN, "单据时间不能为空！");
        }
        if (dto.getDetailList() == null || dto.getDetailList().size() == 0) {
            return result.fail(APIResultCode.FORBIDDEN, "单据明细不能为空！");
        }
        //计算预计箱数
        BigDecimal billExpectQty = new BigDecimal(0);
        for (BillDetailDTO billDetailDTO : dto.getDetailList()) {
            if (StringUtils.isEmpty(billDetailDTO.getMaterialId())) {
                return result.fail(APIResultCode.FORBIDDEN, "产品名称不能为空！");
            }
            if (billDetailDTO.getExpectQtyPcs() > 0) {
                int pcsQty = findMaterialPcsQty(billDetailDTO);
                if (pcsQty > 0) {
                    billExpectQty = billExpectQty.add(new BigDecimal(billDetailDTO.getExpectQtyPcs()).divide(new BigDecimal(pcsQty), 2, BigDecimal.ROUND_HALF_UP));
                }
            } else {
                billExpectQty = billExpectQty.add(new BigDecimal(billDetailDTO.getExpectQty()));
            }
        }
        for (BillDetailDTO billDetailDTO : dto.getDetailList()) {
            int expectQtyPcs = 0;
            if (billDetailDTO.getExpectQtyPcs() > 0) {
                expectQtyPcs = billDetailDTO.getExpectQtyPcs();
            } else {
                expectQtyPcs = (int) billDetailDTO.getExpectQty() * findMaterialPcsQty(billDetailDTO);
            }
            billDetailDTO.setExpectQtyPcs(expectQtyPcs);
        }
        dto.setExpectQty(billExpectQty);
        dto.setRealQty(new BigDecimal(0));
        APIResult<BillHeaderDTO> billheader = appService.getById(dto.getId());
        if (billheader.getCode() == APIResultCode.OK) {
            if (billheader.getData().getBillStatus() != BillStatus.Created.index) {
                return result.fail(APIResultCode.STATUS_CHANGED, "BillHeader.[Status] Is not Created");
            }
            SysUserDTO currentUser = getCurrentUser();
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

    public int findMaterialPcsQty(BillDetailDTO billDetailDTO) {
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
        APIResult<BillHeaderDTO> result = uploadAppService.Upload(code, billTypeId);
        return result;
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

    private List<BillHeaderDTO> splitBill(BillHeaderDTO billHeaderDTO) {
        List<BillHeaderDTO> dtoList = new ArrayList<>();
        //新品明细
        List<BillDetailDTO> detailDTOList = new ArrayList<>();
        detailDTOList.addAll(billHeaderDTO.getDetailList());
        Map<Boolean, List<BillDetailDTO>> detailMap = detailDTOList.stream().collect(Collectors.groupingBy(x -> x.getNewMaterial()));
        if (!detailMap.isEmpty() && detailMap.size() == 1) {
            billHeaderDTO.setNewMaterial( billHeaderDTO.getDetailList().get(0).getNewMaterial());
            dtoList.add(billHeaderDTO);
            return dtoList;
        }
        List<BillDetailDTO> billDetailDTOList = billHeaderDTO.getDetailList().stream().filter(x -> x.getNewMaterial() != null && x.getNewMaterial()).collect(Collectors.toList());
        if (billDetailDTOList != null && !billDetailDTOList.isEmpty()) {
            BillHeaderDTO newHeaderDTO = CommonUtil.map(billHeaderDTO, BillHeaderDTO.class);
            newHeaderDTO.setCode(billHeaderDTO.getCode() + "-new");
            newHeaderDTO.setDetailList(billDetailDTOList);
            newHeaderDTO.setNewMaterial(true);
            billHeaderDTO.getDetailList().removeAll(billDetailDTOList);
            billHeaderDTO.setNewMaterial(false);
            dtoList.add(newHeaderDTO);
        }
        dtoList.add(billHeaderDTO);
        return dtoList;
    }
}
