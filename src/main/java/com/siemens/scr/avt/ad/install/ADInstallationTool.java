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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.StringParser;
import com.martiansoftware.jsap.Switch;

/**
 * <p>
 * Given a properties file specifying database information including the URL,
 * user name, and password, creates the expected AD schema.
 * </p>
 * 
 * @author Xiang Li
 * 
 */
public class ADInstallationTool {
	private static Logger logger = Logger.getLogger(ADInstallationTool.class);

	public static final String CONNECTION_URL = "hibernate.connection.url";

	public static final String CONNECTION_USER = "hibernate.connection.username";

	public static final String CONNECTION_PASSWORD = "hibernate.connection.password";

	public static final String TARGET_SCHMA = "target.schemaname";

	public static final String TARGET_USER = "target.username";
	
	public static final String CREATE_TABLESPACE = "sql.createtablespace";
	
	public static final String HELP = "help";

	private Properties properties;
	
	public static final String[] OPTIONS = new String[] { CONNECTION_URL,
			CONNECTION_USER, CONNECTION_PASSWORD, TARGET_SCHMA,CREATE_TABLESPACE
//			TARGET_USER
	};

	public static final String PROPERTY_FILE = "property.file";

	ADInstallationTool() {
		properties = new Properties();
	}

	public static void main(String[] args) throws JSAPException {
		JSAP jsap = buildOptions();
		JSAPResult config = jsap.parse(args);

		if (!config.success()) {
			logger.error("Error while parsing arguments!");
			logger.error(getUsage(jsap));
			return;
		}
		else if(config.getBoolean(HELP)){
			System.out.println(getUsage(jsap));
		}
		else{
			ADInstallationTool tool = new ADInstallationTool();
			tool.install(config);	
		}
	}

	private static JSAP buildOptions() throws JSAPException {
		JSAP jsap = new JSAP();
		buildOption(jsap, PROPERTY_FILE, "file", 'f', false);
		buildOption(jsap, CONNECTION_URL, "url", 'l', false);
		buildOption(jsap, CONNECTION_USER, "user", 'u', false);
		buildOption(jsap, CONNECTION_PASSWORD, "pwd", 'p', false);
		buildOption(jsap, TARGET_SCHMA, "schema", 's', false);
		buildOption(jsap, TARGET_USER, "newuser", 'n', false);
		jsap.registerParameter(new Switch(CREATE_TABLESPACE, JSAP.NO_SHORTFLAG, "ct", CREATE_TABLESPACE));
		jsap.registerParameter(new Switch(HELP, 'h', "help"));
		return jsap;
	}

	private static String getUsage(JSAP jsap) {
		StringBuffer buf = new StringBuffer();
		buf.append("Usage: java " + ADInstallationTool.class.getName());
		buf.append("\n");
		buf.append(jsap.getUsage());
		buf.append("\n");
		buf.append("NOTE: explicitly given parameters will take priority over options given in the properties file.");
		buf.append("\n");
		
		return buf.toString();
	}
	
		
	private static FlaggedOption buildOption(JSAP jsap, String name, String longFlag,
			char shortFlag, boolean required, StringParser parser) throws JSAPException{
		FlaggedOption opt = new FlaggedOption(name);
		opt.setLongFlag(longFlag);
		opt.setShortFlag(shortFlag);
		opt.setRequired(required);
		opt.setStringParser(JSAP.STRING_PARSER);
		jsap.registerParameter(opt);		
		return opt;
	}

	private static FlaggedOption buildOption(JSAP jsap, String name, String longFlag,
			char shortFlag, boolean required) throws JSAPException {
		return buildOption(jsap, name, longFlag, shortFlag, required, JSAP.STRING_PARSER);
	}

	public void install(JSAPResult config) {

		parseConfig(config);
		
		installDB();
		
	}

	private boolean installDB() {
		if(check()){
			try {
				DBInstall.install(properties);
				logger.info("Tables installed under schema:"
						+ properties.getProperty(TARGET_SCHMA));
				return true;
			} catch (ClassNotFoundException e) {
				logger.error("Unable to find driver class.");
				e.printStackTrace();
			} catch (SQLException e) {
				logger.error("Error while executing SQL script");
				e.printStackTrace();
			} catch (IOException e) {
				logger.error("SQL script not found.");
				e.printStackTrace();
			}	
		}
		return false;
	}

	private void parseConfig(JSAPResult config) {
		String propertyFile = config.getString(PROPERTY_FILE);
		logger.debug(PROPERTY_FILE + "=" + propertyFile);
		if (propertyFile != null && propertyFile.length() > 0) {
			try {
				loadOptionsFromPropertyFile(propertyFile);
			} catch (IOException e) {
				e.printStackTrace();
				logger.warn("Exception while loading properties from file:"
						+ propertyFile);
			}
		}
		else{
			logger.warn("I cannot load the property file:" + propertyFile);
		}

		loadOptionFromExplicitArguments(config);
	}

	private boolean check(){
		for (String optionKey : OPTIONS) {
			if(!properties.containsKey(optionKey)){
				logger.error("Required option missing: " + optionKey);
				return false;
			}
		}
		return true;
	}

	private void loadOptionFromExplicitArguments(JSAPResult result) {
		for (String optionKey : OPTIONS) {
			if(optionKey == CREATE_TABLESPACE){
				properties.put(optionKey, result.getBoolean(optionKey));
			}
			else if (result.getString(optionKey) != null) {
				logger.debug("Option given:" + optionKey);
				properties.put(optionKey, result.getString(optionKey));
			}
		}
	}

	private void loadOptionsFromPropertyFile(String propertyFile)
			throws IOException {
		File propertiesFile = new File(propertyFile);
		InputStream in = new FileInputStream(propertiesFile);
		if(in == null) throw new IOException("Cannot locate resource:" + propertyFile);
		properties.load(in);
		in.close();
	}

}
