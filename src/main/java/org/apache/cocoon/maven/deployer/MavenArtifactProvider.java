/*
 * Copyright 2002-2005 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.maven.deployer;

import org.apache.cocoon.deployer.ArtifactProvider;

import org.apache.commons.lang.Validate;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;

import org.apache.maven.plugin.logging.Log;

import org.apache.maven.project.artifact.MavenMetadataSource;

import java.io.File;
import java.util.List;


/**
 * Implementation of a {@link ArtifactProvider} to bridge from Maven 2
 */
public final class MavenArtifactProvider
    implements ArtifactProvider {
    //~ Instance fields ---------------------------------------------------------------------------------

    /** A field */
    private final Log log;

    /** Artifact factory, needed to download source jars for inclusion in classpath. */
    private ArtifactFactory artifactFactory;

    /** Local maven repository. */
    private ArtifactRepository localRepository;

    /** Artifact resolver, needed to download source jars for inclusion in classpath. */
    private ArtifactResolver artifactResolver;

    /** Remote repositories which will be searched for blocks. */
    private List remoteArtifactRepositories;

    /** Artifact resolver, needed to download source jars for inclusion in classpath. */
    private MavenMetadataSource metadataSource;

    //~ Constructors ------------------------------------------------------------------------------------

    /**
     * Creates a new MavenArtifactProvider object.
     *
     * @param artifactResolver DOCUMENT ME!
     * @param artifactFactory DOCUMENT ME!
     * @param localRepository DOCUMENT ME!
     * @param remoteArtifactRepositories DOCUMENT ME!
     * @param metadataSource DOCUMENT ME!
     * @param log DOCUMENT ME!
     */
    public MavenArtifactProvider(final ArtifactResolver artifactResolver,
                                 final ArtifactFactory artifactFactory,
                                 final ArtifactRepository localRepository,
                                 final List remoteArtifactRepositories,
                                 final MavenMetadataSource metadataSource,
                                 final Log log) {
        super();
        this.log = log;
        this.artifactFactory = artifactFactory;
        this.artifactResolver = artifactResolver;
        this.remoteArtifactRepositories = remoteArtifactRepositories;
        this.metadataSource = metadataSource;
        this.localRepository = localRepository;
    }

    //~ Methods -----------------------------------------------------------------------------------------

    /**
     * @see org.apache.cocoon.deployer.ArtifactProvider#getArtifact(java.lang.String)
     */
    public final File getArtifact(final String artifactId) {
        try {
            final Artifact artifact = getArtifactFor(artifactId);

            return artifact.getFile();
        } catch(Exception ex) {
            this.log.error(ex);
        }

        return null;
    }

    /**
     * @see org.apache.cocoon.deployer.ArtifactProvider#getArtifact(java.lang.String[])
     */
    public final File[] getArtifact(String[] artifactIds) {
    	
    	// FIXME (reinhard) this doesn't use transitive dependency resolving!
    	
    	Validate.notNull(artifactIds, "artifactIds mustn't be null");
    	Validate.noNullElements(artifactIds, "Quering a 'null'-artifact is not possible");
        final File[] files = new File[artifactIds.length];

        for(int i = 0; i < artifactIds.length; i++) {
            files[i] = getArtifact(artifactIds[i]);
        }

        return files;
    }

    /**
     * Splits up an ArtifactId into a Dependency object
     *
     * @param artifactSpec The <code>artifactSpec</code>
     *
     * @return Value
     *
     * @throws ArtifactResolutionException The <code>ArtifactResolutionException</code>
     * @throws ArtifactNotFoundException The <code>ArtifactNotFoundException</code>
     */
    private Artifact getArtifactFor(final String artifactSpec)
        throws ArtifactResolutionException, ArtifactNotFoundException {
        this.log.debug("[MavenArtifactProvider.getArtifactFor: artifactSpec=" + artifactSpec);    	
    	
        final int p1 = artifactSpec.indexOf(':');
        Validate.isTrue(p1 > 0, "invalid artifact specifier: " + artifactSpec);

        final String groupId = artifactSpec.substring(0, p1);
        final int p2 = artifactSpec.indexOf(':', p1 + 1);
        Validate.isTrue(p2 > 0, "invalid artifact specifier: " + artifactSpec);

        final String artifactId = artifactSpec.substring(p1 + 1, p2);
        final int p3 = artifactSpec.indexOf(':', p2 + 1);
        System.out.println("p3: " + p3);
        final String version =
            (p3 > 0) ? artifactSpec.substring(p2 + 1, p3) : artifactSpec.substring(p2 + 1);
        final String type = (p3 > 0) ? artifactSpec.substring(p3 + 1) : "jar";
        
        this.log.debug("[MavenArtifactProvider.getArtifactFor: groupId=" + groupId);
        this.log.debug("[MavenArtifactProvider.getArtifactFor: artifactId=" + artifactId);
        this.log.debug("[MavenArtifactProvider.getArtifactFor: version=" + version);       
        this.log.debug("[MavenArtifactProvider.getArtifactFor: type=" + type);            
        
        final Artifact artifact = 
        	this.artifactFactory.createBuildArtifact(groupId, artifactId, version, type);
        
        this.artifactResolver.resolve(artifact, this.remoteArtifactRepositories, 
            this.localRepository);

        this.log.debug("[MavenArtifactProvider.getArtifactFor: found " + 
            artifact.getFile().getAbsolutePath());
        
        return artifact;
    }
}
