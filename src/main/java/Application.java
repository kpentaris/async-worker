package main.java;

import main.java.network.RequestTemplate;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author KPentaris - 13-Jan-17.
 */
public class Application {

    public static void main(String[] args) throws IOException {
        Logger log = Logger.getLogger("Application");
        log.info("Async Worker application started");

        RequestTemplate template = new RequestTemplate(
                "http://www.w3schools.com/xml/tempconvert.asmx/CelsiusToFahrenheit",
                "POST"
        );
        template.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        template.addRequestHeader("Content-Length", "256");
        template.addRequestParam("Celsius", "40");

        String response = template.performRequest();

        log.info(response);

        log.info("Async Worker application ended");
    }
}
