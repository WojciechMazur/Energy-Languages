import java.io._
import java.nio.file.Paths

@main def normalize(path: String, _outputDir: String): Unit = {
  val inputFile = new File(path)
  val outputDir = new File(_outputDir)
  outputDir.mkdirs

  io.Source
    .fromFile(inputFile)
    .getLines
    .map(_.replace(";", ",").split(",").map(_.trim).toSeq)
    .map(ResultRaw.fromSeq)
    .toSeq
    .groupBy(_.benchmarkName)
    .map { case (benchmarkName, results) =>
      Seq(
        "avg" -> ResultRaw.aggregate(benchmarkName, results)(mean),
        "variance" -> ResultRaw.aggregate(benchmarkName, results)(variance),
        "stddev" -> ResultRaw.aggregate(benchmarkName, results)(stdDev)
      )
    }
    .flatten
    .groupMap(_._1)(_._2)
    .foreach { case (aggregation, results) =>
      val out =
        FileOutputStream(
          Paths.get(
            outputDir.getAbsolutePath,
            s"${inputFile.getName.stripSuffix(".csv")}-$aggregation.csv"
          ).toFile
        )
      def writeLine(str: String) = out.write((str + "\n").getBytes)
      writeLine("benchmark-name,total (J),CPU (J),GPU (J), DRAM (J), time (ms)")
      results.toSeq.sortBy(_.benchmarkName).map(_.toCSV).foreach(writeLine)
      out.close()
    }
}

case class ResultRaw(
    benchmarkName: String,
    total: Double,
    cpu: Double,
    gpu: Double,
    dram: Double,
    time: Double
) {
  def toCSV = {
    (benchmarkName :: List(total, cpu, gpu, dram, time).map(
      String.format("%.3f", _)
    ))
      .mkString(",")
  }
}

object ResultRaw {
  def aggregate(name: String, results: Seq[ResultRaw])(
      fn: Seq[Double] => Double
  ): ResultRaw = {
    ResultRaw(
      benchmarkName = name,
      total = fn(results.map(_.total)),
      cpu = fn(results.map(_.cpu)),
      gpu = fn(results.map(_.gpu)),
      dram = fn(results.map(_.dram)),
      time = fn(results.map(_.time))
    )
  }
  def fromSeq(raw: Seq[String]): ResultRaw = {
    def doubleOrZero(str: String): Double = str.toDoubleOption.getOrElse(0)
    ResultRaw(
      benchmarkName = raw(0),
      total = doubleOrZero(raw(1)),
      cpu = doubleOrZero(raw(2)),
      gpu = doubleOrZero(raw(3)),
      dram = doubleOrZero(raw(4)),
      time = doubleOrZero(raw(5))
    )
  }
}

import Numeric.Implicits._
def mean[T: Numeric](xs: Iterable[T]): Double = xs.sum.toDouble / xs.size

def variance[T: Numeric](xs: Iterable[T]): Double = {
  val avg = mean(xs)

  xs.map(_.toDouble).map(a => math.pow(a - avg, 2)).sum / xs.size
}

def stdDev[T: Numeric](xs: Iterable[T]): Double = math.sqrt(variance(xs))
