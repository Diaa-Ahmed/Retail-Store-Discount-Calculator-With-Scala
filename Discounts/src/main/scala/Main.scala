package Discounts
import java.io.{File, FileOutputStream, PrintWriter}
import scala.io.{BufferedSource, Source}

object Main extends App {
  val source: BufferedSource = Source.fromFile("src/resources/TRX1000.csv")
  val lines: List[String] = source.getLines().drop(1).toList // drop header
  val f: File = new File("Final_discounts.csv")
  val writer = new PrintWriter(new FileOutputStream(f,true))

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
     def aboutToExpire(expdate: String): Double = {
       25.0
     }
     def isHoliday(date: String): Double = {
      10.0
     }
  // -----------------------------------------------

  // Prepare Data to be printed
  def productToString(prod: Product , discountFuncs: List[Product => Double]): String = {
    val discount = calculateDiscount(discountFuncs, prod)
    prod.product_name + ',' + prod.quantity +',' + prod.unit_price + ',' + prod.payment_method + ',' + discount+'%'
  }
  // Contains Discount Functions
  val discountFunctions = List(
    (prod: Product) => aboutToExpire(prod.expiry_date),
    (prod: Product) => isHoliday(prod.timestamp)
  )
  // Write Data Into CSV
  def writeLine(line: String): Unit = writer.write(line+"\n")
  // Add CSV Header
  writer.write("Product Name,Quantity,unit_price,payment_method,discount%"+"\n")
  lines.map(toProduct).map(x => productToString(x, discountFunctions)).foreach(writeLine)
  writer.close()
}
