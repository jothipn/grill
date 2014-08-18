package com.inmobi.grill.server.api.ml;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public abstract class MLModel<PREDICTION> implements Serializable {
  private String id;
  private Date createdAt;
  private String trainerName;
  private String table;
  private List<String> params;
  private String labelColumn;
  private List<String> featureColumns;


  public abstract PREDICTION predict(Object ... args);

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getTrainerName() {
    return trainerName;
  }

  public void setTrainerName(String trainerName) {
    this.trainerName = trainerName;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public List<String> getParams() {
    return params;
  }

  public void setParams(List<String> params) {
    this.params = params;
  }

  public String getLabelColumn() {
    return labelColumn;
  }

  public void setLabelColumn(String labelColumn) {
    this.labelColumn = labelColumn;
  }

  public List<String> getFeatureColumns() {
    return featureColumns;
  }

  public void setFeatureColumns(List<String> featureColumns) {
    this.featureColumns = featureColumns;
  }
}
