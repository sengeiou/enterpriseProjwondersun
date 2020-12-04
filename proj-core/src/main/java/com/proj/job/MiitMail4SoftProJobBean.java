package com.proj.job;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebc.biz.miitprc.Utils;
import com.arlen.ebp.appservice.SysParaAppService;
import com.arlen.ebp.dto.SysParaDTO;
import com.arlen.ebu.job.AbstractJob;
import com.arlen.ebu.service.SystemMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.Properties;

/**向工信部（软促）发送邮件
 *
 */
@Component("MiitMail4SoftProJobBean")
public class MiitMail4SoftProJobBean extends AbstractJob {
    Logger logger = LoggerFactory.getLogger(MiitMail4SoftProJobBean.class);
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private SysParaAppService sysParaAppService;

    @Resource
    private SystemMailService systemMailService;
    @Override
    synchronized protected void executeBusiness(String jobName) {
        logger.info("向工信部（软促）发送邮件定时任务开始");
        try {
            String mailto=sysProperties.getProperty("mail.to",null);
            if(mailto==null ||mailto.length()==0){
                logger.error("向工信部（软促）发送邮件接收地址为空！！");
                return;
            }
            String from=sysProperties.getProperty("mail.from",null);
            if(from==null ||from.length()==0){
                logger.error("向工信部（软促）发送地址为空！！");
                return;
            }
            APIResult<SysParaDTO> sysPara = sysParaAppService.getById("MiitSoftProMailContent");;
            String content=sysPara.getData().getValue();
            if(content==null ||content.length()==0){
                logger.error("向工信部（软促）发送邮件发送内容为空！！");
                return;
            }

            String addresses[]=mailto.split(",");
            String java8dateStr = Utils.getDateString("yyyyMMdd");
            sysPara = sysParaAppService.getById("MiitSoftProMailQty");;
            String miitSoftProMailQty=sysPara.getData().getValue();

            String arr[]=miitSoftProMailQty.split(",");
            if(!java8dateStr.equals(arr[0])){
                logger.error("向工信部（软促）发送邮件数据库中数据不是当天的,统计结果【日期:"+arr[0]+",品项:"+arr[1]+",单品数:"+arr[2]+"】");
                return;
            }
            from="完达山乳业<"+from+">";
            String mailContent= MessageFormat.format(content,arr[1],arr[2]);
            boolean ret=systemMailService.sendMail(from,addresses,"数据统计",mailContent);
            logger.info("向工信部（软促）发送邮件定时任务发送结果-----》"+(ret?"成功":"失败"));
        }catch (Exception e){
            logger.error("向工信部（软促）发送邮件定时任务出错："+e);
        }
        logger.info("向工信部（软促）发送邮件定时任务结束");
    }
}