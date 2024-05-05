package Discounts

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.io.{BufferedSource, Source}
import java.util.logging.{FileHandler, Level, Logger}

object Main extends App {
  val source: BufferedSource = Source.fromFile("src/resources/TRX1000.csv")
  val lines: List[String] = source.getLines().drop(1).toList // drop header
  val f: File = new File("../Final_discounts.csv")
  val writer = new PrintWriter(new FileOutputStream(f,true))

  val fileHandler = new FileHandler("../rules_engine.log")
  val logger: Logger = Logger.getLogger("Discount Engine")
  logger.addHandler(fileHandler)


  case class Product(timestamp:String,	product_name :String ,	expiry_date:String , 	quantity:Int,	unit_price:Float ,	channel:String,	payment_method:String)
  def toProduct(line: String): Product =  {
    val parsed_line = line.split(",")
    Product(parsed_line(0) , parsed_line(1) , parsed_line(2) , parsed_line(3).toInt ,parsed_line(4).toFloat ,parsed_line(5),parsed_line(6))
  }
  def calculateDiscount(Funcs: List[Product => Double], product: Product): Double = {
    val discount_arr = Funcs.map(func => func(product)).filter(x=> x != 0.0).sorted.reverse.take(2)
    discount_arr.length match {
      case 1 => discount_arr.head * 1.0
      case 2 => discount_arr.sum / 2.0
      case _ => 0.0
    }
  }
  // Discount Functions -----------------------------
      def aboutToExpire(orddate: String ,expdate: String): Double = {
        val order_date = LocalDate.parse(orddate.substring(0,10))
        val expire_date = LocalDate.parse(expdate)
        val diff = ChronoUnit.DAYS.between(order_date,expire_date)
        if ( diff <= 29) 30-diff
        else 0
      }
      def categoryDiscount(product_name : String):Double={
        if ( product_name.toLowerCase.contains("cheese") ) 10.0
        else if ( product_name.toLowerCase.contains("wine") ) 5.0
        else 0
      }
      def isHoliday(date: String): Double = {
        val parsed_date = LocalDate.parse(date.substring(0,10))
        if (parsed_date.getMonthValue == 3 && parsed_date.getDayOfMonth == 23) 50.0
        else 0
      }
      def qunatityCheck(quantity:Int) : Double={
        quantity match {
          case q if (q >= 6 && q <= 9 ) => 5
          case q if (q >= 10 && q <= 14 ) => 7
          case q if (q >= 15 ) => 10
          case _ => 0
        }
      }
  logger.warning("My First Log")
  // -----------------------------------------------

  // Prepare Data to be printed
  def productToString(prod: Product , discountFuncs: List[Product => Double]): String = {
    val discount = calculateDiscount(discountFuncs, prod)
    prod.product_name + ',' + prod.quantity +',' + prod.unit_price + ',' + prod.payment_method + ',' + discount+" %" + ',' + ( prod.unit_price - ((discount/100) * prod.unit_price))
  }
  // Contains Discount Functions
  val discountFunctions = List(
    (prod: Product) => aboutToExpire(prod.timestamp,prod.expiry_date),
    (prod: Product) => categoryDiscount(prod.product_name),
    (prod: Product) => isHoliday(prod.timestamp),
    (prod: Product) => qunatityCheck(prod.quantity)
  )
  // Write Data Into CSV
  def writeLine(line: String): Unit = writer.write(line+"\n")
  // Add CSV Header
  writer.write("Product Name,Quantity,Unit_price,Payment_method,discount %,Final_price"+"\n")

  lines.map(toProduct).map(x => productToString(x, discountFunctions)).foreach(writeLine)
  writer.close()
}
