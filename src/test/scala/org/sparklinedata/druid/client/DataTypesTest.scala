/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sparklinedata.druid.client

import org.apache.spark.sql.hive.test.sparklinedata.TestHive._

class DataTypesTest extends BaseTest {

  val starSchemaDataTypes =
    """
      |{
      |  "factTable" : "orderLineItemPartSupplier_datatypes",
      |  "relations" : []
      | }
    """.stripMargin.replace('\n', ' ')

  override def beforeAll() = {
    super.beforeAll()

    /*
        o_orderdate date
        l_commitdate date
        l_receiptdate timestamp
     */
    val cT = s"""CREATE TABLE if not exists
             orderLineItemPartSupplierDataTypesBase(o_orderkey integer,
             o_custkey integer,
      o_orderstatus string, o_totalprice double, o_orderdate date, o_orderpriority string,
      o_clerk string,
      o_shippriority integer, o_comment string, l_partkey integer, l_suppkey integer,
      l_linenumber integer,
      l_quantity double, l_extendedprice double, l_discount double, l_tax double,
      l_returnflag string,
      l_linestatus string, l_shipdate string, l_commitdate date, l_receiptdate timestamp,
      l_shipinstruct string,
      l_shipmode string, l_comment string, order_year string, ps_partkey integer,
      ps_suppkey integer,
      ps_availqty integer, ps_supplycost double, ps_comment string, s_name string, s_address string,
      s_phone string, s_acctbal double, s_comment string, s_nation string,
      s_region string, p_name string,
      p_mfgr string, p_brand string, p_type string, p_size integer, p_container string,
      p_retailprice double,
      p_comment string, c_name string , c_address string , c_phone string , c_acctbal double ,
      c_mktsegment string , c_comment string , c_nation string , c_region string)
      USING com.databricks.spark.csv
      OPTIONS (path "src/test/resources/tpch/datascale1/orderLineItemPartSupplierCustomer.small",
      header "false", delimiter "|")""".stripMargin

    println(cT)
    sql(cT)

    sql(
      s"""CREATE TABLE if not exists orderLineItemPartSupplier_datatypes
      USING org.sparklinedata.druid
      OPTIONS (sourceDataframe "orderLineItemPartSupplierDataTypesBase",
      timeDimensionColumn "l_shipdate",
      druidDatasource "tpch",
      druidHost "localhost",
      zkQualifyDiscoveryNames "true",
      queryHistoricalServers "false",
      columnMapping '$colMapping',
      functionalDependencies '$functionalDependencies',
      starSchema '$starSchemaDataTypes')""".stripMargin
    )

  }

  test("orderDate",
    "select o_orderdate, " +
      "count(*)  " +
      "from orderLineItemPartSupplier_datatypes group by o_orderdate",
    1,
    true,
    true
  )

  test("gbexprtest2",
    "select o_orderdate, " +
      "(substr(CAST(Date_Add(TO_DATE(CAST(CONCAT(o_orderdate, 'T00:00:00.000Z') " +
      "AS TIMESTAMP)), 5) AS TIMESTAMP), 0, 10)) x," +
      "sum(c_acctbal) as bal from orderLineItemPartSupplier_datatypes group by " +
      "o_orderdate, (substr(CAST(Date_Add(TO_DATE(CAST(CONCAT(TO_DATE(o_orderdate)," +
      " 'T00:00:00.000Z') AS TIMESTAMP)), 5) AS TIMESTAMP), 0, 10)) order by o_orderdate, x, bal",
    1,
    true, true)

  test("inclause-inTest1",
    s"""select c_name, sum(c_acctbal) as bal
      from orderLineItemPartSupplier_datatypes
      where to_Date(o_orderdate) >= cast('1993-01-01' as date) and to_Date(o_orderdate) <= cast('1997-12-31' as date)
       and cast(order_year as int) in (1993,1994,1995, null)
      group by c_name
      order by c_name, bal""".stripMargin,
    1,
    true, true)

  test("gbexprtest2-ts",
    "select o_orderdate, " +
      "(substr(CAST(Date_Add(TO_DATE(l_receiptdate), 5) AS TIMESTAMP), 0, 10)) x," +
      "sum(c_acctbal) as bal from orderLineItemPartSupplier_datatypes group by " +
      "o_orderdate, (substr(CAST(Date_Add(TO_DATE(l_receiptdate), 5) AS TIMESTAMP), 0, 10)) " +
      "order by o_orderdate, x, bal",
    1,
    true, true)

  test("inclause-inTest1-ts",
    s"""select c_name, sum(c_acctbal) as bal
      from orderLineItemPartSupplier_datatypes
      where to_Date(l_receiptdate) >= cast('1993-01-01' as date) and to_Date(l_receiptdate) <= cast('1997-12-31' as date)
       and cast(order_year as int) in (1993,1994,1995, null)
      group by c_name
      order by c_name, bal""".stripMargin,
    1,
    true, true)

}
