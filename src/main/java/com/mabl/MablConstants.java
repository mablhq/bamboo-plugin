package com.mabl;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * mabl custom build step
 */
class MablConstants {

    // CreateDeployment constants
    static final Set<String> COMPLETE_STATUSES = ImmutableSet.of(
            "succeeded",
            "failed",
            "cancelled",
            "completed",
            "terminated"
    );
    static final long EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS = 10000;

    private static final String PLUGIN_VERSION = getPluginVersion();
    private static final String PLUGIN_VERSION_UNKNOWN = "unknown";
    static final String PLUGIN_USER_AGENT = String.format("mabl-bamboo-plugin/%s (JVM: %s, Bamboo: %s)",
                PLUGIN_VERSION, System.getProperty("java.version"), getBambooVersion());
    static final String MABL_REST_API_BASE_URL = "https://api.mabl.com";
    static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    static final Duration CONNECTION_SECONDS_TO_LIVE = Duration.ofSeconds(30);;
    static final int RETRY_HANDLER_MAX_RETRIES = 5;
    static final Duration RETRY_HANDLER_RETRY_INTERVAL = Duration.ofMillis(6000);
    static final String MABL_SEND_VARIABLES_FIELD = "mabl.sendvariables";
    static final String REST_API_KEY_FIELD = "mablRestApiKey";
    static final String REST_API_KEY_LABEL_PROPERTY = "createdeployment.restapikey.label";
    static final String APPLICATION_ID_FIELD = "mablApplicationId";
    static final String APPLICATION_ID_LABEL_PROPERTY = "createdeployment.applicationid.label";
    static final String ENVIRONMENT_ID_FIELD = "mablEnvironmentId";
    static final String ENVIRONMENT_ID_LABEL_PROPERTY = "createdeployment.environmentid.label";
    static final String PLAN_LABELS_FIELD = "mablPlanLabels";
    static final String PLAN_LABELS_LABEL_PROPERTY = "createdeployment.planlabels.label";
    static final String PROXY_ADDRESS_FIELD = "mablProxyAddress";
    static final String PROXY_ADDRESS_LABEL_PROPERTY = "createdeployment.proxyaddress.label";
    static final String PROXY_USERNAME_FIELD = "mablProxyUsername";
    static final String PROXY_USERNAME_LABEL_PROPERTY = "createdeployment.proxyusername.label";
    static final String PROXY_PASSWORD_FIELD = "mablProxyPassword";
    static final String PROXY_PASSWORD_LABEL_PROPERTY = "createdeployment.proxypassword.label";
    static final String MABL_LOG_OUTPUT_PREFIX = "[mabl]";
    static final String MABL_JUNIT_REPORT_XML = "report.xml";


    private static final String PLUGIN_SYMBOLIC_NAME = "com.mabl.bamboo.plugin";
    /**
     * Dynamically grab the plugin version, so we can't forget to update it on release.
     *
     * @return plugin version, or {@link #PLUGIN_VERSION_UNKNOWN} on error/missing.
     */
    private static String getPluginVersion() {
        try {
            final Enumeration<URL> resources = MablConstants.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                final Manifest manifest = new Manifest(resources.nextElement().openStream());

                String title = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
                if (PLUGIN_SYMBOLIC_NAME.equalsIgnoreCase(title)) {
                    final String version = manifest.getMainAttributes().getValue("Bundle-Version");
                    return version != null && !version.isEmpty() ? version : PLUGIN_VERSION_UNKNOWN;
                }
            }
        } catch (IOException ignored) {}
        return PLUGIN_VERSION_UNKNOWN;
    }

    private static String getBambooVersion() {
        return Optional.ofNullable(System.getProperty("atlassian.sdk.version")).
                orElseGet(() -> {
                    final String pluginVersion = System.getenv("AMPS_PLUGIN_VERSION");
                    return StringUtils.isEmpty(pluginVersion) ? "unknown" : pluginVersion;
                });
    }
}