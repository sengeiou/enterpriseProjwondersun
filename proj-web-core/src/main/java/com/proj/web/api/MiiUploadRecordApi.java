package com.proj.web.api;

import com.eaf.core.dto.APIResult;
import com.eaf.core.dto.APIResultCode;
import com.eaf.core.utils.StringUtil;
import com.ebp.dto.SysUserDTO;
import com.ebp.web.comm.BaseController;
import com.proj.entity.MiiUploadRecord;
import com.proj.service.MiiUploadRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/proj/miiuploadrecord")
public class MiiUploadRecordApi extends BaseController {
    @Resource
    private MiiUploadRecordService miiUploadRecordService;

    /**
     * 上传到工信部文件记录
     *
     * @param inspectFile
     * @param certificateFile
     * @param batchCode
     * @param prodId
     * @param from
     * @param isFullPackage
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "upload"/*,method = RequestMethod.POST*/)
    @ResponseBody
    public Object upload(@RequestParam("inspectFile") MultipartFile inspectFile, @RequestParam("certificateFile") MultipartFile certificateFile, @RequestParam(value = "batchCode2", required = true) String batchCode, @RequestParam(value = "prodId", required = true) String prodId, String from, @RequestParam(required = false, defaultValue = "true") boolean isFullPackage) throws IOException {
        APIResult<String> apiResult = new APIResult<>();
        String inspectFileOriginFileName = inspectFile.getOriginalFilename();
        String certificateFileOriginFileName = certificateFile.getOriginalFilename();
        if (!miiUploadRecordService.existed(batchCode, prodId)) {
            apiResult.fail(1, "该产品的批号[" + batchCode + "]不存在");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }
        if ((!inspectFileOriginFileName.endsWith(".pdf") && !inspectFileOriginFileName.endsWith(".PDF")) || (!certificateFileOriginFileName.endsWith(".pdf") && !certificateFileOriginFileName.endsWith(".PDF"))) {
            apiResult.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是pdf文件）");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }
        //miiUploadRecordService.runMIIUploadProdVarietyTask();
        //miiUploadRecordService.runMIIUploadSingleProdTask();
        APIResult<Map<String, Object>> result = uploadInternal(prodId, batchCode, inspectFile, certificateFile, from, isFullPackage);
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } else {
            return result;
        }
    }

    private APIResult<Map<String, Object>> uploadInternal(String prodId, String batchCode, MultipartFile inspectFile, MultipartFile certificateFile, String from, boolean isFullPackage) throws IOException {
        APIResult<Map<String, Object>> apiResult = new APIResult<>();
        try {
            apiResult.setData(new HashMap<>());
            SysUserDTO userDTO = getCurrentUser();
            String inspectFileOriginFileName = inspectFile.getOriginalFilename();
            String certificateFileOriginFileName = certificateFile.getOriginalFilename();
            MiiUploadRecord miiUploadRecord = miiUploadRecordService.upload(prodId, batchCode, inspectFileOriginFileName, inspectFile.getBytes(), certificateFileOriginFileName, certificateFile.getBytes(), userDTO.getUserName());
            if (miiUploadRecord != null) {
                apiResult.succeed().getData().put("recordId", miiUploadRecord.getId());
            }
            return apiResult;
        } catch (Exception e) {
            return apiResult.fail(-1, e.getMessage());
        }
    }

    @RequestMapping(value = "download")
    @ResponseBody
    public void download(@RequestParam(value = "id") String id, @RequestParam(value = "type", required = true) String type, HttpServletRequest req, HttpServletResponse res) {

        File file = null;
        if ("0".equals(type)) {
            file = miiUploadRecordService.getConformityCertificatetUploadFileByID(id);
        } else {
            file = miiUploadRecordService.getProdInspectionReportUploadFileByID(id);
        }
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
        MiiUploadRecordApi.downLoadFile(req, res, file);
    }

    public static void downLoadFile(HttpServletRequest req, HttpServletResponse response, File file) {
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

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public Object delete(String[] ids) {
        APIResult<String> result = new APIResult<>();
        if (ids == null) {
            return result.fail(APIResultCode.ARGUMENT_INVALID, "ids is null");
        }
        for (String id : ids) {
            miiUploadRecordService.delete(id);
        }
        return result.succeed();
    }

    @RequestMapping(value = "reupload", method = RequestMethod.POST)
    @ResponseBody
    public Object reupload(String[] ids) {
        APIResult<String> result = new APIResult<>();
        if (ids == null) {
            return result.fail(APIResultCode.ARGUMENT_INVALID, "ids is null");
        }
        for (String id : ids) {
            result = miiUploadRecordService.reupload(id);
            if (result.getCode() != 0) {
                return result;
            }
        }
        return result.succeed();
    }

    @RequestMapping(value = "check", method = RequestMethod.GET)
    @ResponseBody
    public APIResult<String> checkExist(String id) {
        APIResult<String> apiResult = miiUploadRecordService.checkExist(id);
        return apiResult;
    }

    /**
     * 诚信证书上传到工信部
     *
     * @param certFile                   文件
     * @param from                       来源
     * @param productionLicenseNumber    生产许可证编号
     * @param productionLicenseEndDate   生产许可证有效期 ---结束日期
     * @param productionLicenseBeginDate 生产许可证有效期 --- 起始日期
     * @param creditCertificateBeginDate 诚信评价证书有效期 --- 起始日期
     * @param creditCertificateEndDate   诚信评价证书有效期 --- 结束日期
     * @param creditCertificateNumber    诚信评价证书编号
     * @param name                       生产者名称
     * @param production                 食品生产地
     * @return return
     * @throws IOException IOException
     */
    @RequestMapping(value = "doUpload")
    @ResponseBody
    public Object upload(@RequestParam("certFile") MultipartFile certFile, String from, String id, String productionLicenseNumber,
                         String productionLicenseEndDate, String productionLicenseBeginDate, String name, String creditCertificateBeginDate,
                         String creditCertificateEndDate, String creditCertificateNumber, String production) throws IOException {
        APIResult<String> apiResult = new APIResult<>();
        String certFileOriginFileName = certFile.getOriginalFilename();
        if ((!certFileOriginFileName.endsWith(".pdf") && !certFileOriginFileName.endsWith(".PDF")) && (!certFileOriginFileName.endsWith(".jpg") && !certFileOriginFileName.endsWith(".JPG"))) {
            apiResult.fail(APIResultCode.FORBIDDEN, "文件格式不正确（必须是pdf或jpg文件）");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(apiResult);
        }
        if (StringUtil.isEmpty(name)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写生产者名称");
        }
        if (StringUtil.isEmpty(production)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写食品生产地");
        }
        if (StringUtil.isEmpty(productionLicenseNumber)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写生产许可证编号");
        }
        if (StringUtil.isEmpty(productionLicenseBeginDate)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写生产许可证有效期开始时间");
        }
        if (StringUtil.isEmpty(productionLicenseEndDate)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写生产许可证有效期结束时间");
        }
        if (StringUtil.isEmpty(creditCertificateBeginDate)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写诚信评价证书有效期开始时间");
        }
        if (StringUtil.isEmpty(creditCertificateEndDate)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写诚信评价证书有效期结束时间");
        }
        if (StringUtil.isEmpty(creditCertificateNumber)) {
            return apiResult.fail(APIResultCode.FORBIDDEN, "请填写诚信评价证书编号");
        }
        APIResult<Map<String, Object>> result = uploadInternal(certFile, id);
        if (result.getCode() == 0) {
            try {
                miiUploadRecordService.createCompanyForFactory(id, productionLicenseNumber, productionLicenseEndDate, productionLicenseBeginDate, name,
                        creditCertificateBeginDate, creditCertificateEndDate, creditCertificateNumber, production,certFile);
            } catch (Exception e) {
                result.fail(500, e.getMessage());
            }
        }
        if (from != null && "web".equals(from)) {
            //为页面提供，主要是返回的数据的contentType为text，否则浏览器会提示下载文件
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } else {
            return result;
        }
    }

    private APIResult<Map<String, Object>> uploadInternal(MultipartFile certFile, String id) throws IOException {
        APIResult<Map<String, Object>> apiResult = new APIResult<>();
        try {
            apiResult.setData(new HashMap<>());
            SysUserDTO userDTO = getCurrentUser();
            String certFileOriginFileName = certFile.getOriginalFilename();
            String retstr = miiUploadRecordService.uploadCertFile(certFileOriginFileName, certFile.getBytes(), userDTO.getUserName(), id);

            if (!"-1".equals(retstr)) {
                apiResult.succeed().getData().put("addr", retstr);
            } else {
                apiResult.fail(-1, "上传失败");
            }
            return apiResult;
        } catch (Exception e) {
            return apiResult.fail(-1, e.getMessage());
        }
    }
}

class CheckBean {
    private String batchCode = null;
    private String prodId = null;

    public CheckBean() {
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getProdId() {
        return prodId;
    }

    public void setProdId(String prodId) {
        this.prodId = prodId;
    }
}
