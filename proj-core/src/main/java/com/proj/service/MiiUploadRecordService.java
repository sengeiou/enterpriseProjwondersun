package com.proj.service;

import com.arlen.eaf.core.dto.APIResult;
import com.proj.entity.MiiUploadRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * 上传到工信部文件记录服务
 */
public interface MiiUploadRecordService {

    void beginUpload(String id);

    void finishUpload(MiiUploadRecord miiUploadRecordResult);

    /**
     * 上传质检报告和合格证
     *
     * @param prodId
     * @param batchCode
     * @param prodInspectionReportOriginFileName
     * @param prodInspectionReportFileData
     * @param conformityCertificateOriginFileName
     * @param conformityCertificateFileData
     * @param addBy
     * @return
     */
    MiiUploadRecord upload(String prodId, String batchCode, String prodInspectionReportOriginFileName, byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy);


    String doUpload(String originFileName, byte[] fileData, String addBy);

    /**
     * 开始导入
     *
     * @param id
     */
    void beginImport(String id);

    /**
     * 进行导入
     * @param id
     */
    //APIResult<Object> doImport(String id);

    /**
     * 导入异常
     *
     * @param id
     */
    void onImportError(String id, String errMsg);

    /**
     * 获取可导入的上传记录
     * @param size 要获取的最大记录数
     * @return
     */
    // List<MiiUploadRecord> getImportableList(int size);

    /**
     * 得到上传文件
     *
     * @param id
     * @return
     */
    File getConformityCertificatetUploadFileByID(String id);

    /**
     * 得到上传文件
     *
     * @param id
     * @return
     */
    File getProdInspectionReportUploadFileByID(String id);

    APIResult<String> delete(String id);

    APIResult<String> reupload(String id);

    APIResult<String> checkExist(String id);

    /**
     * 上传诚信证书
     *
     * @param certFileOriginFileName
     * @param certFileData
     * @param addBy
     * @return String
     */
    String uploadCertFile(String certFileOriginFileName, byte[] certFileData, String addBy, String id);

    /**
     * 根据批次和materialId查询记录
     *
     * @param batchCode
     * @param materialId
     * @return
     */
    boolean existed(String batchCode, String materialId);

    /**
     * 根据批次和materialId查询记录
     *
     * @param batchCode
     * @param prodId
     * @return
     */
    MiiUploadRecord getOne(String batchCode, String prodId);

    /**
     * 根据fileUploadStatus查询记录
     *
     * @param fileUploadStatus
     * @return
     */
    List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus);

    List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus, int size);

    void save(MiiUploadRecord miiUploadRecord);

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
     * @throws Exception Exception
     */
    APIResult<String> createCompanyForFactory(String id, String productionLicenseNumber, String productionLicenseEndDate, String productionLicenseBeginDate,
                                              String name, String creditCertificateBeginDate, String creditCertificateEndDate, String creditCertificateNumber, String production,
                                              MultipartFile certFile) throws Exception;
}
