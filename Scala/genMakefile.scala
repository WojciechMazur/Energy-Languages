// using scala 3.1.0
import scala.annotation._
import java.nio.file._
import java.io.File

@main()
def genMakefile(): Unit = {
  for {
    config <- configs
    file = config.directory.resolve("Makefile")
  } {
    println(s"Generating ${config.benchmarkName}")
    Files.write(
      file,
      template(config).getBytes(),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
  }
}

val cwd = Paths.get(".").toRealPath()
val configs = List(
  Config(
    benchmarkName = "binary-trees",
    mainClass = "BinaryTrees",
    inputs = Inputs(test = "10", benchmark = "21"),
    files = Filenames(
      fast = "BinaryTrees.scala",
      idiomatic = "BinaryTrees.scala"
    )
  ),
  Config(
    benchmarkName = "fannkuch-redux",
    mainClass = "FannkuchRedux",
    inputs = Inputs(test = "7", benchmark = "12"),
    files = Filenames(
      fast = "FannkuchRedux.scala",
      idiomatic = "FannkuchRedux_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "fasta",
    mainClass = "fasta",
    inputs = Inputs(test = "1000", benchmark = "25000000"),
    files = Filenames(
      fast = "fasta.scala",
      idiomatic = "fasta_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "k-nucleotide",
    mainClass = "knucleotide",
    inputs = Inputs(
      test = "0 < ../fasta/test-output.txt",
      benchmark = "0 < ../../knucleotide-input25000000.txt -J-Xmx512M"
    ),
    files = Filenames(
      fast = "knucleotide.scala",
      idiomatic = "knucleotide_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "mandelbrot",
    mainClass = "mandelbrot",
    inputs = Inputs(test = "200", benchmark = "16000"),
    files = Filenames(
      fast = "mandelbrot.scala",
      idiomatic = "mandelbrot_idiomatic.scala"
    ),
    testCommand = s"cmp test-output.txt -"
  ),
  Config(
    benchmarkName = "n-body",
    mainClass = "nbody",
    inputs = Inputs(test = "1000", benchmark = "50000000"),
    files = Filenames(
      fast = "nbody.scala",
      idiomatic = "nbody_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "pidigits",
    mainClass = "pidigits",
    inputs = Inputs(test = "30", benchmark = "10000"),
    files = Filenames(
      fast = "pidigits.scala",
      idiomatic = "pidigits_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "regex-redux",
    mainClass = "regexredux",
    inputs = Inputs(
      test = "0 < ../fasta/test-output.txt",
      benchmark = "0 < ../../regexredux-input5000000.txt -J-Xmx1G"
    ),
    files = Filenames(
      fast = "regexredux.scala",
      idiomatic = "regexredux_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "reverse-complement",
    mainClass = "revcomp",
    inputs = Inputs(
      test = "0 < ../fasta/test-output.txt",
      benchmark = "0 < ../../revcomp-input25000000.txt -J-Xmx512M"
    ),
    files = Filenames(
      fast = "revcomp.scala",
      idiomatic = "revcomp_idiomatic.scala"
    )
  ),
  Config(
    benchmarkName = "spectral-norm",
    mainClass = "spectralnorm",
    inputs = Inputs(test = "100", benchmark = "5500"),
    files = Filenames(
      fast = "spectralnorm.scala",
      idiomatic = "spectralnorm_idiomatic.scala"
    )
  )
)

case class Config(
    benchmarkName: String,
    mainClass: String,
    inputs: Inputs,
    files: Filenames,
    testCommand: String = "diff test-output.txt -"
) {
  def directory: Path = cwd.resolve(benchmarkName)
}

case class Inputs(
    test: String,
    benchmark: String,
){
  def jvmRunner: String = benchmark.stripPrefix("0 < ")
}
case class Filenames(
    fast: String,
    idiomatic: String
)

def template(ctx: Config): String = {
  import ctx._
  s"""# Info
  |benchmarkName = ${benchmarkName}
  |mainClass = ${mainClass}
  |input-test = ${inputs.test}
  |input-benchmark = ${inputs.benchmark}
  |input-jvm-runner= ${inputs.jvmRunner}
  |warmup-iterations=5
  |warmup-measure=true
  |output = 
  |
  |# Config
  |mode = 
  |configName = Scala
  |
  |# Filenames
  |fast      = ${files.fast}
  |idiomatic = ${files.idiomatic}
  |runner    = JvmRunner.scala
  |
  |default    = $${fast}
  |impl-file  = $${default} 
  |
  |# Environment
  |Scala-library-version=2.13.6
  |Scala3-library-version=3.1.0
  |sRAPLPath  = $${shell pwd}/../sRAPL
  |javaPath  = java
  |scalacClasspath = -cp $${sRAPLPath}/sRAPL.jar:$${sRAPLPath}/jRAPL-1.0.jar
  |classpath = -cp .:$${sRAPLPath}/sRAPL.jar:$${sRAPLPath}/jRAPL-1.0.jar:$${SCALA_HOME}/lib/scala-library-$${Scala-library-version}.jar:$${SCALA_HOME}/lib/scala3-library_3-$${Scala3-library-version}.jar
  |# Logic (do not edit)
  |ifeq ($$(mode),idiomatic)
  |  	impl-file=$${idiomatic}
  |endif
  |
  |compile:
  |	$${scalacPath} -d . $${scalacClasspath} $${impl-file} $${runner}
  |
  |test:
  |	$${javaPath} $${classpath} $${mainClass} $${input-test} $${output} | ${testCommand}
  |
  |run:
  |	$${javaPath} $${classpath} $${mainClass} $${input-benchmark} $${output}
  |
  |measure:
  |	sudo modprobe msr
  |	sudo ../../RAPL/main "$${javaPath} $${classpath} $${mainClass} $${input-benchmark} $${output}" $${configName} $${benchmarkName}
  |
  |measureWithWarmup:
  |${'\t'}make compile
	|${'\t'}sudo modprobe msr
  |${'\t'}sudo $${javaPath} $${classpath} \\
  |   run $${input-jvm-runner} \\
  |   --label $${benchmarkName} \\
	|   --measure-warmup $${warmup-measure} \\
	|   --warmup-iterations $${warmup-iterations} \\
	|   --output ../$${configName}-warmedUp.csv \\
	|   $${output} 
  | 
  |mem:
  |	/usr/bin/time -v $${javaPath} $${mainClass} $${input-benchmark} $${output}
  |
  |valgrind:
  |	valgrind --tool=massif --stacks=yes $${javaPath} $${mainClass} $${input-benchmark} $${output}
  |
  |clean:
  |	rm -rf *.class *.tasty
  |""".stripMargin
}