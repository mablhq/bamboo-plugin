package com.mabl;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
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
    static final String PLUGIN_USER_AGENT = "mabl-bamboo-plugin/" + PLUGIN_VERSION;
    static final String MABL_REST_API_BASE_URL = "https://api.mabl.com";
    static final int REQUEST_TIMEOUT_MILLISECONDS = 60000;
    static final int CONNECTION_SECONDS_TO_LIVE = 30;
    static final int RETRY_HANDLER_MAX_RETRIES = 5;
    static final long RETRY_HANDLER_RETRY_INTERVAL = 6000L;
    static final String REST_API_KEY_FIELD = "mablRestApiKey";
    static final String REST_API_KEY_LABEL_PROPERTY = "createdeployment.restapikey.label";
    static final String ENVIRONMENT_ID_FIELD = "mablEnvironmentId";
    static final String ENVIRONMENT_ID_LABEL_PROPERTY = "createdeployment.environmentid.label";
    static final String APPLICATION_ID_FIELD = "mablApplicationId";
    static final String APPLICATION_ID_LABEL_PROPERTY = "createdeployment.applicationid.label";
    static final String PLAN_TAGS_LABEL_PROPERTY = "createdeployment.plantags.label";
    static final String PLAN_TAGS_FIELD = "mablPlanTags";
    static final String MABL_LOG_OUTPUT_PREFIX = "[mabl]";


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
}