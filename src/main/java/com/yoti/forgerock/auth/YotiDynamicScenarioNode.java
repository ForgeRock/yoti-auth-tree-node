package com.yoti.forgerock.auth;

import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.DATE_OF_BIRTH;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.EMAIL_ADDRESS;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.FAMILY_NAME;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.FULL_NAME;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.GIVEN_NAMES;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.NATIONALITY;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.PHONE_NUMBER;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.SELFIE;
import static com.yoti.api.attributes.AttributeConstants.HumanProfileAttributes.STRUCTURED_POSTAL_ADDRESS;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;

import com.yoti.api.client.shareurl.DynamicScenario;
import com.yoti.api.client.shareurl.DynamicScenarioBuilder;
import com.yoti.api.client.shareurl.DynamicShareException;
import com.yoti.api.client.shareurl.policy.DynamicPolicy;
import com.yoti.api.client.shareurl.policy.DynamicPolicyBuilder;
import com.yoti.api.client.shareurl.policy.WantedAttribute;
import com.yoti.api.client.shareurl.policy.WantedAttributeBuilder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.sm.RequiredValueValidator;
import org.apache.commons.lang.text.StrSubstitutor;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;

@Node.Metadata(outcomeProvider = AbstractYotiAuthNode.YotiAuthOutcomeProvider.class,
        configClass = YotiDynamicScenarioNode.Config.class)
public class YotiDynamicScenarioNode extends AbstractYotiAuthNode<YotiDynamicScenarioNode.Config> {

    /**
     * Configuration for the node.
     */
    public interface Config extends AbstractYotiAuthNode.Config {

        /**
         * Base URI for redirecting to Yoti
         *
         * @return Base URI for redirecting to Yoti
         */
        @Attribute(order = 400, validators = { RequiredValueValidator.class })
        default String callbackUrl() {
            return "/am/XUI/?service=YoTreeDynamic";
        }

        /**
         * Base URI for redirecting to Yoti
         *
         * @return Base URI for redirecting to Yoti
         */
        @Attribute(order = 500, validators = { RequiredValueValidator.class })
        default String yotiScriptPath() {
            return "/clients/browser.2.3.0.js";
        }

        /**
         * Attributes to request in the share
         *
         * @return List of attributes to request in the share
         */
        @Attribute(order = 2000, validators = { RequiredValueValidator.class })
        default List<String> requestedAttributes() {
            final List<String> defaultList = new ArrayList<>();
            defaultList.add(FULL_NAME);
            defaultList.add(FAMILY_NAME);
            defaultList.add(GIVEN_NAMES);
            defaultList.add(DATE_OF_BIRTH);
            defaultList.add(NATIONALITY);
            defaultList.add(EMAIL_ADDRESS);
            defaultList.add(PHONE_NUMBER);
            defaultList.add(STRUCTURED_POSTAL_ADDRESS);
            defaultList.add(SELFIE);
            return defaultList;
        }

        /**
         * Require selfie authentication, or not
         *
         * @return Whether or not to require selfie authentication
         */
        @Attribute(order = 2100, validators = { RequiredValueValidator.class })
        default boolean requireSelfieAuthentication() {
            return false;
        }

    }

    private static final String SCRIPT_TEMPLATE_LOCATION = "com/yoti/forgerock/auth/integrateYoti.js";
    private static final String YOTI_SCRIPT_HOST = "https://sdk.yoti.com";

    private final String scriptTemplate;

    @Inject
    public YotiDynamicScenarioNode(@Assisted Config config) throws NodeProcessException {
        super(config);
        //TODO Need to add some caching to this. This file IO will happen every time the node is executed as nodes
        // get garbage collected each time they are used.
        scriptTemplate = loadScript();
    }

    private String loadScript() throws NodeProcessException {
        try {
            URL resource = Resources.getResource(SCRIPT_TEMPLATE_LOCATION);
            return Resources.toString(resource, Charsets.UTF_8);
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    @Override
    protected Callback createYotiLoginCallback() throws NodeProcessException {
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("appId", config.appId());
        valuesMap.put("url", createRedirectUrl());
        valuesMap.put("yotiScriptSource", YOTI_SCRIPT_HOST + config.yotiScriptPath());

        String script = new StrSubstitutor(valuesMap).replace(scriptTemplate);

        return new ScriptTextOutputCallback(script);
    }

    private String createRedirectUrl() throws NodeProcessException {
        DynamicPolicyBuilder dynamicPolicyBuilder = DynamicPolicyBuilder.newInstance();
        config.requestedAttributes().forEach(requestedAtt -> {
            WantedAttribute wantedAttribute = WantedAttributeBuilder.newInstance()
                    .withName(requestedAtt)
                    .build();
            dynamicPolicyBuilder.withWantedAttribute(wantedAttribute);
        });

        if (config.requireSelfieAuthentication()) {
            dynamicPolicyBuilder.withSelfieAuthorisation(true);
        }

        DynamicPolicy dynamicPolicy = dynamicPolicyBuilder
                .withWantedRememberMe(true)
                .build();
        DynamicScenario dynamicScenario = DynamicScenarioBuilder.newInstance()
                .withPolicy(dynamicPolicy)
                .withCallbackEndpoint(config.callbackUrl())
                .build();
        try {
            debug.message("Creating a URL for the Dynamic Scenario");
            return yotiClient.createShareUrl(dynamicScenario).getUrl();
        } catch (DynamicShareException e) {
            debug.error("Error creating the URL", e);
            throw new NodeProcessException(e);
        }
    }

}
