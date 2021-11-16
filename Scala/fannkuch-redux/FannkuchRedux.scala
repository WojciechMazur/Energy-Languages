import scala.annotation.tailrec
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object FannkuchRedux {
  val BlocksNum = 24
  val ArrSize = 16

  def main(args: Array[String]) = {
    val n = args.head.toInt
    run(n)
  }

  def run(n: Int): Unit = {
    val (checkSum, maxFlips) =
      if (n < 0 || n > 20) (-1, -1)
      else if (n <= 1) (0, 0)
      else fannkuch(n)

    printf("%d\nPfannkuchen(%d) = %d\n", checkSum, n, maxFlips)
  }

  type FlipsAndCheck = (Int, Int)
  private def fannkuch(n: Int): FlipsAndCheck = {
    val factorials = (1 to n).scan(1)(_ * _).toArray
    val maxPermutation = factorials(n)
    val (blocks, blockSize) =
      if (maxPermutation < BlocksNum) (1, maxPermutation)
      else {
        val adjustment = if (maxPermutation % BlocksNum == 0) 0 else 1
        (BlocksNum + adjustment, maxPermutation / BlocksNum)
      }

    def computeBlock(blockIdx: Int): FlipsAndCheck = {
      val initialIdx = blockIdx * blockSize
      val lastPermutationInBlock =
        Math.min(initialIdx + blockSize, maxPermutation) - 1

      val count, tmp = Array.fill(ArrSize)(0)
      val current = Array.iterate(0, ArrSize)(_ + 1)

      @tailrec
      def initialPermute(idx: Long, i: Int): Unit = {
        if (i > 0) {
          val factorial = factorials(i)
          val d = (idx / factorial).toInt
          val nextIdx = idx % factorial

          count(i) = d
          System.arraycopy(current, 0, tmp, 0, ArrSize)
          var j = 0
          while (j <= i) {
            val jd = j + d
            val idx =
              if (jd <= i) jd
              else jd - i - 1
            current(j) = tmp(idx)
            j += 1
          }

          initialPermute(nextIdx, i - 1)
        }
      }

      def doFlip(): Int = {
        var flips = 1
        var firstValue = current(0)

        while (tmp(firstValue) != 0) {
          val newFirstValue = {
            val prev = tmp(firstValue)
            tmp(firstValue) = firstValue
            prev
          }
          if (firstValue > 2) {
            // Reverse elements up to firstValue
            var idx = 1
            while (idx <= firstValue / 2) {
              val tailIdx = firstValue - idx
              val head = tmp(idx)
              tmp(idx) = tmp(tailIdx)
              tmp(tailIdx) = head
              idx += 1
            }
          }
          firstValue = newFirstValue
          flips += 1
        }
        flips
      }

      def permute(): Unit = {
        @tailrec
        def loop(i: Int, firstValue: Int): Unit = {
          if (count(i) >= i) {
            count(i) = 0
            val next = i + 1
            val newFirstValue = current(1)
            current(0) = newFirstValue
            var j = 1
            while (j < next) {
              current(j) = current(j + 1)
              j += 1
            }
            current(next) = firstValue
            loop(next, newFirstValue)
          } else {
            count(i) += 1
          }
        }

        val firstValue = current(1)
        current(1) = current(0)
        current(0) = firstValue
        loop(1, firstValue)
      }

      @tailrec
      def loop(
          permutationIdx: Long,
          checkSum: Int,
          maxFlips: Int
      ): FlipsAndCheck = {
        val result @ (newCheckSum, newMaxFlips) = {
          val firstValue = current(0)
          if (firstValue <= 0) (checkSum, maxFlips)
          else {
            System.arraycopy(current, 0, tmp, 0, ArrSize)
            val flips = doFlip()
            val newCheckSum = {
              val delta = if (permutationIdx % 2 == 0) flips else -flips
              checkSum + delta
            }
            val newMaxFlips = Math.max(maxFlips, flips)
            (newCheckSum, newMaxFlips)
          }
        }

        if (permutationIdx >= lastPermutationInBlock) result
        else {
          permute()
          loop(permutationIdx + 1, newCheckSum, newMaxFlips)
        }
      }

      initialPermute(initialIdx, n - 1)
      loop(initialIdx, 0, 0)
    }

    val tasks = for {
      blockIdx <- 0 until blocks
    } yield Future {
      computeBlock(blockIdx)
    }

    val result = Future.reduceLeft(tasks) {
      case ((lCheckSum, lMaxFlips), (rCheckSum, rMaxFlips)) =>
        val checkSum = lCheckSum + rCheckSum
        val maxFlips = lMaxFlips max rMaxFlips
        (checkSum, maxFlips)
    }

    Await.result(result, Duration.Inf)
  }
}
