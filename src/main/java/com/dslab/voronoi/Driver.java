package com.dslab.voronoi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.expressions.Ascending;

import java.util.Iterator;

import java.io.File;

import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.api.java.function.MapFunction;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;

public class Driver {
   // for maximum performance an initial convex hull size that takes full advantage
   // of the heap space for a single threaded java program. Each job requires time
   // to create and destroy it, so minimizing the amount of "recursive calls" we
   // need to make (AKA jobs) saves us time. No point in creating a bunch of jobs
   // for merging convex hulls of small sizes
   public static int INITIAL_CH_SIZE = 100;

   // AKA number of jobs to complete the D and Q algorithm
   private static int RECURSIVE_DEPTH;
   private static int SIZE;
   private static int NUM_MACHINES = 1;

   // 4 CPUs * 2 cores * 2 threads
   private static final int NUM_THREADS = 16;

   // args: [Npoints, input path, Nmachines]
   public static void main(String[] args) throws Exception {
      // number of recursions (mapreduce jobs) is equal to log_base_2(total_points /
      // initial_size_of_CH) rounded up
      SIZE = Integer.parseInt(args[0]);
      NUM_MACHINES = Integer.parseInt(args[2]);
      INITIAL_CH_SIZE = SIZE / (NUM_THREADS * NUM_MACHINES);
      RECURSIVE_DEPTH = (int) Math.ceil((Math.log((double) SIZE / INITIAL_CH_SIZE) / Math.log(2)));

      // total number of sequential threads possible on the cluster
      int numPartitions = NUM_THREADS * NUM_MACHINES;

      // create directory for intermediate files and output
      String dirPath = "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));

      String input = args[1];

      SparkSession spark = SparkSession.builder()
            .appName("Convex Hull")
            // set partitions to total number of sequential threads possible on the cluster
            .config("spark.sql.shuffle.partitions", "" + numPartitions)
            .getOrCreate();
      Encoder<Point> pointEncoder = Encoders.bean(Point.class);
      Encoder<ConvexHull> convexHullEncoder = Encoders.bean(ConvexHull.class);
      // read CSV file of points
      Dataset<Point> points = spark.read()
            .option("header", "true")
            .schema(pointEncoder.schema())
            .csv(input)
            .as(pointEncoder);

      // sort points by x value. Convex Hull merge algorithm only works from left to
      // right
      System.out.println(points.showString(10, 0, true));
      Dataset<Point> sortedPoints = points.sort(functions.asc("x"));

      // https://spark.apache.org/docs/latest/api/java/org/apache/spark/api/java/function/MapPartitionsFunction.html
      // run the ConvexHull algorithm locally on each partition of points
      Dataset<ConvexHull> hulls = sortedPoints.mapPartitions(new MapPartitionsFunction<Point, ConvexHull>() {
         @Override
         public Iterator<ConvexHull> call(Iterator<Point> pointItr) throws Exception {
            Vector<Point> allPoints = new Vector<>();
            while (pointItr.hasNext()) {
               Point p = pointItr.next();
               allPoints.add(p);
            }
            List<ConvexHull> convexHull = new ArrayList<ConvexHull>(1);
            convexHull.add(new ConvexHull(allPoints));
            return convexHull.iterator();
         }
      }, convexHullEncoder);

      // System.out.println(hulls.showString(10, 10, true));

      // reduce in pairs until single convex hull exists
      while (numPartitions > 1) {
         numPartitions = (int) Math.ceil(numPartitions / 2);
         // combine neighboring partitions
         Dataset<ConvexHull> hullPairs = hulls.coalesce(numPartitions);

         // merge each pair of hulls until 1 is left
         hulls = hullPairs.mapPartitions(new MapPartitionsFunction<ConvexHull, ConvexHull>() {
            @Override
            public Iterator<ConvexHull> call(Iterator<ConvexHull> hullItr) throws Exception {
               Vector<ConvexHull> hullPair = new Vector<>();
               while (hullItr.hasNext()) {
                  ConvexHull ch = hullItr.next();
                  hullPair.add(ch);
               }
               // since only 2 hulls, this is constant time sort
               Collections.sort(hullPair);
               List<ConvexHull> result = new ArrayList<>(1);
               result.add(ConvexHull.merge(hullPair.get(0), hullPair.get(1)));
               return result.iterator();
            }
         }, convexHullEncoder);
      }

      // documentation recommends cache before calling toLocalIterator()
      hulls.cache();
      // this is the action that kicks off all the previous transformations
      Iterator<ConvexHull> resultItr = hulls.toLocalIterator();

      ConvexHull result = resultItr.next();

      System.out.println(result);

      ConvexHullViewer.createImage(1000, result, new File(
            "/home/helzee/voronoi-sequential-2/output/out.png"));

   }

}

// ./spark-submit --class Driver --master local[*]
// /home/helzee/voronoi-sequential-2/target/convexhull-spark-1.0-SNAPSHOT.jar
// 1000 /home/helzee/voronoi-sequential-2/input/1000.csv
// /home/helzee/voronoi-sequential-2/output 1