package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.juntao.commons.po.ASolrDocDto;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SSolrUtil {
    private static final Logger log = LoggerFactory.getLogger(SSolrUtil.class);
/*
//    public static final String CORE_GOODS = "goods";
    public static final String CORE_PRD_STR = "prdStr";

    //	private static final SolrServer solrServerForRead = new HttpSolrServer("http://solr.read.piao.ip:8080/solr/goods");
    private static final Map<String, SolrServer> core_solrServerForRead = SCollectionUtil.toMap(
//            CORE_GOODS, new HttpSolrServer("http://solr.write.piao.ip:8080/solr/" + CORE_GOODS),
            CORE_PRD_STR, new HttpSolrServer("http://solr.write.piao.ip:8080/solr/" + CORE_PRD_STR)
    );
    private static final Map<String, SolrServer> core_solrServerForWrite = SCollectionUtil.toMap(
//            CORE_GOODS, new ConcurrentUpdateSolrServer("http://solr.write.piao.ip:8080/solr/" + CORE_GOODS, 200, 10),
            CORE_PRD_STR, new ConcurrentUpdateSolrServer("http://solr.write.piao.ip:8080/solr/" + CORE_PRD_STR, 200, 10)
    );*/

    public static final SolrDocumentList queryDocumentByQueryStatement(String queryStatement, SolrServer solrServerForRead) {
        if (StringUtils.isBlank(queryStatement)) {
            return null;
        }

        QueryResponse queryResponse = null;
        try {
            queryResponse = queryDocument(new SolrQuery().setQuery(queryStatement).setRows(1000), solrServerForRead);
        } catch (Exception e) {
            log.error("getDocumentByUrl query error!! queryStatement= " + queryStatement, e);
        }

        if (null != queryResponse) {
            SolrDocumentList solrDocumentList = queryResponse.getResults();
            if (!CollectionUtils.isEmpty(solrDocumentList)) {
                return solrDocumentList;
            }
        }

        return null;
    }

    public static final QueryResponse queryDocument(SolrQuery solrQuery, SolrServer solrServerForRead) {
        if (null == solrQuery || null == solrServerForRead) {
            return null;
        }

        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServerForRead.query(solrQuery);
        } catch (Exception e) {
            log.error("queryDocument query error!! ", e);
        }
        return queryResponse;
    }

    public static final NamedList<Object> batchAddDocument(List<SolrInputDocument> solrInputDocumentList, SolrServer solrServerForWrite) {
        if (CollectionUtils.isEmpty(solrInputDocumentList) || null == solrServerForWrite) {
            return null;
        }

        UpdateResponse updateResponse = null;
        try {
            updateResponse = solrServerForWrite.add(solrInputDocumentList);
            updateResponse = solrServerForWrite.commit();
        } catch (Exception e) {
            log.error("batchAddDocument error!! solrInputDocumentList= " + solrInputDocumentList, e);
        }
        return Optional.ofNullable(updateResponse).map(UpdateResponse::getResponse).orElse(null);
    }

    public static final NamedList<Object> batchAddDto(List<? extends ASolrDocDto> solrDocDtoList, SolrServer solrServerForWrite) {
        if (CollectionUtils.isEmpty(solrDocDtoList) || null == solrServerForWrite) {
            return null;
        }

        UpdateResponse updateResponse = null;
        try {
            updateResponse = solrServerForWrite
                    .add(solrDocDtoList.stream().filter(solrDocDto -> null != solrDocDto && StringUtils.isNotBlank(solrDocDto.uniqueKey()))
                            .map(ASolrDocDto::convert).filter(solrInputDocument -> null != solrInputDocument).collect(Collectors.toList()));
            updateResponse = solrServerForWrite.commit();
        } catch (Exception e) {
            log.error("batchAddDocument error!! solrDocDtoList= " + solrDocDtoList, e);
        }
        return Optional.ofNullable(updateResponse).map(UpdateResponse::getResponse).orElse(null);
    }

    public static final NamedList<Object> deleteIndexByQuery(String queryStatement, SolrServer solrServerForWrite) {
        if (StringUtils.isBlank(queryStatement) || null == solrServerForWrite) {
            return null;
        }

        UpdateResponse updateResponse = null;
        try {
            updateResponse = solrServerForWrite.deleteByQuery(queryStatement);
            updateResponse = solrServerForWrite.commit();
        } catch (Exception e) {
            log.error("deleteIndexByQuery error!", e);
        }
        return Optional.ofNullable(updateResponse).map(UpdateResponse::getResponse).orElse(null);
    }

    public static final String formatDate(Date date) {
        return null == date ? StringUtils.EMPTY : DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss").replaceFirst(StringUtils.SPACE, "T") + "Z";
    }
}
