package com.dslab.voronoi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

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
   private static final int NUM_THREADS = 8;

   // args: [Npoints, input path, output path, Nmachines]
   public static void main(String[] args) throws Exception {
      // number of recursions (mapreduce jobs) is equal to log_base_2(total_points /
      // initial_size_of_CH) rounded up
      SIZE = Integer.parseInt(args[0]);
      NUM_MACHINES = Integer.parseInt(args[3]);
      INITIAL_CH_SIZE = SIZE / (NUM_THREADS * NUM_MACHINES);
      RECURSIVE_DEPTH = (int) Math.ceil((Math.log((double) SIZE / INITIAL_CH_SIZE) / Math.log(2)));
      final int INITIAL_REDUCE_TASKS = NUM_THREADS * NUM_MACHINES;

      // create directory for intermediate files and output
      String dirPath = "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
      new File(args[2] + dirPath).mkdirs();
      String input = args[1];
      String output = args[2] + dirPath + "/output";
      SparkSession spark = SparkSession.builder().appName("Convex Hull").getOrCreate();
      Encoder<Point> pointEncoder = Encoders.bean(Point.class);
      Encoder<ConvexHull> convexHullEncoder = Encoders.bean(ConvexHull.class);
      Dataset<Point> points = spark.read().textFile(input).as(pointEncoder);
      Dataset<ConvexHull> hulls = points.mapPartitions(new MapPartitionsFunction<Point, ConvexHull>() {
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

   }

}
