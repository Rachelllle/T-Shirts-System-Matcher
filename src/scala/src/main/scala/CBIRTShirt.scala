import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.feature.{Normalizer, BucketedRandomProjectionLSH}
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.Try
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation

object CBIRTShirt {
  def main(args: Array[String]): Unit = {
    val hdfsUser = sys.env.getOrElse("HADOOP_USER_NAME", "dataUser")
    if (hdfsUser.nonEmpty) {
      // Set Hadoop user to avoid UserGroupInformation.getSubject() error on Windows
      System.setProperty("HADOOP_USER_NAME", hdfsUser)
    }

    val hdfsDefault = sys.env.getOrElse("HDFS_NAMENODE", "hdfs://localhost:9000")

    val spark = SparkSession.builder()
      .appName("CBIRTShirtScala")
      .master("local[*]") // for local testing in IntelliJ
      .config("spark.hadoop.fs.defaultFS", hdfsDefault)
      .getOrCreate()

    import spark.implicits._

    val HDFS_NAMENODE = hdfsDefault
    val DATASET_DIRS = Seq(
      s"$HDFS_NAMENODE/user/$hdfsUser/datasets/plain-tshirt-color/plain_tshirt_dataset",
      s"$HDFS_NAMENODE/user/$hdfsUser/datasets/tshirts"
    )

    // --- 1) Verify HDFS paths and list sample files ---
    val hconf = spark.sparkContext.hadoopConfiguration
    val fs = org.apache.hadoop.fs.FileSystem.get(hconf)

    def exists(path: String): Boolean = fs.exists(new org.apache.hadoop.fs.Path(path))

    def lsSample(path: String, maxItems: Int = 10): Seq[(String, Long)] = {
      val p = new org.apache.hadoop.fs.Path(path)
      if (!fs.exists(p)) return Seq.empty
      val it = fs.listFiles(p, true) // recursive
      val buf = scala.collection.mutable.ArrayBuffer.empty[(String, Long)]
      while (it.hasNext && buf.size < maxItems) {
        val st = it.next()
        buf += ((st.getPath.toString, st.getLen))
      }
      buf.toSeq
    }

    DATASET_DIRS.foreach { d =>
      println(s"Path: $d | exists=${exists(d)}")
      lsSample(d, 10).foreach { case (p, sz) => println(s"  - $p ($sz bytes)") }
      println()
    }

    // --- 2) Ingest images with binaryFile (recursive lookup) ---
    val jpgDf = spark.read.format("binaryFile")
      .option("recursiveFileLookup", "true")
      .option("pathGlobFilter", "*.jpg")
      .load(DATASET_DIRS: _*)
      .select($"path", $"content")

    val pngDf = spark.read.format("binaryFile")
      .option("recursiveFileLookup", "true")
      .option("pathGlobFilter", "*.png")
      .load(DATASET_DIRS: _*)
      .select($"path", $"content")

    val imgDf = jpgDf.unionByName(pngDf).dropDuplicates("path").cache()
    println("Total images: " + imgDf.count())
    imgDf.show(5, truncate = 100)

    // --- 3) Feature extraction: RGB histogram (32 bins/channel -> 96 dims), L2-normalized ---
    val bins = 32
    val featDim = 3 * bins

    def histFeatures(bytes: Array[Byte]): Option[Vector] = {
      val bais = new ByteArrayInputStream(bytes)
      val imgOpt = Try(ImageIO.read(bais)).toOption
      imgOpt.flatMap { bi =>
        if (bi == null) return None
        val w = bi.getWidth
        val h = bi.getHeight
        if (w <= 0 || h <= 0) return None

        val hist = Array.fill[Double](featDim)(0.0)
        var y = 0
        while (y < h) {
          var x = 0
          while (x < w) {
            val rgb = bi.getRGB(x, y)
            val r = (rgb >> 16) & 0xff
            val g = (rgb >>  8) & 0xff
            val b = (rgb      ) & 0xff
            val br = (r * bins) / 256
            val bg = (g * bins) / 256
            val bb = (b * bins) / 256
            hist(br) += 1.0
            hist(bins + bg) += 1.0
            hist(2*bins + bb) += 1.0
            x += 1
          }
          y += 1
        }
        val norm = math.sqrt(hist.map(v => v*v).sum) + 1e-12
        Some(Vectors.dense(hist.map(_ / norm)))
      }
    }

    // Typed Scala UDF (avoids Spark 3.5 untyped UDF error)
    val histUdf = udf((bytes: Array[Byte]) => histFeatures(bytes).orNull)

    val featDf = imgDf
      .withColumn("features", histUdf(col("content")))
      .filter(col("features").isNotNull)
      .select("path", "features")
      .cache()

    println("Feature rows: " + featDf.count())
    featDf.show(5, truncate = 120)

    // --- 4) Normalize and build LSH index ---
    val normalizer = new Normalizer()
      .setInputCol("features")
      .setOutputCol("features_norm")
      .setP(2.0)

    val featNormDf = normalizer.transform(featDf).select("path", "features_norm").cache()

    val lsh = new BucketedRandomProjectionLSH()
      .setInputCol("features_norm")
      .setOutputCol("hashes")
      .setBucketLength(2.0)
      .setNumHashTables(4)

    val lshModel = lsh.fit(featNormDf)
    val indexedDf = lshModel.transform(featNormDf).cache()
    println("Indexed rows: " + indexedDf.count())
    indexedDf.select("path","hashes").show(3, truncate = 120)

    // --- 5) Similarity search (first image as query) ---
    val query = featNormDf.limit(1).collect()(0)
    val queryPath = query.getAs[String]("path")
    val queryVec = query.getAs[Vector]("features_norm")
    println("Query path: " + queryPath)

    val k = 8
    val nn = lshModel.approxNearestNeighbors(indexedDf, queryVec, k)
    nn.select("path", "distCol").show(k, truncate = 120)

    spark.stop()
  }
}
