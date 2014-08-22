package com.inmobi.grill.ml.spark.trainers;

import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.ml.spark.models.BaseSparkClassificationModel;
import com.inmobi.grill.ml.spark.models.LogitRegressionClassificationModel;
import com.inmobi.grill.server.api.ml.Algorithm;
import com.inmobi.grill.server.api.ml.TrainerParam;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.rdd.RDD;

import java.util.Map;

@Algorithm(
  name = "spark_logistic_regression",
  description = "Spark logistic regression trainer"
)
public class LogisticRegressionTrainer extends BaseSparkTrainer {
  @TrainerParam(name = "iterations", help ="Max number of iterations")
  private int iterations;

  @TrainerParam(name = "stepSize", help = "Step size")
  private double stepSize;

  @TrainerParam(name = "minBatchFraction", help = "Fraction for batched learning")
  private double minBatchFraction;

  public LogisticRegressionTrainer(String name, String description) {
    super(name, description);
  }

  @Override
  public void parseTrainerParams(Map<String, String> params) {
    iterations = getParamValue("iterations", 100);
    stepSize = getParamValue("stepSize", 1.0d);
    minBatchFraction = getParamValue("minBatchFraction", 1.0d);
  }

  @Override
  protected BaseSparkClassificationModel trainInternal(String modelId, RDD<LabeledPoint> trainingRDD) throws GrillException {
    LogisticRegressionModel lrModel =
      LogisticRegressionWithSGD.train(trainingRDD, iterations, stepSize, minBatchFraction);
    return new LogitRegressionClassificationModel(modelId, lrModel);
  }
}
