package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;

@Deprecated
public class SXmlUtil {
	private static final Logger log = LoggerFactory.getLogger(SXmlUtil.class);

	public static final Pair<String, ArrayList<Pair<String, ? extends Serializable>>> xml2Obj(String xml) {
		if (StringUtils.isBlank(xml)) {
			return null;
		}

		Element root = null;
		try {
			root = DocumentHelper.parseText(xml).getRootElement();
		} catch (Exception e) {
			log.error("parse xml string error!!", e);
			return null;
		}

		Pair<String, ArrayList<Pair<String, ? extends Serializable>>> result = Pair.of(root.getName(), new ArrayList<>());
		root.elements().forEach(child -> readElement(child, result));
		return result;
	}

	private static final void readElement(final Element parentElement, final Pair<String, ArrayList<Pair<String, ? extends Serializable>>> grandParentPair) {
		String parentName = parentElement.getName().trim().toLowerCase();
		if (parentElement.isTextOnly()) {
			grandParentPair.getRight().add(Pair.of(parentName, parentElement.getText()));
		} else {
			Pair<String, ArrayList<Pair<String, ? extends Serializable>>> parentPair = Pair.of(parentName, new ArrayList<>());
			((ArrayList<Pair<String, ? extends Serializable>>) grandParentPair.getRight()).add(parentPair);
			parentElement.elements().forEach(child -> readElement(child, parentPair));
		}
	}

	public static final String obj2Xml(Pair<String, ArrayList<Pair<String, ? extends Serializable>>> rootPair) {
		if (null == rootPair) {
			return null;
		}

		Document document = DocumentHelper.createDocument();
		Element root = document.addElement(rootPair.getLeft());
		rootPair.getRight().forEach(childPair -> writeElement(childPair.getLeft(), childPair.getRight(), root));

		return document.asXML();
	}

	private static final void writeElement(final String parentName, final Serializable parentValue, final Element grandParentElement) {
		Element parentElement = grandParentElement.addElement(parentName);
		if (!(parentValue instanceof ArrayList)) {
			parentElement.setText(parentValue.toString());
		} else {
			((ArrayList<Pair<String, ? extends Serializable>>) parentValue)
					.forEach(childPair -> writeElement(childPair.getLeft(), childPair.getRight(), parentElement));
		}
	}

	public static void main(String[] args) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<rt>                            " + "    <xyj>                "
				+ "        <zz>作者1</zz>          " + "        <zz><aa>hehehh</aa><bb>中文</bb></zz>          " + "        <cd>明</cd>             "
				+ "    </xyj>                         " + "    <hlm>                " + "        <zz>曹雪芹</zz>           "
				+ "    </hlm>                         " + "</rt>                           ";
		Pair<String, ArrayList<Pair<String, ? extends Serializable>>> p = xml2Obj(xml);
		System.out.println("mmm     " + p);

		String s = obj2Xml(p);
		System.out.println("sss     " + s);
	}

}
