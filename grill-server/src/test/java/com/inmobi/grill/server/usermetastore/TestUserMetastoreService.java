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
import com.inmobi.grill.api.metastore.XDimAttribute;
import com.inmobi.grill.api.metastore.XDimAttributes;
import com.inmobi.grill.api.usermetastore.ObjectFactory;
import com.inmobi.grill.api.usermetastore.XDomain;
import com.inmobi.grill.api.usermetastore.XDomainClass;
import com.inmobi.grill.api.usermetastore.XSchema;
import com.inmobi.grill.server.GrillJerseyTest;
import com.inmobi.grill.server.GrillServices;
import com.inmobi.grill.server.metastore.CubeMetastoreServiceImpl;
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
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
    APIResult result = dbTarget.queryParam("sessionid", grillSessionId).request(mediaType).put(Entity.xml(dbName), APIResult.class);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private String getCurrentDatabase() throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases/current");
    Invocation.Builder builder = dbTarget.queryParam("sessionid", grillSessionId).request(mediaType);
    String response = builder.get(String.class);
    return response;
  }

  private void createDatabase(String dbName) throws Exception {
    WebTarget dbTarget = target().path("metastore").path("databases");

    APIResult result = dbTarget.queryParam("sessionid", grillSessionId).request(mediaType).post(Entity.xml(dbName), APIResult.class);
    assertNotNull(result);
    assertEquals(result.getStatus(), APIResult.Status.SUCCEEDED);
  }

  private XDomain createDomain(String domainName) throws Exception {
    XDomain domain = userObjectFactory.createXDomain();
    domain.setName(domainName);
    domain.setClazz(XDomainClass.DEFAULT);
    domain.setDescription("Domain for storing location");
    domain.setOwner("LatLonger");


    XDimAttributes schemaAttribs = cubeObjectFactory.createXDimAttributes();

    XDimAttribute locid = cubeObjectFactory.createXDimAttribute();
    locid.setName("locationId");
    locid.setType("long");
    locid.setDescription("Location Id");
    locid.setNullable(false);
    locid.setConstraints(">0");

    schemaAttribs.getDimAttributes().add(locid);

    XDimAttribute zip = cubeObjectFactory.createXDimAttribute();
    zip.setName("zip");
    zip.setType("string");
    zip.setDescription("ZIP COde");
    zip.setNullable(true);

    schemaAttribs.getDimAttributes().add(zip);

    XSchema schema = userObjectFactory.createXSchema();
    schema.setAttributes(schemaAttribs);
    schema.setName("Location Schema");

    domain.setSchema(schema);
    return domain;
  }

  @Test
  public void testDomain() throws Exception {
    final String DB = dbPFX + "test_dimension";
    String prevDb = getCurrentDatabase();
    createDatabase(DB);
    setCurrentDatabase(DB);
    try {
      XDomain domain = createDomain("location");
      final WebTarget target = target().path("usermetastore").path("domain");

      // create
      APIResult result = target.queryParam("sessionid", grillSessionId).request(
          mediaType).post(Entity.xml(userObjectFactory.createDomain(domain)), APIResult.class);
      assertNotNull(result);
      assertEquals(result.getStatus(), Status.SUCCEEDED);

      // getall
      StringList domains = target.queryParam("sessionid", grillSessionId).request(mediaType).get(StringList.class);
      boolean foundDim = false;
      for (String c : domains.getElements()) {
        if (c.equalsIgnoreCase("location")) {
          foundDim = true;
          break;
        }
      }

      assertTrue(foundDim);
      assertEquals(domains.getElements().size(), 2);

      // get
      XDomain testDim = target.path("location").queryParam("sessionid", grillSessionId).request(mediaType).get(XDomain.class);
      assertEquals(testDim.getName(), "location");
      assertEquals(testDim.getDescription(), "Location Id");

      /*
      assertTrue(JAXBUtils.mapFromXProperties(testDim.getProperties()).containsKey("dimension.foo"));
      assertEquals(JAXBUtils.mapFromXProperties(testDim.getProperties()).get("dimension.foo"), "dim.bar");
      assertEquals(testDim.getWeight(), 100.0);
      assertEquals(testDim.getAttributes().getDimAttributes().size(), 2);


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

      testDim = target.path("testdim").queryParam("sessionid", grillSessionId).request(mediaType).get(XDimension.class);
      assertEquals(testDim.getName(), "testdim");
      assertTrue(testDim.getProperties().getProperties().size() >= 2);
      assertTrue(JAXBUtils.mapFromXProperties(testDim.getProperties()).containsKey("dim.prop2.name"));
      assertEquals(JAXBUtils.mapFromXProperties(testDim.getProperties()).get("dim.prop2.name"), "dim.prop2.value");
      assertTrue(JAXBUtils.mapFromXProperties(testDim.getProperties()).containsKey("dimension.foo"));
      assertEquals(JAXBUtils.mapFromXProperties(testDim.getProperties()).get("dimension.foo"), "dim.bar");
      assertEquals(testDim.getWeight(), 200.0);
      assertEquals(testDim.getAttributes().getDimAttributes().size(), 2);

      dim = JAXBUtils.dimensionFromXDimension(testDim);
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
