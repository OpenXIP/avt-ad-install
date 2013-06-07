/*
Copyright (c) 2010, Siemens Corporate Research a Division of Siemens Corporation 
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.siemens.scr.avt.ad.install;

import java.util.Properties;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DBInstall {
	private static Logger logger = Logger.getLogger(DBInstall.class);
	private static final String DDL_SCRIPT = "/createSchema.sql";

	static void install(Properties properties) throws ClassNotFoundException, SQLException, IOException {
		Connection conn = buildConnection(properties);

		SQLScript script = new SQLScript(DDL_SCRIPT);

		if(Boolean.TRUE.equals(properties.get(ADInstallationTool.CREATE_TABLESPACE))){
			handleTablespace(conn);	
		}
		
		handleSchema(conn, properties.getProperty(ADInstallationTool.TARGET_SCHMA));
		
		script.execute(conn);
		
		conn.close();
	}
	
	////////////////////////////// Schema Creation ///////////////////////////////////
	
	private static void handleSchema(Connection conn, String schema) throws SQLException{
		if(isSchemaExisting(conn, schema)){
			setSchemaToCurrent(conn, schema);
			clearSchema(conn, schema);
		}
		else{
			createSchema(conn, schema);
			setSchemaToCurrent(conn, schema);
		}
		
		
	}
	
	private static void createSchema(Connection conn, String schema) throws SQLException {
		Statement stat  = conn.createStatement();
		stat.execute("create schema " + schema);
	}

	private static void setSchemaToCurrent(Connection conn, String schema) throws SQLException{
		Statement stat  = conn.createStatement();
		stat.execute("set schema " + schema);
	}
	
	private static boolean isSchemaExisting(Connection conn, String schema) throws SQLException{
		Statement stat  = conn.createStatement();
		return stat.execute("select SCHEMANAME  from SYSCAT.SCHEMATA where  SCHEMANAME = '" + schema + "'");
	}
	
	private static void clearSchema(Connection conn, String schema) throws SQLException{
		PreparedStatement statement = conn.prepareStatement("select TABNAME from SYSCAT.TABLES where TABSCHEMA = ? and TYPE = 'T'");
		statement.setString(1, schema);
		ResultSet rs = statement.executeQuery();
		
		while(rs.next()){
			Statement dropStatement = conn.createStatement();
			logger.debug("dropping " + rs.getString(1));
			dropStatement.execute("drop TABLE " + rs.getString(1));
		}
	}
	////////////////////////////// Table-space Creation ///////////////////////////////////
	
	private static void handleTablespace(Connection conn) throws SQLException{
		String bufferPool = createBufferPool(conn);
		createTablespace(conn, bufferPool);
	}
	
	private static void createTablespace(Connection conn, String bufferPool) throws SQLException {
		Statement stat = conn.createStatement();
		stat.execute("create TABLESPACE AD8K PAGESIZE 8192 MANAGED BY AUTOMATIC STORAGE BUFFERPOOL " + bufferPool);
	}

	private static String createBufferPool(Connection conn) throws SQLException {
		final String bufferName = "AD8KBUFFER";
		Statement stat = conn.createStatement();
		
		stat.execute("CREATE BUFFERPOOL "+ bufferName +" IMMEDIATE  SIZE 250 PAGESIZE 8K ");
		
		return bufferName;
	}
	
	/////////////////////////////// Build Connection //////////////////////////////////

	

	private static Connection buildConnection(Properties properties)
			throws ClassNotFoundException, SQLException {
		String url = properties.getProperty(ADInstallationTool.CONNECTION_URL);
		String username = properties.getProperty(ADInstallationTool.CONNECTION_USER);
		String pwd = properties.getProperty(ADInstallationTool.CONNECTION_PASSWORD);

		Class.forName("com.ibm.db2.jcc.DB2Driver");// TODO: make it a property

		Connection conn = DriverManager.getConnection(url, username, pwd);
		return conn;

	}
	
	
	
}
