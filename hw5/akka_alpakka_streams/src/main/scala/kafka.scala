package akkakafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer, IntegerDeserializer}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import ch.qos.logback.classic.{Level, Logger}
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

object  ConsumerApp extends App{
  implicit val system: ActorSystem = ActorSystem("consumer-sys")
  implicit val mat: Materializer  = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[Logger]
    .setLevel(Level.ERROR)

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  val consume = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .map(record => {
      println(s"Received record: ${record.value()}") // Логирование полученных сообщений
      record.value().toInt
    })
    .runWith(Sink.foreach(println))


  consume onComplete {
    case Success(_) => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()

  }
}

object ProducerApp extends App {
  implicit val system: ActorSystem = ActorSystem("producer-sys")
  implicit val mat: Materializer  = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()
  val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val produce: Future[Done] =
    Source(1 to 100)
      .map(value=> new ProducerRecord[String, String]("test", value.toString))
      .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_) => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()

  }
}