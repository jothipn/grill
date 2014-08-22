package com.inmobi.grill.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.inmobi.grill.cli.skel.GrillPromptProvider;
import com.inmobi.grill.client.GrillClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GrillMLCommands implements CommandMarker {
  public static final Log LOG = LogFactory.getLog(GrillMLCommands.class);
  private static final String MISSING_MODEL_ID = "Model ID not present in session and not provided explicitly";
  private static final String MISSING_REPORT_ID = "Report ID not present in session and not provided explicitly";
  private GrillClient client;
  private String cliSessionModelId;
  private String cliSessionReportId;
  private GrillPromptProvider promptProvider;

  public void setClient(GrillClient client) {
    this.client = client;
  }

  public void setPromptProvider(GrillPromptProvider promptProvider) {
    this.promptProvider = promptProvider;
  }

  void updatePrompt() {
    promptProvider.setPrompt("[Model= %s , Report= %s ]\n",
      cliSessionModelId, cliSessionReportId);
  }

  private boolean isNotBlank(String str) {
    return str != null && !str.isEmpty();
  }

  private String getNonNull(String str1, String str2) {
    if (isNotBlank(str1)) {
      return str1;
    }
    return str2;
  }

  @CliCommand(value = "ml show algorithms",
    help = "Get list of ML algorithms supported by Grill server")
  public String getMLAlgorithms() {
    return Joiner.on('\n').join(client.getMLTrainerNames());
  }

  @CliCommand(value = "ml show models",
  help = "Get list of models for an algorithm")
  public String getModelsOfAlgorithm(
    @CliOption(key = {"algorithm"}, mandatory = true, help="algorithm name") String algorithm) {
    List<String> modelIds = client.getMLModelsForAlgorithm(algorithm);
    if (modelIds == null || modelIds.isEmpty()) {
      return "Models not found for " + algorithm;
    }
    return Joiner.on('\n').join(modelIds);
  }

  @CliCommand(value = "ml describe model",
    help = "Describe model metadata")
  public String describeModel(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory = false, help = "model ID") String modelID) {
    modelID = getNonNull(modelID, cliSessionModelId);
    if (isNotBlank(modelID)) {
      setCliSessionModelID(modelID);
      String modelMetadata = client.getMLModelMetadata(algorithm, modelID).toString();
      return modelMetadata == null ? ("Model not found " + algorithm + "/" + modelID) : modelMetadata;
    } else {
      return MISSING_MODEL_ID;
    }
  }


  @CliCommand(value ="ml delete model", help = "Delete an ML model")
  public String deleteModel(
    @CliOption(key = {"algorithm"}, mandatory = true, help="algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory=false, help="model ID") String modelID) {
    modelID = getNonNull(modelID, cliSessionModelId);
    if (isNotBlank(modelID)) {
      setCliSessionModelID(modelID);
      client.deleteMLModel(algorithm, modelID);
      return "Deleted model " + modelID;
    } else {
      return MISSING_MODEL_ID;
    }
  }

  @CliCommand(value = "ml train model", help = "Train a new ML model")
  public String trainMLModel(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm (trainer) name") String algorithm,
    @CliOption(key = {"params"}, mandatory = true,
      help = "Parameters required to train model K1=V1, K2=V2,..Kn=Vn") String params) {
    Map<String, String> trainParams = new HashMap<String, String>();
    for (String pair : Splitter.on(',').trimResults().split(params)) {
      Iterable<String> keyValue = Splitter.on('=').trimResults().split(pair);

      if (!keyValue.iterator().hasNext()) {
        return "Empty key value pair in trainer params: " + params;
      }
      String key = keyValue.iterator().next();


      if (!keyValue.iterator().hasNext()) {
        return "Param value not specified for key " + key;
      }
      String value = keyValue.iterator().next();

      if (Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(value)) {
        return "Invalid trainer params: " + params;
      } else if (keyValue.iterator().hasNext()) {
        return "More than parameter in the key-value pair: " + pair;
      }

      trainParams.put(key, value);
    }

    setCliSessionModelID(client.trainMLModel(algorithm, trainParams));
    return "Created model " + cliSessionModelId;
  }

  @CliCommand(value = "ml test model", help = "Test an ML model")
  public String testMLModel(
    @CliOption(key = {"table"}, mandatory = true, help = "Name of input table to run the test on") String table,
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory = false, help = "Model ID") String modelID) {
    modelID = getNonNull(modelID, cliSessionModelId);
    if (isNotBlank(modelID)) {
      setCliSessionModelID(modelID);
      setCliSessionReportId(client.testMLModel(table, algorithm, modelID));
      return "Created test report " + cliSessionReportId;
    } else {
      return MISSING_MODEL_ID;
    }
  }

  @CliCommand(value = "ml show reports", help = "Show list of ML Test reports for a given algorithm")
  public String showMLTestReports(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm) {
    List<String> reports = client.getMLTestReportsOfAlgorithm(algorithm);
    if (reports == null || reports.isEmpty()) {
      return "Reports not found for " + algorithm;
    }
    return Joiner.on('\n').join(reports);
  }

  @CliCommand(value = "ml describe report", help = "Describe an ML Test report")
  public String describeMLTestReport(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"reportID"}, mandatory = true, help = "Test report ID") String reportID) {
    reportID = getNonNull(reportID, cliSessionReportId);
    if (isNotBlank(reportID)) {
      setCliSessionReportId(reportID);
      String report = client.getMLTestReport(algorithm, reportID).toString();
      return report == null ? ("Report not found " + algorithm + "/" + reportID) : report;
    } else {
      return MISSING_REPORT_ID;
    }
  }

  @CliCommand(value = "ml delete report", help = "Delete an ML Test report")
  public String deleteMLTestReport(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"reportID"}, mandatory = true, help = "Test report ID") String reportID) {
    reportID = getNonNull(reportID, cliSessionReportId);
    if (isNotBlank(reportID)) {
      setCliSessionReportId(reportID);
      client.deleteMLTestReport(algorithm, reportID);
      return "Deleted report " + reportID;
    } else {
      return MISSING_REPORT_ID;
    }
  }

  @CliCommand(value = "ml show modelID", help = "Returns current model ID in the session")
  public String showSessionModelID() {
    return cliSessionModelId;
  }

  @CliCommand(value = "ml show reportID", help = "Returns current report ID in the session")
  public String showSessionReportID() {
    return cliSessionReportId;
  }


  @CliCommand(value = "ml describe algorithm", help = "Get parameters usage for the algorithm")
  public String getParamDescForAlgorithm(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm) {
    List<String> helps = client.getParamDescriptionOfTrainer(algorithm);
    if (helps == null) {
      return "Description not found";
    }

    return Joiner.on('\n').join(helps);
  }

  void setCliSessionModelID(String cliSessionModelID) {
    this.cliSessionModelId = cliSessionModelID;
    updatePrompt();
  }

  void setCliSessionReportId(String reportId) {
    this.cliSessionReportId = reportId;
    updatePrompt();
  }

}
