package com.ets.mgl804.persistance

import com.ets.mgl804.core.AppContext
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.bdgenomics.formats.avro._
import org.slf4j.LoggerFactory
import parquet.avro.{AvroReadSupport, AvroParquetReader}


/**
 * Created by Ivan Kizema on 17/08/25.
 */
class ParquetLoader[T](filename:String) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val DATA_PATH = "DATA/avro/"
  private val fileName = filename
  private val conf = AppContext.conf
  private val sc = AppContext.sc
  private val sqc = new SQLContext(sc)
  sqc.sql("SET spark.sql.parquet.binaryAsString=true")
  private val parquetFilePath:Path = new Path(this.fileName)
  private var loadedData = scala.collection.mutable.Buffer[T]()

  def showContent(): Unit = {
    logger.debug("******************************************************************")
    val individualsDataFrame:DataFrame = sqc.read.parquet(parquetFilePath.toString())
    individualsDataFrame.printSchema()
    individualsDataFrame.show()
    logger.debug("******************************************************************")
    val parquetReader = new AvroParquetReader[IndexedRecord](parquetFilePath)
    val individual:Individual = parquetReader.read().asInstanceOf[Individual]
    val individual2:Individual = parquetReader.read().asInstanceOf[Individual]
    logger.debug(individual.getIndividualId.toString)
    logger.debug(individual2.getGenotype.toString)
    logger.debug("******************************************************************")
    logger.debug(parquetReader.read().getClass.toString)
  }

  def test() {
    loadData()
    viewLoadedContent()
  }

  def loadData() : scala.collection.mutable.Buffer[T] = {
    val parquetReader = new AvroParquetReader[IndexedRecord](parquetFilePath)
    try {
      this.loadedData.append(parquetReader.read().asInstanceOf[T])
      while (this.loadedData.last != null) {
        this.loadedData.append(parquetReader.read().asInstanceOf[T])
      }
      this.loadedData.remove(this.loadedData.length-1)
      parquetReader.close()
    }
    logger.info("Loaded "+this.loadedData(0).getClass.toString+" : "+this.loadedData.length)
    return this.loadedData
  }

  def viewLoadedContent() {
    logger.debug("Loaded "+this.loadedData(0).getClass.toString+" : "+this.loadedData.length)
    this.loadedData.foreach(item => logger.debug(item.toString))
  }
}
