package com.juntao.commons.autocoder;

import freemarker.template.Template;
import net.sf.cglib.beans.BeanGenerator;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.CollectionUtils;

import com.juntao.commons.util.SCollectionUtil;
import com.juntao.commons.util.SFreemarkerUtil;

import java.io.File;
import java.io.ObjectStreamClass;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoCodeWriter2 {
	private static final String COMMON_PACKAGE = "com.cmbchina.o2o.funmall.rg.commons.";
	private static final String SCHEMA = "RG";
	private static final boolean SCHEMA_IN_CODE = false;
	private static final String TABLE_FIX = "";
	private static final String COLUMN_FIX = "";

	private static String BASE_PACKAGE = "com.cmbchina.o2o.funmall.rg.";  // TODO 请自己设置本机idea的启动参数，不要手动改这一行
	private static String SRC_DIR = "\\src\\main\\java";
	private static String RESOURCES_DIR = "\\src\\main\\resources";

	private static final Set<String> duplicateIngoreColumns = Stream.of("creator", "created").collect(Collectors.toSet());

	private static void init(String projectDir, boolean shouldClearAll) {
		SRC_DIR = projectDir + SRC_DIR;
		RESOURCES_DIR = projectDir + RESOURCES_DIR;

		for (String s : BASE_PACKAGE.split("\\.")) {
			if (StringUtils.isNotBlank(s)) {
				SRC_DIR = SRC_DIR + File.separatorChar + s;
				RESOURCES_DIR = RESOURCES_DIR + File.separatorChar + s;
			}
		}

		// try {
		// if (shouldClearAll) {
		// FileUtils.deleteDirectory(new File(SRC_DIR));
		// }
		//
		// FileUtils.copyDirectory(new File(".\\src\\major\\frameclass"), new File(SRC_DIR + "\\frameclass"));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	// private static final String DB_URL =
	// "jdbc:mysql://mysql.piao.ip:8066/CMBCHINA?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
//    private static final String DB_URL = "jdbc:mysql://mysql.galemall.ip:8066/" + SCHEMA
//	private static final String DB_URL = "jdbc:mysql://99.48.210.175:8066/" + SCHEMA
	//private static final String DB_URL = "jdbc:mysql://172.30.11.175:8066/" + SCHEMA
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:48066/" + SCHEMA
			+ "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
	private static final String DB_USER = "rw_usr";
	private static final String DB_PASSWORD = "rw_usr*2017_test";

	static class ColumnTypeQuerier implements ResultSetHandler<Triple<String, Boolean, Map<String, Pair<String, String>>>> {

		@Override
		public Triple<String, Boolean, Map<String, Pair<String, String>>> handle(ResultSet rs) throws SQLException {
			String idClassSimpleName = null;
			boolean isAutoIncrementId = false;
			Map<String, Pair<String, String>> column_type_comment = new LinkedHashMap<String, Pair<String, String>>();
			while (rs.next()) {
				String column = rs.getString(1).toLowerCase();
				String type = rs.getString(2).toLowerCase();
				String key = rs.getString(5);
				String extra = rs.getString(7);
				String comment = StringUtils.replace(StringUtils.replace(rs.getString(9), "\r\n", ""), "\n", "");
				if (StringUtils.isNotBlank(key) && StringUtils.containsIgnoreCase(key, "PRI")) {
					if (null != idClassSimpleName) {
						System.out.println("error error error error error error, only single primary key allowed!!");
						return null;
					}

					if (StringUtils.containsIgnoreCase(extra, "auto_increment")) {
						isAutoIncrementId = true;
					}

					if (StringUtils.containsIgnoreCase(type, "char")) {
						idClassSimpleName = "String";
					} else if (StringUtils.containsIgnoreCase(type, "int")) {
						if (StringUtils.containsIgnoreCase(type, "big")) {
							idClassSimpleName = "Long";
						} else {
							idClassSimpleName = "Integer";
						}
					}
				}

				column_type_comment.put(column, Pair.of(type, comment));
				System.out.println(column + " " + type);
			}

			return Triple.of(idClassSimpleName, isAutoIncrementId, column_type_comment);
		}
	}

	private static final String convertName(String src, String toBeRemoved) {
		if (StringUtils.isNotBlank(toBeRemoved)) {
			src = StringUtils.removeStart(src, toBeRemoved);
		}

		StringBuilder sb = new StringBuilder();
		for (String s : src.split("_")) {
			if (0 < sb.length()) {
				s = StringUtils.capitalize(s);
			}
			sb.append(s);
		}

		return sb.toString();
	}

	private static final Dto resolveFieldClass(
			String table,
			Triple<String, Boolean, Map<String, Pair<String, String>>> idClassSimpleName_isAutoIncrId_column_type_comment) {
		String idClassSimpleName = idClassSimpleName_isAutoIncrId_column_type_comment.getLeft();
		String isAutoIncrId = idClassSimpleName_isAutoIncrId_column_type_comment.getMiddle() ? "1" : null;
		Map<String, String> column_comment = new HashMap<>();
		List<Triple<String, String, Class>> column_field_class = new ArrayList<Triple<String, String, Class>>();
		for (Map.Entry<String, Pair<String, String>> entry : idClassSimpleName_isAutoIncrId_column_type_comment.getRight().entrySet()) {
			String column = entry.getKey();
			String type = entry.getValue().getLeft();
			String comment = entry.getValue().getRight();

			column_comment.put(column, comment);

			String field = convertName(column, COLUMN_FIX);
			Class clazz = null;
			if (StringUtils.contains(type, "int")) {
				if (StringUtils.containsIgnoreCase(type, "big")) {
					clazz = Long.class;
				} else {
					clazz = Integer.class;
				}
			} else if (StringUtils.contains(type, "char") || StringUtils.contains(type, "text")) {
				clazz = String.class;
			} else if (StringUtils.contains(type, "time") || StringUtils.contains(type, "date")) {
				clazz = Date.class;
			} else if (StringUtils.contains(type, "float") || StringUtils.contains(type, "double") || StringUtils.contains(type, "decimal")) {
				clazz = BigDecimal.class;
			} else {
				System.out.println("null clazz!!! table= " + table + ",  column= " + column + ",  type= " + type);
				System.exit(-1);
			}
			column_field_class.add(Triple.of(column, field, clazz));
			System.out.println(field + " " + clazz.getSimpleName());
		}

		Dto dto = new Dto();
		dto.setTable(table);
		dto.setPo(StringUtils.capitalize(convertName(table, TABLE_FIX)));
		dto.setIdClassSimpleName(idClassSimpleName);
		dto.setIsAutoIncrId(isAutoIncrId);
		dto.setColumn_comment(column_comment);
		dto.setColumn_field_class(column_field_class);
		dto.setIsAutoIncrId(isAutoIncrId);
		dto.setBasePackage(BASE_PACKAGE);
		dto.setCommonPackage(COMMON_PACKAGE);
		dto.setSchemaPackage(SCHEMA_IN_CODE ? SCHEMA + "." : "");
		dto.setClassNameSet(column_field_class.stream().map(Triple::getRight).map(Class::getSimpleName).collect(Collectors.toSet()));
		dto.setDuplicateUpdateColumns(column_field_class.stream().map(Triple::getLeft)
				.filter(c -> !duplicateIngoreColumns.contains(c)).collect(Collectors.toList()));
		return dto;
	}

	private static final Triple<String, Boolean, Map<String, Pair<String, String>>> queryColumnType(String table) {
		DbUtils.loadDriver(DB_DRIVER);
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

			return new QueryRunner().query(conn, "SHOW FULL COLUMNS FROM " + table, new ColumnTypeQuerier());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

	private static final void saveFile(Dto dto, String fileName, String ftlName) {
		String sub = dto.getSub();
		fileName = dto.getPo() + fileName;
		try {
			File file = new File((fileName.endsWith(".java") ? SRC_DIR : RESOURCES_DIR) + File.separatorChar + sub
					+ File.separatorChar
//					+ SCHEMA + File.separatorChar
					+ fileName);

			dto.setHandWritedCodeLineList(null);
			dto.setPoAnnoImportList(new ArrayList<>());
			dto.setPoClassAnnoList(null);
			dto.setPoField_annoList(new HashMap<>());

			if (file.exists()) {
				List<Integer> handWritedCodeLineStartEnd = new ArrayList<>(2);
				List<String> oldFileLineList = FileUtils.readLines(file);
				for (int i = 0; i < oldFileLineList.size(); i++) {
					if (StringUtils.contains(oldFileLineList.get(i), "##################")) {
						handWritedCodeLineStartEnd.add(i);
					}
				}
				if (handWritedCodeLineStartEnd.size() == 2) {
					dto.setHandWritedCodeLineList(oldFileLineList.subList(handWritedCodeLineStartEnd.get(0), 1 + handWritedCodeLineStartEnd.get(1)));
				}

				if ("po".equalsIgnoreCase(sub)) {
					poAnno(oldFileLineList, dto);
				}
			}

			Template template = SFreemarkerUtil.newTemplate(ftlName + ".ftl", ftlName);

			StringWriter stringWriter = new StringWriter();
			template.process(dto, stringWriter);
			FileUtils.write(file, stringWriter.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final void poAnno(List<String> oldFileLineList, Dto dto) throws Exception {
		List<String> tmpAnnoList = null;
		for (String line : oldFileLineList) {
			if (StringUtils.removeAll(line, "\\s").contains("import" + COMMON_PACKAGE + "annotation.")) {
				dto.getPoAnnoImportList().add(line);
			} else if (line.contains("@")) {
				if (CollectionUtils.isEmpty(tmpAnnoList)) {
					tmpAnnoList = new ArrayList<>();
				}
				tmpAnnoList.add(line);
			} else if (StringUtils.isBlank(line)) {
				if (!CollectionUtils.isEmpty(tmpAnnoList)) {
					tmpAnnoList.add(line);
				}
			} else if (line.contains("class ")) {
				if (!CollectionUtils.isEmpty(tmpAnnoList)) {
					dto.setPoClassAnnoList(tmpAnnoList);
					tmpAnnoList = null;
				}
			} else {
				if (!CollectionUtils.isEmpty(tmpAnnoList)) {
					String tmpStr = StringUtils.stripToEmpty(line.substring(0, line.indexOf(";")));
					String field = StringUtils.stripToEmpty(tmpStr.substring(tmpStr.lastIndexOf(StringUtils.SPACE)));
					dto.getPoField_annoList().put(field, tmpAnnoList);
					tmpAnnoList = null;
				}
			}
		}
	}

	public static final void po(Dto dto) {
		dto.setSub("po");

		BeanGenerator beanGenerator = new BeanGenerator();
		beanGenerator.setSuperclass(BlankPo.class);

		for (Triple<String, String, Class> triple : dto.getColumn_field_class()) {
			beanGenerator.addProperty(triple.getMiddle(), triple.getRight());
		}

		Long serialVersionUID = null;
		try {
			serialVersionUID = ObjectStreamClass.lookup(beanGenerator.create().getClass()).getSerialVersionUID();
			System.out.println(serialVersionUID);
			dto.setSerialVersionUID(String.valueOf(serialVersionUID));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		saveFile(dto, ".java", "po");
	}

	public static final void mapper(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		mapperJava(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
		mapperXml(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
	}

	private static final void mapperJava(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		dto.setSub("mapper");
		saveFile(dto, "Mapper.java", "mapper_java");
	}

	private static final void mapperXml(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		dto.setSub("mapper");
		saveFile(dto, "Mapper.xml", "mapper_xml");
	}

	public static final void dao(Dto dto, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		dto.setSub("dao");
		saveFile(dto, "Dao.java", "dao");
	}

	private static final Map<String, String> idClassSimpleName_str2IdFunc = SCollectionUtil.toMap(
			"Integer", "Integer::valueOf",
			"Long", "Long::valueOf",
			"String", "idStr -> idStr"
	);

	public static final void redisCacheDao(Dto dto) {
		dto.setSub("dao");
		dto.setIdClassSimpleName_str2IdFunc(idClassSimpleName_str2IdFunc);
		saveFile(dto, "Dao.java", "redis_cache_dao");
	}

	public static class Dto {
		private String table;
		private String po;
		private String idClassSimpleName;
		private String isAutoIncrId;
		private Map<String, String> column_comment = new HashMap<>();
		private List<Triple<String, String, Class>> column_field_class;
		private String basePackage;
		private String sub;
		private String commonPackage;
		private String schemaPackage;
		private Set<String> classNameSet;
		private String serialVersionUID;
		private List<String> duplicateUpdateColumns;
		private String needDelete;
		private String needInsertIgnore;
		private String needSelectForUpdate;
		private String needUnion;
		private String isRedisCache;
		private Map<String, String> idClassSimpleName_str2IdFunc;
		private List<String> handWritedCodeLineList;
		private List<String> poAnnoImportList = new ArrayList<>();
		private List<String> poClassAnnoList;
		private Map<String, List<String>> poField_annoList = new HashMap<>();

		public String getTable() {
			return table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public String getPo() {
			return po;
		}

		public void setPo(String po) {
			this.po = po;
		}

		public String getIdClassSimpleName() {
			return idClassSimpleName;
		}

		public void setIdClassSimpleName(String idClassSimpleName) {
			this.idClassSimpleName = idClassSimpleName;
		}

		public String getIsAutoIncrId() {
			return isAutoIncrId;
		}

		public void setIsAutoIncrId(String isAutoIncrId) {
			this.isAutoIncrId = isAutoIncrId;
		}

		public Map<String, String> getColumn_comment() {
			return column_comment;
		}

		public void setColumn_comment(Map<String, String> column_comment) {
			this.column_comment = column_comment;
		}

		public List<Triple<String, String, Class>> getColumn_field_class() {
			return column_field_class;
		}

		public void setColumn_field_class(List<Triple<String, String, Class>> column_field_class) {
			this.column_field_class = column_field_class;
		}

		public String getBasePackage() {
			return basePackage;
		}

		public void setBasePackage(String basePackage) {
			this.basePackage = basePackage;
		}

		public String getSub() {
			return sub;
		}

		public void setSub(String sub) {
			this.sub = sub;
		}

		public String getCommonPackage() {
			return commonPackage;
		}

		public void setCommonPackage(String commonPackage) {
			this.commonPackage = commonPackage;
		}

		public String getSchemaPackage() {
			return schemaPackage;
		}

		public void setSchemaPackage(String schemaPackage) {
			this.schemaPackage = schemaPackage;
		}

		public Set<String> getClassNameSet() {
			return classNameSet;
		}

		public void setClassNameSet(Set<String> classNameSet) {
			this.classNameSet = classNameSet;
		}

		public String getSerialVersionUID() {
			return serialVersionUID;
		}

		public void setSerialVersionUID(String serialVersionUID) {
			this.serialVersionUID = serialVersionUID;
		}

		public List<String> getDuplicateUpdateColumns() {
			return duplicateUpdateColumns;
		}

		public void setDuplicateUpdateColumns(List<String> duplicateUpdateColumns) {
			this.duplicateUpdateColumns = duplicateUpdateColumns;
		}

		public String getNeedDelete() {
			return needDelete;
		}

		public void setNeedDelete(String needDelete) {
			this.needDelete = needDelete;
		}

		public String getNeedInsertIgnore() {
			return needInsertIgnore;
		}

		public void setNeedInsertIgnore(String needInsertIgnore) {
			this.needInsertIgnore = needInsertIgnore;
		}

		public String getNeedSelectForUpdate() {
			return needSelectForUpdate;
		}

		public void setNeedSelectForUpdate(String needSelectForUpdate) {
			this.needSelectForUpdate = needSelectForUpdate;
		}

		public String getNeedUnion() {
			return needUnion;
		}

		public void setNeedUnion(String needUnion) {
			this.needUnion = needUnion;
		}

		public String getIsRedisCache() {
			return isRedisCache;
		}

		public void setIsRedisCache(String isRedisCache) {
			this.isRedisCache = isRedisCache;
		}

		public Map<String, String> getIdClassSimpleName_str2IdFunc() {
			return idClassSimpleName_str2IdFunc;
		}

		public void setIdClassSimpleName_str2IdFunc(Map<String, String> idClassSimpleName_str2IdFunc) {
			this.idClassSimpleName_str2IdFunc = idClassSimpleName_str2IdFunc;
		}

		public List<String> getHandWritedCodeLineList() {
			return handWritedCodeLineList;
		}

		public void setHandWritedCodeLineList(List<String> handWritedCodeLineList) {
			this.handWritedCodeLineList = handWritedCodeLineList;
		}

		public List<String> getPoAnnoImportList() {
			return poAnnoImportList;
		}

		public void setPoAnnoImportList(List<String> poAnnoImportList) {
			this.poAnnoImportList = poAnnoImportList;
		}

		public List<String> getPoClassAnnoList() {
			return poClassAnnoList;
		}

		public void setPoClassAnnoList(List<String> poClassAnnoList) {
			this.poClassAnnoList = poClassAnnoList;
		}

		public Map<String, List<String>> getPoField_annoList() {
			return poField_annoList;
		}

		public void setPoField_annoList(Map<String, List<String>> poField_annoList) {
			this.poField_annoList = poField_annoList;
		}
	}

	public static void main(String[] args) {
		init(args[0], false); // TODO 请自己设置idea的启动参数，不要改这一行

		for (Object[] table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache : new Object[][]{
				// "briefing", "cmm_task", "limited_keyword", "limited_url",
				// "media_industry",
				// "print_media", "sys_group", "users", "watch_channel",
				// "watch_company_note", "watch_geographies",
				// "watch_industries", "watch_list_page", "watch_log",
				// "watch_release_industries", "watch_result",
				// "watch_result_cmm", "watch_result_mobile", "watch_task",
				// "watch_thread", "watch_website",
				// "watch_website_types"

				// new Object[]{"t_mer_merch_info", false, false, false},
				// new Object[]{"t_mer_store_info", false, false, false},
//				 new Object[]{"t_prd_base_book", true, false, false},
//				 new Object[]{"t_prd_bak_book", true, false, false},
				// new Object[]{"t_approve_status_log", false, false, false},
				// new Object[]{"t_delete_flag_log", false, false, false},
				// new Object[]{"t_oms_user", true,true,true},
				// new Object[]{"t_oms_role", false, false, false},
				// new Object[]{"t_mer_merch_approve_info", true, true, false},
				// new Object[]{"t_mer_store_approve_info", true, true, false},
//				 new Object[]{"t_prd_aprv_list", true, true, false},
				// new Object[]{"t_auth_department", false, true, false},
				// new Object[]{"t_mer_reltype_info", true, false, false},
				// new Object[]{"t_mer_merchant_type_info", false, false, false},
				// new Object[]{"t_role_menu", false, false, false},
				// new Object[]{"t_auth_user_info", false, false, false},
				// new Object[]{"t_mer_reltype_approve_info", true, false, false},
				// new Object[]{"t_config", false, false, false},
				// new Object[]{"t_mer_acc_approve_info", false, true, false},
				// new Object[]{"t_mer_acc_info", false, false, false},
				// new Object[]{"t_mer_relacct_his", false, false, false},
				// new Object[]{"t_mer_relclear_approve_info", true, false, false},
				// new Object[]{"t_mer_relclear_info", true, false, false},
				// new Object[]{"t_mer_attach_approve_info", true, false, false},
				// new Object[]{"t_mer_link_bank", false, false, false},
				// new Object[]{"t_mer_relacct_his", false, false, false},
				// new Object[]{"t_bas_city_info", false, false, false},
				// new Object[]{"t_bas_distinct_info", false, false, false},
				// new Object[]{"t_bas_region_info", false, false, false},
				// new Object[]{"t_prd_labe_sale_info", true, false, false},
//				 new Object[]{"t_prd_merch_info", true, false, true},
				// new Object[]{"t_prd_label_info", false, false, false},
				// new Object[]{"t_prd_prod_stock", false, true, false},
				// new Object[]{"t_prd_rush_stock", false, false, false},
				// new Object[]{"t_prd_sold_info", false, false, false},
				// new Object[]{"t_prd_activity", true, false, false},
//				 new Object[]{"t_test", true, true, true},
				// new Object[]{"t_prd_adver_map_info", true, false, false},
				// new Object[]{"t_prd_advertise_info", false, true, true},
				// new Object[]{"t_prd_adver_city", true, false, false},
				// new Object[]{"t_mer_onego_rate_approve_info", false, false, false},
//				 new Object[]{"t_mer_onego_rate_info", false, false, false},
//				 new Object[]{"t_mer_prd_score_rule", false, false, false},
//				 new Object[]{"t_mer_prd_info", false, false, false},
//			new Object[]{"t_mer_netpay_info", false, false, true},
//			new Object[]{"t_vou_voucher_info", false, true, false},
//		 new Object[]{"t_vou_arithmetic", false, false, false},
//			new Object[]{"t_check_point", false, false, false},
//				new Object[]{"t_quartz_log", false, false, false},
//				new Object[]{"t_prd_json_cache_info", false, false, false},
//				new Object[]{"t_quartz_info", false, false, false},
//				new Object[]{"t_update_solr_queue", true, false, false},
//				new Object[]{"t_subsidy_party", false, false, false},
//				new Object[]{"t_chnl_shopwindow_info", false, true, false},
//				new Object[]{"t_chnl_shopwindow_pic_list", true, false, false},
//				new Object[]{"t_chnl_shopwindow_city_list", true, false, false},
//				new Object[]{"t_oms_user_petition", false, true, false},
				//
				//
				//
				// new Object[]{"t_cou_code_info", false, false, false},
				// new Object[]{"t_cou_cop_list", false, false, false},
				// new Object[]{"t_cou_vfc_list", false, false, false},
				// new Object[]{"t_ord_onego_list", false, false, false},
				// new Object[]{"t_ord_order_info", false, false, false},
				// new Object[]{"t_ord_tran_list", false, false, false},
//			new Object[]{"t_vou_code_info", false, false, false},
				//
				//
				//
				// new Object[]{"t_cop_code_org", true, false, false},
//				new Object[]{"t_cop_fill_code", true, false, false},
//				new Object[]{"t_cop_coupon", false, false, false},
//				new Object[]{"t_test_cop", false, false, false},
//				new Object[]{"t_cop_org_coupon", false, false, false},
//				new Object[]{"t_cop_org", false, false, false},
//				new Object[]{"t_cop_code", false, false, false},
				//
				//
				//
//				new Object[]{"t_etl_prd_list", false, false, false},


//				new Object[]{"cogs_expend",false,false,false,false,false},
//				new Object[]{"cogs_ori",false,false,false,false,false},
//				new Object[]{"cogs_recv",false,false,false,false,false},
//				new Object[]{"pay_dtl_cmbmb",true,false,false,false,false},
//				new Object[]{"conf_alias",true,false,false,false,false},
//				new Object[]{"conf_biz_mod",false,false,false,false,true},
//				new Object[]{"conf_province",false,false,false,false,true},
//				new Object[]{"conf_city",false,false,false,false,true},
//				new Object[]{"conf_cmb_branch",false,false,false,false,true},
//				new Object[]{"conf_district",false,false,false,false,true},
//				new Object[]{"conf_chnl",false,false,false,false,true},
//				new Object[]{"conf_chnl_pay_way",false,false,false,false,true},
//				new Object[]{"conf_dict",false,false,false,false,true},
//				new Object[]{"conf_pay_way",false ,false ,false,false,true},
//				new Object[]{"conf_platform",false,false,false,false,true},
//				new Object[]{"conf_platform_reg",false,false,false,false,true},
//				new Object[]{"conf_platform_stl_subject",false,false,false,false,true},
//				new Object[]{"conf_ship_carrier",false,false,false,false,true},
//				new Object[]{"conf_sku_category",false,false,false,false,true},
//				new Object[]{"conf_stl_diff", false, false, false, false, true},
//				new Object[]{"conf_stl_diff_except", false, false, false, false, true},
//				new Object[]{"conf_stl_diff_relation", false, false, false, false, true},
//				new Object[]{"conf_stl_except", false, false, false, false, true},
//				new Object[]{"conf_stl_opt", false, false, false, false, true},
//				new Object[]{"config",false ,false ,false,false,true},

//				new Object[]{"cust_address",true,false,false,false,false},
//				new Object[]{"cust_cart",true ,false ,false,false,false},
//				new Object[]{"cust_cart_his",false,false,false,false,false},

//				new Object[]{"cust_info",false,false,false,true,false},
//				new Object[]{"inv_sku_chk", true, true, false, false, false},
//              new Object[]{"inv_change_petition",false,false,false,false,false},
//				new Object[]{"inv_log",false,false,false,false,false},
//				new Object[]{"inv_log_queue",true ,false ,false,false,false},
//				new Object[]{"inv_remaining",false,true,false,false,false},
//				new Object[]{"inv_sold",false ,false ,false,false,false},
//				new Object[]{"inv_sold_cust_limit",false,false,false,false,false},
//				new Object[]{"inv_sold_platform_monthly",false,false,false,false,false},
//				new Object[]{"inv_sold_repo_monthly",false,false,false,false,false},
//				new Object[]{"oms_controller_role",true ,false ,false,true,false},
//				new Object[]{"oms_div_role",true,false,false,true,true},
//				new Object[]{"oms_op_log",false ,false ,false,false,false},
//				new Object[]{"oms_role",false ,false ,false,false,true},
//				new Object[]{"oms_req_param_role_val",false ,false ,false,false,true},
//				new Object[]{"oms_usr",false,false,false,false,false},
//				new Object[]{"oms_usr_role",true,false,false,true,false},
//				new Object[]{"ord_invoice",true ,false ,false,false,false},
//				new Object[]{"ord_item",true,false,false,false,false},
//				new Object[]{"ord_master",true,false,false,false,false},
//				new Object[]{"ord_op_log",true,false,false,false,false},
//				new Object[]{"ord_vendor",true,false,false,false,false},
//				new Object[]{"ord_vou",true ,false ,false,false,false},
//				new Object[]{"rma_op_log",true,false,false,false,false},
//				new Object[]{"rma_ord",true ,false ,false,false,false},
//				new Object[]{"rma_pic",true ,false ,false,false,false},
//				new Object[]{"rma_subtotal",true,false,false,false,false},
//				new Object[]{"opt_pos",false,false,false,false,false},
//				new Object[]{"opt_sku_category",false,false,false,true,false},
//				new Object[]{"platform_sku",true,false,false,false,false},
//				new Object[]{"platform_sku_chk",true,false,false,false,false},
//				new Object[]{"platform_sku_chk_log",false ,false ,false,false,false},
//				new Object[]{"platform_sku_quota",true,false,false,false,false},
//				new Object[]{"platform_sku_quota_log",false ,false ,false,false,false},
//				new Object[]{"repo_price",true,false,false,true,false},
//				new Object[]{"repo_price_chk",true,false,false,false,false},
//                new Object[]{"repo_price_chk_log",false ,false ,false,false,false},
//				new Object[]{"repo_sku",true,false,false,false,false},
//				new Object[]{"repo_sku_chk",true,false,false,false,false},
//                new Object[]{"repo_sku_quota",true,false,false,false,false},
//                new Object[]{"repo_sku_quota_log",true,false,false,false,false},
//				new Object[]{"shlf_sku",true,false,false,true,false},
//				new Object[]{"shlf_info",false,false,false,false,false},
//				new Object[]{"sku_grp_category_chk",true,false,false,false,false},
//				new Object[]{"sku_grp_desc_chk",true,false,false,false,false},
//				new Object[]{"sku_grp_info_chk",true,false,false,false,false},
//				new Object[]{"sku_info_chk",true,false,false,false,false},
//				new Object[]{"sku_pic_chk",true ,false ,false,false,false},
//				new Object[]{"sku_prop_chk",true,false,false,false,false},
//				new Object[]{"sku_chk_diff",false ,false ,false,false,false},
//				new Object[]{"sku_chk_log",false ,false ,false,false,false},
//				new Object[]{"sku_draft",true ,false ,false,false,false},
//				new Object[]{"sku_grp_category",true,false,false,false,false},
//				new Object[]{"sku_grp_desc",true,false,false,false,false},
//				new Object[]{"sku_grp_info",true,false,false,false,false},
//				new Object[]{"sku_grp_tag",true ,false ,false,false,false},
//				new Object[]{"sku_info",true,false,false,false,false},
//				new Object[]{"sku_pic",true ,false ,false,true,false},
//				new Object[]{"sku_platform_black_list",true ,false ,false,false,true},
//				new Object[]{"sku_platform_black_list_chk",true ,false ,false,false,false},
//				new Object[]{"sku_prop",true,false,false,false,false},
//				new Object[]{"sp_controller_role",true,false,false,true,false},
//				new Object[]{"sp_div_role",true ,false ,false,true,false},
//				new Object[]{"sp_op_log",false,false,false,false,false},
//				new Object[]{"sp_role",true ,false ,false,false,true},
//				new Object[]{"sp_req_param_role_val",false ,false ,false,false,true},
//				new Object[]{"sp_usr",false ,false ,false,false,false},
//				new Object[]{"sp_usr_role",true ,false ,false,true,false},
//				new Object[]{"stl_batch_date", false,false,false,false,false},
//				new Object[]{"stl_cmb", false,false,false,false,false},
//				new Object[]{"stl_except_patron", false,false,false,false,false},
//				new Object[]{"stl_except_self", false,false,false,false,false},
//				new Object[]{"stl_master", true,false,false,false,false},
//				new Object[]{"stl_pay_dtl", false,false,false,false,false},
//				new Object[]{"stl_pay_dtl_subject", false,false,false,false,false},
// 				new Object[]{"stl_monthly_bill_result", false,false,false,false,false},

//				new Object[]{"vou_code_cust",true,false,false,true,false},
//				new Object[]{"vou_code",false,true,false,false,false},
//				new Object[]{"vou_info",false ,false ,false,true,false},
//                new Object[]{"vou_op_log",false ,false ,false,false,false},
//                new Object[]{"vou_op_fail",true ,false ,false,false,false},
//				new Object[]{"vou_limit_date",true,false,false,false,false},
//				new Object[]{"vou_limit_pay_way",true ,false ,false,false,false},
//				new Object[]{"vou_limit_platform_chnl_biz",true ,false ,false,false,false},
//				new Object[]{"vou_limit_sku_grp",true ,false ,false,false,false},
//				new Object[]{"vou_limit_vendor",true,false,false,false,false},
//				new Object[]{"vndr_attachment",true ,false ,false,false,false},
//				new Object[]{"vndr_info",false,false,false,false,false},
//				new Object[]{"vndr_rma_warehouse",false ,false ,false,false,false},
//                new Object[]{"vndr_attachment_chk",true ,false ,false,false,false},
//                new Object[]{"vndr_info_chk",false,false,false,false,false},
//                new Object[]{"vndr_rma_warehouse_chk",false ,false ,false,false,false},
//				new Object[]{"lck_function",false,false,true,false,false},
//                new Object[]{"lck_platform_resale",false,false,true,false,false},
//                new Object[]{"lck_platform_retail",false,false,true,false,false},
//                new Object[]{"lck_sku",false,false,true,false,false},
//                new Object[]{"lck_vndr",false ,false ,true,false,false},
//                new Object[]{"lck_vou",false,false,true,false,false},
//                new Object[]{"log_upload_oms",false,false,false,false,false},
//                new Object[]{"log_upload_sp",false,false,false,false,false},
//                new Object[]{"refund_info",false,false,false,false,false},
//                new Object[]{"data_vendor",true,false,false,false,false},
//                new Object[]{"data_vendor_instant",true,false,false,false,false},
//                new Object[]{"data_oms",true,false,false,false,false},
//                new Object[]{"data_oms_instant",true,false,false,false,false},


//				new Object[]{"node_enrollment",false,false,false,false,false},
//				new Object[]{"oms_op_log",false ,false ,false,false,false},
//                new Object[]{"oms_div",false ,false ,false,false,true},
//				new Object[]{"oms_div_role",true,false,false,true,false},
//				new Object[]{"oms_role",false ,false ,false,false,true},
//				new Object[]{"oms_usr",false,false,false,false,false},
//				new Object[]{"oms_usr_role",true,false,false,true,false},
//				new Object[]{"oms_auth_api_role",true,false,false,true,false},
//				new Object[]{"mt_oms_api_role",true,false,false,true,false},
//				new Object[]{"mt_ban",false,false,false,false,false},
//				new Object[]{"mt_sku",true,false,false,false,false},
//				new Object[]{"rp_oms_api_role",true,false,false,true,false},
//				new Object[]{"pay_dtl",true,true,false,false,false},
//				new Object[]{"pay_notify",true,true,false,false,false},
//				new Object[]{"pay_treasure",true,true,false,false,false},
//				new Object[]{"mt_ord", true, false, false, false, false},
//				new Object[]{"mt_rma",true,true,false,false,false},
//				new Object[]{"mt_cust_monthly_lock", true, false, false, false, false},
//				new Object[]{"rp_ord",true,false,false,false,false},
//				new Object[]{"rp_ord_item", true, false, false, true, false},
//				new Object[]{"rp_rma", false, false, false, false, false},
//				new Object[]{"rp_cust_issue_ord_limit",false,true,true,false,false},
//				new Object[]{"rp_cust_recv_point_limit",false,true,true,false,false},
				null}) {
			if (null == table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache) {
				continue;
			}

			String table = ((String) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[0]).toLowerCase();
			Boolean needDelete = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[1];
			Boolean needInsertIgnore = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[2];
			Boolean needSelectForUpdate = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[3];
			Boolean needUnion = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[4];
			Boolean isRedisCache = table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache.length < 5 ? false : (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[5];
			Triple<String, Boolean, Map<String, Pair<String, String>>> idClassSimpleName_isAutoIncrId_column_type_comment = queryColumnType(table);
			if (null != idClassSimpleName_isAutoIncrId_column_type_comment) {
//				Dto dto = resolveFieldClass(table, idClassSimpleName_isAutoIncrId_column_type_comment);
//				dto.setNeedDelete(needDelete ? "1" : null);
//				dto.setNeedInsertIgnore(needInsertIgnore ? "1" : null);
//				dto.setNeedSelectForUpdate(needSelectForUpdate ? "1" : null);
//				dto.setNeedUnion(needUnion ? "1" : null);
//				dto.setIsRedisCache(isRedisCache ? "1" : null);
//				po(dto);
//				mapper(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
//				if (isRedisCache) {
//					redisCacheDao(dto);
//				} else {
//					dao(dto, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
//				}
			}
		}

	}
}
