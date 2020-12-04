package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebp.appservice.FileUploadAppService;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.ProjRdcToDealerOutAppService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2017/5/16.
 * 单据导入
 */
@Controller
@RequestMapping({"api/proj/projrdctodealerout"})
public class ProjRdcToDealerOutApi extends BaseController {

    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private ProjRdcToDealerOutAppService projRdcToDealerOutAppService;
    @Resource
    private FileUploadAppService fileUploadAppService;

    /**
     * 检查导入的单据数据是否正确
     *
     * @param file       文件
     * @param importType 导入类型
     * @return String
     * @throws JsonProcessingException JsonProcessingException
     */
    @RequestMapping(value = "importExcelForData", method = RequestMethod.POST)
    @ResponseBody
    public String importExcelForData(@RequestParam MultipartFile file, @RequestParam(defaultValue = "head") String importType) throws JsonProcessingException {
        String name = file.getOriginalFilename();
        APIResult<Map<String, Object>> result = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return objectMapper.writeValueAsString(result);
        }
        if (!file.isEmpty()) {
            try {

                // EXCEL文件保存到本地
                InputStream inputStream = file.getInputStream();
                fileUploadAppService.saveFile(inputStream, file.getOriginalFilename(), "仓库业务/销售出库单", "api/proj/importbilldata", getCurrentUser().getUserName(), "importExcelForData");

                //解析EXCEL内容
                List<Map<String, String>> rowList = parseByXssWorkBook(file,importType);
                result = projRdcToDealerOutAppService.checkDataIsNormal(rowList, importType, getCurrentUser().getSysOrganizationId());
                String jsonStr;
                if (result.getCode()==0&&(boolean) result.getData().get("isNormal")) {
                    // 根据出库单号分组并校验同一出库单号 的 发货方 和 收货方 是否一致
                    result = projRdcToDealerOutAppService.groupByRefCodeDataAndCheck(rowList, importType, getCurrentUser().getSysOrganizationId());
                    // 出库单号分组
                    Map<String, List<Map<String, String>>> groupMap = (Map<String, List<Map<String, String>>>) result.getData().get("groupMap");
                    projRdcToDealerOutAppService.saveGroupData(groupMap, getCurrentUser());
                }
                jsonStr = objectMapper.writeValueAsString(result);
                return jsonStr;
            } catch (Exception e) {
                e.printStackTrace();
                result.fail(APIResultCode.UNEXPECTED_ERROR, e.getMessage());
                return objectMapper.writeValueAsString(result);
            }
        }
        result.fail(APIResultCode.NOT_FOUND, "file is empty");
        return objectMapper.writeValueAsString(result);
    }

    /**
     * Excel解析（2007）
     *
     * @param file file
     * @return List
     * @throws IOException IOException
     */
    private List<Map<String, String>> parseByXssWorkBook(MultipartFile file, String importType) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        XSSFWorkbook wb;
        try {
            is = file.getInputStream();
            try {
                //新版本Excel（2007）解析
                wb = new XSSFWorkbook(is);
            } catch (Exception e) {
                // 如果是老版本（2003），异常后尝试此方法解析。
                return parseByHssWorkBook(file,importType);
            }
            int num = wb.getNumberOfSheets();
            for (int j = 0; j < num; j++) {
                XSSFSheet sheet = wb.getSheetAt(j);
                int trLength = sheet.getLastRowNum();
                for (int i = 1; i <= trLength; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        break;
                    }
                    Map<String, String> map;
                    if ("org".equals(importType)) {
                        map = getOrgRowDataMap(row);
                    } else {
                        map = getRowDataMap(row);
                    }
                    if (excelUtilAppService.checkRowAllBlank(map)) {
                        break;
                    }
                    map.put("index", i + 1 + "");
                    rowList.add(map);
                }
            }
            return rowList;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Excel解析（2003）
     *
     * @param file file
     * @return List
     * @throws IOException
     */
    private List<Map<String, String>> parseByHssWorkBook(MultipartFile file, String importType) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        try {
            is = file.getInputStream();
            HSSFWorkbook e = new HSSFWorkbook(is);
            int num = e.getNumberOfSheets();
            for (int j = 0; j < num; j++) {
                HSSFSheet sheet = e.getSheetAt(j);
                int trLength = sheet.getLastRowNum();
                for (int i = 1; i <= trLength; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        break;
                    }
                    Map<String, String> map;
                    if ("org".equals(importType)) {
                        map = getOrgRowDataMap(row);
                    } else {
                        map = getRowDataMap(row);
                    }
                    if (excelUtilAppService.checkRowAllBlank(map)) {
                        break;
                    }
                    map.put("index", i + 1 + "");
                    rowList.add(map);
                }
            }
            return rowList;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private Map<String, String> getRowDataMap(Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>(16);
        for (int j = 0; j < tdLength; j++) {
            Cell cell = row.getCell(j);
            String value = cell == null ? "" : excelUtilAppService.getCellValue(cell);
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            switch (j) {
                case 0:
                    map.put("refCode", value);
                    //出库单号*
                    break;
                case 1:
                    map.put("operateTime", value);
                    //单据日期*
                    break;
                case 2:
                    map.put("srcCode", value);
                    //发货方编码*
                    break;
                case 3:
                    map.put("srcName", value);
                    //发货方名称
                    break;
                case 4:
                    map.put("destCode", value);
                    //收货方编码*
                    break;
                case 5:
                    map.put("destName", value);
                    //收货方名称
                    break;
                case 6:
                    map.put("sku", value);
                    //产品ERP代码*
                    break;
                case 7:
                    map.put("fullName", value);
                    //产品名称
                    break;
                case 8:
                    map.put("expectQtyPcs", value);
                    //应发单品数*
                    break;
                default:
            }
        }
        return map;
    }

    private Map<String, String> getOrgRowDataMap(Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>(16);
        for (int j = 0; j < tdLength; j++) {
            Cell cell = row.getCell(j);
            String value = cell == null ? "" : excelUtilAppService.getCellValue(cell);
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            switch (j) {
                case 0:
                    map.put("destCode", value);
                    //收货方代码
                    break;
                case 1:
                    map.put("destName", value);
                    //收货方名称
                    break;
                case 2:
                    map.put("province", value);
                    //省份
                    break;
                case 3:
                    map.put("city", value);
                    //城市
                    break;
                case 4:
                    map.put("district", value);
                    //区县
                    break;
                case 5:
                    map.put("address", value);
                    //详细地址
                    break;
                case 6:
                    map.put("contact", value);
                    //联系人
                    break;
                case 7:
                    map.put("phone", value);
                    //联系方式
                    break;
                case 8:
                    map.put("materialSku", value);
                    //产品编码
                    break;
                case 9:
                    map.put("materialName", value);
                    //产品名称
                    break;
                case 10:
                    map.put("expectQtyPcs", value);
                    //应发单罐数
                    break;
                case 11:
                    map.put("remark", value);
                    //备注
                    break;
                default:
            }
        }
        return map;
    }
}
