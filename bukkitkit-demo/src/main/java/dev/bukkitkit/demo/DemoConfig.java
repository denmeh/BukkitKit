package dev.bukkitkit.demo;

import dev.bukkitkit.api.Config;

import java.util.List;

@Config
public final class DemoConfig {

    public String welcomeMessage = "Welcome to the server!";
    public boolean joinMessageEnabled = true;
    public int maxHomes = 3;
    public List<String> motdLines = List.of("Have fun!", "Be kind.");

    public DemoConfig() {
    }
}
