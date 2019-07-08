package com.yoti.forgerock.auth.matcher;

import com.yoti.api.client.shareurl.policy.WantedAttribute;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WantedAttributeMatcher extends TypeSafeDiagnosingMatcher<WantedAttribute> {

    private final String name;

    public WantedAttributeMatcher(String name) {
        this.name = name;
    }

    @Override
    public boolean matchesSafely(WantedAttribute wantedAttribute, Description description) {
        description.appendText("name: " + name);
        return wantedAttribute.getName().equals(name);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("name: " + name);
    }

    public static WantedAttributeMatcher forName(String name) {
        return new WantedAttributeMatcher(name);
    }

}
