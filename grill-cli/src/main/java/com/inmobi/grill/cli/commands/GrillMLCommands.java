package com.inmobi.grill.cli.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.inmobi.grill.client.GrillClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GrillMLCommands implements CommandMarker {
  public static final Log LOG = LogFactory.getLog(GrillMLCommands.class);
  private GrillClient client;

  public void setClient(GrillClient client) {
    this.client = client;
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
    return Joiner.on('\n').join(client.getMLModelsForAlgorithm(algorithm));
  }

  @CliCommand(value = "ml describe model",
  help = "Describe model metadata")
  public String describeModel(
    @CliOption(key = {"algorithm"}, mandatory = true, help="algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory=true, help="model ID") String modelID) {
    return client.getMLModelMetadata(algorithm, modelID).toString();
  }

  @CliCommand(value ="ml delete model", help = "Delete an ML model")
  public String deleteModel(
    @CliOption(key = {"algorithm"}, mandatory = true, help="algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory=true, help="model ID") String modelID) {
    client.deleteMLModel(algorithm, modelID);
    return "Deleted model " + modelID;
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

    String modelId = client.trainMLModel(algorithm, trainParams);
    return "Created model " + modelId;
  }

  @CliCommand(value = "ml test model", help = "Test an ML model")
  public String testMLModel(
    @CliOption(key = {"table"}, mandatory = true, help = "Name of input table to run the test on") String table,
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"modelID"}, mandatory = true, help = "Model ID") String modelID) {
    String testReportID = client.testMLModel(table, algorithm, modelID);
    return "Created test report " + testReportID;
  }

  @CliCommand(value = "ml show reports", help = "Show list of ML Test reports for a given algorithm")
  public String showMLTestReports(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm) {
    return Joiner.on('\n').join(client.getMLTestReportsOfAlgorithm(algorithm));
  }

  @CliCommand(value = "ml describe report", help = "Describe an ML Test report")
  public String describeMLTestReport(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"reportID"}, mandatory = true, help = "Test report ID") String reportID) {
    return client.getMLTestReport(algorithm, reportID).toString();
  }

  @CliCommand(value = "ml delete report", help = "Delete an ML Test report")
  public String deleteMLTestReport(
    @CliOption(key = {"algorithm"}, mandatory = true, help = "Algorithm name") String algorithm,
    @CliOption(key = {"reportID"}, mandatory = true, help = "Test report ID") String reportID) {
    client.deleteMLTestReport(algorithm, reportID);
    return "Deleted report " + reportID;
  }
}
