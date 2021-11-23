/* The Computer Language Benchmarks Game
   http://benchmarksgame.alioth.debian.org/
  based on original contributed by Isaac Gouy
  updated for 2.9 and optimized by Rex Kerr
 */

import java.io._

object fasta {
  val ALU =
    ("GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" +
      "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
      "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
      "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
      "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
      "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
      "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA").getBytes

  val IUB = (
    "acgtBDHKMNRSVWY".getBytes,
    (Array(0.27, 0.12, 0.12, 0.27) ++ Array.fill(11)(0.02))
  )
  val HomoSapiens = (
    "acgt".getBytes,
    Array(0.3029549426680, 0.1979883004921, 0.1975473066391, 0.3015094502008)
  )

  def main(args: Array[String]) = {
    val n = args(0).toInt
    run(n)
  }

  def run(n: Int): Unit = {
    val s = new FastaOutputStream(System.out)

    s.write(">ONE Homo sapiens alu\n".getBytes)
    s.writeRepeating(ALU, n * 2)

    s.write(">TWO IUB ambiguity codes\n".getBytes)
    s.writeRandom(IUB, n * 3)

    s.write(">THREE Homo sapiens frequency\n".getBytes)
    s.writeRandom(HomoSapiens, n * 5)

    s.close
  }
}

// Extend the Java BufferedOutputStream class
class FastaOutputStream(out: OutputStream) extends BufferedOutputStream(out) {
  private final val LineLength = 60
  private final val BufLines = 100
  private final val EOL = '\n'.toByte

  def writeRepeating(alu: Array[Byte], length: Int) = {
    val limit = alu.length
    var n = length
    var idx = 0
    while (n > 0) {
      val m = n.min(LineLength)
      var i = 0
      while (i < m) {
        if(idx == limit) idx = 0
        bufferedWrite(alu(idx))
        idx += 1
        i += 1
      }

      write(EOL)
      n -= LineLength
    }
  }

  def writeRandom(distribution: (Array[Byte], Array[Double]), length: Int) = {
    val (symb, probability) = distribution
    val hash = buildHash(symb, probability)
    val buffer = Array.fill(BufLines * (LineLength + 1))(EOL)
    val buffers = length / LineLength / BufLines
    for (i <- 0 until buffers) {
      for (j <- 0 until BufLines) {
        for (k <- 0 until LineLength) {
          val v = random()
          buffer(j * (LineLength + 1) + k) = hash(v)
        }
      }
      write(buffer, 0, (LineLength + 1) * BufLines)
    }

    val lines = length / LineLength - buffers * BufLines
    for (j <- 0 until lines) {
      for (k <- 0 until LineLength) {
        val v = random()
        buffer(j * (LineLength + 1) + k) = hash(v)
      }
    }
    val partials = length - LineLength * lines - buffers * BufLines * LineLength
    for (k <- 0 until partials) {
      val v = random()
      buffer(lines * (LineLength + 1) + k) = hash(v)
    }
    write(buffer, 0, lines * (LineLength + 1) + partials)

    if (length % LineLength != 0) write(EOL)
  }

  private def buildHash(symb: Array[Byte], probability: Array[Double]): Array[Byte] = {
    val result = Array.ofDim[Byte](IM)
    val len = symb.length
    var sum = probability(0)
    var i, j = 0
    while (i < IM && j < len) {
      val r = 1.0 * i / IM
      if (r >= sum) {
        j += 1
        sum += probability(j)
      }
      result(i) = symb(j)
      i += 1
    }
    return result
  }

  private def bufferedWrite(b: Byte): Unit = {
    if (count < buf.length) {
      buf(count) = b
      count += 1
    } else {
      write(b) // flush buffer
    }
  }

  private final val IM = 139968
  private final val IA = 3877
  private final val IC = 29573
  private final val IMinv = 1.0 / IM
  private var seed = 42

  private final def random() = {
    seed = (seed * IA + IC) % IM
    seed
  }
}
