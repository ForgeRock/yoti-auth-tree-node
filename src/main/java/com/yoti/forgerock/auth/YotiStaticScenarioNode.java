/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

package com.yoti.forgerock.auth;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.RequiredValueValidator;
import org.apache.commons.lang.text.StrSubstitutor;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import com.google.inject.assistedinject.Assisted;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A node that checks to see if zero-page login headers have specified username and shared key
 * for this request.
 */
@Node.Metadata(outcomeProvider = AbstractYotiAuthNode.YotiAuthOutcomeProvider.class,
        configClass = YotiStaticScenarioNode.Config.class)
public class YotiStaticScenarioNode extends AbstractYotiAuthNode<YotiStaticScenarioNode.Config> {

    /**
     * Configuration for the node.
     */
    public interface Config extends AbstractYotiAuthNode.Config {

        /**
         * the scenario id.
         *
         * @return the scenario id
         */
        @Attribute(order = 250, validators = { RequiredValueValidator.class })
        String scenarioId();

    }

    private static final String SCRIPT_TEMPLATE_LOCATION = "com/yoti/forgerock/auth/integrateYoti.js";
    private static final String YOTI_SCRIPT_HOST = "https://yoti.com";
    private static final String YOTI_SHARE_TYPE = "STATIC";

    private final String scriptTemplate;

    private String loadScript() throws NodeProcessException {
        try {
            URL resource = Resources.getResource(SCRIPT_TEMPLATE_LOCATION);
            return Resources.toString(resource, Charsets.UTF_8);
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public YotiStaticScenarioNode(@Assisted Config config) throws NodeProcessException {
        super(config);
        scriptTemplate = loadScript();
    }

    @Override
    protected Callback createYotiLoginCallback() {
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("clientSdkId", config.sdkId());
        valuesMap.put("scenarioId", config.scenarioId());
        valuesMap.put("yotiShareType", YOTI_SHARE_TYPE);
        valuesMap.put("yotiScriptSource", YOTI_SCRIPT_HOST + config.yotiScriptPath());

        String script = new StrSubstitutor(valuesMap).replace(scriptTemplate);

        return new ScriptTextOutputCallback(script);
    }

}
