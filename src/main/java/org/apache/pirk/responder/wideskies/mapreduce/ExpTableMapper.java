/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.pirk.responder.wideskies.mapreduce;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.apache.pirk.encryption.ModPowAbstraction;
import org.apache.pirk.query.wideskies.Query;
import org.apache.pirk.utils.LogUtils;

/**
 * Class to generate the expTable given the input query vectors
 *
 */
public class ExpTableMapper extends Mapper<LongWritable,Text,Text,Text>
{
  private static Logger logger = LogUtils.getLoggerForThisClass();

  Text keyOut = null;
  Text valueOut = null;

  int dataPartitionBitSize = 0;
  int maxValue = 0;
  BigInteger NSquared = null;
  Query query = null;

  @Override
  public void setup(Context ctx) throws IOException, InterruptedException
  {
    super.setup(ctx);

    valueOut = new Text();

    FileSystem fs = FileSystem.newInstance(ctx.getConfiguration());
    String queryDir = ctx.getConfiguration().get("pirMR.queryInputDir");
    query = Query.readFromHDFSFile(new Path(queryDir), fs);

    dataPartitionBitSize = query.getQueryInfo().getDataPartitionBitSize();
    maxValue = (int) Math.pow(2, dataPartitionBitSize) - 1;

    NSquared = query.getNSquared();
  }

  // key is line number; value is the index of the queryVec
  @Override
  public void map(LongWritable key, Text value, Context ctx) throws IOException, InterruptedException
  {
    logger.info("key = " + key.toString() + " value = " + value.toString());

    BigInteger element = query.getQueryElement(Integer.parseInt(value.toString()));
    for (int i = 0; i <= maxValue; ++i)
    {
      BigInteger modPow = ModPowAbstraction.modPow(element, BigInteger.valueOf(i), NSquared);

      valueOut.set(i + "-" + modPow.toString()); // val: <power>-<element^power mod N^2>
      ctx.write(value, valueOut);
    }
  }
}
