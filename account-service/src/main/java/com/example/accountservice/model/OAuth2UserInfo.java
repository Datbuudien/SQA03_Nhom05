package com.example.accountservice.model;

import lombok.Data;
import java.util.Map;

@Data
public class OAuth2UserInfo {
    private Map<String, Object> attributes;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String picture;
    private String sub; // Google's unique identifier

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.email = (String) attributes.get("email");
        this.name = (String) attributes.get("name");
        this.givenName = (String) attributes.get("given_name");
        this.familyName = (String) attributes.get("family_name");
        this.picture = (String) attributes.get("picture");
        this.sub = (String) attributes.get("sub");
    }
}