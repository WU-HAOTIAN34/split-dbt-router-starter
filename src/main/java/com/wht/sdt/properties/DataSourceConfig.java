package com.wht.sdt.properties;


import lombok.Data;

@Data
public class DataSourceConfig {

    private String driverClassName;
    private String url;
    private String username;
    private String password;

}
