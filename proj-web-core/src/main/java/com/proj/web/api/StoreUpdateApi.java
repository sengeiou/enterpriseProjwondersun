package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.StoreD2RelationAppService;
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
 * Created by ehsure on 2017/6/6.
 * 门店信息修改
 */
@Controller
@RequestMapping("/api/proj/storeupdate")
public class StoreUpdateApi extends BaseController {

    @Resource
    private ExcelUtilAppService excelUtilAppService;

    @Resource
    private StoreD2RelationAppService storeD2RelationAppService;

    /**
     * 检查导入的单据数据是否正确
     *
     * @param file 文件
     * @return String
     * @throws JsonProcessingException
     */
    @RequestMapping(value = "importExcelForData", method = RequestMethod.POST)
    @ResponseBody
    public String importExcelForData(@RequestParam MultipartFile file) throws JsonProcessingException {
        String name = file.getOriginalFilename();
        APIResult<Map<String, Object>> result = new APIResult<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return objectMapper.writeValueAsString(result);
        }
        if (!file.isEmpty()) {
            try {
                //解析EXCEL内容
                List<Map<String, String>> rowList = parseByXssWorkBook(file);
                XSSFWorkbook book = new XSSFWorkbook();
                result = storeD2RelationAppService.checkStoreIsNormal(rowList);
                if(result.getCode()==0){
                    if ((boolean) result.getData().get("isNormal")) {
                        storeD2RelationAppService.saveData(rowList, getCurrentUser());
                    }
                }
                String jsonStr = objectMapper.writeValueAsString(result);
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
     * @throws IOException
     */
    private List<Map<String, String>> parseByXssWorkBook(MultipartFile file) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        XSSFWorkbook wb = null;
        try {
            is = file.getInputStream();
            try {
                wb = new XSSFWorkbook(is);  //新版本Excel（2007）解析
            } catch (Exception e) {
                return parseByHssWorkBook(file); // 如果是老版本（2003），异常后尝试此方法解析。
            }
            int num = wb.getNumberOfSheets();
            for (int j = 0; j < num; j++) {
                XSSFSheet sheet = wb.getSheetAt(j);
                Row rows = sheet.getRow(0);
                int cellNum = rows.getLastCellNum();
                Cell cell = rows.getCell(0);
                String s = cell.getStringCellValue();
                int trLength = sheet.getLastRowNum();
                for (int i = 1; i <= trLength; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        break;
                    }
                    Map<String, String> map = getRowDataMap(i, row);
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
    private List<Map<String, String>> parseByHssWorkBook(MultipartFile file) throws IOException {
        List<Map<String, String>> rowList = new ArrayList<>();
        InputStream is = null;
        try {
            is = file.getInputStream();
            HSSFWorkbook e = new HSSFWorkbook(is);
            int num = e.getNumberOfSheets();
            for (int j = 0; j < num; j++) {
                HSSFSheet sheet = e.getSheetAt(j);
                Row rows = sheet.getRow(0);
                int cellNum = rows.getLastCellNum();
                Cell cell = rows.getCell(0);
                String cellName = cell.getStringCellValue();
                int trLength = sheet.getLastRowNum();
                for (int i = 1; i <= trLength; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        break;
                    }
                    Map<String, String> map = getRowDataMap(i, row);
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

//    private Map<String, String> getRowDataMap(int i, Row row) {
//        int tdLength = row.getLastCellNum();
//        Map<String, String> map = new HashMap<>();
//        for (int j = 0; j < tdLength; j++) {
//            Cell cell = row.getCell(j);
//            String value = cell == null ? "" : excelUtilAppService.getCellValue(cell);
//            if (value != null) {
//                value = value.trim();
//            } else {
//                value = "";
//            }
//            switch (j) {
//                case 0:
//                    map.put("dealerCode", value); //门店代码
//                    break;
//                case 1:
//                    map.put("dealerName", value); //门店名称
//                    break;
//                case 2:
//                    map.put("regionalManager", value); //区域经理
//                    break;
//                case 3:
//                    map.put("regionalManagerCall", value); //区域经理电话
//                    break;
//                case 4:
////                    map.put("manager", value); //客户经理
//                    break;
//                case 5:
////                    map.put("managerCall", value); //客户经理电话
//                    break;
//                case 6:
////                    map.put("businessManager", value);//业务经理
//                    break;
//                case 7:
////                    map.put("businessManagerCall", value);//业务经理电话
//                    break;
//                case 8:
////                    map.put("accountExecutive", value); //业务代表
//                    break;
//                case 9:
////                    map.put("accountExecutiveCall", value); //业务代表电话
//                    break;
//                case 10:
//                    map.put("pinCommissioner", value); //动销专员
//                    break;
//                case 11:
//                    map.put("pinCommissionerCall", value); //动销专员电话
//                    break;
//                case 12:
//                    map.put("salesMan", value); //业务专员
//                    break;
//                case 13:
//                    map.put("salesManCall", value); //业务专员电话
//                    break;
//                case 14:
//                    map.put("promotionSpecialist", value); //推广专员
//                    break;
//                case 15:
//                    map.put("promotionSpecialistCall", value); //推广专员电话
//                    break;
//            }
//        }
//        return map;
//    }


    private Map<String, String> getRowDataMap(int i, Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>();
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
                    map.put("dealerCode", value); //门店代码
                    break;
                case 1:
                    map.put("dealerName", value); //门店名称
                    break;
                case 2:
                    map.put("regionalManager", value); //区域经理
                    break;
                case 3:
                    map.put("regionalManagerCall", value); //区域经理电话
                    break;
                case 4:
                    map.put("qytgjlName", value); //区域推广经理
                    break;
                case 5:
                    map.put("qytgjlCall", value); //区域推广经理电话
                    break;
                case 6:
                    map.put("pinCommissioner", value);//动销专员
                    break;
                case 7:
                    map.put("pinCommissionerCall", value);//动销专员电话
                    break;
                case 8:
                    map.put("salesMan", value); //业务专员
                    break;
                case 9:
                    map.put("salesManCall", value); //业务专员电话
                    break;
                case 10:
                    map.put("promotionSpecialist", value); //推广专员
                    break;
                case 11:
                    map.put("promotionSpecialistCall", value); //推广专员电话
                    break;
                case 12:
                    map.put("yydbName", value); //营养代表
                    break;
                case 13:
                    map.put("yydbCall", value); //营养代表电话
                    break;
            }
        }
        return map;
    }


}
