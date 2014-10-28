package com.inmobi.grill.server.usermetastore;

/*
 *
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
 *
*/

import com.inmobi.grill.api.metastore.*;
import com.inmobi.grill.api.usermetastore.ObjectFactory;
import com.inmobi.grill.api.usermetastore.XDomain;
import com.inmobi.grill.api.usermetastore.XDomainClass;
import com.inmobi.grill.api.usermetastore.XSchema;
import com.inmobi.grill.server.metastore.JAXBUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.cube.metadata.*;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;

import static com.inmobi.grill.server.metastore.JAXBUtils.hiveDimAttrFromXDimAttr;

/**
 * Wrapper class around cubemetastore.
 */
public class UserMetastoreClient {

  public static final Logger LOG = LogManager.getLogger(UserMetastoreClient.class);
  private static final ObjectFactory UserXCF = new ObjectFactory();
  private static final com.inmobi.grill.api.metastore.ObjectFactory CubeXCF =
      new com.inmobi.grill.api.metastore.ObjectFactory();

  private static final String OWNER = "owner";
  private static final String DESCRIPTION = "description";
  private static final String DOMAIN_CLASS = "domain_class";

  private static final String DEFAULT_INPUT_FORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat";
  private static final String DEFAULT_OUTPUT_FORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat";
  private static final String DEFAULT_FIELD_DELIMITER = " '\\001'";
//  private static final String DEFAULT_FIELD_ESCAPER = ;
//  private static final String DEFAULT_COL_ITEM_DELIMITER;
//  private static final String DEFAULT_LINE_DELIMITER;
//  private static final String DEFAULT_MAP_KEY_DELIMITER;
//TODO Take all these from a conf
  private static final String DEFAULT_SERIALIZER = "org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe" ;
  private static final String BASELOCATION = "/tmp/baselocation";
  private static final String DOMAIN = "domain";

  private static UserMetastoreClient instance;
  private final CubeMetastoreClient cubeClient;

  private UserMetastoreClient(HiveConf conf) throws HiveException {
    this.cubeClient = CubeMetastoreClient.getInstance(conf);
  }

  public static UserMetastoreClient getInstance(HiveConf conf)
      throws HiveException {
    if (instance == null) {
      instance = new UserMetastoreClient(conf);
    }
    return instance;
  }

  //TODO. Do we need this?
  public void close() {
    cubeClient.close();
  }

  public void createDomain(XDomain domain) throws ParseException, HiveException {
    System.out.println("Inside client, CD");
    cubeClient.createDimension(getDimensionFromXDomain(domain));

    //We will also create the underlying dimensionTable and its storage
    double weight = 0.0; //TODO. What is weight?

    List<FieldSchema> columns = getColumnsForDomain(domain);
    Map<String, UpdatePeriod> updateTimePeriodMap = getUpdatePeriodMapForDomain(domain);
    Map<String, StorageTableDesc> storageDesc = getStorageDescForDomain(domain);
    Map<String, String> dimensionProperties = getPropertiesForDomain(domain);

    cubeClient.createCubeDimensionTable(domain.getName(), domain.getName() + "_cube",
        columns, weight, updateTimePeriodMap, dimensionProperties, storageDesc);
  }

  private Map<String, String> getPropertiesForDomain(XDomain domain) {
    return null;
  }

  private Map<String, StorageTableDesc> getStorageDescForDomain(XDomain domain) throws HiveException {
    String defaultDomainStorageName = "domain_storage_" + domain.getName();
    createDomainStorage(defaultDomainStorageName);
    Map<String, StorageTableDesc> storageTableMap = new HashMap<String, StorageTableDesc>();
    StorageTableDesc std = createStorageTableDesc(domain);
    storageTableMap.put(defaultDomainStorageName, std);
    return storageTableMap;
  }

  private void createDomainStorage(String name) throws HiveException {
    List<Storage> allStorage = cubeClient.getAllStorages();
    for (Storage s: allStorage) {
      if (s.getName().equalsIgnoreCase(name)) {
        return;
      }
    }

    //TODO. Get all this from config
    XStorage localStorage = CubeXCF.createXStorage();
    XProperties xprops = CubeXCF.createXProperties();
    XProperty xprop = CubeXCF.createXProperty();
    xprop.setName("storage.url");
    xprop.setValue("file:///");
    xprops.getProperties().add(xprop);
    localStorage.setClassname("org.apache.hadoop.hive.ql.cube.metadata.HDFSStorage");
    localStorage.setName(name);
    localStorage.setProperties(xprops);
    cubeClient.createStorage(JAXBUtils.storageFromXStorage(localStorage));
  }

  private StorageTableDesc createStorageTableDesc(XDomain domain) {
    StorageTableDesc tableDesc = new StorageTableDesc();
    tableDesc.setTblProps(null);
    tableDesc.setSerdeProps(null);

    XDomainClass domainClass = domain.getClazz();

    if (domainClass == XDomainClass.USER) {
      String source = domain.getSource().getName();
      for (XDimAttribute xd : domain.getSchema().getAttributes().getDimAttributes()) {
        if (xd.getName().equalsIgnoreCase(source)) {
          FieldSchema fs = new FieldSchema(xd.getName(), xd.getType(), xd.getDescription());
          ArrayList<FieldSchema> afs = new ArrayList<FieldSchema>();
          afs.add(fs);
          tableDesc.setPartCols(afs);
        }
       }
    }

    tableDesc.setExternal(true);
    tableDesc.setLocation(generateTableLocationForDomain(domain));
    tableDesc.setInputFormat(DEFAULT_INPUT_FORMAT);
    tableDesc.setOutputFormat(DEFAULT_OUTPUT_FORMAT);
    tableDesc.setFieldDelim(DEFAULT_FIELD_DELIMITER);
    tableDesc.setSerName(DEFAULT_SERIALIZER);
    return tableDesc;
  }

  private String generateTableLocationForDomain(XDomain domain) {
    Path path = new Path(BASELOCATION, DOMAIN + "_" + domain.getName());
    return path.toString();
  }

  private Map<String, UpdatePeriod> getUpdatePeriodMapForDomain(XDomain domain) {
    Map<String, UpdatePeriod> map = new LinkedHashMap<String, UpdatePeriod>();
    UpdatePeriod dumpPeriod = null;
    //TODO. We need to create one common storage for all our needs
    map.put("domain_storage_" + domain.getName(), dumpPeriod);
    return map;
  }

  private List<FieldSchema>
  getColumnsForDomain(XDomain domain) {
    List<FieldSchema> lfs = new ArrayList<FieldSchema>();
    for (XDimAttribute xd : domain.getSchema().getAttributes().getDimAttributes()) {
      lfs.add(new FieldSchema(xd.getName(), xd.getType(), xd.getDescription()));
    }
    return lfs;
  }

   Dimension getDimensionFromXDomain(XDomain domain) {
    Set<CubeDimAttribute> dims = new LinkedHashSet<CubeDimAttribute>();
    for (XDimAttribute xd : domain.getSchema().getAttributes().getDimAttributes()) {
      System.out.println(xd.getName());
      dims.add(hiveDimAttrFromXDimAttr(xd));
    }

    //TODO. Want to prefix the keys of constrains with some thing. For now,
    // anything that is not the other three is a constraint

    Map<String, String> properties = new LinkedHashMap<String, String>();
    properties.put(DOMAIN_CLASS, domain.getClazz().value());
    properties.put(DESCRIPTION, domain.getDescription());
    properties.put(OWNER, domain.getOwner());

    return new Dimension(domain.getName(), dims, null, properties, 0);
  }

  public List<XDomain> getAllDomains() throws HiveException {

    // The assumption here is that, in this database, all the dimensions are
    // actually domains. If there are other domains, we need to add more checks.

    List<Dimension> dimensions = cubeClient.getAllDimensions();
    List<XDomain> domainList = new ArrayList<XDomain>();
    if (dimensions != null && !dimensions.isEmpty()) {
      for (Dimension d : dimensions) {
        domainList.add(getXDomainFromDimension(d));
      }
    }
    return domainList;
  }

  private XDomain getXDomainFromDimension(Dimension d) {
    XDomain xd = UserXCF.createXDomain();
    xd.setName(d.getName());
    Map<String, String> properties = d.getProperties();
    String domainClass = properties.get(DOMAIN_CLASS);
    if (domainClass!= null) {
      xd.setClazz(XDomainClass.fromValue(domainClass));
    }

    String description = properties.get(DESCRIPTION);
    if (description != null) {
      xd.setDescription(description);
    }

    String owner = properties.get(OWNER);
    if (owner != null) {
      xd.setOwner(owner);
    }

    XSchema schema = UserXCF.createXSchema();

    XDimAttributes xdm = CubeXCF.createXDimAttributes();
    List<XDimAttribute> xdmList = xdm.getDimAttributes();
    for (CubeDimAttribute cd : d.getAttributes()) {
      xdmList.add(JAXBUtils.xDimAttrFromHiveDimAttr(cd));
    }
    schema.setAttributes(xdm);
    xd.setSchema(schema);
    return xd;
  }

  public XDomain getDomain(String domainName) throws HiveException {
    Dimension dim = cubeClient.getDimension(domainName);
    return getXDomainFromDimension(dim);
  }
}
