package akka_akka_streams.homework

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipN, ZipWith}
import akkakafka.ConsumerApp.system
import akkakafka.ProducerApp.{config, producerSettings}
import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{IntegerDeserializer, StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

object homework5 {
  implicit val system = ActorSystem("fusion")
  implicit val materializer = ActorMaterializer()
  implicit val mat: Materializer  = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[Logger]
    .setLevel(Level.ERROR)

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
  val outputTopic = "test_out"
  val graph =
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
      // 1
      import GraphDSL.Implicits._

      //2
      //Hardcode
//      val input =  builder.add(Source(1 to 5))
//      val output = builder.add(Sink.foreach(println))

      //Kafka
      val input = builder.add(Consumer.plainSource(consumerSettings, Subscriptions.topics("test"))
        .map(record => {
          println(s"Received record: ${record.value()}") // Логирование полученных сообщений
          record.value().toInt
        }))
      val producerMessage = builder.add(Flow[Int].map[ProducerRecord[String, String]](value => {
        println(s"Received value for produce: ${value}")
        new ProducerRecord[String, String](outputTopic, value.toString)
      }))
      val output = builder.add(Producer.plainSink(producerSettings))

      val mul10 = builder.add(Flow[Int].map(x=>x*10))
      val mul2 = builder.add(Flow[Int].map(x=>x*2))
      val mul3 = builder.add(Flow[Int].map(x=>x*3))


      val broadcast = builder.add(Broadcast[Int](3))

      //Zip работает только с 2 аргументами... Костыль? val zip = builder.add(Zip[Int, Int, Int]) - не катит

      //Variant 1
      val zip = builder.add(ZipN[Int](3))
      //3
      input ~> broadcast
      broadcast.out(0) ~> mul10 ~> zip.in(0)
      broadcast.out(1) ~> mul2 ~> zip.in(1)
      broadcast.out(2) ~> mul3 ~> zip.in(2)
//      zip.out.map( x => x.sum ) ~> output
      //Kafka
      zip.out.map( x => x.sum ) ~> producerMessage ~> output

      //Variant 2
//        val zip1 = builder.add(ZipWith[Int, Int, (Int, Int)]((a, b) => (a, b)))
//        val zip2 = builder.add(ZipWith[(Int, Int), Int, (Int, Int, Int)]((ab, c) => (ab._1, ab._2, c)))
//        input ~> broadcast
//        broadcast.out(0) ~> mul10 ~> zip1.in0
//        broadcast.out(1) ~> mul2 ~> zip1.in1
//        zip1.out ~> zip2.in0
//        broadcast.out(2) ~> mul3 ~> zip2.in1
//
//        zip2.out.map { case (a, b, c) => a + b + c } ~> output
////        Kafka
//        zip2.out.map { case (a, b, c) => a + b + c } ~> producerMessage ~> output

      //4
      ClosedShape
    }

  def main(args: Array[String]) : Unit ={
    RunnableGraph.fromGraph(graph).run()

  }
}