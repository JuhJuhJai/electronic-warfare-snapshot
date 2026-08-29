package com.badlogic.ew;

import com.badlogic.gdx.Game;
import ew.playerData.Commander;
import ew.server.Server;
import ew.server.TestServer;

import java.util.Arrays;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainMenu extends Game {
    private final Server server;
    Commander tester;

    public MainMenu(String[] args) {
        if (Arrays.asList(args).contains("Test")) {
            tester = new Commander(0, "Tester");
            server = new TestServer(tester);
        }
        else {
            server = new Server();
        }
    }

    @Override
    public void create() {
        setScreen(new LoginScreen(server, tester == null ? -1 : tester.getPlayerID()));
    }
}
