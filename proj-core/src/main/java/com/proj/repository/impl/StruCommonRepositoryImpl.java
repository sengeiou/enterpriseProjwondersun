package com.proj.repository.impl;

import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebp.entity.SysStructure;
import com.proj.repository.StruCommonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2016/8/25.
 */
@Repository
public class StruCommonRepositoryImpl extends BaseHibernateRepository implements StruCommonRepository {

    /**
     * 分页查询
     *
     * @param pageReq   分页查询条件
     * @param scriptStr sql
     * @return 分页结果
     */
    @Override
    public Page<Map> getPageList(PageReq pageReq, String userId, String scriptStr) {
        Pageable pageable = new PageRequest(pageReq.getPage() - 1, pageReq.getRows());
        HashMap<String, Object> kvList = new HashMap<>();
        String hql = scriptStr.replace("\r\n", " ").replace("\n", " ");
        hql = buildSql(kvList, pageReq.getConditionItems(), hql);
//        if(checkCityIdList!=null && checkCityIdList.size()>0){
//            kvList.put("checkCityIds", checkCityIdList);
//        }else{
//            checkCityIdList = new ArrayList<>();
//            checkCityIdList.add("@#$@_(*&^%");
            kvList.put("userId",userId);
//        }
        return this.findResultMapsSql(hql, kvList, pageable);
    }

    /**
     * 根据ID查询数据
     *
     * @param id        数据ID
     * @param scriptStr sql
     * @return 数据
     */
    @Override
    public Map getMapById(String id, List<String> checkCityIdList, String scriptStr) {
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("id", id);
        if(checkCityIdList!=null && checkCityIdList.size()>0){
            String[] checkCityIds = (String[])checkCityIdList.toArray( (new String[checkCityIdList.size()]));
            kvList.put("checkCityIds", checkCityIds);
        }else{
            kvList.put("checkCityIds","abcdefgck99199999939");
        }
        List<Map> list = this.findAllResutlMapsBySql(scriptStr, kvList);
        return list.stream().findFirst().orElse(null);
    }

    /**
     * 查询集合
     *
     * @param paramMap  查询条件参数
     * @param scriptStr sql语句
     * @return 查询结果集合
     */
    @Override
    public List<Map> getPageListByQueryMap(Map<String, String> paramMap, String scriptStr) {
        String hql = scriptStr.replace("\r\n", " ").replace("\n", " ");
        return this.findAllResutlMapsBySql(hql, paramMap);
    }

    @Override
    public List<SysStructureDTO> getChildStructures(List<SysStructure> struList) {
        String idsSqlStr = parseStruToSqlStr(struList);
        if(idsSqlStr==null){
            return null;
        }
        String sql = "select Id,ParentId,Name  from sysStructure where ParentId in";
        if(StringUtil.isNotEmpty(idsSqlStr)){
            sql+=idsSqlStr;
        }
        HashMap<String, Object> kvList = new HashMap<>();
        return this.findAllResutlBeansBySql(sql,kvList,SysStructureDTO.class);
    }


    private String parseStruToSqlStr(List<SysStructure> struList){
        if(struList==null || struList.size()==0){
            return null;
        }else{
            String idsSqlStr = null;
            for (SysStructure org:struList
                    ) {
                if(idsSqlStr==null){
                    idsSqlStr = "("+"'"+org.getId()+"'";
                }else{
                    idsSqlStr = idsSqlStr +","+"'"+org.getId()+"'";
                }
            }
            idsSqlStr+= ")";
            return idsSqlStr;
        }
    }
}
