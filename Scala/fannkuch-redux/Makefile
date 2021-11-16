# Info
benchmarkName = fannkuch-redux
mainClass = FannkuchRedux
input-test = 7
input-benchmark = 12
input-jvm-runner= 12
warmup-iterations=5
warmup-measure=true
output = 

# Config
mode = 
configName = Scala

# Filenames
fast      = FannkuchRedux.scala
idiomatic = FannkuchRedux_idiomatic.scala
runner    = JvmRunner.scala

default    = ${fast}
impl-file  = ${default} 

# Environment
Scala-library-version=2.13.6
Scala3-library-version=3.1.0
sRAPLPath  = ${shell pwd}/../sRAPL
javaPath  = java
scalacClasspath = -cp ${sRAPLPath}/sRAPL.jar:${sRAPLPath}/jRAPL-1.0.jar
classpath = -cp .:${sRAPLPath}/sRAPL.jar:${sRAPLPath}/jRAPL-1.0.jar:${SCALA_HOME}/lib/scala-library-${Scala-library-version}.jar:${SCALA_HOME}/lib/scala3-library_3-${Scala3-library-version}.jar
# Logic (do not edit)
ifeq ($(mode),idiomatic)
  	impl-file=${idiomatic}
endif

compile:
	${scalacPath} -d . ${scalacClasspath} ${impl-file} ${runner}

test:
	${javaPath} ${classpath} ${mainClass} ${input-test} ${output} | diff test-output.txt -

run:
	${javaPath} ${classpath} ${mainClass} ${input-benchmark} ${output}

measure:
	sudo modprobe msr
	sudo ../../RAPL/main "${javaPath} ${classpath} ${mainClass} ${input-benchmark} ${output}" ${configName} ${benchmarkName}

measureWithWarmup:
	make compile
	sudo modprobe msr
	sudo ${javaPath} ${classpath} \
   run ${input-jvm-runner} \
   --label ${benchmarkName} \
   --measure-warmup ${warmup-measure} \
   --warmup-iterations ${warmup-iterations} \
   --output ../${configName}-warmedUp.csv \
   ${output} 
 
mem:
	/usr/bin/time -v ${javaPath} ${mainClass} ${input-benchmark} ${output}

valgrind:
	valgrind --tool=massif --stacks=yes ${javaPath} ${mainClass} ${input-benchmark} ${output}

clean:
	rm -rf *.class *.tasty
