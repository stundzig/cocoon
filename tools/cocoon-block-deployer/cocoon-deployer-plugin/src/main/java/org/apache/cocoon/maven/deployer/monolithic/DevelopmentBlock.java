/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.maven.deployer.monolithic;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A container for configuration parameters. Depending on the localPath
 * parameter, all other 'path' parameters are set.
 * 
 * @version $Id$
 */
public class DevelopmentBlock {
    // TODO this won't work if user chooses a different resources directory in
    // pom.xml. There should be a way to get this path using Maven API.
    private static final String RESOURCES_DIR = "src/main/resources/";
    public String artifactId;
    public String groupId;

    public String springConfPath;
    public String xconfConfPath;
    public String sitemapAdditionsConfPath;
    public String targetClassesPath;
    public String cobInfPath;
    public String propsPath;
    public String xPatchPath;

    public void setLocalPath(String localPath) throws FileNotFoundException {
        File localPathDir = new File(localPath);
        if (!localPathDir.exists()) {
            throw new FileNotFoundException("Directory '" + localPath + "' does not exist!");
        }

        springConfPath = checkDir(new File(localPath, RESOURCES_DIR + "META-INF/spring"));
        xconfConfPath = checkDir(new File(localPath, RESOURCES_DIR + "META-INF/legacy/xconf"));
        sitemapAdditionsConfPath = checkDir(new File(localPath, RESOURCES_DIR + "META-INF/legacy/sitemap-additions"));
        xPatchPath = checkDir(new File(localPath, RESOURCES_DIR + "META-INF/xpatch"));
        targetClassesPath = checkDir(new File(localPath, "target/classes"));
        cobInfPath = checkDir(new File(localPath, RESOURCES_DIR + "COB-INF"));
        propsPath = checkDir(new File(localPath, RESOURCES_DIR + "META-INF/properties"));
    }

    private String checkDir(File dir) {
        if (dir.exists()) {
            return dir.toURI().toString();
        }
        return null;
    }

}