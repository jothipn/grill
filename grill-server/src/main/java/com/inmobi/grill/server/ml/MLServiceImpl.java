package com.inmobi.grill.server.ml;

import com.inmobi.grill.api.GrillConf;
import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.api.GrillSessionHandle;
import com.inmobi.grill.api.query.GrillQuery;
import com.inmobi.grill.api.query.QueryHandle;
import com.inmobi.grill.api.query.QueryStatus;
import com.inmobi.grill.ml.GrillML;
import com.inmobi.grill.ml.ModelLoader;
import com.inmobi.grill.ml.TestQueryRunner;
import com.inmobi.grill.server.GrillService;
import com.inmobi.grill.server.GrillServices;
import com.inmobi.grill.server.api.GrillConfConstants;
import com.inmobi.grill.server.api.ml.*;
import com.inmobi.grill.server.api.query.QueryExecutionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.cli.CLIService;

import java.util.*;

public class MLServiceImpl extends GrillService implements MLService {
  public static final Log LOG = LogFactory.getLog(GrillML.class);
  private GrillML mlHandler;

  public MLServiceImpl(String name, CLIService cliService) {
    super(NAME, cliService);
  }

  public MLServiceImpl(CLIService cliService) {
    this(NAME, cliService);
  }

  @Override
  public List<String> getAlgorithms() {
    return mlHandler.getAlgorithms();
  }

  @Override
  public MLTrainer getTrainerForName(String algorithm) throws GrillException {
    return mlHandler.getTrainerForName(algorithm);
  }

  @Override
  public String train(String table, String algorithm, String[] args) throws GrillException {
    return mlHandler.train(table, algorithm, args);
  }

  @Override
  public List<String> getModels(String algorithm) throws GrillException {
    return mlHandler.getModels(algorithm);
  }

  @Override
  public MLModel getModel(String algorithm, String modelId) throws GrillException {
    return mlHandler.getModel(algorithm, modelId);
  }

  @Override
  public synchronized void init(HiveConf hiveConf) {
    mlHandler = new GrillML(hiveConf);
    mlHandler.init(hiveConf);
    super.init(hiveConf);
    LOG.info("Inited ML service");
  }

  @Override
  public synchronized void start() {
    mlHandler.start();
    super.start();
    LOG.info("Started ML service");
  }

  @Override
  public synchronized void stop() {
    mlHandler.stop();
    super.stop();
    LOG.info("Stopped ML service");
  }

  public void clearModels() {
    ModelLoader.clearCache();
  }

  @Override
  public String getModelPath(String algorithm, String modelID) {
    return mlHandler.getModelPath(algorithm, modelID);
  }

  @Override
  public MLTestReport testModel(GrillSessionHandle sessionHandle,
                                String table,
                                String algorithm,
                                String modelID) throws GrillException {

    return mlHandler.testModel(sessionHandle, table, algorithm, modelID, new DirectQueryRunner(sessionHandle));
  }

  @Override
  public List<String> getTestReports(String algorithm) throws GrillException {
    return mlHandler.getTestReports(algorithm);
  }

  @Override
  public MLTestReport getTestReport(String algorithm, String reportID) throws GrillException {
    return mlHandler.getTestReport(algorithm, reportID);
  }

  @Override
  public LabelledPrediction predict(String algorithm, String modelID, Object[] features) throws GrillException {
    return null;
  }

  @Override
  public void deleteModel(String algorithm, String modelID) throws GrillException {
    mlHandler.deleteModel(algorithm, modelID);
  }

  @Override
  public void deleteTestReport(String algorithm, String reportID) throws GrillException {
    mlHandler.deleteTestReport(algorithm, reportID);
  }

  /**
   * Run the test model query directly in the current grill server process
   */
  private class DirectQueryRunner extends TestQueryRunner {

    public DirectQueryRunner(GrillSessionHandle sessionHandle) {
      super(sessionHandle);
    }

    @Override
    public QueryHandle runQuery(String testQuery) throws GrillException {
      // Run the query in query executions service
      QueryExecutionService queryService = (QueryExecutionService) GrillServices.get().getService("query");

      GrillConf queryConf = new GrillConf();
      queryConf.addProperty(GrillConfConstants.GRILL_PERSISTENT_RESULT_SET, false + "");
      queryConf.addProperty(GrillConfConstants.QUERY_PERSISTENT_RESULT_INDRIVER, false + "");

      QueryHandle testQueryHandle = queryService.executeAsync(sessionHandle,
        testQuery,
        queryConf
      );

      // Wait for test query to complete
      GrillQuery query = queryService.getQuery(sessionHandle, testQueryHandle);
      LOG.info("Submitted query " + testQueryHandle.getHandleId());
      while (!query.getStatus().isFinished()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new GrillException(e);
        }

        query = queryService.getQuery(sessionHandle, testQueryHandle);
      }

      if (query.getStatus().getStatus() != QueryStatus.Status.SUCCESSFUL) {
        throw new GrillException("Failed to run test query: " + testQueryHandle.getHandleId()
          + " reason= " + query.getStatus().getErrorMessage());
      }

      return testQueryHandle;
    }
  }
}
