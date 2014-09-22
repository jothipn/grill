package com.inmobi.grill.server.api.usermetastore;

/*
 * #%L
 * Grill API for server and extensions
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

import java.util.List;


public interface UserMetastoreService {

  /**
   * Create a domain
   *
   * @param sessionid
   * @param domain
   * @throws com.inmobi.grill.api.GrillException
   */
  public void createDomain(GrillSessionHandle sessionid, XDomain domain) throws GrillException;

  /**
   * Drop a domain specified by name
   *
   * @param sessionid
   * @param domainName
   * @throws com.inmobi.grill.api.GrillException
   */
  public void dropDomain(GrillSessionHandle sessionid, String domainName) throws GrillException;

  /**
   * Alter domain specified by name, with new definition
   *
   * @param sessionid
   * @param domainName
   * @param domain
   * @throws com.inmobi.grill.api.GrillException
   */
  public void alterDomain(GrillSessionHandle sessionid, String domainName, XDomain domain) throws GrillException;

  /**
   * Get domain specified by name
   *
   * @param sessionid
   * @param domainName
   * @throws com.inmobi.grill.api.GrillException
   */
  public XDomain getDomain(GrillSessionHandle sessionid, String domainName) throws GrillException;

  /**
   * Get all domains
   *
   * @param sessionid
   *
   * @return returns list of the storage names
   * @throws com.inmobi.grill.api.GrillException
   */
  public List<String> getAllDomainNames(GrillSessionHandle sessionid) throws GrillException;

}
