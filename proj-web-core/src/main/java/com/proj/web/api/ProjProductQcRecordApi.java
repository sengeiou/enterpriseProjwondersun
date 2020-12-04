package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.ebc.appservice.ProductQcRecordAppService;
import com.ebc.biz.codefile.ThreadLocalDateUtil;
import com.ebc.dto.ProductQcRecordDTO;
import com.ebc.entity.ProductQcRecord;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.proj.appservice.ProjProductQcRecordAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by arlenChen on 2016/11/22.
 *
 * @author arlenChen
 */
@Controller
@RequestMapping("/api/ebc/projproductqcrecord")
public class ProjProductQcRecordApi extends BaseController {

    @Resource
    private ProductQcRecordAppService productQcRecordAppService;
    @Resource
    private ProjProductQcRecordAppService appService;

    @Resource(name = "sysProperties")
    private Properties sysProperties;

    private Logger logger = LoggerFactory.getLogger(ProjProductQcRecordApi.class);

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public APIResult<ProductQcRecordDTO> getById(@RequestParam(value = "id") String id) {
        return productQcRecordAppService.getById(id);
    }

    /**
     * 上传质检报告
     *
     * @param file 文件
     * @param from 来源
     * @param type 1：主要产品合格证明  2：质检报告
     * @return return
     * @throws IOException IOException
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    public Object upload(@RequestParam(value = "id") String id, @RequestParam("file") MultipartFile file, String type,
                         String from) throws IOException {
        APIResult<String> apiResult = new APIResult<>();
        String certFileOriginFileName = file.getOriginalFilename();
        boolean isTrue = (!certFileOriginFileName.endsWith(".pdf") && !certFileOriginFileName.endsWith(".PDF")) && (!certFileOriginFileName.endsWith(".jpg") && !certFileOriginFileName.endsWith(".JPG"));
        if (isTrue) {
            apiResult.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是pdf或jpg文件）");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }

        APIResult<Object> result = uploadInternal(id, file, type);
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } else {
            return result;
        }
    }

    private APIResult<Object> uploadInternal(String id, MultipartFile file, String type) throws IOException {
        APIResult<Object> apiResult = new APIResult<>();
        try {
            apiResult.setData(new HashMap<>(0));
            SysUserDTO userDTO = getCurrentUser();
            String certFileOriginFileName = file.getOriginalFilename();
            APIResult<ProductQcRecord> result = productQcRecordAppService.upload(id, certFileOriginFileName, file.getBytes(), userDTO.getUserName(), type);
            if (result.getCode() != 0) {
                return apiResult.fail(-1, result.getErrMsg());
            }
            return apiResult.succeed();
        } catch (Exception e) {
            return apiResult.fail(-1, e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param id   id
     * @param type 1：下载主要产品合格证明  2：下载质检报告
     * @param req  req
     * @param res  res
     */
    @RequestMapping(value = "download")
    @ResponseBody
    public void download(@RequestParam(value = "id") String id,
                         @RequestParam(required = false) String type,
                         HttpServletRequest req, HttpServletResponse res) {
        File file = null;
        file = productQcRecordAppService.getProdInspectionReportUploadFileByID(id, type);
        if (file == null) {
            ServletOutputStream out = null;
            try {
                out = res.getOutputStream();
                out.write("文件不存在".getBytes());
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
        ProjProductQcRecordApi.downLoadFile(req, res, file);
    }

    /**
     * 发送文件到工信部 单品文件
     *
     * @param req req
     * @param res res
     */
    @RequestMapping(value = "spx.xml")
    @ResponseBody
    public void sendFileToMIIT(HttpServletRequest req, HttpServletResponse res) {
        sendFile(req, res, "spx");
    }

    /**
     * 发送文件到工信部 单品文件
     *
     * @param req req
     * @param res res
     */
    @RequestMapping(value = "pvx.xml")
    @ResponseBody
    public void sendFileToMIIT2(HttpServletRequest req, HttpServletResponse res) {
        sendFile(req, res, "pvx");
    }

    private void sendFile(HttpServletRequest req, HttpServletResponse res, String what) {
        String midPath = getFileDir();
        //XML文件输出目录
        String xmlMidPath = sysProperties.getProperty("SOURCEXMLOUTPUTPATH") + File.separator + midPath;
        String fileName;
        if ("pvx".equals(what)) {
            fileName = "prodvariety.xml";
            try {
                Files.createFile(Paths.get(xmlMidPath + File.separator + "prodvariety.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fileName = "singleprod.xml";
            try {
                Files.createFile(Paths.get(xmlMidPath + File.separator + "singleprod.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String xmlOutputFile = xmlMidPath + fileName;
        int index = xmlOutputFile.lastIndexOf(File.separator);
        String parentPath = xmlOutputFile.substring(0, index);
        Path dirPath = Paths.get(parentPath);
        Path file = Paths.get(xmlOutputFile);
        if (!Files.exists(dirPath) || !Files.exists(file)) {
            ServletOutputStream out = null;
            try {
                out = res.getOutputStream();
                out.write("404".getBytes());
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
        ProjProductQcRecordApi.downLoadFile(req, res, new File(xmlOutputFile));
    }

    /**
     * 获取文件存放目录，包含当前年月日
     *
     * @return 文件目录
     */
    private String getFileDir() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR) + "\\" + (calendar.get(Calendar.MONTH) + 1) + "\\" + ThreadLocalDateUtil.getDateFormat().format(calendar.getTime()) + "\\";
    }

    private static void downLoadFile(HttpServletRequest req, HttpServletResponse response, File file) {
        if (file != null && file.exists()) {
            ServletOutputStream out = null;
            String fileName = "";
            try {
                String ua = req.getHeader("User-Agent");
                if (ua.toUpperCase().indexOf("FIREFOX") > -1) {
                    fileName = new String(file.getName().getBytes("UTF-8"), "ISO-8859-1");
                } else {
                    fileName = URLEncoder.encode(file.getName(), "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                fileName = file.getName();
            }

            try {
                response.reset();
                response.setContentType("application/octet-stream; charset=utf-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                out = response.getOutputStream();
                out.write(FileUtils.readFileToByteArray(file));
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

    /**
     * 删除
     *
     * @param ids ids
     * @return return
     */
    @RequestMapping(value = {"delete"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object delete(@RequestParam("ids") String[] ids) {
        APIResult<String> mes = new APIResult<>();
        int k = 0;
        if (ids == null) {
            return mes.fail(809, "ids not null");
        } else {
            for (String id : ids) {
                APIResult<String> result = this.productQcRecordAppService.delete(id);
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


    /**
     * 撤销质检
     *
     * @param ids ids
     * @return return
     */
    @RequestMapping(value = {"unqc"}, method = {RequestMethod.POST})
    @ResponseBody
    public Object unQc(@RequestParam("ids") String[] ids) {
        APIResult<String> mes = new APIResult<>();
        int k = 0;
        if (ids == null) {
            return mes.fail(809, "ids not null");
        } else {
            for (String id : ids) {
                APIResult<String> result = this.productQcRecordAppService.unQc(id);
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


    /**
     * 新增质检记录
     *
     * @param inspectFile     产品检验报告
     * @param certificateFile 主要原料合格证明
     * @param batchCode       批次
     * @param prodId          产品
     * @param from            来源
     * @return return
     * @throws IOException 异常
     */
    @RequestMapping(value = "uploadnorecord")
    @ResponseBody
    public Object uploadNoRecord(@RequestParam(value = "inspectFile") MultipartFile inspectFile,
                                 @RequestParam(value = "certificateFile") MultipartFile certificateFile,
                                 @RequestParam(value = "batchCode") String batchCode,
                                 @RequestParam(value = "materialSource") String materialSource,
                                 @RequestParam(value = "prodId") String prodId,
                                 @RequestParam(value = "productStandard") String productStandard,
                                 @RequestParam(value = "productStandardPdf") MultipartFile productStandardPdf, String from) throws IOException {
        APIResult<Map<String, Object>> result = uploadInternalNoRecord(prodId, batchCode, inspectFile, certificateFile, materialSource,productStandard,productStandardPdf);
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } else {
            return result;
        }
    }


    /**
     * 上传无记录的质检报告 -无记录，文件存放本地
     *
     * @param prodId          产品
     * @param batchCode       批次
     * @param inspectFile     产品检验报告
     * @param certificateFile 主要原料合格证明
     * @return return
     * @throws IOException IOException
     */
    private APIResult<Map<String, Object>> uploadInternalNoRecord(String prodId, String batchCode, MultipartFile inspectFile,
                                                                  MultipartFile certificateFile, String materialSource,String productStandard,MultipartFile productStandardPdf) throws IOException {
        APIResult<Map<String, Object>> apiResult = new APIResult<>();
        try {
            apiResult.setData(new HashMap<>(0));
            SysUserDTO userDTO = getCurrentUser();
            String inspectFileOriginFileName = inspectFile.getOriginalFilename();
            String certificateFileOriginFileName = certificateFile.getOriginalFilename();
            String productStandardPdfFileName = productStandardPdf.getOriginalFilename();
            APIResult result = appService.uploadNoRecord(prodId, batchCode, inspectFileOriginFileName, inspectFile.getBytes(), certificateFileOriginFileName, certificateFile.getBytes(), userDTO.getUserName(), materialSource,productStandard,productStandardPdfFileName,productStandardPdf.getBytes());
            if (result.getCode() == 0) {
                return apiResult.succeed();
            } else {
                return apiResult.fail(result.getCode(), result.getErrMsg());
            }
        } catch (Exception e) {
            return apiResult.fail(-1, e.getMessage());
        }
    }

    /**
     * 校验批次下是否存在编码
     *
     * @param batchCode 批次
     * @param prodId    产品
     * @return return
     */
    @RequestMapping(value = "check", method = RequestMethod.POST)
    @ResponseBody
    public APIResult<String> checkBatchCode(String batchCode, String prodId) {
        APIResult<String> apiResult = new APIResult<>();
        Map<String, Object> map = appService.existed(batchCode, prodId);
        boolean existed = (boolean) map.get("existed");
        if (!existed) {
            return apiResult.fail(1, "不存在");
        } else {
            return (apiResult.succeed().attachData((String) map.get("rawMaterialSource")));
        }
    }

    @RequestMapping(value = "getComboBox")
    @ResponseBody
    public List<Map<String, String>> getComboBox() {
        return appService.getComboBox();
    }

    /**
     * 重新上传质检报告和主要产品合格证明
     *
     * @param inspectFile     产品检验报告
     * @param certificateFile 主要原料合格证明
     * @param id              id
     * @param from            来源
     * @return return
     * @throws IOException IOException
     */
    @RequestMapping(value = "reuploadqcandcc")
    @ResponseBody
    public Object reuploadQCAndCC(@RequestParam(value = "inspectFile") MultipartFile inspectFile,
                                  @RequestParam(value = "certificateFile") MultipartFile certificateFile,
                                  @RequestParam(value = "id") String id, String from) throws IOException {
        APIResult apiResult;
        SysUserDTO userDTO = getCurrentUser();
        String inspectFileOriginFileName = inspectFile.getOriginalFilename();
        String certificateFileOriginFileName = certificateFile.getOriginalFilename();
        apiResult = productQcRecordAppService.reuploadQCAndCC(id, inspectFileOriginFileName, inspectFile.getBytes(), certificateFileOriginFileName, certificateFile.getBytes(), userDTO.getUserName());
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        } else {
            return apiResult;
        }
    }

    /**
     * 上传到工信部文件记录
     *
     * @param inspectFile     产品检验报告
     * @param certificateFile 主要原料合格证明
     * @param batchCode       批次
     * @param prodId          产品
     * @param from            来源
     * @return return
     * @throws IOException IOException
     */
    @RequestMapping(value = "uploaddubfile")
    @ResponseBody
    public Object uploadFiles(@RequestParam("inspectFile") MultipartFile inspectFile,
                              @RequestParam("certificateFile") MultipartFile certificateFile,
                              @RequestParam(value = "batchCode") String batchCode,
                              @RequestParam(value = "materialSource") String materialSource,
                              @RequestParam(value = "prodId") String prodId,
                              @RequestParam(value = "productStandard") String productStandard,
                              @RequestParam(value = "productStandardPdf") MultipartFile productStandardPdf,String from) throws IOException {
        APIResult<String> apiResult = new APIResult<>();
        if (!productQcRecordAppService.existed(batchCode, prodId)) {
            apiResult.fail(1, "该产品的批号[" + batchCode + "]不存在");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }
        String inspectFileOriginFileName = inspectFile.getOriginalFilename();
        String certificateFileOriginFileName = certificateFile.getOriginalFilename();
        String productStandardPdfFileName = productStandardPdf.getOriginalFilename();
        boolean isTure = (!inspectFileOriginFileName.endsWith(".pdf") && !inspectFileOriginFileName.endsWith(".PDF")) || (!certificateFileOriginFileName.endsWith(".pdf") && !certificateFileOriginFileName.endsWith(".PDF")) || (!productStandardPdfFileName.endsWith(".pdf") && !productStandardPdfFileName.endsWith(".PDF"));
        if (isTure) {
            apiResult.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是pdf文件）");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }
        APIResult<Map<String, Object>> result = uploadFileInternal(prodId, batchCode, inspectFile, certificateFile, materialSource,productStandard,productStandardPdf);
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } else {
            return result;
        }
    }

    /**
     * 保存文件  完达山
     *
     * @param prodId          产品
     * @param batchCode       批次
     * @param inspectFile     产品检验报告
     * @param certificateFile 主要原料合格证明
     * @return return
     * @throws IOException IOException
     */
    private APIResult<Map<String, Object>> uploadFileInternal(String prodId, String batchCode, MultipartFile inspectFile, MultipartFile certificateFile, String materialSource,String productStandard, MultipartFile productStandardPdf) throws IOException {
        APIResult<Map<String, Object>> apiResult = new APIResult<>();
        try {
            apiResult.setData(new HashMap<>(0));
            SysUserDTO userDTO = getCurrentUser();
            String inspectFileOriginFileName = inspectFile.getOriginalFilename();
            String certificateFileOriginFileName = certificateFile.getOriginalFilename();
            String productStandardPdfFileName = productStandardPdf.getOriginalFilename();
            ProductQcRecord miiUploadRecord = appService.upload(prodId, batchCode, inspectFileOriginFileName, inspectFile.getBytes(), certificateFileOriginFileName, certificateFile.getBytes(), userDTO.getUserName(), materialSource,productStandard,productStandardPdfFileName,productStandardPdf.getBytes());
            if (miiUploadRecord != null) {
                apiResult.succeed().getData().put("recordId", miiUploadRecord.getId());
            }
            return apiResult;
        } catch (Exception e) {
            return apiResult.fail(-1, e.getMessage());
        }
    }

    /**
     * 完达山重新上传
     *
     * @param ids ids
     * @return return
     */
    @RequestMapping(value = "reupload", method = RequestMethod.POST)
    @ResponseBody
    public Object reupload(String[] ids, @RequestParam(value = "type") int type) {
        APIResult<String> result = new APIResult<>();
        if (ids == null) {
            return result.fail(APIResultCode.ARGUMENT_INVALID, "ids is null");
        }
        for (String id : ids) {
            result = productQcRecordAppService.reupload(id, type);
            if (result.getCode() != 0) {
                return result;
            }
        }
        return result.succeed();
    }


    /**
     * 重新上传产品标准
     * @param id
     * @param productStandard 产品标准
     * @param productStandardPdf 产品标准PDF
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "reuploadproductstandard")
    @ResponseBody
    public Object reUploadProductStandard(String id,
                              @RequestParam(value = "productStandard") String productStandard,
                              @RequestParam(value = "productStandardPdf") MultipartFile productStandardPdf) throws IOException {
        APIResult<Object> apiResult = new APIResult<>();
        try{
            SysUserDTO userDTO = getCurrentUser();
            apiResult = appService.reUploadProductStandard(id,productStandard,productStandardPdf,userDTO);
        }catch (Exception ex){
            logger.error("重新上传产品标准-异常",ex);
        }
        return  apiResult;
    }

}
