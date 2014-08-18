package com.inmobi.grill.server.api.ml;

import java.util.List;

public interface MultiPrediction {
  public List<LabelledPrediction> getPredictions();
}
