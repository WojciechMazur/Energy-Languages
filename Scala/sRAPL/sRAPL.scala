// using scala 3.1
// using lib jRAPL::jRAPL:1.0

package sRAPL

import jRAPL._
import java.io._

def measureEnergyConsumption(fn: () => Unit)(using config: Settings): Unit = {
  withFreshSyncMonitor {
    withConfigPrintStream {
      val warmupConfig = config.warmupConfig
      val label = config.label
      for
        i <- 0 until warmupConfig.iterations
      do
        if warmupConfig.measureWarmup then
          measureEnergyConsumptionSample(s"$label-warmup")(fn)
        else fn()
      end for

      for i <- 0 until 10
      do measureEnergyConsumptionSample(label)(fn)
    }
  }
}

private def measureEnergyConsumptionSample[T](
    label: String
)(fn: () => T)(using output: PrintStream)(using Settings, SyncEnergyMonitor): T = {
  val (sample, result) = executeAndMeasureEnergyConsumption(fn)
  output.println(sample.toCSV(label))
  result
}

private def executeAndMeasureEnergyConsumption[T](
    fn: () => T
): (EnergyDiff, T) = withFreshSyncMonitor{ monitor ?=>
  val before = monitor.getSample()
  val result = fn()
  val after = monitor.getSample()
  EnergyDiff.between(before, after) -> result
}

private def withFreshSyncMonitor[T](fn: SyncEnergyMonitor ?=> T): T = {
  val monitor = SyncEnergyMonitor()
  monitor.activate()
  try fn(using monitor)
  finally monitor.deactivate()
}

private def withConfigPrintStream[T](using
    config: Settings
)(fn: PrintStream ?=> T): T = {
  val MeasurmentCsvHeader = "Name, Package (J), CPU (J), GPU (J), DRAM(J), Time (ms)"
  val (printStream, writeHeader) = config.resultFile match {
    case None => System.out -> true
    case Some(file) =>
      val shouldWriteHeader = file.length() == 0
      PrintStream(FileOutputStream(file, true)) -> shouldWriteHeader
  }
  if (writeHeader)
    printStream.println(MeasurmentCsvHeader)

  try fn(using printStream)
  finally config.resultFile.foreach(_ => printStream.close())
}

extension (diff: EnergyDiff)
  def toCSV(label: String) = {
    val values = label :: List(
      diff.getPackage(),
      diff.getCore(),
      diff.getGpu(),
      diff.getDram(),
      diff.getTimeElapsed().toNanos / 1e6
    ).map(String.format("%.3f", _))

    values.mkString(", ")
  }

case class Settings(
    resultFile: Option[File] = None,
    label: String = "benchmark",
    warmupConfig: WarmupSettings = WarmupSettings()
) {
  def withWarmupConfig(fn: WarmupSettings => WarmupSettings): Settings = {
    copy(warmupConfig = fn(warmupConfig))
  }

  override val toString = {
    s"""Settings {
    |  results file: ${resultFile.getOrElse("<stdout>")},
    |  label:        ${label}
    |  warmupConfig: ${warmupConfig}
    |}""".stripMargin
  }
}

// Config model
case class WarmupSettings(
    iterations: Int = 10,
    measureWarmup: Boolean = false
) {
  override val toString = {
    s"""Warmup Settings {
      |  iterations:      $iterations
      |  measure warmup:  $measureWarmup
      |}""".stripMargin
  }
}

object Settings {
  def parse(args: Seq[String]): Settings = {
    args
      .collect {
        case str if !str.isEmpty => str.trim
      } // Split argument word into groups
      .foldLeft(List.empty[List[String]]) {
        case (Nil, word) => List(word :: Nil)
        case (groups @ lastGroup :: prevGroups, word) =>
          if word.startsWith("--") then // start of new group
            val newGroup = word :: Nil
            newGroup :: groups
          else
            val updatedGroup = lastGroup :+ word
            updatedGroup :: prevGroups
          end if
      }
      .reverse
      // Parse groups of arguments
      .foldRight(Settings()) {
        case ("--output" :: path :: Nil, config) =>
          config.copy(resultFile = Some(new File(path).getAbsoluteFile))
        case ("--label" :: name :: Nil, config) =>
          config.copy(label = name)
        // Warmup config
        case ("--warmup-iterations" :: n :: Nil, config) =>
          config.withWarmupConfig { _.copy(iterations = n.toInt) }
        case ("--measure-warmup" :: args, config) =>
          val shouldMeasure = args.headOption.fold(false)(_.toBoolean)
          config.withWarmupConfig { _.copy(measureWarmup = shouldMeasure) }
        case (other, config) =>
          System.err.println(s"Unknown argument: $other")
          config
      }
  }
}
