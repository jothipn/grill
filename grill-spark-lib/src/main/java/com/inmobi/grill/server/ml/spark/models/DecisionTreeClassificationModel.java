package com.inmobi.grill.server.ml.spark.models;

import org.apache.spark.mllib.tree.model.DecisionTreeModel;

public class DecisionTreeClassificationModel extends BaseSparkClassificationModel<SparkDecisionTreeModel> {
  public DecisionTreeClassificationModel(String modelId, SparkDecisionTreeModel model) {
    super(modelId, model);
  }
}
