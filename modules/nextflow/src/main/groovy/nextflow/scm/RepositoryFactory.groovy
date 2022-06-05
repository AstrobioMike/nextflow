/*
 * Copyright 2020-2022, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nextflow.scm

import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import nextflow.exception.AbortOperationException
import nextflow.plugin.Plugins
import org.pf4j.ExtensionPoint
/**
 * Implements a factory for Git {@link RepositoryProvider}
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
class RepositoryFactory implements ExtensionPoint {

    /**
     * Create a new instance of the {@link RepositoryProvider} if a matching configuration
     * is found
     *
     * @param config
     *      The {@link ProviderConfig} object holding the Git configuration
     * @param project
     *      The Git project name e.g. `nextflow-io/hello`
     * @return
     *      An instance of a {@link RepositoryProvider} that matches the specified config
     *      or {@code null} if no match is found
     */
    protected RepositoryProvider newInstance(ProviderConfig config, String project) {

        switch(config.platform) {
            case 'github':
                return new GithubRepositoryProvider(project, config)

            case 'bitbucket':
                return new BitbucketRepositoryProvider(project, config)

            case 'bitbucketserver':
                return new BitbucketServerRepositoryProvider(project, config)

            case 'gitlab':
                return new GitlabRepositoryProvider(project, config)

            case 'gitea':
                return new GiteaRepositoryProvider(project, config)

            case 'azurerepos':
                return new AzureRepositoryProvider(project, config)

            case 'file':
                // remove the 'local' prefix for the file provider
                def localName = project.tokenize('/').last()
                return new LocalRepositoryProvider(localName, config)

            default:
                return null
        }
    }

    protected ProviderConfig findConfig(List<ProviderConfig> providers, GitUrl url) {
        final result = providers.find(it -> it.domain == url.domain)
        log.debug "Git url=$url -> config=$result"
        return result
    }

    @Memoized
    private static List<RepositoryFactory> factories() {
        // scan for available plugins
        final result = Plugins.getPriorityExtensions(RepositoryFactory)
        log.debug "Found Git repository result: ${ result.collect(it->it.class.simpleName) }"
        return result
    }

    static RepositoryProvider create( ProviderConfig config, String project ) {
        // scan all installed Git repository factories and find the first
        // returning an provider instance for the specified parameters
        final provider = factories().findResult( it -> it.newInstance(config, project) )
        if( !provider ) {
            throw new AbortOperationException("Unable to find a Git repository provider matching platform: ${config.platform}")
        }
        // return matching provider
        return provider
    }

    static ProviderConfig detect(List<ProviderConfig> providers, GitUrl url ) {
        final provider = factories().findResult( it -> it.findConfig(providers, url) )
        if( !provider ) {
            throw new AbortOperationException("Unable to find a Git provider config for ${url}")
        }
        // return matching provider
        return provider
    }

}
