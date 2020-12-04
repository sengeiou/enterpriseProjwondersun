package com.proj.appservice.impl;

import com.alibaba.druid.util.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.ebc.appservice.ProductQcRecordAppService;
import com.arlen.ebc.biz.miitprc.NewSingleProdXMLProcessor;
import com.arlen.ebc.entity.ProductQcRecord;
import com.arlen.ebc.repository.ProductQcRecordRepository;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebc.util.CommUtils;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.repository.SysParaRepository;
import com.proj.appservice.ProjProductQcRecordAppService;
import com.arlen.utils.common.DateTimeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Created by arlenChen on 2017/12/25.
 * 质检单
 *
 * @author arlenChen
 */
@Service
public class ProjProductQcRecordAppServiceImpl implements ProjProductQcRecordAppService {
    protected static Logger logger = LoggerFactory.getLogger(NewSingleProdXMLProcessor.class);

    @Resource
    private ProductionCodeRepository productionCodeRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private ProductQcRecordAppService productQcRecordAppService;
    @Resource
    private ProductQcRecordRepository productQcRecordRepository;

    @Resource(name = "sysProperties")
    private Properties sysProperties;
    //图片的默认保存路径
    private static final String QCFILE_IMAGE_DEFAULT_PATH = "upload/QCFile/";

    /**
     * 校验批次是否生产该产品
     *
     * @param batchCode  批次
     * @param materialId 产品
     * @return return
     */
    @Override
    public Map<String, Object> existed(String batchCode, String materialId) {
        Map<String, Object> map = new HashMap<>(2);
        boolean existed = productionCodeRepository.existed(batchCode, materialId);
        SysPara sysPara = sysParaRepository.load("RawMaterialSource");
        if (sysPara == null || sysPara.getValue() == null) {
            throw new RuntimeException("数据库中[原始物料来源地字符串]未配置");
        }
        String rawMaterialSource = sysPara.getValue();
        map.put("existed", existed);
        map.put("rawMaterialSource", getRawMaterialSource(batchCode, rawMaterialSource));
        return map;
    }

    /**
     * 主要原料来源地下拉
     *
     * @return 下拉
     */
    @Override
    public List<Map<String, String>> getComboBox() {
        List<Map<String, String>> mapList = new ArrayList<>();
        SysPara sysPara = sysParaRepository.load("RawMaterialSource");
        if (sysPara == null || sysPara.getValue() == null) {
            throw new RuntimeException("数据库中[原始物料来源地字符串]未配置");
        }
        String value = sysPara.getValue();
        String[] codeName = value.split(",");
        for (String code : codeName) {
            Map<String, String> map = new HashMap<>(2);
            String[] codeNameArr = code.split(":");
            if (codeNameArr.length < 2) {
                continue;
            }
            map.put("text", codeNameArr[1]);
            map.put("value", codeNameArr[1]);
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * 上传质检报告
     *
     * @param prodId                              产品
     * @param batchCode                           批次
     * @param prodInspectionReportOriginFileName  产品检验报告
     * @param prodInspectionReportFileData        产品检验报告
     * @param conformityCertificateOriginFileName 主要原料合格证明
     * @param conformityCertificateFileData       主要原料合格证明
     * @param addBy                               添加人
     * @param materialSource                      主要原料来源地
     * @return return
     */
    @Override
    public ProductQcRecord upload(String prodId, String batchCode, String prodInspectionReportOriginFileName,
                                  byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy, String materialSource ,
                                  String productStandard,String productStandardPdfFileName ,byte[] productStandardPdfFileData) {
        ProductQcRecord productQcRecord = productQcRecordAppService.upload(prodId, batchCode, prodInspectionReportOriginFileName,
                prodInspectionReportFileData, conformityCertificateOriginFileName, conformityCertificateFileData, addBy,productStandard,productStandardPdfFileName,productStandardPdfFileData);
        productQcRecord.setAttr1(materialSource);
        productQcRecordRepository.save(productQcRecord);
        return productQcRecord;
    }

    @Override
    public APIResult uploadNoRecord(String prodId, String batchCode, String prodInspectionReportOriginFileName, byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy, String materialSource,
                                    String productStandard,String productStandardPdfFileName ,byte[] productStandardPdfFileData) {
        APIResult result = productQcRecordAppService.uploadNoRecord(prodId, batchCode, prodInspectionReportOriginFileName,
                prodInspectionReportFileData, conformityCertificateOriginFileName, conformityCertificateFileData, addBy,productStandard,productStandardPdfFileName,productStandardPdfFileData);
        if (result.getCode() != 0) {
            return result;
        }
        ProductQcRecord productQcRecord = productQcRecordRepository.load(result.getData());
        productQcRecord.setAttr1(materialSource);
        productQcRecordRepository.save(productQcRecord);
        return result;
    }

    /**
     * 重新上传产品标准
     * @param id
     * @param productStandard
     * @param productStandardPdf
     * @param userDTO
     * @return
     * @throws Exception
     */
    @Override
    public APIResult<Object> reUploadProductStandard(String id, String productStandard, MultipartFile productStandardPdf, SysUserDTO userDTO) throws Exception {
        APIResult<Object> apiResult = new APIResult<>();
        // 之前的批次质检单
        ProductQcRecord productQcRecord = productQcRecordRepository.load(id);
        if (null == productQcRecord) {
            return apiResult.fail(APIResultCode.NOT_FOUND, "质检记录不存在");
        }
        String productStandardPdfFileName = productStandardPdf.getOriginalFilename();
        if (!checkFileFormat(productStandardPdfFileName)) {
            return apiResult.fail(6603, "产品标准不支持该文件格式");
        }
        String addBy = userDTO.getUserName();
        byte[] productStandardPdfFileData = productStandardPdf.getBytes();
        // 产品标准PDF上传（报错到磁盘）
        String productStandardPdfPath = doUpload(productStandardPdfFileName, productStandardPdfFileData,addBy);
        // 数据库赋值
        productQcRecord.setEditBy(addBy);
        productQcRecord.setEditTime(DateTimeTool.getCurDatetime());
        productQcRecord.setProductStandard(productStandard);
        productQcRecord.setProductStandardPdf(productStandardPdfPath);
        productQcRecordRepository.save(productQcRecord);
        apiResult.succeed();
        return apiResult;
    }

    private String getRawMaterialSource(String batchCode, String rawMaterialSourceKv) {
        String rawMaterialSource = "";
        try {
            //解析出代码，第9到10位为代码
            if (StringUtils.isEmpty(batchCode) || batchCode.length() < 10) {
                return "";
            }
            String rawMaterialSourceCode = batchCode.substring(8, 10);
            if (rawMaterialSourceKv == null || StringUtils.isEmpty(rawMaterialSourceKv)) {
                return "";
            }
            String[] codeName = rawMaterialSourceKv.split(",");
            for (String code : codeName) {
                String[] codeNameArr = code.split(":");
                if (codeNameArr.length < 2) {
                    continue;
                }
                if (rawMaterialSourceCode.equals(codeNameArr[0])) {
                    rawMaterialSource = codeNameArr[1];
                    break;
                }
            }
        } catch (Exception ex) {
            logger.error("单品获取对应的原料来源地出错", ex);
            throw new RuntimeException("单品获取对应的原料来源地出错:" + ex.getMessage());
        }
        return rawMaterialSource;
    }


    /**
     * 上传文件
     *
     * @param originFileName
     * @param fileData
     * @param addBy
     * @return
     */
    public String doUpload(String originFileName, byte[] fileData, String addBy) {
        //获取保存目录
        String fileBaseDir = getUploadDir();
        String fileDir = QCFILE_IMAGE_DEFAULT_PATH;
        String fileFullDir = fileBaseDir + fileDir;
        logger.info("上传检单文件路径：" + fileFullDir);
        Path dirPath = Paths.get(fileFullDir);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("文件目录创建失败:" + originFileName, e);
                throw new RuntimeException("文件目录创建失败");
            }
        }
        //保存
        Date now = new Date();
        String fileName = now.getTime() + "-" + UUID.randomUUID().toString() + originFileName.substring(originFileName.indexOf('.'));
        Path file = Paths.get((fileFullDir + fileName));
        try {
            Files.write(file, fileData, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("文件保存失败:" + originFileName, e);
            throw new RuntimeException("文件保存失败");
        }
        String recordFileName = fileName;
        if (originFileName.lastIndexOf(".zip") > 0) {
            try {
                recordFileName = unzipFile(fileFullDir, fileName);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("文件解压失败:" + originFileName, e);
                throw new RuntimeException("文件解压失败");
            }
        }
        return (fileDir + recordFileName);
    }

    /**
     * 解压zip文件
     *
     * @param dir      文件所在目录
     * @param fileName 文件名
     * @return 操作结果
     * @throws IOException IO异常
     */
    private String unzipFile(String dir, String fileName) throws IOException {
        String unzipedFileName;
        final int BUFFER = 2048;
        BufferedOutputStream dest;
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(dir + fileName));  // 定义压缩输入流
        ZipEntry entry = zipInputStream.getNextEntry(); // 得到一个压缩实体
        if (entry == null) {
            logger.warn("zip file invalid:" + dir + fileName);
            return "";
        }
        unzipedFileName = entry.getName();
        int count;
        byte data[] = new byte[BUFFER];
        FileOutputStream fos = new FileOutputStream(dir + entry.getName());
        dest = new BufferedOutputStream(fos, BUFFER);
        while ((count = zipInputStream.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
        zipInputStream.close();
        return unzipedFileName;
    }

    /**
     * 获取上传文件保存目录
     *
     * @return 上传文件保存目录
     */
    public String getUploadDir() {
        String result = CommUtils.getValueFromProp(sysProperties, "ProductQcFileLocalDir", null);
        String fileDir;
        if (result != null && org.apache.commons.lang3.StringUtils.isNotEmpty(result)) {
            fileDir = result;
        } else {
            System.out.println("Working Directory = " +
                    System.getProperty("user.dir"));
            fileDir = System.getProperty("user.dir") + "/ProductQcFile_upload/";
        }
        return fileDir;
    }

    /**
     * 验证文件格式
     *
     * @param fileName
     * @return
     */
    private boolean checkFileFormat(String fileName) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(fileName)) {
            return false;
        }
        fileName = fileName.toLowerCase();
        //验证文件格式
        String fileFormatStr = CommUtils.getValueFromProp(sysProperties, "ebp.QCFileFormat", null);
        String[] fileFormat = {".pdf", ".jpg", ".jpeg", ".png"};
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(fileFormatStr)) {
            fileFormat = fileFormatStr.trim().toLowerCase().split(",");
        }
        int len = fileFormat.length;
        for (int i = 0; i < len; i++) {
            if (fileName.endsWith(fileFormat[i])) {
                return true;
            } else if (i == len - 1) {
                return false;
            }
        }
        return false;
    }

}
