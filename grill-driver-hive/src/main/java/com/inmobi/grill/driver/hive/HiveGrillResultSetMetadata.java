package com.inmobi.grill.driver.hive;

/*
 * #%L
 * Grill Hive Driver
 * %%
 * Copyright (C) 2014 Inmobi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.inmobi.grill.server.api.driver.GrillResultSetMetadata;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hive.service.cli.ColumnDescriptor;

import java.util.List;

/**
 * The top level Result Set metadata class which is used by the jackson to
 * serialize to JSON.
 */

@NoArgsConstructor
public class HiveGrillResultSetMetadata extends GrillResultSetMetadata {

  @Setter
  private List<ColumnDescriptor> columns;


  @Override
  public List<ColumnDescriptor> getColumns() {
    return columns;
  }
}
