import java.io._

@main def merge(column: Int, fileNames: String*): Unit = {
  case class Content(file: String, raws: Vector[Raw])
  case class Raw(label: String, value: String)
  val columnDelimiter = ","
  val hasHeader: Boolean = true
  if(fileNames.isEmpty){
    sys.error("No inputs provided")
  }

  val files = fileNames.map(new File(_))
  .map{ f => 
    Content(f.getName(), io.Source.fromFile(f)
    .getLines
    .filterNot(_.isEmpty)
    .drop(if(hasHeader) 1 else 0)
    .map(_.split(columnDelimiter).map(_.trim()))
    .map(arr => Raw(arr(0), arr(column)))
    .toVector
    .sortBy(_.label)
    )
  }.toVector
  .sortBy(_.file)

  val maxLength = files.map(_.raws.length).max
  val labels = files.flatMap(_.raws).map(_.label).distinct.sorted

  println(("label" +: files.map(_.file)).mkString(", "))
  labels.foreach{ label => 
    val values = files.map(_.raws
      .find(_.label == label)
      .map(_.value)
      .getOrElse("-1")
      )
    println((label +: values).mkString(", "))
  }
}
