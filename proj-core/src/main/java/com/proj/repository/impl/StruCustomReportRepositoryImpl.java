package com.proj.repository.impl;

import com.arlen.eaf.core.dto.ConditionItem;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.enums.ConditionOperator;
import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.repository.StruCustomReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nark on 2015/12/29.
 */

@Repository
public class StruCustomReportRepositoryImpl extends BaseHibernateRepository implements StruCustomReportRepository {

    @Override
    public Page<HashMap<String, String>> getPageList(PageReq pageReq,String userId, String dataSourceScript) {
        HashMap<String, Object> kvList = new HashMap();
        buildSql(kvList, pageReq.getConditionItems(), "");
        String sql = this.ApplyTemplate(dataSourceScript, pageReq.getConditionItems()).replace("\r\n", "  ");
        kvList.put("userId",userId);
        Page<HashMap<String, String>> page = this.findResultMapsSql(sql, kvList, new PageRequest(pageReq.getPage() - 1, pageReq.getRows()));
        return page;
    }

    private String ApplyTemplate(String tmpl, List<ConditionItem> conditions) {
        String result = tmpl;
        Pattern conditionSentence = Pattern.compile("\\{\\{#.*?#\\}\\}");
        Pattern field = Pattern.compile("\\{\\{:.*?:\\}\\}");
        Matcher matcher = conditionSentence.matcher(tmpl);
        while (matcher.find()) {
            String originSentence = matcher.group().substring("{{#".length(), matcher.group().length() - "#}}".length()).trim();
            Matcher fieldMatcher = field.matcher(originSentence);
            ConditionItem item = null;
            String fieldStr;
            boolean isSentenceNeeded = false;
            if (fieldMatcher.find()) {
                fieldStr = fieldMatcher.group();
                String propName = fieldStr.substring("{{:".length(), fieldStr.length() - ":}}".length()).trim();
                item = conditions.stream().filter(x -> x.getField().equals(propName)).findFirst().orElse(null);
                if (item != null) {
                    isSentenceNeeded = true;
                }
            }
            if (!isSentenceNeeded) {
                result = result.replace(matcher.group(), " ");
            } else {
                if(item.getOperator()== ConditionOperator.IS_NULL||item.getOperator()==ConditionOperator.IS_NOT_NULL){
                    String sentence = originSentence.replace(fieldMatcher.group(), item.getOperator().getOperator());
                    result = result.replace(matcher.group(), sentence);
                }else{
                    String sentence = originSentence.replace(fieldMatcher.group(), ":" + item.getField());
                    result = result.replace(matcher.group(), sentence);
                }
            }
            matcher = conditionSentence.matcher(result);
        }
        return result;
    }

}
