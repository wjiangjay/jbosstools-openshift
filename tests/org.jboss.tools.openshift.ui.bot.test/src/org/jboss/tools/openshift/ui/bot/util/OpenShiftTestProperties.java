package org.jboss.tools.openshift.ui.bot.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Just static properties holder
 * 
 * @author sbunciak
 *
 */
public class OpenShiftTestProperties {

    public static Properties props = new Properties();

    static {

        try {
            props.load(new FileInputStream(
                    "resources/openshift.ui.bot.test.properties"));

        } catch (FileNotFoundException e) {
            Logger.getLogger(OpenShiftTestProperties.class).error(
                    "Property file not found !", e);
        } catch (IOException e) {
            Logger.getLogger(OpenShiftTestProperties.class).error(
                    "IO Exception !", e);
        }

    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

}
