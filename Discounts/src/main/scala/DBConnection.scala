package Discounts

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, DatabaseMetaData}
import Engine.Product

// Class to manage database connection and operations
case class DBConnection(connection: Connection) {

  // Establishing a connection to the database and creating table "product_discounts"
  def createDBTable(): Boolean = {
    try {
      val metaData = connection.getMetaData
      val tableName = "product_discounts"
      val resultSet = metaData.getTables(null, null, tableName.toUpperCase, null)
      if (!resultSet.next()) {
        // Table does not exist, create it
        val createTableSQL =
          """
            | CREATE TABLE product_discounts (
            |  timestamp VARCHAR(255),
            |  product_name VARCHAR(255),
            |  expiry_date VARCHAR(255),
            |  quantity NUMBER,
            |  unit_price NUMBER(10,2),
            |  channel VARCHAR(255),
            |  payment_method VARCHAR(255),
            |  discount VARCHAR(255),
            |  final_price NUMBER(10,2)
            |)
            |""".stripMargin

        val statement = connection.createStatement()
        statement.execute(createTableSQL)
        statement.close()
        resultSet.close()
        true;
      }
      else {
        // Table exists, truncate it to clear previous data
        val statement = connection.createStatement()
        statement.execute(
          """
            |Truncate Table product_discounts""".stripMargin)
        statement.close()
        true
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        connection.close()
        false
    }
  }

  // Insert data for a single product into the "product_discounts" table
  def insertProduct(product: Product, discount: Double, final_price: Double ): Boolean = {
    try {
      val insertSQL =
        """
          |INSERT INTO product_discounts (timestamp, product_name, expiry_date, quantity, unit_price, channel, payment_method , discount , final_price)
          |VALUES (?, ?, ?, ?, ?, ?, ? , ? , ?)
          |""".stripMargin

      val preparedStatement = connection.prepareStatement(insertSQL)
      preparedStatement.setString(1, product.timestamp)
      preparedStatement.setString(2, product.product_name)
      preparedStatement.setString(3, product.expiry_date)
      preparedStatement.setInt(4, product.quantity)
      preparedStatement.setFloat(5, product.unit_price)
      preparedStatement.setString(6, product.channel)
      preparedStatement.setString(7, product.payment_method)
      preparedStatement.setString(8, discount.toString + " %")
      preparedStatement.setDouble(9, final_price)

      val out = preparedStatement.executeUpdate()
      preparedStatement.close()
      out > 0
    } catch {
      case e: Exception =>
        e.printStackTrace()
        connection.close()
        false
    }
  }

  // Close the database connection
  def closeConnection(): Unit = {
    connection.close();
  }
}
