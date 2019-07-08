package com.yoti.forgerock.auth;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import org.apache.commons.lang.reflect.FieldUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@PrepareForTest({ AuthD.class, ConfigurationObserver.class, InjectorHolder.class })
@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
public class YotiRepositoryUtilTest {

    private static final String REALM = "realm";

    YotiRepositoryUtil testObj;

    @Mock AMIdentityRepository amIdentityRepositoryMock;
    @Mock AMIdentity amIdentityMock;
    @Mock AccountProvider accountProviderMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        PowerMockito.suppress(method(InjectorHolder.class, "getInstance", java.lang.Class.class));
        AuthD authDWhitebox = Whitebox.newInstance(AuthD.class);
        Whitebox.setInternalState(authDWhitebox, "idRepoMap", new ConcurrentHashMap(of(REALM, amIdentityRepositoryMock)));
        PowerMockito.mockStatic(AuthD.class);
        PowerMockito.when(AuthD.getAuth()).thenReturn(authDWhitebox);
    }

    @Test
    public void contructor_shouldInstantiateSpecifiedClass() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());

        SomeAccountProvider accountProvider = getAccountProvider(testObj);
        assertEquals(null, (accountProvider).getParamOne());
        assertEquals(null, (accountProvider).getParamTwo());
    }

    @Test
    public void contructor_shouldInstantiateSpecifiedClassWithConstructorParams() throws Exception {
        String className = SomeAccountProvider.class.getName() + "|value1|value2";
        testObj = new YotiRepositoryUtil(className);

        SomeAccountProvider accountProvider = getAccountProvider(testObj);
        assertEquals("value1", (accountProvider).getParamOne());
        assertEquals("value2", (accountProvider).getParamTwo());
    }

    @Test
    public void userExistsInTheDataStore_shouldReturnNotPresentWhenUserNamesIsNull() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());

        Optional<String> result = testObj.userExistsInTheDataStore(null, null);

        assertFalse(result.isPresent());
    }

    @Test
    public void userExistsInTheDataStore_shouldReturnNotPresentWhenUserNamesIsEmpty() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());

        Optional<String> result = testObj.userExistsInTheDataStore(null, emptyMap());

        assertFalse(result.isPresent());
    }

    @Test
    public void userExistsInTheDataStore_shouldReturnNotPresentWhenUserNotFound() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());
        overwriteAccountProvider(testObj, accountProviderMock);

        Optional<String> result = testObj.userExistsInTheDataStore(REALM, of("searchFieldName", singleton("notFound")));

        assertFalse(result.isPresent());
    }

    @Test
    public void userExistsInTheDataStore_shouldReturnNotPresentWhenUserHasNoName() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());
        overwriteAccountProvider(testObj, accountProviderMock);
        ImmutableMap<String, Set<String>> userNames = of("searchFieldName", singleton("notFound"));
        when(accountProviderMock.searchUser(amIdentityRepositoryMock, userNames)).thenReturn(amIdentityMock);

        Optional<String> result = testObj.userExistsInTheDataStore(REALM, userNames);

        assertFalse(result.isPresent());
    }

    @Test
    public void userExistsInTheDataStore_shouldReturnUsername() throws Exception {
        testObj = new YotiRepositoryUtil(SomeAccountProvider.class.getName());
        overwriteAccountProvider(testObj, accountProviderMock);
        ImmutableMap<String, Set<String>> userNames = of("searchFieldName", singleton("userName"));
        when(accountProviderMock.searchUser(amIdentityRepositoryMock, userNames)).thenReturn(amIdentityMock);
        when(amIdentityMock.getName()).thenReturn("userName");

        Optional<String> result = testObj.userExistsInTheDataStore(REALM, userNames);

        assertEquals("userName", result.get());
    }

    private static SomeAccountProvider getAccountProvider(YotiRepositoryUtil created) throws Exception {
        return (SomeAccountProvider) FieldUtils.getField(YotiRepositoryUtil.class, "accountProvider", true).get(created);
    }

    private static void overwriteAccountProvider(YotiRepositoryUtil created, AccountProvider accountProvider) throws Exception {
        Field accountProviderField = FieldUtils.getField(YotiRepositoryUtil.class, "accountProvider", true);
        FieldUtils.writeField(accountProviderField, created, accountProvider);
    }

    public static class SomeAccountProvider implements AccountProvider {

        private final String paramOne;
        private final String paramTwo;

        public SomeAccountProvider() {
            this(null, null);
        }

        public SomeAccountProvider(String paramOne, String paramTwo) {
            this.paramOne = paramOne;
            this.paramTwo = paramTwo;
        }

        public String getParamOne() {
            return paramOne;
        }

        public String getParamTwo() {
            return paramTwo;
        }

        @Override
        public AMIdentity searchUser(AMIdentityRepository amIdentityRepository, Map<String, Set<String>> map) {
            throw new NotImplementedException();
        }

        @Override
        public AMIdentity provisionUser(AMIdentityRepository amIdentityRepository, Map<String, Set<String>> map) throws AuthLoginException {
            throw new NotImplementedException();
        }

    }

}
