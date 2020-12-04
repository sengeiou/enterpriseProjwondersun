package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.PageReq;
import com.eaf.core.dto.PageResult;
import com.eaf.core.utils.DataFileWriter;
import com.eaf.core.utils.csv.CsvWriter;
import com.eaf.core.utils.excel.ExcelWriter;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.api.ColumnDef;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.StruCommonQueryAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
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

/**
 * 通用查询api
 */
@Controller
@RequestMapping("/api/proj/strucommonquery")
public class StruCommonQueryApi extends BaseController {
    @Resource
    private StruCommonQueryAppService appService;


    @RequestMapping(value = "{module}/{submodule}/{action}")
    @ResponseBody
    public PageResult<Map> query(
            @RequestBody PageReq pageReq,
            @PathVariable String module, @PathVariable String submodule, @PathVariable String action
    ) {
        APIResult<PageResult<Map>> result = new APIResult<>();
        String authCode = module + "_" + submodule + ":" + (action.equals("list") ? "browse" : action);
        SecurityUtils.getSubject().checkPermission(authCode);

        InputStream scriptStream = this.getClass().getClassLoader()
                .getResourceAsStream("viewcfg/" + module.toLowerCase() + "/" + submodule + "/" + action + ".sql");
        try {
            if (scriptStream == null) {
                throw new FileNotFoundException();
            }
            //获取用户信息
            SysUserDTO userDTO = getCurrentUser();
            //根据用户信息设置单据对应组织ID条件
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            result = appService.getPageList(pageReq,userDTO.getId(),scriptStr);
            return result.getData();
        } catch (IOException e) {
            e.printStackTrace();
            result = new APIResult<>();
            return result.getData();
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


    @RequestMapping(value = "{module}/{submodule}/getbyid/{id}")
    @ResponseBody
    public Map getById(@PathVariable String module, @PathVariable String submodule, @PathVariable String id) {
        String authCode = module + "_" + submodule + ":" + "browse";
        SecurityUtils.getSubject().checkPermission(authCode);

        APIResult<Map> result;
        InputStream scriptStream = this.getClass().getClassLoader()
                .getResourceAsStream("viewcfg/" + module.toLowerCase() + "/" + submodule + "/view.sql");
        try {
            if (scriptStream == null) {
                throw new FileNotFoundException();
            }
            //获取用户信息
            SysUserDTO userDTO = getCurrentUser();
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            result = appService.getMapById(id,userDTO.getId(),scriptStr);
            return result.getData();
        } catch (IOException e) {
            e.printStackTrace();
            result = new APIResult<>();
            return result.getData();
        }
    }

    @RequestMapping(value = "{module}/{submodule}/query/{action}")
    @ResponseBody
    public APIResult<List<Map>> queryWithQueryString(
            @PathVariable String module, @PathVariable String submodule, @PathVariable String action, HttpServletRequest req
    ) {
        APIResult<List<Map>> result = new APIResult<>();
        String authCode = module + "_" + submodule + ":" + "browse";
        SecurityUtils.getSubject().checkPermission(authCode);
        InputStream scriptStream = this.getClass().getClassLoader()
                .getResourceAsStream("viewcfg/" + module.toLowerCase() + "/" + submodule + "/" + action + ".sql");

        Map<String, String[]> queryString = req.getParameterMap();
        Map<String, String> onlyFirstParamMap = new HashMap<>();
        queryString.forEach((k, v) -> {
            if (k.equals("_")) {
                return;
            }
            onlyFirstParamMap.put(k, v[0]);
        });

        try {
            if (scriptStream == null) {
                throw new FileNotFoundException();
            }
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            return result.succeed().attachData(appService.getRowsByQueryMap(onlyFirstParamMap, scriptStr));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "{module}/{submodule}/export", method = RequestMethod.POST)
    public void export(
            HttpServletResponse res,HttpServletRequest req, String pageReqStr, String pageDefStr, String pageTitle,@RequestParam(defaultValue = "excel", required = false) String pageExportType,
            @PathVariable String module, @PathVariable String submodule) throws Exception {
        APIResult<PageResult<Map>> result = new APIResult<>();
        ObjectMapper mapper = new ObjectMapper();
        PageReq pageReq = null;
        ArrayList<ColumnDef> pageDef = null;

        //read the sql file to do the query
        String cfgSQLPath = "viewcfg/" + module.toLowerCase() + "/" + submodule + "/" + "list.sql";
        InputStream scriptStream = this.getClass().getClassLoader().getResourceAsStream(cfgSQLPath);

        try {
            pageReq = mapper.readValue(pageReqStr, PageReq.class);
            //and this query need no pagination
            pageReq.setPage(1);
            pageReq.setRows(Integer.MAX_VALUE);

            //parse 'pageDefStr' into List<ColumnDef>
            //this list is used to decide which columns is needed in the exported file
            //just the same as 'datagrid columns' in the front-end page
            pageDef = mapper.readValue(pageDefStr, mapper.getTypeFactory().constructCollectionType(ArrayList.class, ColumnDef.class));

            if (scriptStream == null) {
                throw new FileNotFoundException(cfgSQLPath);
            }
            //获取用户信息
            SysUserDTO userDTO = getCurrentUser();
            //根据用户信息设置单据对应组织ID条件
            String scriptStr = IOUtils.toString(scriptStream, "utf8");
            result = appService.getPageList(pageReq,userDTO.getId(),scriptStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //默认是EXCEL
        if (StringUtils.isEmpty(pageExportType)) {
            pageExportType = "excel";
        }
        String fileExt = "csv".equals(pageExportType) ? ".csv" : ".xlsx";
        String fileName = null;
        try {
            String ua = req.getHeader("User-Agent");
            if(ua.toUpperCase().indexOf("FIREFOX") > -1) {
                fileName = new String((pageTitle + fileExt).getBytes("UTF-8"), "ISO-8859-1");
            } else {
                fileName = URLEncoder.encode(pageTitle + fileExt, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        res.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        res.setContentType("application/octet-stream; charset=utf-8");
        DataFileWriter ew = null;
        if ("csv".equals(pageExportType)) {
            ew = new CsvWriter();
        } else {
            ew = new ExcelWriter();
        }
        for (ColumnDef columnDef : pageDef) {
            ew.addColumnInfo(columnDef.getName(), columnDef.getTitle());
        }
        ew.write(res.getOutputStream(), result.getData().getRows(), "sheet1");
    }
}
