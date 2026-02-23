package io.hydrosphere.mist.master.store

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor
import javax.sql.DataSource

class HikariDataSourceTransactor(
                                  ds: DataSource,
                                  connectExecutor: ExecutionContext
                                ) {
  val xa: Transactor[IO] = Transactor.fromDataSource[IO](ds, connectExecutor)

  def rawTransactor: Transactor[IO] = xa
  def dataSource: DataSource = ds
}

object HikariDataSourceTransactor {
  def apply(
             ds: DataSource,
             connectThreads: Int
           ): HikariDataSourceTransactor = {
    val connectEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(connectThreads))
    new HikariDataSourceTransactor(ds, connectEC)
  }
}