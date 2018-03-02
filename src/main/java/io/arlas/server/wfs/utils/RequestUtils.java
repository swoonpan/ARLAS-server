/*
 * Licensed to Gisaïa under one or more contributor
 * license agreements. See the NOTICE.txt file distributed with
 * this work for additional information regarding copyright
 * ownership. Gisaïa licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.arlas.server.wfs.utils;

import io.arlas.server.app.ArlasServerConfiguration;
import io.arlas.server.exceptions.ArlasConfigurationException;
import org.deegree.protocol.wfs.WFSRequestType;

public class RequestUtils {

    public static WFSRequestType getRequestTypeByName(String requestName, ArlasServerConfiguration configuration )
            throws ArlasConfigurationException {
        if ( configuration.wfsConfiguration.getSupportedRequest().indexOf(requestName)<0 ) {
            return null;
        }else if(WFSRequestType.valueOf(requestName)==null){
            String msg = "Request type '" + requestName + "' is not supported.";
            return null;
        }
        return WFSRequestType.valueOf(requestName);
    }
}