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

package io.arlas.server.ogc.common.utils;

import io.arlas.server.exceptions.OGCException;
import io.arlas.server.exceptions.OGCExceptionCode;

import java.util.SortedSet;
import java.util.TreeSet;

public class VersionUtils {

    public static Version checkVersion(Version requestedVersion, String supportedVersion) {
        Version version = requestedVersion;
        SortedSet<Version> offeredVersions = new TreeSet<Version>();
        try {
            offeredVersions.add(getVersion(supportedVersion));
        } catch (OGCException e) {
            new OGCException(OGCExceptionCode.INVALID_PARAMETER_VALUE, "INVALID VERSION", "version");
        }
        if (!offeredVersions.contains(version)) {
            new OGCException(OGCExceptionCode.INVALID_PARAMETER_VALUE, "INVALID VERSION", "version");
        }
        return version;
    }

    public static Version getVersion(String versionString) throws OGCException {
        Version version = null;
        if (versionString != null && !"".equals(versionString)) {
            try {
                version = Version.parseVersion(versionString);

            } catch (OGCException e) {
                throw new OGCException(OGCExceptionCode.INVALID_PARAMETER_VALUE, e.getMessage(), "version");
            }
        }
        return version;
    }
}