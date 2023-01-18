package com.dslab.voronoi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.spark.sql.Dataset;

import java.io.File;

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
      input = new Path(args[1]);
      output = new Path(args[2] + dirPath + "/output");

   }

}
