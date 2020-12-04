package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebc.entity.ProductQcRecord;
import com.arlen.ebp.dto.SysUserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Created by arlenChen on 2017/12/25.
 * 质检单
 *
 * @author arlenChen
 */
public interface ProjProductQcRecordAppService {
    /**
     * 校验批次是否生产该产品
     *
     * @param batchCode  批次
     * @param materialId 产品
     * @return return
     */
    Map<String, Object> existed(String batchCode, String materialId);

    /**
     * 主要原料来源地下拉
     *
     * @return 下拉
     */
    List<Map<String, String>> getComboBox();

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
     * @param productStandard                     产品标准文本字段
     * @param productStandardPdfFileData          产品标准上传PDF文件
     * @return return
     */
    ProductQcRecord upload(String prodId, String batchCode, String prodInspectionReportOriginFileName,
                           byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy, String materialSource,String productStandard,String productStandardPdfFileName ,byte[] productStandardPdfFileData);
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
    APIResult uploadNoRecord(String prodId, String batchCode, String prodInspectionReportOriginFileName,
                             byte[] prodInspectionReportFileData, String conformityCertificateOriginFileName, byte[] conformityCertificateFileData, String addBy, String materialSource,String productStandard,String productStandardPdfFileName ,byte[] productStandardPdfFileData);

    /**
     * 重新上传产品标准
     * @param id
     * @param productStandard
     * @param productStandardPdf
     * @param userDTO
     * @return
     */
    APIResult<Object> reUploadProductStandard(String id, String productStandard, MultipartFile productStandardPdf, SysUserDTO userDTO) throws  Exception;

}