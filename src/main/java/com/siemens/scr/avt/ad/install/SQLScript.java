package com.siemens.scr.avt.ad.install;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Iterator;

/**
 * Currently I only accept comments with double dashes "--" leading
 * a line. 
 * 
 * @author Xiang Li
 *
 */
public class SQLScript {
	public final static char QUERY_ENDS = ';';

	private InputStream scriptStream;
	
	public SQLScript(String scriptFileName) {
		scriptStream = this.getClass().getResourceAsStream(scriptFileName);
	}
	
	public SQLScript(InputStream fileInputStream){
		scriptStream = fileInputStream;
	}

	

	protected void loadScript(Statement stat) throws IOException, SQLException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream));
		String line;
		boolean queryEnds = false;
		StringBuffer query = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			if (isComment(line))
				continue;
			queryEnds = checkStatementEnds(line);
			
			if (queryEnds) {
				query.append(line.substring(0, line.indexOf(QUERY_ENDS)));	
				stat.addBatch(query.toString());
				query.setLength(0);
			}
			else{
				query.append(line);
			}
		}
	}

	private boolean isComment(String line) {
		if ((line != null) && (line.length() > 0))
			return (line.startsWith("--"));
		return false;
	}

	public void execute(Connection conn) throws IOException {
		
		try {
			Statement stat = conn.createStatement();
			loadScript(stat);
			stat.executeBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Iterator<Throwable> it = e.iterator();
			while(it.hasNext()){
				it.next().printStackTrace();
			}
//			e.printStackTrace();
		}
	}

	private boolean checkStatementEnds(String s) {
		return (s.indexOf(QUERY_ENDS) != -1);
	}

}
