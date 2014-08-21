package com.inmobi.grill.server.ml;


import com.inmobi.grill.server.api.ml.MLModel;

import java.util.List;

/**
 * Return a single double value as a prediction. This is useful in classifiers where the classifier
 * returns a single class label as a prediction.
 */
public abstract class ClassifierBaseModel extends MLModel<Double> {
  public final double[] getFeatureVector(Object[] args) {
    double[] features = new double[args.length];
    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof  Double) {
        features[i] = (Double) args[i];
      } else {
        features[i] = 0.0;
      }
    }
    return features;
  }
}