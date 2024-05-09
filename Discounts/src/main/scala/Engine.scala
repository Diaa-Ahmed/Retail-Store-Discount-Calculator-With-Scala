package Discounts

import java.io.{File, FileOutputStream, PrintWriter}
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.logging.{FileHandler, Logger, SimpleFormatter}
import scala.io.{BufferedSource, Source}

object Engine extends App {
  // Define Log Path
  val fileHandler = new FileHandler("../Output/rules_engine.log")
  val logger: Logger = Logger.getLogger("Discount Engine")
  // Create a custom formatter for logger
  val formatter = new SimpleFormatter {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    override def format(record: java.util.logging.LogRecord): String = {
      val timestamp = dateFormat.format(record.getMillis())
      s"$timestamp\t${record.getLevel}\t${record.getMessage}\n"
    }
  }
  // Set configurations for the logger
  fileHandler.setFormatter(formatter)
  logger.addHandler(fileHandler)

  logger.info("Opening Source File For Reading")
  // Read data from CSV file
  val source: BufferedSource = Source.fromFile("src/resources/TRX1000.csv")

  val lines: List[String] = source.getLines().drop(1).toList // drop header
  val f: File = new File("../Output/Final_discounts.csv")
  val writer = new PrintWriter(new FileOutputStream(f,true))

  // Define case class to represent product information
  case class Product(timestamp:String,	product_name :String ,	expiry_date:String , 	quantity:Int,	unit_price:Float ,	channel:String,	payment_method:String)
  // Function to convert CSV line to Product object
  def toProduct(line: String): Product =  {
    val parsed_line = line.split(",")
    Product(parsed_line(0) , parsed_line(1) , parsed_line(2) , parsed_line(3).toInt ,parsed_line(4).toFloat ,parsed_line(5),parsed_line(6))
  }

  // Discount Functions -----------------------------
  // Check if product is about to expire
  def checkExpire(product: Product): Boolean ={
    val order_date = LocalDate.parse(product.timestamp.substring(0,10))
    val expire_date = LocalDate.parse(product.expiry_date)
    ChronoUnit.DAYS.between(order_date,expire_date) <= 29
  }
  // Calculate discount for expiring products
  def expireDiscount(product: Product): Double = {
    val order_date = LocalDate.parse(product.timestamp.substring(0,10))
    val expire_date = LocalDate.parse(product.expiry_date)
    val diff = ChronoUnit.DAYS.between(order_date,expire_date)
    if ( diff <= 29) {
      //logger.info("Product is about to expire.")
      30 - diff
    }
    else 0
  }
  // Check if product belongs to a specific category
  def checkCategory(product: Product):Boolean={
    if ( product.product_name.contains("Cheese") || product.product_name.contains("Wine") ) true
    else false
  }
  // Calculate discount for specific category of products
  def categoryDiscount(product: Product):Double={
    if ( product.product_name.contains("Cheese") ) 10.0
    else 5.0
  }
  // Check if it's a holiday
  def checkHoliday(product: Product) : Boolean= {
    val parsed_date = LocalDate.parse(product.timestamp.substring(0,10))
    if (parsed_date.getMonthValue == 3 && parsed_date.getDayOfMonth == 23) true
    else false
  }
  // Calculate discount for holiday
  def holidayDiscount(product: Product): Double = {
    50.0
  }
  // Check quantity of product
  def checkQunatity(product: Product) : Boolean={if (product.quantity > 5) true else false}
  // Calculate discount based on quantity
  def qunatityDiscount(product: Product) : Double={
    product.quantity match {
      case q if (q >= 6 && q <= 9 ) => 5
      case q if (q >= 10 && q <= 14 ) => 7
      case q if (q >= 15 ) => 10
      case _ => 0
    }
  }
  // Check channel of purchase
  def checkChannel(product: Product): Boolean={ if (product.channel == "App") true else false}
  // Calculate discount based on channel
  def channelDiscount(product: Product):Double = {
    val quantity = product.quantity
    product.channel match {
      case "App" => {
        val mod = quantity%5 ;
        if (mod == 0) quantity
        else quantity-mod +5
      }
      case _ => 0
    }
  }

  // Check payment method
  def checkPaymentMethod(product: Product): Boolean = {
    if (product.payment_method == "Visa") true else false
  }
  // Calculate discount based on payment method
  def paymentMethodDiscount(product: Product): Double =  5.0

  // -----------------------------------------------

  // Prepare Data to be printed
  def productToString(prod: Product , discount : Double): String = {
    prod.product_name + ',' + prod.quantity +',' + prod.unit_price + ',' + prod.payment_method + ',' + discount+" %" + ',' + ( prod.unit_price - ((discount/100) * prod.unit_price))*prod.quantity
  }
  // Contains Discount Functions
  val discountFunctions : List[(Product => Boolean, Product => Double)]  = List(
    (checkExpire,expireDiscount),
    (checkCategory,categoryDiscount),
    (checkHoliday,holidayDiscount),
    (checkQunatity,qunatityDiscount),
    (checkChannel , channelDiscount),
    (checkPaymentMethod,paymentMethodDiscount)
  )
  // Calculate total discount for product
  def calculateDiscount(Funcs: List[(Product => Boolean, Product => Double)], product: Product): Double = {
    logger.info("Checking discount qualifying rules ")
    val discount_arr = Funcs.map(func => if(func._1(product)) func._2(product) else 0 ).filter(x=> x != 0.0).sorted.reverse.take(2)
    discount_arr.length match {
      case 1 => discount_arr.head * 1.0
      case 2 => discount_arr.sum / 2.0
      case _ => 0.0
    }
  }
  // Write Data Into CSV
  def writeLine(line: String): Unit = writer.write(line+"\n")

  // Create Database Connection
  val dbconnection = DBConnection(DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/XE" , "hr" ,"hr"));
  if(dbconnection.createDBTable()) logger.info("Connected to database successfully")
  else logger.warning("Failed to connect to database server")

  // Add CSV Header
  writer.write("Product Name,Quantity,Unit_price,Payment_method,discount %,Final_price"+"\n")

  // Process each line from the CSV file
  lines.map(toProduct).map { product =>
    val discount = calculateDiscount(discountFunctions, product)
    val final_price = (product.unit_price - ((discount / 100) * product.unit_price)) * product.quantity
    dbconnection.insertProduct(product, discount, final_price)
    productToString(product , discount)
  }.foreach(writeLine)

  dbconnection.closeConnection()
  logger.info("Close database connection")
  writer.close()
  logger.info("Close csv output file")
}
