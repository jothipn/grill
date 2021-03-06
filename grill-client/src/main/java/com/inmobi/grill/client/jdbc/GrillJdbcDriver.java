package com.inmobi.grill.client.jdbc;

/*
 * #%L
 * Grill client
 * %%
 * Copyright (C) 2014 Inmobi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * Top level JDBC driver for Grill
 */
public class GrillJdbcDriver implements Driver {

  static {
    try {
      DriverManager.registerDriver(new GrillJdbcDriver());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Is the JDBC driver fully JDBC Compliant
   */
  private static final boolean JDBC_COMPLIANT = false;


  @Override
  public Connection connect(String s, Properties properties) throws SQLException {
    return null;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return Pattern.matches(JDBCUtils.URL_PREFIX + ".*", url);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
      throws SQLException {
    if (info == null) {
      info = new Properties();
    }
    if ((url != null) && url.startsWith(JDBCUtils.URL_PREFIX)) {
      info = JDBCUtils.parseUrlForPropertyInfo(url, info);
    }
    DriverPropertyInfo hostProp = new DriverPropertyInfo(
        JDBCUtils.HOST_PROPERTY_KEY, info.getProperty(JDBCUtils.HOST_PROPERTY_KEY));
    hostProp.required = false;
    hostProp.description = "Hostname of Grill Server. Defaults to localhost";

    DriverPropertyInfo portProp = new DriverPropertyInfo(
        JDBCUtils.PORT_PROPERTY_KEY, info.getProperty(JDBCUtils.PORT_PROPERTY_KEY));
    portProp.required = false;
    portProp.description = "Portnumber where grill server runs. " +
        "Defaults to 8080";

    DriverPropertyInfo dbProp = new DriverPropertyInfo(
        JDBCUtils.DB_PROPERTY_KEY, info.getProperty(JDBCUtils.DB_PROPERTY_KEY));
    dbProp.required = false;
    dbProp.description = "Database to connect to on grill server. " +
        "Defaults to 'default'";


    return new DriverPropertyInfo[]{hostProp, portProp, dbProp};
  }

  @Override
  public int getMajorVersion() {
    return JDBCUtils.getVersion(0);
  }

  @Override
  public int getMinorVersion() {
    return JDBCUtils.getVersion(1);
  }

  @Override
  public boolean jdbcCompliant() {
    return JDBC_COMPLIANT;
  }

  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }
}
