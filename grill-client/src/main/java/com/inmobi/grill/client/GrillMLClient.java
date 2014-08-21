package com.inmobi.grill.client;

import com.inmobi.grill.api.StringList;
import com.inmobi.grill.api.ml.ModelMetadata;
import com.inmobi.grill.api.ml.TestReport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/*
 * Client code to invoke server side ML API
 */
public class GrillMLClient {
  public static final Log LOG = LogFactory.getLog(GrillMLClient.class);

  private final GrillConnection connection;

  public GrillMLClient(GrillConnection connection) {
    this.connection = connection;
  }

  protected WebTarget getMLWebTarget() {
    Client client = ClientBuilder
      .newBuilder()
      .register(MultiPartFeature.class)
      .build();
    GrillConnectionParams connParams = connection.getGrillConnectionParams();
    String baseURI = connParams.getBaseConnectionUrl();
    String mlURI = connParams.getMLResourcePath();
    return client.target(baseURI).path(mlURI);
  }


  ModelMetadata getModelMetadata(String algorithm, String modelID) {
    try {
      return getMLWebTarget()
        .path("models")
        .path(algorithm).path(modelID).request().get(ModelMetadata.class);
    } catch (NotFoundException exc) {
      return null;
    }
  }

  void deleteModel(String algorithm, String modelID) {
    getMLWebTarget()
      .path("models")
      .path(algorithm)
      .path(modelID)
      .request().delete();
  }

  List<String> getModelsForAlgorithm(String algorithm) {
    try {
      StringList models = getMLWebTarget()
        .path("models")
        .path(algorithm)
        .request().get(StringList.class);
      return models == null ? null : models.getElements();
    } catch (NotFoundException exc) {
      return null;
    }
  }

  List<String> getTrainerNames() {
    StringList trainerNames = getMLWebTarget()
      .path("trainers").request().get(StringList.class);
    return trainerNames == null ? null : trainerNames.getElements();
  }

  String trainModel(String algorithm, Map<String, String> params) {
    Form form = new Form();

    for (Map.Entry<String, String> entry : params.entrySet()) {
      form.param(entry.getKey(), entry.getValue());
    }

    return getMLWebTarget()
      .path(algorithm)
      .path("train")
      .request(MediaType.APPLICATION_JSON_TYPE)
      .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
  }

  String testModel(String table, String algorithm, String modelID) {
    WebTarget modelTestTarget = getMLWebTarget()
      .path("test")
      .path(table)
      .path(algorithm)
      .path(modelID);

    FormDataMultiPart mp = new FormDataMultiPart();
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("sessionid").build(),
      connection.getSessionHandle(), MediaType.APPLICATION_XML_TYPE));

    return modelTestTarget.request()
      .post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
  }

  List<String> getTestReportsOfAlgorithm(String algorithm) {
    try {
      StringList list = getMLWebTarget()
        .path("reports")
        .path(algorithm)
        .request()
        .get(StringList.class);
      return list == null ? null : list.getElements();
    } catch (NotFoundException exc) {
      return null;
    }
  }

  TestReport getTestReport(String algorithm, String reportID) {
    try {
      return getMLWebTarget()
        .path("reports")
        .path(algorithm)
        .path(reportID)
        .request()
        .get(TestReport.class);
    } catch (NotFoundException exc) {
      return null;
    }
  }

  String deleteTestReport(String algorithm, String reportID) {
    return getMLWebTarget()
      .path("reports")
      .path(algorithm)
      .path(reportID)
      .request().delete(String.class);
  }

  String predictSingle(String algorithm, String modelID, Map<String,String> features) {
    WebTarget target = getMLWebTarget()
      .path("predict")
      .path(algorithm)
      .path(modelID);

    for (Map.Entry<String, String> entry : features.entrySet()) {
      target.queryParam(entry.getKey(), entry.getValue());
    }

    return target.request().get(String.class);
  }
}
