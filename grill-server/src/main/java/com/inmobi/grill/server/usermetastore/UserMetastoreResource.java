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
import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.api.GrillSessionHandle;
import com.inmobi.grill.api.StringList;
import com.inmobi.grill.api.usermetastore.ObjectFactory;
import com.inmobi.grill.api.usermetastore.XDomain;
import com.inmobi.grill.server.GrillServices;
import com.inmobi.grill.server.api.usermetastore.UserMetastoreService;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

/**
 * User - metastore resource api
 *
 * This provides api for all things user - metastore.
 */
@Path("usermetastore")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class UserMetastoreResource {
  public static final Logger LOG = LogManager.getLogger(UserMetastoreResource.class);
  public static final APIResult SUCCESS = new APIResult(Status.SUCCEEDED, "");
  public static final ObjectFactory objectFactory = new ObjectFactory();

  public UserMetastoreService getSvc() {
    return (UserMetastoreService)GrillServices.get().getService("usermetastore");
  }

  private void checkSessionId(GrillSessionHandle sessionHandle) {
    if (sessionHandle == null) {
      throw new BadRequestException("Invalid session handle");
    }
  }

  /**
   * API to know if metastore service is up and running
   * 
   * @return Simple text saying it up
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getMessage() {
    return "Metastore is up";
  }

  /**
   * Get all dimensions in the metastore
   *
   * @param sessionid The sessionid in which user is working
   *
   * @return StringList consisting of all the dimension names
   *
   * @throws GrillException
   */
  @GET @Path("domain")
  public StringList getAllDomainNames(@QueryParam("sessionid") GrillSessionHandle sessionid) {
    checkSessionId(sessionid);
    try {
      return new StringList(getSvc().getAllDomainNames(sessionid));
    } catch (GrillException e) {
      LOG.error("Error getting dimensions", e);
      throw new WebApplicationException(e);
    }
  }

  /**
     * Create new domain
     *
     * @param sessionid The sessionid in which user is working
     * @param domain The XDomain representation of the domain
     *
     * @return {@link APIResult} with state {@link Status#SUCCEEDED}, if create was successful.
     * {@link APIResult} with state {@link Status#FAILED}, if create has failed
     */
    @POST
    @Path("domain")
    public APIResult createDomain(@QueryParam("sessionid") GrillSessionHandle sessionid, XDomain domain) {
        checkSessionId(sessionid);
        try {
          System.out.println("Inside createdomain");
            getSvc().createDomain(sessionid, domain);
        } catch (GrillException e) {
            LOG.error("Error creating domain " + domain.getName(), e);
            return new APIResult(Status.FAILED, e.getMessage());
        }
        return SUCCESS;
    }

  /**
   * Get the dimension specified by name
   *
   * @param sessionid The sessionid in which user is working
   * @param domainName The domain name
   *
   * @return JAXB representation of {@link com.inmobi.grill.api.usermetastore.XDomain}
   */
  @GET @Path("/domain/{domainName}")
  public JAXBElement<XDomain> getDomain(@QueryParam("sessionid") GrillSessionHandle sessionid,
                                        @PathParam("domainName") String domainName) throws Exception {
    checkSessionId(sessionid);
    try {
      return objectFactory.createDomain(getSvc().getDomain(sessionid, domainName));
    } catch (GrillException e) {
      checkTableNotFound(e, domainName);
      throw e;
    }
  }

  //TODO. Copied this method over from Metastore. Need to refactor
  private void checkTableNotFound(GrillException e, String table) {
    if (e.getCause() instanceof HiveException) {
      HiveException hiveErr = (HiveException) e.getCause();
      if (hiveErr.getMessage().startsWith("Could not get table")) {
        throw new NotFoundException("Table not found " + table, e);
      }
    }
  }


}
