package com.yoti.forgerock.auth;

import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.google.common.base.Optional;
import com.yoti.api.attributes.AttributeConstants;
import com.yoti.api.client.ActivityDetails;
import com.yoti.api.client.FileKeyPairSource;
import com.yoti.api.client.KeyPairSource;
import com.yoti.api.client.ProfileException;
import com.yoti.api.client.YotiClient;
import com.yoti.api.client.YotiClientBuilder;

import com.google.common.collect.ImmutableList;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

public abstract class AbstractYotiAuthNode<T extends AbstractYotiAuthNode.Config> implements Node {

    private static final String YOTI_AUTH_NODE = "YotiAuthNode";
    private static final String BUNDLE = "com/yoti/forgerock/auth/AbstractYotiAuthNode";

    protected static final String TOKEN = "token";
    protected static final String SS_USER_INFO = "userInfo";
    protected static final String SS_ATTRIBUTES = "attributes";
    protected static final String SS_USER_NAMES = "userNames";

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Private key for the Yoti Application.
         *
         * @return private key PEM.
         */
        @Attribute(order = 100, validators = { RequiredValueValidator.class })
        String pemFileLocation();

        /**
         * the sdk id.
         *
         * @return the sdk id
         */
        @Attribute(order = 200, validators = { RequiredValueValidator.class })
        String sdkId();

        @Attribute(order = 800, validators = { RequiredValueValidator.class })
        default Map<String, String> attributeMappings() {
            final Map<String, String> defaultMappings = new HashMap<>();
            defaultMappings.put(AttributeConstants.HumanProfileAttributes.FULL_NAME, "cn");
            defaultMappings.put(AttributeConstants.HumanProfileAttributes.FAMILY_NAME, "sn");
            defaultMappings.put(AttributeConstants.HumanProfileAttributes.GIVEN_NAMES, "givenName");
            defaultMappings.put(AttributeConstants.HumanProfileAttributes.EMAIL_ADDRESS, "mail");
            defaultMappings.put(AttributeConstants.HumanProfileAttributes.PHONE_NUMBER, "telephoneNumber");
            return defaultMappings;
        }

        /**
         * Script path to use for the QR Modal
         *
         * @return the script path to use for the QR Modal
         */
        @Attribute(order = 500, validators = { RequiredValueValidator.class })
        default String yotiScriptPath() {
            return "/share/client/";
        }

        /**
         * Account provider class.
         *
         * @return the string
         */
        @Attribute(order = 900, validators = { RequiredValueValidator.class })
        default String accountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }

        /**
         * Specifies if the user attributes must be saved in session.
         */
        @Attribute(order = 1000, validators = { RequiredValueValidator.class })
        default boolean saveUserAttributesToSession() {
            return true;
        }

    }

    /**
     * The possible outcomes for an AbstractYotiAuthNode
     */
    public enum YotiAuthOutcome {
        /**
         * Successful authentication for an existing ForgeRock user
         */
        ACCOUNT_EXISTS,
        /**
         * Successful authentication for a new ForgeRock user
         */
        NO_ACCOUNT,
        /**
         * Authentication failed
         */
        FAILED
    }

    protected final T config;
    protected final Debug debug = Debug.getInstance(YOTI_AUTH_NODE);
    protected final YotiClient yotiClient;
    private final YotiAttributeMapper yotiAttributeMapper;
    private final YotiRepositoryUtil yotiRepositoryUtil;

    protected AbstractYotiAuthNode(T config) throws NodeProcessException {
        this.config = config;
        this.yotiClient = createClient();
        this.yotiAttributeMapper = new YotiAttributeMapper(config.attributeMappings());
        yotiRepositoryUtil = new YotiRepositoryUtil(config.accountProviderClass());
    }

    private YotiClient createClient() throws NodeProcessException {
        debug.message("Creating the client...");
        try {
            KeyPairSource keyPairSource = FileKeyPairSource.fromFile(
                    new File(config.pemFileLocation()));
            return YotiClientBuilder.newInstance()
                    .forApplication(config.sdkId())
                    .withKeyPair(keyPairSource)
                    .build();
        } catch (IOException e) {
            debug.error("Error creating the client", e);
            throw new NodeProcessException(e);
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (context.request.parameters.containsKey(TOKEN)) {
            debug.message("the request parameters contains a token");
            return processYotiToken(context);
        } else {
            debug.message("redirecting for sign-in with yoti");
            return redirectToYotiSignIn(context);
        }
    }

    private Action processYotiToken(TreeContext context) throws NodeProcessException {
        String token = context.request.parameters.get(TOKEN).get(0);
        debug.message("Got token: " + token);

        if (token == null || token.length() == 0) {
            return goTo(YotiAuthOutcome.FAILED.name()).build();
        }
        ActivityDetails activityDetails = fetchActivityDetails(token);
        Optional<String> repositoryId = findUserInRepository(context, activityDetails);

        Map<String, List<String>> attributes = yotiAttributeMapper.mapAttributes(activityDetails);
        Action.ActionBuilder actionBuilder;
        if (repositoryId.isPresent()) {
            actionBuilder = actionForExistingUser(context, repositoryId.get());
        } else {
            actionBuilder = actionForNewUser(context, attributes, activityDetails);
        }
        if (config.saveUserAttributesToSession()) {
            debug.message("Saving user attributes in the session");
            attributes.forEach((key, value) -> actionBuilder.putSessionProperty(key, value.stream().findFirst().get()));
        }
        return actionBuilder.build();
    }

    private ActivityDetails fetchActivityDetails(String token) throws NodeProcessException {
        try {
            return yotiClient.getActivityDetails(token);
        } catch (ProfileException ex) {
            debug.error("Error retrieving profile", ex);
            throw new NodeProcessException(ex);
        }
    }

    private Optional<String> findUserInRepository(TreeContext context, ActivityDetails activityDetails) {
        debug.message("Checking for existence of user " + activityDetails.getRememberMeId());
        Map<String, Set<String>> userNames = yotiAttributeMapper.mapAccountAttributes(activityDetails);
        return yotiRepositoryUtil.userExistsInTheDataStore(context.sharedState.get(REALM).asString(), userNames);
    }

    private Action.ActionBuilder actionForExistingUser(TreeContext context, String repositoryid) {
        debug.message("User exists.  Creating action " + YotiAuthOutcome.ACCOUNT_EXISTS.name());
        return goTo(YotiAuthOutcome.ACCOUNT_EXISTS.name())
                .replaceSharedState(context.sharedState.add(USERNAME, repositoryid));
    }

    private Action.ActionBuilder actionForNewUser(TreeContext context, Map<String, List<String>> attributes, ActivityDetails activityDetails) {
        debug.message("User does not exist.  Adding attributes and names to the sharedState");
        JsonValue sharedState = context.sharedState.put(SS_USER_INFO, JsonValue.json(
                JsonValue.object(
                        JsonValue.field(SS_ATTRIBUTES, attributes),
                        JsonValue.field(SS_USER_NAMES, new HashMap<>())
                ))
        );
        if (activityDetails.getUserProfile().getEmailAddress() != null) {
            sharedState.put(SharedStateConstants.EMAIL_ADDRESS, activityDetails.getUserProfile().getEmailAddress().getValue());
        }
        debug.message("Creating action " + YotiAuthOutcome.NO_ACCOUNT.name());
        return goTo(YotiAuthOutcome.NO_ACCOUNT.name())
                .replaceSharedState(sharedState);
    }

    private Action redirectToYotiSignIn(TreeContext context) throws NodeProcessException {
        return send(createYotiLoginCallback())
                .build();
    }

    protected abstract Callback createYotiLoginCallback() throws NodeProcessException;

    /**
     * Defines the possible outcomes from this node.
     */
    public static class YotiAuthOutcomeProvider implements OutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ClassLoader classLoader = YotiAuthOutcomeProvider.class.getClassLoader();
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, classLoader);
            return ImmutableList.of(
                    new Outcome(YotiAuthOutcome.ACCOUNT_EXISTS.name(), bundle.getString("accountExistsOutcome")),
                    new Outcome(YotiAuthOutcome.NO_ACCOUNT.name(), bundle.getString("noAccountOutcome")),
                    new Outcome(YotiAuthOutcome.FAILED.name(), bundle.getString("failedOutcome")));
        }
    }

}
