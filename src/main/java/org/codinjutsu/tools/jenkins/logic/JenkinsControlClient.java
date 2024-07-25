package org.codinjutsu.tools.jenkins.logic;

import org.codinjutsu.tools.jenkins.security.JenkinsSecurityClient;

import java.net.URI;

public class JenkinsControlClient extends com.offbytwo.jenkins.client.JenkinsHttpClient {

    public JenkinsControlClient(URI uri, JenkinsSecurityClient jenkinsSecurityClient) {
        super(uri, jenkinsSecurityClient.getHttpClient());
        setLocalContext(jenkinsSecurityClient.getHttpClientContext());
    }
}
