package com.juntao.commons.autocoder;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RespConstWriter {
	private static File targetFile = new File("D:\\workspace\\funmall-rg-commons\\src\\main\\java\\com\\cmbchina\\o2o\\funmall\\rg\\commons\\consts\\ResponseConsts.java");

	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	// private static final String DB_URL =
	// "jdbc:mysql://mysql.piao.ip:8066/CMBCHINA?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
//	private static final String DB_URL = "jdbc:mysql://172.30.11.175:3306/"
//	+ "conf?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
//	private static final String DB_USER = "mall";
//	private static final String DB_PASSWORD = "mall-";
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:48066/"
			+ "RG?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
	private static final String DB_USER = "rw_usr";
	private static final String DB_PASSWORD = "rw_usr*2017_test";

	static class CodeMsgQuerier implements ResultSetHandler<Map<String, Pair<String, String>>> {

		@Override
		public Map<String, Pair<String, String>> handle(ResultSet rs) throws SQLException {
			Map<String, Pair<String, String>> code_varName_msg = new LinkedHashMap<>();
			while (rs.next()) {
				String code = StringUtils.leftPad(rs.getString(1), 4, "0");
				String varName = rs.getString(2).toUpperCase();
				String msg = rs.getString(3);

				code_varName_msg.put(code, Pair.of(varName, msg));
				System.out.println(code + " " + varName + " " + msg);
			}

			return code_varName_msg;
		}
	}

	private static final Map<String, Pair<String, String>> queryCodeMsg() {
		DbUtils.loadDriver(DB_DRIVER);
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

			return new QueryRunner().query(conn,
					"SELECT id, var_name, msg FROM conf_resp_code_msg ORDER BY ID", new CodeMsgQuerier());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

	public static void main(String[] args) throws Exception {
		List<String> lines = new ArrayList<>();
		lines.add("package com.cmbchina.o2o.funmall.rg.commons.consts;");
		lines.add("");
		lines.add("import org.apache.commons.lang3.tuple.Pair;");
		lines.add("");
		lines.add("public class ResponseConsts {");

		boolean emptyLineFlag = true;
		for (Map.Entry<String, Pair<String, String>> code_varName_msg : queryCodeMsg().entrySet()) {
			String code = code_varName_msg.getKey();
			if (emptyLineFlag && 0 >= "1000".compareTo(code)) {
				emptyLineFlag = false;
				lines.add("");
			}
			lines.add("\tpublic static final Pair<String, String> " + code_varName_msg.getValue().getLeft() + " = Pair.of(\"" + code + "\", \"" + code_varName_msg.getValue().getRight() + "\");");
		}

		lines.add("}");

		FileUtils.writeLines(targetFile, lines);
	}
}
