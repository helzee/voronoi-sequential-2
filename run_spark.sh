#!/bin/bash

./home/helzee/spark-3.3.1-bin-hadoop3/bin/spark-submit --master local 
/home/helzee/voronoi-sequential-2/target/convexhull-spark-1.0-SNAPSHOT.jar 1000 /home/helzee/voronoi-sequential-2/input/1000.csv /home/helzee/voronoi-sequential-2/output 1