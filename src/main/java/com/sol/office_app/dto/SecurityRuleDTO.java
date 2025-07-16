package com.sol.office_app.dto;

public class SecurityRuleDTO {

    private Long id;
    private String httpMethod;
    private String urlPattern;
    private PrivilegeDTO authority;
    public SecurityRuleDTO(Long id, PrivilegeDTO authority, String httpMethod, String urlPattern) {
        this.id = id;
        this.httpMethod = httpMethod;
        this.urlPattern = urlPattern;
        this.authority = authority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public PrivilegeDTO getAuthority() {
        return authority;
    }

    public void setAuthority(PrivilegeDTO authority) {
        this.authority = authority;
    }
}
