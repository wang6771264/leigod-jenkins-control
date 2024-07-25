
package org.codinjutsu.tools.jenkins.api;

import com.cdancy.jenkins.rest.JenkinsApi;
import com.cdancy.jenkins.rest.JenkinsAuthentication;
import com.cdancy.jenkins.rest.auth.AuthenticationType;
import com.cdancy.jenkins.rest.config.JenkinsAuthenticationModule;
import com.cdancy.jenkins.rest.domain.job.BuildInfo;
import com.cdancy.jenkins.rest.domain.job.JobInfo;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

import static org.jclouds.util.Strings2.toStringAndClose;

/**
 * Base class for Jenkins mock tests and some Live tests.
 */
public class BaseJenkinsApi {
    public static final String USERNAME_APITOKEN = "user:token";

    protected final String provider;

    public BaseJenkinsApi() {
        provider = "jenkins";
    }

    /**
     * Create API from passed URL.
     *
     * The default authentication is the ApiToken, for it requires no crumb and simplifies mockTests expectations.
     *
     * @param url endpoint of instance.
     * @return instance of JenkinsApi.
     */
    public JenkinsApi api(final URL url) {
        return api(url, AuthenticationType.UsernameApiToken, USERNAME_APITOKEN);
    }

    /**
     * Create API for Anonymous access using the passed URL.
     *
     * @param url endpoint of instance.
     * @return instance of JenkinsApi.
     */
    public JenkinsApi anonymousAuthApi(final URL url) {
        return api(url, AuthenticationType.Anonymous, AuthenticationType.Anonymous.name().toLowerCase());
    }

    /**
     * Create API for the given authentication type and string.
     *
     * @param url the endpoint of the instance.
     * @param authType the type of authentication.
     * @param authString the string to use as the credential.
     * @return instance of JenkinsApi.
     */
    public JenkinsApi api(final URL url, final AuthenticationType authType, final String authString) {
        final JenkinsAuthentication creds = creds(authType, authString);
        final JenkinsAuthenticationModule credsModule = new JenkinsAuthenticationModule(creds);
        return ContextBuilder.newBuilder(provider)
                .endpoint(url.toString())
                .overrides(setupProperties())
                .buildApi(JenkinsApi.class);
    }

    /**
     * Create the Jenkins Authentication instance.
     *
     * @param authType authentication type. Falls back to anonymous when null.
     * @param authString the authentication string to use (username:password, username:apiToken, or base64 encoded).
     * @return an authentication instance.
     */
    public JenkinsAuthentication creds(final AuthenticationType authType, final String authString) {
        final JenkinsAuthentication.Builder authBuilder = JenkinsAuthentication.builder();
        if (authType == AuthenticationType.UsernamePassword) {
            authBuilder.credentials(authString);
        } else if (authType == AuthenticationType.UsernameApiToken) {
            authBuilder.apiToken(authString);
        }
        // Anonymous authentication is the default when not specified
        return authBuilder.build();
    }

    protected Properties setupProperties() {
        final Properties properties = new Properties();
        properties.setProperty(Constants.PROPERTY_MAX_RETRIES, "0");
        properties.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "60");
        return properties;
    }

    /**
     * Get the String representation of some resource to be used as payload.
     *
     * @param resource
     *            String representation of a given resource
     * @return payload in String form
     */
    public String payloadFromResource(String resource) {
        try {
            return new String(toStringAndClose(Objects.requireNonNull(getClass().getResourceAsStream(resource))).getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void main(String[] args) throws MalformedURLException {
        BaseJenkinsApi api = new BaseJenkinsApi();
        JenkinsApi jenkinsApi = api.api(new URL("http://172.31.4.7:8090/jenkins"),
                AuthenticationType.UsernameApiToken, "test:118df27d57cb2bd00a8d54675158718729");
        JobInfo jobInfo = jenkinsApi.jobsApi().jobInfo(null, "leigod-nn-bms-test");
        BuildInfo lastBuildInfo = jobInfo.lastBuild();
        System.out.println(jobInfo);
    }
}
