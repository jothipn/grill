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

import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.api.GrillSessionHandle;
import com.inmobi.grill.api.usermetastore.XDomain;
import com.inmobi.grill.server.GrillService;
import com.inmobi.grill.server.api.usermetastore.UserMetastoreService;
import com.inmobi.grill.server.session.GrillSessionImpl;
import org.apache.hadoop.hive.ql.cube.metadata.CubeMetastoreClient;
import org.apache.hadoop.hive.ql.cube.metadata.Dimension;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hive.service.cli.CLIService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class UserMetastoreServiceImpl extends GrillService implements UserMetastoreService {
  public static final Logger LOG = LogManager.getLogger(UserMetastoreServiceImpl.class);

  public UserMetastoreServiceImpl(CLIService cliService) {
    super("usermetastore", cliService);
  }


  synchronized UserMetastoreClient getClient(GrillSessionHandle sessionid) throws GrillException {
    return ((GrillSessionImpl) getSession(sessionid)).getUserMetastoreClient();
  }

  synchronized CubeMetastoreClient getCubeClient(GrillSessionHandle sessionid) throws GrillException {
    return ((GrillSessionImpl) getSession(sessionid)).getCubeMetastoreClient();
  }

  @Override
  public void createDomain(GrillSessionHandle sessionid, XDomain domain) throws GrillException {
    try {
      //TODO. Understand what acquire does...
      acquire(sessionid);
      getClient(sessionid).createDomain(domain);
      LOG.info("Created domain " + domain.getName());
    } catch (HiveException e) {
      throw new GrillException(e);
    } catch (ParseException e) {
      throw new GrillException(e);
    } finally {
      release(sessionid);
    }
  }

  @Override
  public void dropDomain(GrillSessionHandle sessionid, String domainName) throws GrillException {
  }

  @Override
  public void alterDomain(GrillSessionHandle sessionid, String domainName, XDomain domain) throws GrillException {

  }

  @Override
  public XDomain getDomain(GrillSessionHandle sessionid, String domainName) throws GrillException {
    try {
      acquire(sessionid);
      return getClient(sessionid).getDomain(domainName);
    } catch (HiveException e) {
      throw new GrillException(e);
    } finally {
      release(sessionid);
    }
  }

  @Override
  public List<String> getAllDomainNames(GrillSessionHandle sessionid) throws GrillException {
    try {
      acquire(sessionid);
      List<XDomain> domains = getClient(sessionid).getAllDomains();
      if (domains != null && !domains.isEmpty()) {
        List<String> names = new ArrayList<String>(domains.size());
        for (XDomain d : domains) {
          names.add(d.getName());
        }
        return names;
      }
    } catch (HiveException e) {
      throw new GrillException(e);
    } finally {
      release(sessionid);
    }
    return null;
  }

  //TODO Added for test case purpose. Need to refactor
  Dimension getDimensionFromXDomain(GrillSessionHandle sessionid, XDomain domain) throws GrillException {
    return getClient(sessionid).getDimensionFromXDomain(domain);
  }
}
