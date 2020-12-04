package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebp.appservice.FileUploadAppService;
import com.ebp.dto.SysUserDTO;
import com.ebp.enums.OrgType;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.*;
import com.proj.dto.ImportStoreDealerRelationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by ehsure on 2016/8/1.
 */
@Controller
@RequestMapping({"api/proj/importdata"})
public class ImportDataApi extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(ImportDataApi.class);
    @Resource
    private StoreDealerRelationAppService storeDealerRelationAppService;

    @Resource
    private StoreD2RelationAppService storeD2RelationAppService;

    @Resource
    private DealerD2RelationAppService dealerD2RelationAppService;

    @Resource
    private DealerUpdateCMAppService dealerUpdateCMAppService;

    private static Map<String, String> typeNameMap = null;
    @Resource
    private ImportDataAppService appService;
    @Resource
    private FileUploadAppService fileUploadAppService;

    @RequestMapping(value = "importExcelForOrg", method = RequestMethod.POST)
    @ResponseBody
    public String importExcelForOrg(@RequestParam MultipartFile file, int orgType) throws JsonProcessingException {
//        Integer orgType = OrgType.DEALER.index;
        int sheetIndex = 0;
        if (orgType == OrgType.STORE.index) {
            sheetIndex = 1;
        }
        String name = file.getOriginalFilename();
        APIResult<Map<String, Object>> result = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return objectMapper.writeValueAsString(result);
        }
        if (!file.isEmpty()) {
            try {
                SysUserDTO userDTO = getCurrentUser();
                List<Map<String, String>> rowList = parseByXssWorkBook(file, orgType, sheetIndex);
                if (rowList == null || rowList.size() == 0) {
                    result.fail(404, "内容为空！");
                } else {
                    result = appService.importDistributor(rowList, userDTO, orgType);
                }
                String jsonStr = objectMapper.writeValueAsString(result);
                return jsonStr;
            } catch (Exception e) {
                e.printStackTrace();
                result.fail(APIResultCode.UNEXPECTED_ERROR, e.getStackTrace().toString());
                return objectMapper.writeValueAsString(result);
            }
        }
        result.fail(APIResultCode.NOT_FOUND, "file is empty");
        return objectMapper.writeValueAsString(result);
    }

    @RequestMapping(value = "importExcelForStore", method = RequestMethod.POST)
    @ResponseBody
    public String importExcelForStore(@RequestParam MultipartFile file) throws JsonProcessingException {
        Integer orgType = OrgType.STORE.index;
        int sheetIndex = 1;
        String name = file.getOriginalFilename();
        APIResult<Map<String, Object>> result = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return objectMapper.writeValueAsString(result);
        }
        if (!file.isEmpty()) {
            try {
                SysUserDTO userDTO = getCurrentUser();
                List<Map<String, String>> rowList = parseByXssWorkBook(file, orgType, sheetIndex);
                if (rowList == null || rowList.size() == 0) {
                    result.fail(404, "内容为空！");
                } else {
                    result = appService.importDistributor(rowList, userDTO, orgType);
                }
                String jsonStr = objectMapper.writeValueAsString(result);
                return jsonStr;
            } catch (Exception e) {
                e.printStackTrace();
                result.fail(APIResultCode.UNEXPECTED_ERROR, e.getStackTrace().toString());
                return objectMapper.writeValueAsString(result);
            }
        }
        result.fail(APIResultCode.NOT_FOUND, "file is empty");
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 导入门店与经(分)销商供货关系
     *
     * @param file
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    public String importExcel(@RequestParam MultipartFile file, @RequestParam(value = "type", required = true) String type) throws JsonProcessingException {
        String name = file.getOriginalFilename();
        APIResult<List<ImportStoreDealerRelationDTO>> result = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return objectMapper.writeValueAsString(result);
        }
        try {
            if ("dealerstore".equals(type)) {//经销商和门店

                // EXCEL文件保存到本地
                InputStream inputStream = file.getInputStream();
                fileUploadAppService.saveFile(inputStream, file.getOriginalFilename(), "基础信息/供货关系/经销商和门店", "api/proj/importdata", getCurrentUser().getUserName(), "dealerstore");
                result = storeDealerRelationAppService.importExcel(file.getInputStream(), name.substring(name.lastIndexOf(".") + 1), getCurrentUser());
            } else if ("dealerd2".equals(type)) {//经销商和分销商

                // EXCEL文件保存到本地
                InputStream inputStream = file.getInputStream();
                fileUploadAppService.saveFile(inputStream, file.getOriginalFilename(), "基础信息/供货关系/经销商和分销商", "api/proj/importdata", getCurrentUser().getUserName(), "dealerd2");
                result = dealerD2RelationAppService.importExcel(file.getInputStream(), name.substring(name.lastIndexOf(".") + 1), getCurrentUser());
            } else if ("d2store".equals(type)) {//分销商和门店

                // EXCEL文件保存到本地
                InputStream inputStream = file.getInputStream();
                fileUploadAppService.saveFile(inputStream, file.getOriginalFilename(), "基础信息/供货关系/分销商和门店", "api/proj/importdata", getCurrentUser().getUserName(), "d2store");
                result = storeD2RelationAppService.importExcel(file.getInputStream(), name.substring(name.lastIndexOf(".") + 1), getCurrentUser());
            } else if ("dealerupdateCM".equals(type)) {//经销商更新客户经理

                // EXCEL文件保存到本地
                InputStream inputStream = file.getInputStream();
                fileUploadAppService.saveFile(inputStream, file.getOriginalFilename(), "基础信息/供货关系/经销商更新客户经理", "api/proj/importdata", getCurrentUser().getUserName(), "dealerupdateCM");
                result = dealerUpdateCMAppService.importExcel(file.getInputStream(), name.substring(name.lastIndexOf(".") + 1), getCurrentUser());
            } else {
                result.fail(APIResultCode.FORBIDDEN, "参数无效");
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("导入出错", e);
            result.fail(APIResultCode.UNEXPECTED_ERROR, e.getMessage());
            return objectMapper.writeValueAsString(result);
        }
        return objectMapper.writeValueAsString(result);
    }

    @RequestMapping(value = "download")
    @ResponseBody
    public void download(@RequestParam(value = "type", required = true) String type, HttpServletRequest req, HttpServletResponse res) {

        File file = null;
        String newName = typeNameMap.get(type);
        String suffix = ".xls";
        InputStream input = null;
        if (newName != null) {//经销商和门店EXCEL模板
            try {
                input = this.getClass().getClassLoader()
                        .getResourceAsStream("template/" + type + ".xls");
                if (input == null) {
                    input = this.getClass().getClassLoader()
                            .getResourceAsStream("template/" + type + ".xlsx");
                    suffix = ".xlsx";
                }
            } catch (Exception e) {
                input = this.getClass().getClassLoader()
                        .getResourceAsStream("template/" + type + ".xlsx");
                suffix = ".xlsx";
            }
        }
        if (input == null) {
            ServletOutputStream out = null;
            try {
                out = res.getOutputStream();
                out.write("文件不存在或无效参数".getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            return;
        }
        ImportDataApi.downLoadFile(req, res, input, newName + suffix);
    }

    public static void downLoadFile(HttpServletRequest req, HttpServletResponse response, InputStream input, String newName) {
        if (input != null) {
            ServletOutputStream out = null;
            String fileName = "";
            try {
                String ua = req.getHeader("User-Agent");
                if (ua.toUpperCase().indexOf("FIREFOX") > -1) {
                    fileName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
                } else {
                    fileName = URLEncoder.encode(newName, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                fileName = newName;
            }

            try {
                response.reset();
                response.setContentType("application/octet-stream; charset=utf-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                out = response.getOutputStream();
                out.write(IOUtils.toByteArray(input));
                out.flush();
            } catch (IOException var12) {
                var12.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException var11) {
                        var11.printStackTrace();
                    }
                }

            }

        }
    }

//    @RequestMapping(value = "saveNoCodeData", method = RequestMethod.POST)
//    @ResponseBody
//    public Object saveNoCodeData(@RequestParam  String dataStr,@RequestParam String billTypeId){
//        APIResult<Object> result = new APIResult<>();
//        if(StringUtil.isEmpty(dataStr)){
//            result.fail(APIResultCode.FORBIDDEN,"导入内容不能为空！");
//        }
//        if(StringUtil.isEmpty(billTypeId)){
//            result.fail(APIResultCode.FORBIDDEN,"导入单据类型不能为空！");
//        }
//        try{
//            JSONObject json = JSONObject.parseObject(dataStr);
//            JSONArray jsonArray = json.getJSONArray("data");
//            List<Map<String,String>>  rowList = new ArrayList<>();
//            Map<String,List<BillHeaderDTO>> dtoMapList = new HashMap<>();
//            for (int i = 0; i <jsonArray.size(); i++) {
//                JSONObject rowJson = jsonArray.getJSONObject(i);
//                Map<String,String> map = new HashMap<>();
//                map.put("index",rowJson.get("index").toString());
//                map.put("refCode",rowJson.get("refCode").toString());
//                map.put("operateTime",rowJson.get("operateTime").toString());
//                map.put("destCode",rowJson.get("destCode").toString());
//                map.put("materialSku",rowJson.get("materialSku").toString());
//                map.put("qty",rowJson.get("qty").toString());
//                map.put("salesAmount",rowJson.get("salesAmount").toString()); //销售金额
//                rowList.add(map);
//            }
//            return  null;
//        }catch (Exception e){
//            e.printStackTrace();
//            result.fail(505,e.getMessage());
//        }
//        return result;
//    }

    /**
     * Excel解析（2007）
     *
     * @param
     * @return
     * @throws IOException
     */
    private List<Map<String, String>> parseByXssWorkBook(MultipartFile file, Integer orgType, Integer sheetIndex) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        XSSFWorkbook wb = null;
        try {
            is = file.getInputStream();
            try {
                wb = new XSSFWorkbook(is);  //新版本Excel（2007）解析
            } catch (Exception e) {
                return parseByHssWorkBook(file, orgType, sheetIndex); // 如果是老版本（2003），异常后尝试此方法解析。
            }
            if (sheetIndex == null) {
                sheetIndex = 0;
            }
            XSSFSheet sheet = wb.getSheetAt(sheetIndex);
            int trLength = sheet.getLastRowNum();
            for (int i = 1; i <= trLength; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    break;
                }
                if (orgType == OrgType.STORE.index) {
                    Map<String, String> map = getStoreRowDataMap(i, row);
                    rowList.add(map);
                } else {
                    Map<String, String> map = getD2RowDataMap(i, row);
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
     * @param file
     * @return
     * @throws IOException
     */
    private List<Map<String, String>> parseByHssWorkBook(MultipartFile file, Integer orgType, Integer sheetIndex) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        try {
            is = file.getInputStream();
            HSSFWorkbook e = new HSSFWorkbook(is);
            if (sheetIndex == null) {
                sheetIndex = 0;
            }
            HSSFSheet sheet = e.getSheetAt(sheetIndex);
            int trLength = sheet.getLastRowNum();
            for (int i = 1; i <= trLength; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    break;
                }
                if (orgType == OrgType.STORE.index) {
                    Map<String, String> map = getStoreRowDataMap(i, row);
                    rowList.add(map);
                } else {
                    Map<String, String> map = getD2RowDataMap(i, row);
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

    private Map<String, String> getD2RowDataMap(int i, Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>();
        map.put("index", i + 1 + "");
        for (int j = 0; j < tdLength; j++) {
            Cell cell = row.getCell(j);
            String value = "";
            if (cell != null) {
                value = getCellValue(cell);
            }
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            switch (j) {
                case 0:
                    map.put("code", value);
                    break;
                case 1:
                    map.put("fullName", value);
                    break;
                case 2:
                    map.put("shortName", value);
                    break;
                case 3:
                    map.put("parentCode", value);
                    break;
                case 4:
                    map.put("bigArea", value);
                    break;
                case 5:
                    map.put("area", value);
                    break;
                case 6:
                    map.put("checkCity", value);
                    break;
                case 7:
                    map.put("inUse", value);
                    break;
                case 8:
                    map.put("editTime", value);
                    break;
            }
        }
        return map;
    }

    private Map<String, String> getStoreRowDataMap(int i, Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>();
        map.put("index", i + 1 + "");
        for (int j = 0; j < tdLength; j++) {
            Cell cell = row.getCell(j);
            String value = "";
            if (cell != null) {
                value = getCellValue(cell);
            }
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            switch (j) {
                case 0:
                    map.put("code", value);
                    break;
                case 1:
                    map.put("fullName", value);
                    break;
                case 2:
                    map.put("shortName", value);
                    break;
                case 3:
                    map.put("parentCode", value);
                    break;
                case 4:
                    map.put("bigArea", value);
                    break;
                case 5:
                    map.put("area", value);
                    break;
                case 6:
                    map.put("checkCity", value);
                    break;
                case 7:
                    map.put("storeType", value);
                    break;
                case 8:
                    map.put("inUse", value);
                    break;
                case 10:
                    map.put("editTime", value);
                    break;
                default:
                    break;
            }
        }
        return map;
    }

    private String getCellValue(Cell cell) {
        String result;
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:// 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 处理日期格式、时间格式
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = cell.getDateCellValue();
                    result = sdf.format(date);
                } else {
                    NumberFormat nf = NumberFormat.getInstance();
                    nf.setGroupingUsed(false);
                    result = nf.format(cell.getNumericCellValue());
                }
                break;
            case HSSFCell.CELL_TYPE_STRING:// String类型
                result = cell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                result = "";
                break;
            default:
                result = "";
                result = "";
                break;
        }
        return result;
    }

    static {
        typeNameMap = new HashMap<>();
        typeNameMap.put("dealerstore", "经销商与门店");
        typeNameMap.put("dealerd2", "经销商与分销商");
        typeNameMap.put("d2store", "分销商与门店");
        typeNameMap.put("dealerupdateCM", "经销商与客户经理");
        typeNameMap.put("store", "门店信息修改");
        typeNameMap.put("storechaininfo", "门店连锁信息");
        typeNameMap.put("importStoreChain", "门店导入连锁信息");
        typeNameMap.put("importStoreInUse", "门店导入启用信息");
        typeNameMap.put("importStoreEdit", "门店导入批量修改信息");
        typeNameMap.put("expressbill", "销售出库单");
    }
}