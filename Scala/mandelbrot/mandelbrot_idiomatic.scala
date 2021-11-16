/* The Computer Language Benchmarks Game
   http://benchmarksgame.alioth.debian.org/
   original contributed by Isaac Gouy
   made to use single array and parallelized by Stephen Marsh
   converted to Scala 2.8 by Rex Kerr
   made to use parallel collections and removed synchronized blocks by Steve Vickers
 */
import java.io.BufferedOutputStream
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object mandelbrot {
  final val limitSquared = 4.0
  final val max = 50

  def main(args: Array[String]) = {
    val size = args(0).toInt
    run(size)
  }
  
  def run(size: Int): Unit = {
    val bytesPerRow = (size + 7) / 8 // ceiling of (size / 8)
    val writer = new BufferedOutputStream(System.out)
    val tasks = Future.traverse(0 until size) { idx =>
      Future { calculateRow(size, bytesPerRow)(idx) }
    }

    println(s"""P4
    |$size $size""".stripMargin)

    Await
      .result(tasks, Duration.Inf)
      .foreach(writer.write(_))
    writer.close()
  }

  def calculateRow(size: Int, bytesPerRow: Int)(y: Int): Array[Byte] = {
    val ci = 2.0 * y / size - 1.0
    val bitmap = new Array[Byte](bytesPerRow);

    var bits = 0
    var bitnum = 0
    var aindex = 0

    for {
      x <- 0 until size
      cr = 2.0 * x / size - 1.5
    } {
      var tr, ti = 0.0
      locally {
        var zr, zi = 0.0
        var j = max
        while ({
          zi = 2.0 * zr * zi + ci
          zr = tr - ti + cr
          ti = zi * zi
          tr = zr * zr
          j = j - 1
          !(tr + ti > limitSquared) && j > 0
        }) ()
      }

      bits = bits << 1
      if (!(tr + ti > limitSquared)) bits += 1
      bitnum += 1

      if (x == size - 1) {
        bits = bits << (8 - bitnum)
        bitnum = 8
      }

      if (bitnum == 8) {
        bitmap(aindex) = bits.toByte
        aindex += 1
        bits = 0
        bitnum = 0
      }
    }

    bitmap
  }
}
