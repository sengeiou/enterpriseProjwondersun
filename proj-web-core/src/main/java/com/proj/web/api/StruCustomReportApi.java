package com.proj.web.api;

import com.eaf.core.dto.PageReq;
import com.eaf.core.dto.PageResult;
import com.eaf.core.utils.DataFileWriter;
import com.eaf.core.utils.csv.CsvWriter;
import com.eaf.core.utils.excel.ExcelWriter;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.api.ColumnDef;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.StruCustomReportAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* 自定义报表数据Api
* */
@Controller
@RequestMapping("/api/proj/strucustomreport")
public class StruCustomReportApi extends BaseController {
    @Resource
    private StruCustomReportAppService appService;

    /**
     * 分页查询数据
     *
     * @return 分页结果
     */
    @RequestMapping(value = "{reportname}/list")
    @ResponseBody
    public PageResult<HashMap<String, String>> list(@PathVariable String reportname, @RequestBody PageReq pageReq) {
        PageResult<HashMap<String, String>> result;
        InputStream scriptStream = this.getClass().getClassLoader().getResourceAsStream("datasource_script/" + reportname + ".sql");
        try {
            if (scriptStream == null) {
                throw new FileNotFoundException();
            }
            //获取用户信息
            SysUserDTO userDTO = getCurrentUser();
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            result = appService.getPageList(pageReq, userDTO.getId(), scriptStr);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result = new PageResult<>();
            return result;
        } finally {
            if (scriptStream != null) {
                try {
                    scriptStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 导出
     *
     * @param response   http响应对象
     * @param reportname 报表名称
     * @param pageReqStr 分页请求参数字符串
     * @param pageDefStr 字段定义字符串
     * @param pageTitle  标题
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "{reportname}/export", method = RequestMethod.POST)
    public void export(
            HttpServletRequest req,
            HttpServletResponse response,
            @PathVariable String reportname, @RequestParam(defaultValue = "excel", required = false) String pageExportType,
            String pageReqStr,
            String pageDefStr,
            String pageTitle
    ) throws UnsupportedEncodingException {
        PageResult<HashMap<String, String>> result;
        ObjectMapper mapper = new ObjectMapper();
        PageReq pageReq = null;
        ArrayList<ColumnDef> pageDef = null;
        InputStream scriptStream = this.getClass().getClassLoader().getResourceAsStream("datasource_script/" + reportname + ".sql");
        try {
            pageReq = mapper.readValue(pageReqStr, PageReq.class);
            pageReq.setRows(Integer.MAX_VALUE);

            pageDef = mapper.readValue(pageDefStr, mapper.getTypeFactory().constructCollectionType(ArrayList.class, ColumnDef.class));

            if (scriptStream == null) {
                throw new FileNotFoundException();
            }
            //获取用户信息
            SysUserDTO userDTO = getCurrentUser();
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            result = appService.getPageList(pageReq, userDTO.getId(), scriptStr);
            //构造汇总箱数
            List<HashMap<String, String>> list = buildBoxStr(pageDefStr, result.getRows());
            if (list.size() > 0) {
                result.getRows().addAll(list);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = new PageResult<>();
        }
        //默认是EXCEL
        if (StringUtils.isEmpty(pageExportType)) {
            pageExportType = "excel";
        }
        String fileExt = "csv".equals(pageExportType) ? ".csv" : ".xlsx";
        String fileName = null;
        try {
            String ua = req.getHeader("User-Agent");
            if (ua.toUpperCase().indexOf("FIREFOX") > -1) {
                fileName = new String((pageTitle + fileExt).getBytes("UTF-8"), "ISO-8859-1");
            } else {
                fileName = URLEncoder.encode(pageTitle + fileExt, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/octet-stream; charset=utf-8");
        DataFileWriter ew = null;
        if ("csv".equals(pageExportType)) {
            ew = new CsvWriter();
        } else {
            ew = new ExcelWriter();
        }
        for (ColumnDef columnDef : pageDef) {
            ew.addColumnInfo(columnDef.getName(), columnDef.getTitle());
        }
        try {
            ew.write(response.getOutputStream(), result.getRows(), "sheet1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<HashMap<String, String>> buildBoxStr(String pageDefStr, List<HashMap<String, String>> list) {
        List<HashMap<String, String>> boxList = new ArrayList<>();
        //导出时有箱数字段则对其汇总
        HashMap<String, String> amountMap = new HashMap<>();
        //一列对应的箱数和个数
        HashMap<String, Map<String, Integer>> boxMap = new HashMap<>();
        if (list.size() > 0) {
            amountMap.putAll(list.get(0));
            for (HashMap.Entry<String, String> entry : amountMap.entrySet()) {
                entry.setValue("");
            }
        }
        String a = pageDefStr.substring(pageDefStr.indexOf(":") + 2, pageDefStr.indexOf(",") - 1);
        for (HashMap<String, String> x : list) {
            for (HashMap.Entry<String, String> entry : x.entrySet()) {
                int boxQty = 0;
                int pcsQty = 0;
                if (!entry.getKey().contains("QTYSTR")) {
                    continue;
                } else {
                    String key = entry.getKey();
                    String qtyStr = entry.getValue();
                    int boxIndex = qtyStr.lastIndexOf("箱");
                    int qtyIndex = qtyStr.lastIndexOf("个");
                    if (boxIndex != -1) {
                        boxQty = Integer.parseInt(qtyStr.substring(0, boxIndex));
                    }
                    if (qtyIndex != -1) {
                        pcsQty = Integer.parseInt(qtyStr.substring(boxIndex + 1, qtyIndex));
                    }
                    if (boxMap.containsKey(key)) {
                        Map<String, Integer> integerMap = boxMap.get(key);
                        integerMap.put("箱", integerMap.get("箱") + boxQty);
                        integerMap.put("个", integerMap.get("个") + pcsQty);
                    } else {
                        HashMap<String, Map<String, Integer>> hashMap = new HashMap<>();
                        Map<String, Integer> integerMap = new HashMap<>();
                        integerMap.put("箱", boxQty);
                        integerMap.put("个", pcsQty);
                        hashMap.put(key, integerMap);
                        boxMap.putAll(hashMap);
                    }
                }
            }
        }
        for (HashMap.Entry<String, Map<String, Integer>> box : boxMap.entrySet()) {
            for (HashMap.Entry<String, String> as : amountMap.entrySet()) {
                if (box.getKey().equals(as.getKey())) {
                    Map<String, Integer> zxc = box.getValue();
                    int box1 = zxc.get("箱");
                    int pec1 = zxc.get("个");
                    if (box1 > 0 || pec1 > 0) {
                        String str = "";
                        if (box1 > 0 && pec1 > 0) {
                            str = box1 + "箱" + pec1 + "个";
                        } else if (box1 <= 0 && pec1 > 0) {
                            str = pec1 + "个";
                        } else if (box1 > 0 && pec1 <= 0) {
                            str = box1 + "箱";
                        }
                        amountMap.put(as.getKey(), str);
                    }
                }
            }
        }
        amountMap.put(a, "总计");
        boxList.add(amountMap);
        return boxList;
    }
}
