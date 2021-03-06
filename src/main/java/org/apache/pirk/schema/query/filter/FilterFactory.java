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
package org.apache.pirk.schema.query.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.pirk.schema.query.QuerySchema;
import org.apache.pirk.utils.SystemConfiguration;

/**
 * Factory class to instantiate filters and set the necessary properties map
 */
public class FilterFactory
{
  public static Object getFilter(String filterName, QuerySchema qSchema) throws Exception
  {
    Object obj = null;

    if (filterName.equals(StopListFilter.class.getName()))
    {
      FileSystem fs = FileSystem.get(new Configuration());

      // Grab the stopList
      HashSet<String> stopList = new HashSet<String>();
      String stopListFile = SystemConfiguration.getProperty("pir.stopListFile", "none");

      if (!stopListFile.equals("none"))
      {
        BufferedReader br = null;
        if (fs.exists(new Path(stopListFile)))
        {
          br = new BufferedReader(new InputStreamReader(fs.open(new Path(stopListFile))));
        }
        else
        {
          FileReader fr = new FileReader(new File(stopListFile));
          br = new BufferedReader(fr);
        }

        String qLine = null;
        while ((qLine = br.readLine()) != null)
        {
          stopList.add(qLine);
        }

        obj = new StopListFilter(qSchema.getFilterElementNames(), stopList);
      }
    }
    else
    {
      // Instantiate and validate the interface implementation
      Class c = Class.forName(filterName);
      obj = c.newInstance();
      if (!(obj instanceof DataFilter))
      {
        throw new Exception("filterName = " + filterName + " DOES NOT implement the DataFilter interface");
      }
    }

    return obj;
  }
}
