/*import java.time.LocalDate
import java.time.temporal.ChronoUnit

def aboutToExpire(orddate: String ,expdate: String): Double = {
  val order_date = LocalDate.parse(orddate.substring(0,10))
  val expire_date = LocalDate.parse(expdate)
  val diff = ChronoUnit.DAYS.between(order_date,expire_date)
  if ( diff <= 29) 30-diff
  else 0
}*/

def categoryDiscount(product_name : String):Double={
  if ( product_name.toLowerCase.contains("cheese") ) 10.0
  else if ( product_name.toLowerCase.contains("wine") ) 5.0
  else 0
}