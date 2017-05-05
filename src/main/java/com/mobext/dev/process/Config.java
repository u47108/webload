/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mobext.dev.process;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author luis
 */
public class Config {

    private Properties prop = new Properties();
    private String fileName;

    public Config() {
        this("config.properties");
    }

    public Config(String file) {
        this.fileName = file;

        try {
            //load a properties file
            prop.load(new FileInputStream(fileName));
        } catch (IOException ex) {
            System.err.println("Error loading config from " + fileName + ": " + ex);
        }
    }

    public String getProperty(String name) {
        return prop.getProperty(name, null);
    }

    public String getProperty(String name, String value) {
        return prop.getProperty(name, value);
    }
}
