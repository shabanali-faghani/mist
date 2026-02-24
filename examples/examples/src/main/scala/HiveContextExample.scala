//import mist.api._
//import mist.api.encoding._
//import mist.api.data.{JsData, JsList, JsString}
//import mist.api.MistFn
//import org.apache.spark.sql.{Dataset, Row}
//
//object HiveContextExample extends MistFn {
//
//  // Custom encoder for Dataset[Row]
//  implicit val dsEncoder: JsEncoder[Dataset[Row]] = JsEncoder { ds =>
//    JsList(ds.collect().map(row => JsString(row.mkString(","))).toList)
//  }
//
//  def handle = withHiveContext { (hive: HiveContext) =>
//    import hive.implicits._
//    val ds: Dataset[Row] = hive.sql("SELECT * FROM some_table")
//    ds
//  }
//}