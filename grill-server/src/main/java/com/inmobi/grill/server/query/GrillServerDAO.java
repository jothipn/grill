package com.inmobi.grill.server.query;
/*
 * #%L
 * Grill API for server and extensions
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
import com.inmobi.grill.server.api.GrillConfConstants;
import com.inmobi.grill.server.api.query.FinishedGrillQuery;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Top level class which logs and retrieves finished query from Database
 */
public class GrillServerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(GrillServerDAO.class);
  private DataSource ds;

  public void init(Configuration conf) {
    String className = conf.get(GrillConfConstants.GRILL_SERVER_DB_DRIVER_NAME,
        GrillConfConstants.DEFAULT_SERVER_DB_DRIVER_NAME);
    String jdbcUrl = conf.get(GrillConfConstants.GRILL_SERVER_DB_JDBC_URL,
        GrillConfConstants.DEFAULT_SERVER_DB_JDBC_URL);
    String userName = conf.get(GrillConfConstants.GRILL_SERVER_DB_JDBC_USER,
        GrillConfConstants.DEFAULT_SERVER_DB_USER);
    String pass = conf.get(GrillConfConstants.GRILL_SERVER_DB_JDBC_PASS,
        GrillConfConstants.DEFAULT_SERVER_DB_PASS);
    BasicDataSource tmp = new BasicDataSource();
    tmp.setDriverClassName(className);
    tmp.setUrl(jdbcUrl);
    tmp.setUsername(userName);
    tmp.setPassword(pass);
    ds = tmp;
  }

  public Connection getConnection() throws SQLException {
    return ds.getConnection();
  }


  private void createTable(String sql) throws SQLException {
    QueryRunner runner = new QueryRunner(ds);
    runner.update(sql);
  }

  public void dropFinishedQueriesTable() {
    QueryRunner runner = new QueryRunner(ds);
    try {
      runner.update("drop table finished_queries");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method to create finished queries table, this is required for embedded grill server.
   * For production server we will not be creating tables as it would be created upfront.
   */
  public void createFinishedQueriesTable() throws Exception {
    String sql = "CREATE TABLE if not exists finished_queries (handle varchar(255) not null unique," +
        "userquery varchar(255) not null,submitter varchar(255) not null," +
        "starttime bigint, endtime bigint,result varchar(255)," +
        "status varchar(255), metadata varchar(100000), rows int, errormessage varchar(10000), " +
        "driverstarttime bigint, driverendtime bigint, metadataclass varchar(10000))";
    try {
      createTable(sql);
    } catch (SQLException e) {
      LOG.warn("Unable to create finished queries table", e);
    }
  }

  /**
   * DAO method to insert a new Finished query into Table
   * @param query to be inserted
   */
  public void insertFinishedQuery(FinishedGrillQuery query) throws Exception {
    String sql = "insert into finished_queries (handle, userquery,submitter," +
        "starttime,endtime,result,status,metadata,rows," +
        "errormessage,driverstarttime,driverendtime, metadataclass) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    QueryRunner runner = new QueryRunner(ds);
    try {
      runner.update(sql, query.getHandle(),
          query.getUserQuery(),
          query.getSubmitter(),
          query.getStartTime(),
          query.getEndTime(),
          query.getResult(),
          query.getStatus(),
          query.getMetadata(),
          query.getRows(),
          query.getErrorMessage(),
          query.getDriverStartTime(),
          query.getDriverEndTime(),
          query.getMetadataClass());
    } catch (SQLException e) {
      throw new Exception(e);
    }
  }

  /**
   * Fetch Finished query from Database
   * @param handle to be fetched
   * @return Finished query.
   */
  public FinishedGrillQuery getQuery(String handle) {
    ResultSetHandler<FinishedGrillQuery> rsh = new BeanHandler<FinishedGrillQuery>(FinishedGrillQuery.class);
    String sql = "select * from finished_queries where handle=?";
    QueryRunner runner = new QueryRunner(ds);
    try {
      return runner.query(sql,rsh, handle);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

}
