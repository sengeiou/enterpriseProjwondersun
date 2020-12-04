package com.proj.service.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.biz.miitprc.HttpClientUploadProcessor;
import com.arlen.ebc.biz.miitprc.Utils;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebc.util.CommUtils;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.entity.CompanyForFactory;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.repository.CompanyForFactoryRepository;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebt.util.ComUtils;
import com.proj.biz.ThreadLocalDateUtil;
import com.proj.entity.MiiUploadRecord;
import com.proj.enums.MiiUploadStatus;
import com.proj.repository.MiiUploadRecordRepository;
import com.proj.service.MiiUploadRecordService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 上传到工信部文件记录服务
 */
@Service
public class MiiUploadRecordServiceImpl implements MiiUploadRecordService {
    private static Logger logger = LoggerFactory.getLogger(MiiUploadRecordServiceImpl.class);

    @Resource
    private MiiUploadRecordRepository miiUploadRecordRepository;
    @Resource
    private ProductionCodeRepository productionCodeRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private CompanyForFactoryRepository repo;

    /**
     * 上传文件
     *
     * @param originFileName 原始文件名
     * @param fileData       数据
     * @param addBy          添加人
     **/
    @Override
    public String doUpload(String originFileName, byte[] fileData, String addBy) {
        //获取保存目录
        String fileBaseDir = getUploadDir();
        String fileDir = getFileDir();
        String fileFullDir = fileBaseDir + fileDir;
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
        String fileName = now.getTime() + "-" + originFileName;
        Path file = Paths.get(fileFullDir + fileName);
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

    @Override
    public void beginUpload(String id) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(id);
        miiUploadRecord.setFileUploadStatus(MiiUploadStatus.UPLOADING.index);
        miiUploadRecord.setEditTime(new Date());
        miiUploadRecordRepository.save(miiUploadRecord);
    }

    @Override
    public void finishUpload(MiiUploadRecord miiUploadRecordResult) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(miiUploadRecordResult.getId());
        miiUploadRecord.setFileUploadStatus(miiUploadRecordResult.getFileUploadStatus());
        miiUploadRecord.setResult(miiUploadRecordResult.getResult());
        miiUploadRecord.setMiiUploadTime(miiUploadRecordResult.getMiiUploadTime());
        miiUploadRecord.setEditTime(new Date());
        miiUploadRecord.setCodeQty(miiUploadRecordResult.getCodeQty());
        miiUploadRecordRepository.save(miiUploadRecord);
    }


    /**
     * 上传质检报告和合格证
     *
     * @param prodId                              产品id
     * @param batchCode                           批号
     * @param prodInspectionReportOriginFileName  产品检验报告名称
     * @param prodInspectionReportFileData        产品检验报告
     * @param conformityCertificateOriginFileName 主要原料合格证明名称
     * @param conformityCertificateFileData       主要原料合格证明
     * @param addBy                               添加人
     * @return return
     */
    @Override
    public MiiUploadRecord upload(String prodId, String batchCode, String prodInspectionReportOriginFileName, byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy) {
        String prodInspectionReportPath = doUpload(prodInspectionReportOriginFileName, prodInspectionReportFileData, addBy);
        String conformityCertificatePath = doUpload(conformityCertificateOriginFileName, conformityCertificateFileData, addBy);
        //生成数据库记录
        Material material = materialRepository.load(prodId);
        MiiUploadRecord importCodeRecord = new MiiUploadRecord();
        importCodeRecord.setProdId(prodId);
        importCodeRecord.setProdInspectionReport(prodInspectionReportPath);
        importCodeRecord.setConformityCertificate(conformityCertificatePath);
        importCodeRecord.setBatchCode(batchCode);
        importCodeRecord.setUploadLocalTime(new Date());
        importCodeRecord.setProdSKU(material.getSku());
        importCodeRecord.setAddBy(addBy);
        importCodeRecord.setAddTime(new Date());
        importCodeRecord.setEditBy(addBy);
        importCodeRecord.setEditTime(new Date());
        importCodeRecord.setFileUploadStatus(MiiUploadStatus.TOBEUPLOADED.index);
        miiUploadRecordRepository.insert(importCodeRecord);
        return importCodeRecord;
    }

    /**
     * 删除
     *
     * @param id ID
     * @return 操作结果
     */
    @Override
    public APIResult<String> delete(String id) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(id);
        if (miiUploadRecord == null) {
            return new APIResult<String>().fail(APIResultCode.NOT_FOUND, "数据不存在");
        }
        miiUploadRecordRepository.delete(miiUploadRecord);
        return new APIResult<String>().succeed();
    }

    @Override
    public APIResult<String> reupload(String id) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(id);
        if (miiUploadRecord == null) {
            return new APIResult<String>().fail(APIResultCode.NOT_FOUND, "数据不存在");
        }
        miiUploadRecord.setFileUploadStatus(MiiUploadStatus.TOBEUPLOADED.index);
        miiUploadRecord.setResult(null);
        miiUploadRecordRepository.save(miiUploadRecord);
        return new APIResult<String>().succeed();
    }

    @Override
    public APIResult<String> checkExist(String id) {
        APIResult<String> result = new APIResult<>();
        Organization org = organizationRepository.load(id);
        if (org == null) {
            result.fail(-1, "工厂不存在");
        } else {
            String val = org.getAttr3();
            if (StringUtil.isEmpty(val)) {
                result.fail(-2, "未上传");
            } else {
                result.succeed().setData(val);
            }
        }
        return result;
    }

    private void loadPropFromDB() {
        // 诚信评价证书地址 上传到工信部软件促进中心
        sysProperties.setProperty("CERTIFICATEURL", cacheData("CertificateUrl"));
        // 诚信评价证书地址 上传到工信部一所
        sysProperties.setProperty("CERTIFICATEURL4ONE", cacheData("CertificateUrl4one"));
        // 诚信评价证书有效期
        sysProperties.setProperty("CERTIFICATEDATE", cacheData("CertificateDate"));
        // 诚信评价证书编号
        sysProperties.setProperty("CERTIFICATENUMBER", cacheData("CertificateNumber"));
        // 企业名称
        sysProperties.setProperty("COMPANYNAME", cacheData("CompanyName"));
        // 企业地址
        sysProperties.setProperty("PRODUCERADDRESS", cacheData("ProducerAddress"));
        // 企业网址
        sysProperties.setProperty("PRODUCERWEB", cacheData("ProducerWeb"));
        // 生产者名称
        sysProperties.setProperty("PRODUCERNAME", cacheData("ProducerName"));
        // 联系方式
        sysProperties.setProperty("PRODUCERCONTACT", cacheData("ProducerContact"));
        // 标码前缀
        sysProperties.setProperty("STANDARD_CODE_PREFIX", cacheData("StandardCodePrefix"));

    }

    private static final Map<String, String> PROP_CACHE_MAP = new HashMap<>();

    /**
     * 获取属性
     *
     * @param keyName 系统参数
     * @return return
     */
    private String cacheData(String keyName) {
        String val = PROP_CACHE_MAP.get(keyName);
        if (val == null) {
            SysPara sp = sysParaRepository.load(keyName);
            if (sp != null) {
                PROP_CACHE_MAP.put(keyName, sp.getValue());
                return sp.getValue();
            }
            return "";
        }
        return val;
    }

    @Override
    public String uploadCertFile(String certFileOriginFileName, byte[] certFileData, String addBy, String id) {
        HttpClientUploadProcessor httpClientUploadProcessor = null;
        String certFilePath;
        String certUrl;
        try {
            loadPropFromDB();
            certFilePath = doUpload(certFileOriginFileName, certFileData, addBy);
            //诚信评价证书网址
            Organization factory = organizationRepository.load(id);
            String oldCertificateUrl = factory.getAttr3();
            httpClientUploadProcessor = getHttpClientUploadProcessor();
            try {
                certUrl = getReturnQYCXZSUrl("诚信证书", certFilePath, httpClientUploadProcessor);
            } catch (Throwable e) {
                logger.error("诚信评价证书上传失败", e);
                return "-1";
            }
            //诚信评价证书地址 上传到工信部软件促进中心
            sysProperties.setProperty("CERTIFICATEURL", certUrl);
            factory.setAttr3(certUrl);
            factory.setEditTime(new Date());
            factory.setEditBy(addBy);

            //诚信评价证书网址 上传到工信部一所
            SysPara certificateUrl4one = sysParaRepository.load("CertificateUrl4one");
            httpClientUploadProcessor = Utils.getHttpClientUploadProcessor(sysProperties);
            Map<String, Object> certFileResultMap ;
            try {
                Map<String, Object> inputMap = new HashMap<>(16);
                inputMap.put("file", new File(getUploadDir() + certFilePath));
                inputMap.put("handleId", CommUtils.getValueFromProp(sysProperties, "DataRegister_Config_HandleId"));
                inputMap.put("index", CommUtils.getValueFromProp(sysProperties, "DataRegister_Config_Index"));
                inputMap.put("handleRepeatDeal", CommUtils.getValueFromProp(sysProperties, "DataRegister_Config_HandleRepeatDeal"));
                certFileResultMap = httpClientUploadProcessor.uploadPDF(inputMap, CommUtils.getValueFromProp(sysProperties, "DataRegister_Config_FileUploadUrl"));
            } catch (Throwable e) {
                logger.error("诚信评价证书上传失败", e);
                factory.setAttr3(oldCertificateUrl);
                sysProperties.setProperty("CERTIFICATEURL", oldCertificateUrl);
                return "-1";
            }
            String type = certFileResultMap.get("TYPE").toString();
            String message = certFileResultMap.get("MESSAGE").toString();
            String retHandle;
                if (ComUtils.ONE.equals(type)) {
                //成功
                retHandle = certFileResultMap.get("HANDLEID").toString();
            } else {//fail
                logger.error("诚信评价证书上传失败:" + message);
                factory.setAttr3(oldCertificateUrl);
                sysProperties.setProperty("CERTIFICATEURL", oldCertificateUrl);
                return "-1";
            }
            String url = CommUtils.getValueFromProp(sysProperties, "DataRegister_Config_FileSite") + retHandle;
            // 诚信评价证书地址 上传到工信部一所
            sysProperties.setProperty("CERTIFICATEURL4ONE", url);
            certificateUrl4one.setValue(url);
            certificateUrl4one.setEditTime(new Date());
            certificateUrl4one.setEditBy(addBy);

            return factory.getAttr3();

        } catch (Throwable e) {
            logger.error("诚信证书文件上传失败", e);
            throw new RuntimeException("诚信证书文件上传失败--" + e.getMessage());
        } finally {
            if (httpClientUploadProcessor != null) {
                httpClientUploadProcessor.shutdown();
            }
        }
    }

    /**
     * 创建上传对象
     *
     * @return return
     */
    private HttpClientUploadProcessor getHttpClientUploadProcessor() {
        HttpClientUploadProcessor httpClientUploadProcessor;
        httpClientUploadProcessor = new HttpClientUploadProcessor(sysProperties, true,
                "kzsclient",
                CommUtils.getValueFromProp(sysProperties, "NEWSSLKEYSTOREPASSWORD"),
                CommUtils.getValueFromProp(sysProperties, "SSLKEYSTORETYPE"),
                "tzsclient",
                CommUtils.getValueFromProp(sysProperties, "NEWSSLTRUSTSTOREPASSWORD"),
                CommUtils.getValueFromProp(sysProperties, "SSLTRUSTSTORETYPE"), CommUtils.getValueFromProp(sysProperties, "NewDataRegister_Config_FileUploadUrl"));
        return httpClientUploadProcessor;
    }

    /**
     * 诚信证书
     * 上传文件到工信部 返回完整URL
     *
     * @param fileTypeName  文件类型
     * @param relativePath  文件路径
     * @param httpClientUploadProcessor 上下文
     * @return return
     */
    private String getReturnQYCXZSUrl(String fileTypeName, String relativePath, HttpClientUploadProcessor httpClientUploadProcessor) {
        Map<String, Object> certFileResultMap ;
        String type, message, url;
        try {
            //只用于只有一个生产厂家的情况
            certFileResultMap = httpClientUploadProcessor.uploadQYCXZS(new File(getUploadDir() + relativePath), CommUtils.getValueFromProp(sysProperties, "STANDARD_CODE_PREFIX"), CommUtils.getValueFromProp(sysProperties, "PRODUCERNAME"), CommUtils.getValueFromProp(sysProperties, "CERTIFICATENUMBER"));
        } catch (Throwable e) {
            logger.error(fileTypeName + "上传失败:" + e.getMessage());
            throw new RuntimeException(fileTypeName + "上传失败");
        }
        type = certFileResultMap.get("TYPE").toString();
        message = certFileResultMap.get("MESSAGE").toString();

        if (ComUtils.ZERO.equals(type)) {
            //成功
            url = certFileResultMap.get("RESULT").toString();
        } else {//fail
            logger.error(fileTypeName + "上传失败:" + message);
            throw new RuntimeException(fileTypeName + "上传失败");
        }
        return url;
    }

    /**
     * 开始导入
     *
     * @param id id
     */
    @Override
    public void beginImport(String id) {
        //找到上传记录
        MiiUploadRecord importCodeRecord = miiUploadRecordRepository.load(id);
        //修改状态为上传中
        importCodeRecord.setFileUploadStatus(MiiUploadStatus.UPLOADING.index);
        importCodeRecord.setMiiUploadTime(new Date());
        importCodeRecord.setEditTime(new Date());
        miiUploadRecordRepository.save(importCodeRecord);
    }

    @Resource
    private MaterialRepository materialRepository;

    /**
     * 结束导入
     *
     * @param id id
     */
    @Override
    public void onImportError(String id, String errMsg) {
        //找到上传记录
        MiiUploadRecord importCodeRecord = miiUploadRecordRepository.load(id);
        //修改状态为处理中
        importCodeRecord.setFileUploadStatus(MiiUploadStatus.FAIL.index);
        importCodeRecord.setResult(errMsg);
        miiUploadRecordRepository.save(importCodeRecord);
    }

    /**
     * 获取上传工信部文件保存目录
     *
     * @return 上传文件保存目录
     */
    public String getUploadDir() {
        String result = sysProperties.getProperty("DataRegister_Config_FileLocalDir");
        String fileDir;
        if (result != null && StringUtils.isNotEmpty(result)) {
            fileDir = result + "\\";
        } else {
            System.out.println("Working Directory = " +
                    System.getProperty("user.dir"));
            //"c:\\production_code_upload\\";
            fileDir = System.getProperty("user.dir") + "\\mii_upload\\";
        }
        return fileDir;
    }

    /**
     * 获取文件存放目录，包含当前年月日
     *
     * @return 文件目录
     */
    public String getFileDir() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR) + "\\" + (calendar.get(Calendar.MONTH) + 1) + "\\" + ThreadLocalDateUtil.getDateFormat().format(calendar.getTime()) + "\\";
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
        final int buffer = 2048;
        BufferedOutputStream dest;
        // 定义压缩输入流
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(dir + fileName));
        // 得到一个压缩实体
        ZipEntry entry = zipInputStream.getNextEntry();
        if (entry == null) {
            logger.warn("zip file invalid:" + dir + fileName);
            return "";
        }
        unzipedFileName = entry.getName();
        int count;
        byte[]  data= new byte[buffer];
        FileOutputStream fos = new FileOutputStream(dir + entry.getName());
        dest = new BufferedOutputStream(fos, buffer);
        while ((count = zipInputStream.read(data, 0, buffer)) != -1) {
            dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
        zipInputStream.close();
        return unzipedFileName;
    }

    /**
     * 获取上传记录文件的完整路径
     *
     * @param miiUploadRecord 上传记录
     * @return 文件路径
     */
    private String getConformityCertificateFileFullPath(MiiUploadRecord miiUploadRecord) {
        //配置的根目录
        String rootDir = getUploadDir();
        return rootDir + "\\" + miiUploadRecord.getConformityCertificate();
    }

    /**
     * 获取上传记录文件的完整路径
     *
     * @param miiUploadRecord 上传记录
     * @return 文件路径
     */
    private String getProdInspectionReportFileFullPath(MiiUploadRecord miiUploadRecord) {
        //配置的根目录
        String rootDir = getUploadDir();
        return rootDir + "\\" + miiUploadRecord.getProdInspectionReport();
    }

    /**
     * 主要原料合格证明
     *
     * @param id id
     * @return  return
     */
    @Override
    public File getConformityCertificatetUploadFileByID(String id) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(id);
        String path = getConformityCertificateFileFullPath(miiUploadRecord);
        return new File(path);
    }

    /**
     * 产品检验报告
     *
     * @param id id
     * @return  return
     */
    @Override
    public File getProdInspectionReportUploadFileByID(String id) {
        MiiUploadRecord miiUploadRecord = miiUploadRecordRepository.load(id);
        String path = getProdInspectionReportFileFullPath(miiUploadRecord);
        return new File(path);
    }

    /**
     * 根据批次和materialId查询记录
     *
     * @param batchCode 批号
     * @param materialId 产品
     * @return return
     */
    @Override
    public boolean existed(String batchCode, String materialId) {
        return productionCodeRepository.existed(batchCode, materialId);
    }

    /**
     * 根据批次和materialId查询记录
     *
     * @param batchCode 批号
     * @param prodId 产品
     * @return return
     */
    @Override
    public MiiUploadRecord getOne(String batchCode, String prodId) {
        return miiUploadRecordRepository.getOne(batchCode, prodId);
    }

    /**
     * 根据批次和materialId、status查询记录
     *
     * @param batchCode 批号
     * @param prodId 产品
     * @param status 状态
     * @return return
     */
    public List<MiiUploadRecord> findList(String batchCode, String prodId, int status) {
        return miiUploadRecordRepository.findList(batchCode, prodId, status);
    }

    /**
     * 根据fileUploadStatus查询记录
     *
     * @param fileUploadStatus 上传状态
     * @return  return
     */
    @Override
    public List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus) {
        return miiUploadRecordRepository.findListByFileUploadStatus(fileUploadStatus);
    }

    @Override
    public List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus, int size) {
        return miiUploadRecordRepository.findListByFileUploadStatus(fileUploadStatus, size);
    }

    @Resource(name = "sysProperties")
    private Properties sysProperties;

    @Override
    public void save(MiiUploadRecord mii) {
        miiUploadRecordRepository.save(mii);
    }

    /**
     * 编辑企业信息
     *
     * @param id                         工厂ID
     * @param productionLicenseNumber    生产许可证编号
     * @param productionLicenseEndDate   生产许可证有效期
     * @param productionLicenseBeginDate 生产许可证有效期
     * @param creditCertificateNumber    诚信评价证书编号
     * @param creditCertificateBeginDate 诚信评价证书有效期
     * @param creditCertificateEndDate   诚信评价证书有效期
     * @param name                       生产者名称
     * @param production                 食品生产地
     * @param certFile                   诚信评价证书
     * @return 结果
     */
    @Override
    public APIResult<String> createCompanyForFactory(String id, String productionLicenseNumber, String productionLicenseEndDate,
                                                     String productionLicenseBeginDate, String name, String creditCertificateBeginDate, String creditCertificateEndDate,
                                                     String creditCertificateNumber, String production, MultipartFile certFile) throws Exception {
        APIResult<String> result = new APIResult<>();
        CompanyForFactory companyForFactory = repo.getByFactoryId(id);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String creditCertificateUrl = doUpload(certFile.getOriginalFilename(), certFile.getBytes(), null);
        Organization factory = organizationRepository.load(id);
        if (factory != null) {
            factory.setAttr4(creditCertificateUrl);
            organizationRepository.save(factory);
        }
        if (companyForFactory == null) {
            companyForFactory = new CompanyForFactory();
            companyForFactory.setFactoryId(id);
            companyForFactory.setProductionLicenseNumber(productionLicenseNumber);
            companyForFactory.setProductionLicenseEndDate(format.parse(productionLicenseEndDate));
            companyForFactory.setProductionLicenseBeginDate(format.parse(productionLicenseBeginDate));
            companyForFactory.setName(name);
            companyForFactory.setCreditCertificateBeginDate(format.parse(creditCertificateBeginDate));
            companyForFactory.setCreditCertificateEndDate(format.parse(creditCertificateEndDate));
            companyForFactory.setCreditCertificateNumber(creditCertificateNumber);
            companyForFactory.setAttr1(production);
            repo.insert(companyForFactory);
        } else {
            companyForFactory.setProductionLicenseNumber(productionLicenseNumber);
            companyForFactory.setProductionLicenseEndDate(format.parse(productionLicenseEndDate));
            companyForFactory.setProductionLicenseBeginDate(format.parse(productionLicenseBeginDate));
            companyForFactory.setName(name);
            companyForFactory.setCreditCertificateBeginDate(format.parse(creditCertificateBeginDate));
            companyForFactory.setCreditCertificateEndDate(format.parse(creditCertificateEndDate));
            companyForFactory.setCreditCertificateNumber(creditCertificateNumber);
            companyForFactory.setAttr1(production);
            repo.save(companyForFactory);
        }
        return result.succeed();
    }
}
