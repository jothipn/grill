package com.inmobi.grill.server.ml.spark.trainers;

import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.server.api.ml.Algorithm;
import com.inmobi.grill.server.ml.spark.models.BaseSparkClassificationModel;
import com.inmobi.grill.server.ml.spark.models.NaiveBayesClassificationModel;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.rdd.RDD;

import java.util.Map;

@Algorithm(
  name = "spark_naive_bayes",
  description = "Spark Naive Bayes classifier trainer"
)
public class NaiveBayesTrainer extends BaseSparkTrainer {
  private double lambda = 1.0;

  public NaiveBayesTrainer(String name, String description) {
    super(name, description);
  }

  @Override
  public void parseTrainerParams(Map<String, String> params) {
    lambda = getParamValue("lambda", 1.0d);
  }

  @Override
  protected BaseSparkClassificationModel trainInternal(String modelId, RDD<LabeledPoint> trainingRDD) throws GrillException {
    return new NaiveBayesClassificationModel(modelId, NaiveBayes.train(trainingRDD, lambda));
  }
}