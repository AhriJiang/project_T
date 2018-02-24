package com.juntao.commons.po;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.LoggerFactory;

import com.juntao.commons.consts.ToStringObject;

public abstract class ASolrDocDto extends ToStringObject {
	private static final Logger log = LoggerFactory.getLogger(ASolrDocDto.class);

	abstract public String uniqueKey();

	public final SolrInputDocument convert() {
		SolrInputDocument solrInputDocument = new SolrInputDocument();
		try {
			PropertyUtils.describe(this).entrySet().stream().filter(entry -> !Class.class.getSimpleName().toLowerCase().equals(entry.getKey()))
					.forEach(entry -> solrInputDocument.addField(entry.getKey(), entry.getValue()));
		} catch (Exception e) {
			log.error("PropertyUtils describe error!! this= " + this, e);
			return null;
		}

		return solrInputDocument;
	}
}
