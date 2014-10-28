package com.inmobi.grill.server.usermetastore;

/*
 * #%L
 * Grill Server
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

import com.inmobi.grill.api.APIResult;
import com.inmobi.grill.api.APIResult.Status;
import com.inmobi.grill.api.GrillSessionHandle;
import com.inmobi.grill.api.StringList;
import com.inmobi.grill.api.metastore.*;
import com.inmobi.grill.api.usermetastore.ObjectFactory;
import com.inmobi.grill.api.usermetastore.*;
import com.inmobi.grill.server.GrillJerseyTest;
import com.inmobi.grill.server.GrillServices;
import com.inmobi.grill.server.metastore.CubeMetastoreServiceImpl;
import org.apache.hadoop.hive.ql.cube.metadata.CubeDimAttribute;
import org.apache.hadoop.hive.ql.cube.metadata.Dimension;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.*;

import static com.inmobi.grill.server.metastore.JAXBUtils.hiveDimAttrFromXDimAttr;
import static org.testng.Assert.*;

@Test(groups="unit-test")
public class TestUserMetastoreService extends GrillJerseyTest {

  public static final Logger LOG = LogManager.getLogger(TestUserMetastoreService.class);
  private ObjectFactory userObjectFactory;
  private com.inmobi.grill.api.metastore.ObjectFactory cubeObjectFactory;
  protected String mediaType = MediaType.APPLICATION_XML;
  protected MediaType medType = MediaType.APPLICATION_XML_TYPE;
  protected String dbPFX = "TestUserMetastoreService_";
  UserMetastoreServiceImpl metastoreService;
  CubeMetastoreServiceImpl cubeMetastoreService;
  GrillSessionHandle grillSessionId;

  private static final String LOCATION_DOMAIN_NAME = "location";
  private static final String LOCATION_DESCRIPTION ="Domain for storing location";
  private static final String LOCATION_OWNER = "LatLonger";

  private static final String APP_DOMAIN_NAME = "app";
  private static final String APP_DESCRIPTION = "Domain for App";
  private static final String APP_OWNER = "Subway Surfer";


  @BeforeTest
  public void setUp() throws Exception {
    super.setUp();
    BasicConfigurator.configure();
    userObjectFactory = new ObjectFactory();
    cubeObjectFactory = new com.inmobi.grill.api.metastore.ObjectFactory();
    metastoreService = (UserMetastoreServiceImpl)GrillServices.get().getService("usermetastore");
    cubeMetastoreService = (CubeMetastoreServiceImpl)GrillServices.get().getService("metastore");
    grillSessionId = metastoreService.openSession("foo", "bar", new HashMap<String, String>());
  }

  @AfterTest
  public void tearDown() throws Exception {
    metastoreService.closeSession(grillSessionId);
    super.tearDown();
  }

  protected int getTestPort() {
    return 8082;
  }

  @Override
  protected Application configure() {
    return new UserMetastoreApp();
  }

  @Override
  protected void configureClient(ClientConfig config) {
    config.register(MultiPartFeature.class);
  }

  private void dropDatabase(String dbName) throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases").path(dbName);

    APIResult result = dbTarget.queryParam("cascade", "true")
        .queryParam("sessionid", grillSessionId).request(mediaType).delete(APIResult.class);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private void setCurrentDatabase(String dbName) throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases/current");
    APIResult result = dbTarget.queryParam("sessionid", grillSessionId).
        request(mediaType).put(Entity.xml(dbName),
        APIResult.class);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private String getCurrentDatabase() throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases/current");
    Invocation.Builder builder = dbTarget.queryParam("sessionid", grillSessionId).
        request(mediaType);
    String response = builder.get(String.class);
    return response;
  }

  private void createDatabase(String dbName) throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases");
    APIResult result = dbTarget.queryParam("sessionid", grillSessionId).
        request(mediaType).post(Entity.xml(dbName),
        APIResult.class);
    assertNotNull(result);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private void createDefaultStorage() throws Exception {
    XStorage localStorage = cubeObjectFactory.createXStorage();
    XProperties xprops = cubeObjectFactory.createXProperties();
    XProperty xprop = cubeObjectFactory.createXProperty();
    xprop.setName("storage.url");
    xprop.setValue("file:///");
    xprops.getProperties().add(xprop);
    localStorage.setClassname("org.apache.hadoop.hive.ql.cube.metadata.HDFSStorage");
    localStorage.setName("domain_storage");
    localStorage.setProperties(xprops);
    WebTarget dbTarget = target().path("metastore").path("storages");

    APIResult result = dbTarget.queryParam("sessionid", grillSessionId)
        .request(mediaType).post(Entity.xml(cubeObjectFactory.createXStorage(localStorage)),
            APIResult.class);
    assertNotNull(result);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private XDomain createLocationDomain() throws Exception {
    String domainName = LOCATION_DOMAIN_NAME;
    XDomain domain = userObjectFactory.createXDomain();
    Source src = userObjectFactory.createSource();
    src.setDescription("Network");
    src.setLicense("FreeForAll");
    src.setName("Network");
    domain.setName(domainName);
    domain.setClazz(XDomainClass.USER);
    domain.setDescription(LOCATION_DESCRIPTION);
    domain.setOwner(LOCATION_OWNER);
    domain.setSource(src);

    XDimAttributes schemaAttribs = cubeObjectFactory.createXDimAttributes();

    XDimAttribute locid = cubeObjectFactory.createXDimAttribute();
    locid.setName("locationId");
    locid.setType("int");
    locid.setDescription("Location Id");
    locid.setNullable(false);
    locid.setConstraints(">0");

    schemaAttribs.getDimAttributes().add(locid);

    XDimAttribute zip = cubeObjectFactory.createXDimAttribute();
    zip.setName("zip");
    zip.setType("string");
    zip.setDescription("ZIP Code");
    zip.setNullable(true);

    schemaAttribs.getDimAttributes().add(zip);

    XSchema schema = userObjectFactory.createXSchema();
    schema.setAttributes(schemaAttribs);

    domain.setSchema(schema);
    return domain;
  }

  private XDomain createAppDomain() throws Exception {
    String domainName = APP_DOMAIN_NAME;
    XDomain domain = userObjectFactory.createXDomain();
    Source src = userObjectFactory.createSource();
    src.setDescription("UAC");
    src.setLicense("FreeForAll");
    src.setName("UAC");
    domain.setName(domainName);
    domain.setClazz(XDomainClass.OTHER);
    domain.setDescription(APP_DESCRIPTION);
    domain.setOwner(APP_OWNER);
    domain.setSource(src);

    XDimAttributes schemaAttribs = cubeObjectFactory.createXDimAttributes();

    XDimAttribute appid = cubeObjectFactory.createXDimAttribute();
    appid.setName("appId");
    appid.setType("int");
    appid.setDescription("App Id");
    appid.setNullable(false);
    appid.setConstraints(">0");

    schemaAttribs.getDimAttributes().add(appid);

    XDimAttribute appName = cubeObjectFactory.createXDimAttribute();
    appName.setName("name");
    appName.setType("string");
    appName.setDescription("APP Name");
    appName.setNullable(true);

    schemaAttribs.getDimAttributes().add(appName);

    XDimAttribute appRating = cubeObjectFactory.createXDimAttribute();
    appRating.setName("rating");
    appRating.setType("int");
    appRating.setDescription("APP Rating");
    appRating.setNullable(true);
    appRating.setConstraints(">0");

    schemaAttribs.getDimAttributes().add(appRating);

    XSchema schema = userObjectFactory.createXSchema();
    schema.setAttributes(schemaAttribs);

    domain.setSchema(schema);
    return domain;
  }

  //Should not be here. Copied over for testing
  Dimension getDimensionFromXDomain(XDomain domain) {
    Set<CubeDimAttribute> dims = new LinkedHashSet<CubeDimAttribute>();
    for (XDimAttribute xd : domain.getSchema().getAttributes().getDimAttributes()) {
      System.out.println(xd.getName());
      dims.add(hiveDimAttrFromXDimAttr(xd));
    }

    //TODO. Want to prefix the keys of constrains with some thing. For now,
    // anything that is not the other three is a constraint

    Map<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("domain_class", domain.getClazz().value());
    properties.put("description", domain.getDescription());
    properties.put("owner", domain.getOwner());

    return new Dimension(domain.getName(), dims, null, properties, 0);
  }

  @Test
  public void testDomain() throws Exception {
    final String DB = dbPFX + "test_domain";
    String prevDb = getCurrentDatabase();
    createDatabase(DB);
    setCurrentDatabase(DB);
    //createDefaultStorage();


    try {
      XDomain domain = createLocationDomain();
      final WebTarget target = target().path("usermetastore").path("domain");

      // create
      APIResult result = target.queryParam("sessionid", grillSessionId).
          request(mediaType).post(Entity.xml(userObjectFactory.createDomain(domain)),
          APIResult.class);
      assertNotNull(result);
      assertEquals(result.getStatus(), Status.SUCCEEDED);

      // getall
      StringList domains = target.queryParam("sessionid", grillSessionId).
          request(mediaType).get(StringList.class);
      boolean foundDim = false;
      for (String c : domains.getElements()) {
        if (c.equalsIgnoreCase(LOCATION_DOMAIN_NAME)) {
          foundDim = true;
          break;
        }
      }

      assertTrue(foundDim);
      assertEquals(domains.getElements().size(), 1);

      // get
      XDomain testDomain = target.path(LOCATION_DOMAIN_NAME).queryParam("sessionid", grillSessionId).
          request(mediaType).get(XDomain.class);
      assertEquals(testDomain.getName(), LOCATION_DOMAIN_NAME);
      assertEquals(testDomain.getDescription(), LOCATION_DESCRIPTION);

      assertEquals(testDomain.getSchema().getAttributes().getDimAttributes().size(), 2);

      /****************************************************************************************************************
       * Little Bogus. The following are checking the internals. Should be moved elsewhere
       * BEGIN - INTERNAL TESTS
       ****************************************************************************************************************/
      Dimension testDim = getDimensionFromXDomain(testDomain);
      assertNotNull(testDim.getAttributeByName("locationId"));
      assertEquals(testDim.getAttributeByName("locationid").getDescription(), "Location Id");

      assertNotNull(testDim.getAttributeByName("zip"));
      assertEquals(testDim.getAttributeByName("zip").getDescription(),"ZIP Code");

      assertTrue(testDim.getProperties().containsKey("domain_class"));
      assertTrue(testDim.getProperties().containsKey("description"));
      assertTrue(testDim.getProperties().containsKey("owner"));

      assertEquals(testDim.getProperties().get("domain_class"), "User");
      assertEquals(testDim.getProperties().get("description"), LOCATION_DESCRIPTION);
      assertEquals(testDim.getProperties().get("owner"), LOCATION_OWNER);

      StringList allDimTables = target().path("metastore").path("dimtables").
          queryParam("sessionid", grillSessionId).request(mediaType).get(StringList.class);

      String locationDimTable = LOCATION_DOMAIN_NAME + "_cube";

      boolean found = false;
      for (String s: allDimTables.getElements()) {
        if (s.equalsIgnoreCase(locationDimTable)) {
          found = true;
          break;
        }
      }

      assertTrue(found);

      StringList allStorages = target().path("metastore").path("dimtables/" + locationDimTable + "/" + "storages").
          queryParam("sessionid", grillSessionId).request(mediaType).get(StringList.class);

      String domainStorage = "domain_storage_" + LOCATION_DOMAIN_NAME;
      found = false;
      for (String s: allStorages.getElements()) {
        if (s.equalsIgnoreCase(domainStorage)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
//
//      XPartition locationPartitions = target().path("metastore").
//          path("dimtables/" + locationDimTable + "/" + "storages" + "/" + domainStorage + "/partitions").
//          queryParam("sessionid", grillSessionId).request(mediaType).get(XPartition.class);
//      assertNotNull(locationPartitions);
//      System.out.println("PART INFO -- " + locationPartitions.getName());

      /****************************************************************************************************************
       * END INTERNAL TESTS
       ****************************************************************************************************************/
      domain = createAppDomain();

      // create
      result = target.queryParam("sessionid", grillSessionId).
          request(mediaType).post(Entity.xml(userObjectFactory.createDomain(domain)),
          APIResult.class);
      assertNotNull(result);
      assertEquals(result.getStatus(), Status.SUCCEEDED);

      // getall
      domains = target.queryParam("sessionid", grillSessionId).
          request(mediaType).get(StringList.class);
      foundDim = false;
      for (String c : domains.getElements()) {
        if (c.equalsIgnoreCase(APP_DOMAIN_NAME)) {
          foundDim = true;
          break;
        }
      }

      assertTrue(foundDim);
      assertEquals(domains.getElements().size(), 2);

      // get
     testDomain = target.path(APP_DOMAIN_NAME).queryParam("sessionid", grillSessionId).
          request(mediaType).get(XDomain.class);
      assertEquals(testDomain.getName(), APP_DOMAIN_NAME);
      assertEquals(testDomain.getDescription(), APP_DESCRIPTION);

      assertEquals(testDomain.getSchema().getAttributes().getDimAttributes().size(), 3);

/*
      Dimension dim = JAXBUtils.dimensionFromXDimension(dimension);
      assertNotNull(dim.getAttributeByName("col1"));
      assertEquals(dim.getAttributeByName("col1").getDescription(), "first column");
      assertEquals(dim.getAttributeByName("col1").getDisplayString(), "Column1");
      assertNotNull(dim.getAttributeByName("col2"));
      assertEquals(dim.getAttributeByName("col2").getDescription(), "second column");
      assertEquals(dim.getAttributeByName("col2").getDisplayString(), "Column2");
      assertNotNull(dim.getExpressionByName("dimexpr"));
      assertEquals(dim.getExpressionByName("dimexpr").getDescription(), "dimension expression");
      assertEquals(dim.getExpressionByName("dimexpr").getDisplayString(), "Dim Expression");

/*
      // alter dimension
      XProperty prop = cubeObjectFactory.createXProperty();
      prop.setName("dim.prop2.name");
      prop.setValue("dim.prop2.value");
      dimension.getProperties().getProperties().add(prop);

      dimension.getAttributes().getDimAttributes().remove(1);
      XDimAttribute xd1 = cubeObjectFactory.createXDimAttribute();
      xd1.setName("col3");
      xd1.setType("string");
      dimension.getAttributes().getDimAttributes().add(xd1);
      dimension.setWeight(200.0);

      result = target.path("testdim")
          .queryParam("sessionid", grillSessionId)
          .request(mediaType).put(Entity.xml(cubeObjectFactory.createXDimension(dimension)), APIResult.class);
      assertEquals(result.getStatus(), Status.SUCCEEDED);

      testDomain = target.path("testdim").queryParam("sessionid", grillSessionId).request(mediaType).get(XDimension.class);
      assertEquals(testDomain.getName(), "testdim");
      assertTrue(testDomain.getProperties().getProperties().size() >= 2);
      assertTrue(JAXBUtils.mapFromXProperties(testDomain.getProperties()).containsKey("dim.prop2.name"));
      assertEquals(JAXBUtils.mapFromXProperties(testDomain.getProperties()).get("dim.prop2.name"), "dim.prop2.value");
      assertTrue(JAXBUtils.mapFromXProperties(testDomain.getProperties()).containsKey("dimension.foo"));
      assertEquals(JAXBUtils.mapFromXProperties(testDomain.getProperties()).get("dimension.foo"), "dim.bar");
      assertEquals(testDomain.getWeight(), 200.0);
      assertEquals(testDomain.getAttributes().getDimAttributes().size(), 2);

      dim = JAXBUtils.dimensionFromXDimension(testDomain);
      System.out.println("Attributes:" + dim.getAttributes());
      assertNotNull(dim.getAttributeByName("col3"));
      assertNull(dim.getAttributeByName("col2"));
      assertNotNull(dim.getAttributeByName("col1"));

      // drop the dimension
      result = target.path("testdim")
          .queryParam("sessionid", grillSessionId).request(mediaType).delete(APIResult.class);
      assertEquals(result.getStatus(), Status.SUCCEEDED);

      // Now get should give 404
      try {
        JAXBElement<XDimension> got =
            target.path("testdim").queryParam("sessionid", grillSessionId).request(
                mediaType).get(new GenericType<JAXBElement<XDimension>>() {});
        fail("Should have thrown 404, but got" + got.getValue().getName());
      } catch (NotFoundException ex) {
        ex.printStackTrace();
      }

      try {
        result = target.path("testdim")
            .queryParam("sessionid", grillSessionId).request(mediaType).delete(APIResult.class);
        fail("Should have thrown 404, but got" + result.getStatus());
      } catch (NotFoundException ex) {
        ex.printStackTrace();
      }
      */
    }
    finally {
      dropDatabase(DB);
      setCurrentDatabase(prevDb);
    }
  }
}
