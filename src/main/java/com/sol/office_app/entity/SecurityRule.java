package com.sol.office_app.entity;

import jakarta.persistence.*;

@Entity
public class SecurityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String httpMethod;
    private String urlPattern;

    @ManyToOne
    @JoinColumn(name = "authority_id")
    private Privilege authority;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public Privilege getAuthority() {
        return authority;
    }

    public void setAuthority(Privilege authority) {
        this.authority = authority;
    }
}
