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

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.sm.validation.URLValidator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.RedirectCallback;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

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
         * Base URI for redirecting to Yoti
         *
         * @return Base URI for redirecting to Yoti
         */
        @Attribute(order = 400, validators = { URLValidator.class })
        default String redirectUri() {
            return "https://www.yoti.com/connect/";
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
    }

    @Override
    protected Callback createYotiLoginCallback() {
        String uri = this.config.redirectUri() + this.config.appId();
        debug.message("YotiStaticScenarioNode: Creating redirect callback to {}", uri);
        RedirectCallback redirectCallback = new RedirectCallback(uri, null, "GET");
        redirectCallback.setTrackingCookie(true);
        return redirectCallback;
    }

}
