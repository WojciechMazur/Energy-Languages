# Prerequisites

Benchmarks on it's own are Scala version agnostic and would compile with both Scala 3 and Scala 2.13. JVM runners however are using `@main()` annotation present only in Scala 3. 

None of the benchmarks needs explicit external dependendencies other then Scala standard library.

RAPL library needs an admin/sudo privilage in order to collect measurments.
RAPL library can collect only measurments from select list of supported Intel CPU architectures, if the given CPU measurment is not supported result would contain empty string or "-1"

# Makefile tasks
Makefile for each project contains commands standard for all EnergyLanguages benchmarks, that is:
- `compile` - compiles benchmarks and JVM runners,
- `run` - perform single run of compiled benchmark with default argument
- `measure` - use RAPL library to run compiled benchmark and collect measurments
- `mem` - perform single run of benchmark and collect measurments using /usr/bin/time eg. peak memory usage, however for Scala it does write outputs to defined file in csv format instead printing to stdout. 

Additionally, we define additional tasks that are specific to Scala:
- `clean` - remove all temporal files (*.tasty, *.class)
- `test` - run benchmark with predefined output and check if it produces correct output
- `measureWithWarmup` - use sRAPL library to run benchmarks using single JVM instance (measure starts new instance for each iteration)

# Makefile configuration
Makefile contains settings that can be used when running them:
- `configName` - Name of the config to use, results with written to file with name derived from this value.
- `warmup-iterations` - number of initial warmup iterations (used only in `measureWithWarmup`)
- `warmup-measure` - should collect measurments when performing warmup iterations, results would be logged using `<benchmark-name>_warmup` label in the results (used only in `measureWithWarmup`)
- `output` - pass output redirect options, eg `output=>/dev/null` to ignore output as it logging to console can significetlly slow down execution
- `mode` - use non standard benchmark implementation (applies only to `compile`), supported modes: 
  - `mode=idiomatic` - Use Scala idiomatic implementation
- `scalaPath` - path to `scala` binary. Default value assumes that `scala` has been installed via SDKMAN.
- `scalacPath` - path to `scalac` binary

# Makefiles management
In general Makefiles should not be edited manually, instead `genMakefile.scala` script should be used. This ensures that they would stay consistent thanks to usage of common template. 
Recommended way of running `genMakefile.scala` is via [scala-cli](https://scala-cli.virtuslab.org/) eg. `scala-cli genMakefile.scala` - running this command would update Makefiles in all benchmark directories. Alternativelly it can be compiled and runned manually using `scalac` and `scala`/`java` programs. 
`genMakefile.scala` needs Scala in version 3.0.0 or higher.

# Running benchmarks
All benchmarks need to be compiled before running them. 

Before running benchmarks make sure that `RAPL` and `sRAPL` libraries are published
```sh
  # pwd=EnergyLanguages
  make -C RAPL
  make -C Scala/sRAPL
``` 

## Running single benchmark
To run single benchmark use Makefile directly using `make` command.  
  ```sh
  cd EnergyLanugages/Scala 
  #make -C <benchmark directory> <commands*>
  make -C binary-trees clean compile test measure measureWithWarmup
  # alternatively you can run it from inside of benchmark directory
  cd binary-trees
  make clean compile test measure measureWithWarmup 
  # run idiomatic implementation and set change config name
  make clean compile test measure measureWithWarmup mode=idiomatic configName=Scala-idiomatic
  ```
It is recommended to redirect standard output from benchmarks as writing to the console can slow down execution
```
  make clean compile test
  make measure measureWithWarmup output=>/dev/null
```
  
## Running all benchmarks
To run all benchmarks use python script `compile_all.py` - it does iterate over all directories containing Makefile. 
```sh
 #`python3 compile_all.py <command> <make-opts*>`
 # pwd=EnergyLanuguages/Scala
 python3 compile_all.py clean
 python3 compile_all.py compile mode=idiomatic
 python3 compile_all.py test
 python3 compile_all.py measure configName=Scala-idiomatic
 python3 compile_all.py measureWithWarmup configName=Scala-idiomatic
```

Results of `measure*` would be stored in given files in csv format with file name correspending to command:
- `measure` - <configName>.csv
- `measure` - <configName>-warmuped.csv

Eg. after running `make measure measureWithWarmup configName=MyTests` we would find 2 files in `Scala` directory: 
- `MyTests.csv`
- `MyTests-warmuped.csv`

## Additional scripts
All the following scripts use Scala3 and are recommened to be run using scala-cli

- `aggregate.scala`:
  - args: <measurments results csv file> ?<output directory>
  - reads given csv file, normalizes it's output and creates aggreation for avergage, variance and standard deviation for each measurments
  - outputs 3 files in current directory or inside optinal output directory passed as second argument
- `aggregateByCollumn.scala`
  - args: <collumn> <aggregated csv file>*
  - creates aggregation for collumn with given number for passed csv files (created by `aggregate.scala` script).
  - writes to standard output aggregation in csv format