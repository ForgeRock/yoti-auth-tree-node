package com.yoti.forgerock.auth;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;


public class YotiRepositoryUtil {

    private final AccountProvider accountProvider;

    public YotiRepositoryUtil(final String className) throws NodeProcessException {
        String[] split = className.split("\\|");
        String actualClassName = split[0];
        String[] parameters = new String[split.length - 1];
        System.arraycopy(split, 1, parameters, 0, split.length - 1);

        accountProvider = tryCreateAccountProvider(actualClassName, parameters);
    }

    private static AccountProvider tryCreateAccountProvider(final String actualClassName, final String[] parameters) throws NodeProcessException {
        try {
            Class<? extends AccountProvider> clazz = Class.forName(actualClassName).asSubclass(AccountProvider.class);
            Class<?>[] parameterTypes = new Class<?>[parameters.length];
            Arrays.fill(parameterTypes, String.class);
            return clazz.getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception e) {
            throw new NodeProcessException("Problem when trying to instantiate " + actualClassName, e);
        }
    }

    public Optional<String> userExistsInTheDataStore(final String realm, final Map<String, Set<String>> userNames) {
        if ((userNames != null) && !userNames.isEmpty()) {
            AMIdentity userIdentity = accountProvider.searchUser(AuthD.getAuth().getAMIdentityRepository(realm), userNames);
            if (userIdentity != null && userIdentity.getName() != null) {
                return Optional.of(userIdentity.getName());
            }
        }
        return Optional.absent();
    }

}
