package com.inmobi.grill.ml.spark.trainers;

import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.ml.TrainerArgParser;
import com.inmobi.grill.ml.spark.HiveTableRDD;
import com.inmobi.grill.ml.spark.models.KMeansClusteringModel;
import com.inmobi.grill.server.api.ml.Algorithm;
import com.inmobi.grill.server.api.ml.MLModel;
import com.inmobi.grill.server.api.ml.MLTrainer;
import com.inmobi.grill.server.api.ml.TrainerParam;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hive.hcatalog.data.HCatRecord;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import scala.Tuple2;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Algorithm(
  name = "spark_kmeans_trainer",
  description = "Spark MLLib KMeans trainer"
)
public class KMeansTrainer implements MLTrainer {
  private transient Configuration conf;
  private JavaSparkContext sparkContext;

  @TrainerParam(name = "table", help = "Name of HCatalog table containing input data")
  private String table;

  @TrainerParam(name = "db", help = "Name of HCatalog database containing the input table")
  private String db;

  @TrainerParam(name = "partition", help = "Partition filter to be used while constructing table RDD")
  private String partFilter;

  @TrainerParam(name = "k", help = "Number of cluster")
  private int k;

  @TrainerParam(name ="maxIterations", help = "Maximum number of iterations",
  defaultValue = "100")
  private int maxIterations = 100;

  @TrainerParam(name = "runs", help = "Number of parallel run",
  defaultValue = "1")
  private int runs = 1;

  @TrainerParam(name = "initializationMode", help = "initialization model, either \"random\" or \"k-means||\" (default).",
  defaultValue = "k-means||")
  private String initializationMode = "k-means||";

  @Override
  public String getName() {
    return getClass().getAnnotation(Algorithm.class).name();
  }

  @Override
  public String getDescription() {
    return getClass().getAnnotation(Algorithm.class).description();
  }

  @Override
  public void configure(Configuration configuration) {
    this.conf = conf;
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public MLModel train(Configuration conf, String db, String table, String modelId, String... params)
    throws GrillException {
    List<String> features = TrainerArgParser.parseArgs(this, params);
    final int featurePositions[] = new int[features.size()];
    final int NUM_FEATURES = features.size();

    JavaPairRDD<WritableComparable, HCatRecord> rdd = null;
    try {
      // Map feature names to positions
      Table tbl = Hive.get(new HiveConf(conf, this.getClass())).getTable(db, table);
      List<FieldSchema> allCols = tbl.getAllCols();
      int f = 0;
      for (int i = 0; i < tbl.getAllCols().size(); i++) {
        String colName = allCols.get(i).getName();
        if (features.contains(colName)) {
          featurePositions[f++] = i;
        }
      }

      rdd = HiveTableRDD.createHiveTableRDD(sparkContext, conf, db, table, partFilter);
      JavaRDD<Vector> trainableRDD = rdd.map(new Function<Tuple2<WritableComparable, HCatRecord>, Vector>() {
        @Override
        public Vector call(Tuple2<WritableComparable, HCatRecord> v1) throws Exception {
          HCatRecord hCatRecord = v1._2();
          double arr[] = new double[NUM_FEATURES];
          for (int i = 0; i < NUM_FEATURES; i++) {
            Object val = (Double) hCatRecord.get(featurePositions[i]);
            arr[i] = val == null ? 0d : (Double) val;
          }
          return Vectors.dense(arr);
        }
      });

      KMeansModel model =
        KMeans.train(trainableRDD.rdd(), k, maxIterations, runs, initializationMode);
      return new KMeansClusteringModel(modelId, model);
    } catch (Exception e) {
      throw new GrillException("KMeans trainer failed for " + db + "." + table, e);
    }
  }
}