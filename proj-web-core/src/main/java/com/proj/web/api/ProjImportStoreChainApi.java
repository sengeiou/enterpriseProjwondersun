package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.utils.StringUtil;
import com.ebp.web.comm.BaseController;
import com.proj.service.ProjImportStoreChainService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 门店连锁信息API
 * NEIL
 */
@Controller
@RequestMapping("/api/proj/importStoreChain")
public class ProjImportStoreChainApi extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(ProjImportStoreChainApi.class);

    @Resource
    private ProjImportStoreChainService projImportStoreChainService;


    /**
     * 日期格式化
     */
    private static final String FORMAT_STR = "yyyy-MM-dd";

    @RequestMapping(value = "importExcel", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> importExcel(@RequestParam MultipartFile file) throws JsonProcessingException {
        APIResult<String> result = new APIResult<>();
        String name = file.getOriginalFilename();
        if (!name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            result.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是Excel文件）");
            return result;
        }
        if (file.isEmpty()){
            result.fail(APIResultCode.NOT_FOUND, "文件不能为空文件");
            return result;
        }
        try {
            // 解析EXCEL内容
            List<Map<String, String>> rowList = parseByXssWorkBook(file);
            result =projImportStoreChainService.importExcel(rowList,getCurrentUser());
            return result;
        }catch (Exception e){
            e.printStackTrace();
            logger.error("门店连锁信息管理出现异常：",e);
            result.fail(APIResultCode.UNEXPECTED_ERROR,"门店连锁信息管理出现异常");
            return result;
        }
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
                    if (checkRowAllBlank(map)) {
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
                    if (checkRowAllBlank(map)) {
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


    public boolean checkRowAllBlank(Map<String, String> map) {
        boolean flag = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtil.isNotEmpty(entry.getValue())) {
                flag = false;
                break;
            }
        }
        return flag;
    }


    private Map<String, String> getRowDataMap(int i, Row row) {
        int tdLength = row.getLastCellNum();
        Map<String, String> map = new HashMap<>();
        for (int j = 0; j < tdLength; j++) {
            Cell cell = row.getCell(j);
            String value = cell == null ? "" : getCellValue(cell);
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            switch (j) {
                case 0:
                    map.put("storeName", value); // 门店名称
                    break;
                case 1:
                    map.put("storeMainCode", value); // 门店主代码
                    break;
                case 2:
                    map.put("storeSecondaryCode", value);// 门店副代码
                    break;
                case 3:
                    map.put("chainCode", value);// 连锁代码
                    break;
                case 4:
                    map.put("chainName", value);// 连锁名称
                    break;
            }
        }
        return map;
    }

    /**
     * 格式化列数据
     *
     * @param cell cell
     * @return String
     */
    public String getCellValue(Cell cell) {
        String result;
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(6);
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:
                // 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    // 处理日期格式、时间格式
                    SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_STR);
                    Date date = cell.getDateCellValue();
                    result = sdf.format(date);
                } else {
                    result = nf.format(cell.getNumericCellValue());
                }
                break;
            case HSSFCell.CELL_TYPE_STRING:
                // String类型
                result = cell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                result = "";
                break;
            case HSSFCell.CELL_TYPE_FORMULA:
                try {
                    result = nf.format(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    result = String.valueOf(cell.getRichStringCellValue());
                }
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

}
