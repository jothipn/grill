package com.inmobi.grill.server.ml.spark.trainers;

import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.server.api.ml.Algorithm;
import com.inmobi.grill.server.ml.spark.models.BaseSparkClassificationModel;
import com.inmobi.grill.server.ml.spark.models.DecisionTreeClassificationModel;
import com.inmobi.grill.server.ml.spark.models.SparkDecisionTreeModel;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree$;
import org.apache.spark.mllib.tree.configuration.Algo$;
import org.apache.spark.mllib.tree.impurity.Entropy$;
import org.apache.spark.mllib.tree.impurity.Gini$;
import org.apache.spark.mllib.tree.impurity.Impurity;
import org.apache.spark.mllib.tree.impurity.Variance$;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.rdd.RDD;
import scala.Enumeration;

import java.util.Map;

@Algorithm(
  name = "spark_decision_tree",
  description = "Spark Decision Tree classifier trainer"
)
public class DecisionTreeTrainer extends BaseSparkTrainer {
  private Enumeration.Value algo;
  private Impurity decisionTreeImpurity;
  private int maxDepth;

  public DecisionTreeTrainer(String name, String description) {
    super(name, description);
  }

  @Override
  public void parseTrainerParams(Map<String, String> params) {
    String dtreeAlgoName = params.get("algo");
    if ("classification".equalsIgnoreCase(dtreeAlgoName)) {
      algo = Algo$.MODULE$.Classification();
    } else if ("regression".equalsIgnoreCase(dtreeAlgoName)) {
      algo = Algo$.MODULE$.Regression();
    }

    String impurity = params.get("impurity");
    if ("gini".equals(impurity)) {
      decisionTreeImpurity = Gini$.MODULE$;
    } else if ("entropy".equals(impurity)) {
      decisionTreeImpurity = Entropy$.MODULE$;
    } else if ("variance".equals(impurity)) {
      decisionTreeImpurity = Variance$.MODULE$;
    }

    maxDepth = getParamValue("maxDepth", 100);
  }

  @Override
  protected BaseSparkClassificationModel trainInternal(String modelId, RDD<LabeledPoint> trainingRDD) throws GrillException {
    DecisionTreeModel model = DecisionTree$.MODULE$.train(trainingRDD,
      algo, decisionTreeImpurity, maxDepth);
    return new DecisionTreeClassificationModel(modelId, new SparkDecisionTreeModel(model));
  }
}