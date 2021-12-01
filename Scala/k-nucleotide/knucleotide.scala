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
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object knucleotide {
  def main(args: Array[String]): Unit = {
    run(System.in)
  }

  def run(input: java.io.InputStream): Unit = {
    val sequence = extractSequence(input, "THREE")
    val tasks = Future.sequence {
      Seq(18, 12, 6, 4, 3, 2, 1)
        .map(count(sequence, _))
        .reverse
    }

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
      val n = c.get(encode(toCodes(s.getBytes), 0, s.length)).fold(0)(_.n)
      printf("%d\t%s%n", n, s.toUpperCase)
    }
  }

  def extractSequence(input: InputStream, name: String): Array[Byte] = {
    val description = ">" + name
    val builder = Array.newBuilder[Byte]
    builder.sizeHint(4 << 24)
    val in = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1))
    var line: String = null
    while ({
      line = in.readLine()
      !line.startsWith(description)
    }) ()
    while ({
      line = in.readLine()
      line != null && !line.startsWith(">")
    }) {
      builder.addAll(line.getBytes)
    }
    toCodes(builder.result)
  }

  class Counter(var n: Int)

  def count(
      sequence: Array[Byte],
      length: Int
  ): Future[mutable.LongMap[Counter]] = Future {
    val counters = mutable.LongMap.empty[Counter]
    val end = sequence.length - length + 1
    var i = 0
    while (i < end) {
      val key = encode(sequence, i, length)
      val counter = counters.getOrElseUpdate(key, new Counter(0))
      counter.n += 1
      i += 1
    }
    counters
  }

  def frequency(
      length: Int,
      count: collection.Map[Long, Counter]
  ): Iterable[(String, Double)] = {
    val builder =
      SortedSet.newBuilder[(String, Double)](
        Ordering
          .by[(String, Double), Double](- _._2)
      )
    val sum = count.values
      .foldLeft(0.0)(_ + _.n)
    for ((k, v) <- count) {
      val key = new String(decode(k, length))
      val value = v.n / sum
      builder += ((key, value))
    }
    builder.result()
  }

  private val codes = Array[Byte](-1, 0, -1, 1, 3, -1, -1, 2)
  private val nucleotides = Array[Char]('A', 'C', 'G', 'T')

  // Convert array of nucleotides to codes (0 = A, 1 = C, 2 = G, 3 = T)
  def toCodes(sequence: Array[Byte]): Array[Byte] = {
    val length = sequence.length
    val result = new Array[Byte](length)
    var i = 0
    while (i < length) {
      result(i) = codes(sequence(i) & 0x7)
      i += 1
    }
    result
  }

  def encode(sequence: Array[Byte], offset: Int, length: Int): Long = {
    // assert(length <= 32)
    var n = 0L
    var i = offset
    while (i < offset + length) {
      n <<= 2
      n |= sequence(i)
      i += 1
    }
    n
  }

  def decode(n: Long, length: Int): Array[Char] = {
    val bs = new Array[Char](length)
    var nn = n
    var i = length - 1
    while (i >= 0) {
      bs(i) = nucleotides((nn & 3).toInt)
      nn >>= 2
      i -= 1
    }
    bs
  }
}
