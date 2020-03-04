package org.onedatashare.server;

import com.box.sdk.BoxConfig;
import org.apache.log4j.BasicConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServerApplication {

  public static void main(String[] args) {
    BasicConfigurator.configure();
    SpringApplication.run(ServerApplication.class, args);
  }
}
