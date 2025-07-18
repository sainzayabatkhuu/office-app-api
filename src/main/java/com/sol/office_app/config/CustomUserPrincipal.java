package com.sol.office_app.config;

import java.security.Principal;
import java.util.*;

public class CustomUserPrincipal implements Principal {
    private final String username;
    private final List<String> rolePermissions;

    private final Map<String, List<String>> rules;

    public CustomUserPrincipal(String username, List<String> rolePermissions, Map<String, List<String>> rules) {
        this.username = username;
        this.rolePermissions = rolePermissions;
        this.rules = rules;
    }

    @Override
    public String getName() {
        return username;
    }

    public List<String> getRolePermissions() {
        return rolePermissions;
    }

    public Map<String, List<String>> getRules() {return rules;}
}