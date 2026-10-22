package com.osdiag;

import java.awt.Desktop;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class App {

    @Value("${app.open-browser:false}")
    boolean openBrowser;

    @Value("${server.port:8080}")
    int port;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    // The run scripts set OPEN_BROWSER=true so the dashboard opens like a desktop app.
    @EventListener(ApplicationReadyEvent.class)
    void open() throws Exception {
        if (openBrowser) {
            System.setProperty("java.awt.headless", "false");
            Desktop.getDesktop().browse(new URI("http://localhost:" + port));
        }
    }
}
