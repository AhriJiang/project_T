package com.juntao.commons.util;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.*;
import org.apache.commons.io.FileUtils;

import com.juntao.commons.consts.SConsts;

import java.io.File;

public class SFreemarkerUtil {
	private static final String MEANINGLESS = "MEANINGLESS";

	public static final Template newTemplate(String ftlFileName, String templateName) throws Exception {
		String sourceCodeFtl = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource("").getPath()
				+ "/ftl/" + ftlFileName));

		StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
		stringTemplateLoader.putTemplate(templateName, sourceCodeFtl);

		DefaultObjectWrapperBuilder defaultObjectWrapperBuilder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
		defaultObjectWrapperBuilder.setForceLegacyNonListCollections(false);
		defaultObjectWrapperBuilder.setDefaultDateType(TemplateDateModel.DATETIME);

		Configuration configuration = new Configuration(freemarker.template.Configuration.VERSION_2_3_25);
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		configuration.setDefaultEncoding(SConsts.UTF_8);
		configuration.setObjectWrapper(defaultObjectWrapperBuilder.build());
		configuration.setTemplateLoader(stringTemplateLoader);
		configuration.setNumberFormat(SConsts.POUND_SIGN);

		Template template = configuration.getTemplate(templateName);
		if (null == template) {
			System.out.println(">>>>>>>>>>>>>>null template");
			System.exit(-1);
		}

		return template;
	}
}
