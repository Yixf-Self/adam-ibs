package com.ets.mgl804.avro

import java.io.File
import org.apache.avro.generic.IndexedRecord
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import com.ets.mgl804.avro.{Message, User}
import org.bdgenomics.formats.avro.{Variant, Genotype}
import parquet.avro.AvroParquetWriter

/**
 * Created by ikizema on 15-06-30.
 */
object ParquetAvroSparkExample {
  //This is the path where Parquet files will be created
  private val DATA_PATH = "DATA/avro/"

  private var conf:SparkConf = _
  private var sqc:SQLContext = _

  def main(args: Array[String]) {
    //Initialize Spark; this variable is always required
    conf = new SparkConf(true).setAppName("ParquetAvroExample").setMaster("local").set("spark.driver.allowMultipleContexts", "true")
    new SparkContext(conf)

    //Create a Spark Context and wrap it inside a SQLContext
    sqc = new SQLContext(new SparkContext(conf))

    //genotype_test()
    userAndMessages_test()

    //Close Spark to free up the memory
    sqc.sparkContext.stop()
  }

  def userAndMessages_test()
  {
    //This is the number of Genotype class that will be generated by the createParquetFile function.
    val NUMBER_OF_USERS:Int = 2000;
    val NUMBER_OF_MESSAGES:Int = 2000000;

    //Define the Users and Messages parquet file paths
    val userParquetFilePath:Path = new Path(DATA_PATH, "users.parquet")
    val messageParquetFilePath:Path = new Path(DATA_PATH, "messages.parquet")

    createUsersParquetFile(NUMBER_OF_USERS, userParquetFilePath)
    createMessagesParquetFile(NUMBER_OF_USERS, NUMBER_OF_MESSAGES, messageParquetFilePath)

    queryUserAndMessageFiles(userParquetFilePath, messageParquetFilePath);
  }

  def createUsersParquetFile(numberOfUser: Int, parquetFilePath: Path): Unit =
  {
    //We must make sure that the parquet file(s) are deleted because the following script doesn't replace the file.
    deleteIfExist(parquetFilePath.getParent().toString(), parquetFilePath.getName());

    val parquetWriter = new AvroParquetWriter[IndexedRecord](parquetFilePath, User.getClassSchema())

    for (i <- 0 until numberOfUser by 1)
    {
      parquetWriter.write(createUser(i))
    }

    parquetWriter.close()
  }

  def createMessagesParquetFile(numberOfUser: Int, numberOfMessages: Int, parquetFilePath: Path): Unit =
  {
    //We must make sure that the parquet file(s) are deleted because the following script doesn't replace the file.
    deleteIfExist(parquetFilePath.getParent().toString(), parquetFilePath.getName());

    val parquetWriter = new AvroParquetWriter[IndexedRecord](parquetFilePath, Message.getClassSchema())

    for (i <- 0 until numberOfMessages by 1)
    {
      parquetWriter.write(createMessage(i, numberOfUser))
    }

    parquetWriter.close()
  }

  def queryUserAndMessageFiles(userParquetFilePath:Path, messageParquetFilePath:Path): Unit =
  {
    //Here we load the parquet files into DataFrames object.  This will allow us to query the data
    //Consult the following documentation if you want to know what your query options are: https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.DataFrame
    val usersDataFrame:DataFrame = sqc.read.parquet(userParquetFilePath.toString())
    val messagesDataFrame:DataFrame = sqc.read.parquet(messageParquetFilePath.toString())

    val usersMessagesDataFrame:DataFrame = usersDataFrame.join(messagesDataFrame, usersDataFrame("id") === messagesDataFrame("sender"), "inner")

    println("")
    println("******************************************************************");
    println("******************************************************************");

    //This example show the messages sent by the users with the id between 20 and 30
    usersMessagesDataFrame.select("id", "name", "age", "favorite_color", "recipient", "content")
      .filter("id >= 20").filter("id <= 30")
      .show()

    //This example show you how to select the users who sent message(s) to themself
    usersMessagesDataFrame.filter("sender = recipient")
      .select("id", "name", "age", "favorite_color", "recipient", "content")
      .filter("id >= 20")
      .filter("id <= 30")
      .show()

    println("******************************************************************");
    println("******************************************************************");
    println("")
  }

  /**
   * This function delete the file on the disk if it exist.
   */
  def deleteIfExist(path:String, fileName:String)
  {
    val fileTemp = new File(path, fileName);
    if (fileTemp.exists())
    {
      fileTemp.delete();
    }
  }

  def createUser(idx: Int): User =
  {
    val r = scala.util.Random
    val age:Int = 18 + r.nextInt(100-18)//we want users between 18 and 100 years old
  val color:String = if(idx % 5 == 0) "purple" else if(idx % 3 == 0) "blue" else if(idx % 2 == 0) "orange" else "red"

    return User.newBuilder()
      .setId(idx)
      .setName("UserName" + idx.toString())
      .setAge(age)
      .setFavoriteColor(color)
      .build()
  }

  def createMessage(idx:Int, maxUsers: Int): com.ets.mgl804.avro.Message =
  {
    val r = scala.util.Random
    val senderId:Int = r.nextInt(maxUsers)
    val recipientId:Int = r.nextInt(maxUsers)//We allow a user to send an email to himself... why not... I do it, you do it too...

    return Message.newBuilder()
      .setID(idx.toLong)
      .setSender(senderId)
      .setRecipient(recipientId)
      .setContent("The message #" + idx.toString())
      .build()
  }

}
