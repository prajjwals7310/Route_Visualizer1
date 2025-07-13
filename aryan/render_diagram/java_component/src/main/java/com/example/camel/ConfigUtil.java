//  package com.example.camel;

// import java.io.FileInputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.util.Properties;

// public class ConfigUtil {
//     private static final Properties props = new Properties();

//     static {
//         try (InputStream input = new FileInputStream("input/application.properties")) {
//             props.load(input);
//         } catch (IOException e) {
//             System.err.println("❌ Failed to load application.properties");
//             e.printStackTrace();
//         }
//     }

//     public static String get(String key) {
//         return props.getProperty(key);
//     }

//     public static String getOrDefault(String key, String defaultValue) {
//         return props.getProperty(key, defaultValue);
//     }
// }


package com.example.camel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {
    private static final Properties props = new Properties();

    static {
        try {
            // 👇 Dynamic config file path: default = input/config.properties
            String propFilePath = System.getProperty("config.file", "input/config.properties");
            try (InputStream input = new FileInputStream(propFilePath)) {
                props.load(input);
                System.out.println("🔧 Loaded config: " + propFilePath);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to load config properties");
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
