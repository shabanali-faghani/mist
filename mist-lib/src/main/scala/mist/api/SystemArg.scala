package mist.api

import mist.api.data.JsMap
import org.apache.spark.{SparkContext, SparkSessionUtils}
import org.apache.spark.api.java.{JavaRDD, JavaSparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, DataFrameReader, Dataset, Encoder, ExperimentalMethods, Row, RuntimeConfig, SQLContext, SQLImplicits, SparkSession, TableValuedFunction, UDFRegistration}
import org.apache.spark.sql.catalog.Catalog
import org.apache.spark.sql.internal.{SessionState, SharedState}
import org.apache.spark.sql.sources.BaseRelation
import org.apache.spark.sql.streaming.{DataStreamReader, StreamingQueryManager}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.ExecutionListenerManager
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.JavaStreamingContext

import java.net.URI
import java.{lang, util}
import scala.reflect.runtime.universe

trait SystemArg[A] extends ArgDef[A] {
  final def validate(params: JsMap): Extraction[Unit] = Extracted(())
}

object SystemArg {

  def apply[A](tags: Seq[String], f: => Extraction[A]): ArgDef[A] = new SystemArg[A] {
    override def extract(ctx: FnContext): Extraction[A] = f
    override def describe() = Seq(InternalArgument(tags))
  }

  def apply[A](tags: Seq[String], f: FullFnContext => Extraction[A]): ArgDef[A] = new SystemArg[A] {
    override def extract(ctx: FnContext): Extraction[A] = ctx match {
      case c: FullFnContext => f(c)
      case _ =>
        val desc = s"Unknown type of job context ${ctx.getClass.getSimpleName} " +
          s"expected ${FullFnContext.getClass.getSimpleName}"
        Failed.InternalError(desc)
    }
    override def describe() = Seq(InternalArgument(tags))
  }
}

trait SparkArgs {

  val sparkContextArg: ArgDef[SparkContext] = SystemArg(
    Seq.empty,
    c => Extracted(c.sc)
  )

  val streamingContextArg: ArgDef[StreamingContext] = SystemArg(Seq(ArgInfo.StreamingContextTag),
    ctx => {
      val ssc = StreamingContext.getActiveOrCreate(() => new StreamingContext(ctx.sc, ctx.streamingDuration))
      Extracted(ssc)
    }
  )

  val sqlContextArg: ArgDef[SQLContext] = SystemArg(Seq(ArgInfo.SqlContextTag),
    ctx => sparkContextArg.map(SQLContext.getOrCreate).extract(ctx)
  )

  // HiveContext should be cached per jvm
  // see #325
  val hiveContextArg: ArgDef[SparkSession] = new SystemArg[SparkSession] {

    var cache: SparkSession = _

    override def extract(ctx: FnContext): Extraction[SparkSession] = synchronized {
      ctx match {
        case c: FullFnContext =>
          if (cache == null)
            cache = SparkSession.builder().config(c.sc.getConf).enableHiveSupport().getOrCreate()
          Extracted(cache)
        case _ =>
          Failed.InternalError(s"Unknown type of job context ${ctx.getClass.getSimpleName} expected ${FullFnContext.getClass.getSimpleName}")
      }
    }

    override def describe(): Seq[ArgInfo] = Seq(InternalArgument(
      Seq(ArgInfo.HiveContextTag, ArgInfo.SqlContextTag)))
  }

  val javaSparkContextArg: ArgDef[JavaSparkContext] = sparkContextArg.map(sc => new JavaSparkContext(sc))
  val javaStreamingContextArg: ArgDef[JavaStreamingContext] = SystemArg(Seq(ArgInfo.StreamingContextTag),
    ctx => streamingContextArg.map(scc => new JavaStreamingContext(scc)).extract(ctx))

  val sparkSessionArg: ArgDef[SparkSession] = SystemArg(Seq(ArgInfo.SqlContextTag),
    ctx => sparkContextArg.map(sc => SparkSessionUtils.getOrCreate(sc, false)).extract(ctx)
  )

  val sparkSessionWithHiveArg: ArgDef[SparkSession] = SystemArg(
    Seq(ArgInfo.SqlContextTag, ArgInfo.HiveContextTag),
    ctx => sparkContextArg.map(sc => SparkSessionUtils.getOrCreate(sc, true)).extract(ctx))
}

object SparkArgs extends SparkArgs
