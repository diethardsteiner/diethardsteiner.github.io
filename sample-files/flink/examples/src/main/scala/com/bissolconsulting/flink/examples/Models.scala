package com.bissolconsulting.flink.examples

/**
  * Created by dsteiner on 29/09/16.
  */
object Models {

  // middleName is optional - does not have to be supplied
  case class Purchase(
    purchaseId:Int
    , firstName:String
    , lastName:String
    , department:String
    , purchaseValue:Double
    , middleName:Option[String]
  )

  case class PurchaseAggregation(
   department:String
   , purchaseValue:Double
 )

}
