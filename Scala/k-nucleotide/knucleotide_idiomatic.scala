/* The Computer Language Benchmarks Game
   http://benchmarksgame.alioth.debian.org/

   Contributed by Jimmy Lu
 */

import java.io.InputStream

import scala.annotation.switch
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.io.Source
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object knucleotide {
  def main(args: Array[String]): Unit = {
    run(System.in)
  }

  def run(input: java.io.InputStream): Unit = {
    val sequence = extractSequence(input, "THREE")
    val tasks = Future
      .traverse(Seq(1, 2, 3, 4, 6, 12, 18))(count(sequence, _))

    val (cs1, cs2) = Await
      .result(tasks, Duration.Inf)
      .splitAt(2)

    for ((c, i) <- cs1.zipWithIndex) {
      for ((s, freq) <- frequency(i + 1, c))
        printf("%s %.3f%n", s.toUpperCase, freq * 100)
      println()
    }
    for {
      (c, s) <- cs2.zip(
        Seq("ggt", "ggta", "ggtatt", "ggtattttaatt", "ggtattttaatttatagt")
      )
    } {
      val n = c.get(encode(s.getBytes, 0, s.length)).fold(0)(_.n)
      printf("%d\t%s%n", n, s.toUpperCase)
    }
  }

  def extractSequence(input: InputStream, name: String): Array[Byte] = {
    val description = ">" + name
    val builder = Array.newBuilder[Byte]
    builder.sizeHint(4 << 24)
    val lines = Source
      .fromInputStream(input)
      .getLines()
      .dropWhile {
        !_.startsWith(description)
      }
      .drop(1)
      .takeWhile(!_.startsWith(">"))
    lines.foreach(builder ++= _.getBytes)
    builder.result()
  }

  class Counter(var n: Int)

  def count(
      sequence: Array[Byte],
      length: Int
  ): Future[mutable.LongMap[Counter]] = Future {
    val counters = mutable.LongMap.empty[Counter]
    val end = sequence.length - length + 1
    for (i <- 0 until end) {
      val key = encode(sequence, i, length)
      val counter = counters.getOrElseUpdate(key, new Counter(0))
      counter.n += 1
    }
    counters
  }

  def frequency(
      length: Int,
      count: collection.Map[Long, Counter]
  ): Iterable[(String, Double)] = {
    implicit val ordering: Ordering[(String, Double)] =
      Ordering
        .by[(String, Double), Double](_._2)
        .reverse
    val builder = SortedSet.newBuilder[(String, Double)]
    val sum = count.values.foldLeft(0.0)(_ + _.n)
    for ((k, v) <- count) {
      val key = new String(decode(k, length))
      val value = v.n / sum
      builder += ((key, value))
    }
    builder.result()
  }

  def encode(sequence: Array[Byte], offset: Int, length: Int): Long = {
    // assert(length <= 32)
    0.until(length)
      .foldLeft(0L) { case (n, i) =>
        val m = (sequence(offset + i): @switch) match {
          case 'a' => 0
          case 'c' => 1
          case 'g' => 2
          case 't' => 3
        }
        n << 2 | m
      }
  }

  def decode(n: Long, length: Int): Array[Byte] = {
    val bs = Array.ofDim[Byte](length)
    0.until(length)
      .reverse
      .foldLeft(n) { case (n, i) =>
        val value = ((n & 3).toInt: @switch) match {
          case 0 => 'a'
          case 1 => 'c'
          case 2 => 'g'
          case 3 => 't'
        }
        bs(i) = value.toByte
        n >> 2
      }
    bs
  }
}
