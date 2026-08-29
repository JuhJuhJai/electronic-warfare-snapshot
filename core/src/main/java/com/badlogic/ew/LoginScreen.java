package com.badlogic.ew;

import com.badlogic.gdx.Screen;
import ew.server.Server;

/**
 * First screen of the application. Displayed after the application is created.
 * On this screen, someone could make a commander or login as an existing one found on the server, using commander ID for now.
 * The commander ID is the "key". The player does not get access to anything other than connecting to the server with their key.
 * If the commanderID has been set earlier, then some debug is being used. This screen instead immediately continues to GameScreen,
 * the commander being set to the given ID.
 */
public class LoginScreen implements Screen {
    final Server server;
    final long commanderID; // is -1 if no commander is assigned by MainMenu.

    public LoginScreen(Server server, long commanderID) {
        this.server = server;
        this.commanderID = commanderID;
    }

    @Override
    public void show() {
        // Prepare your screen here.
    }

    @Override
    public void render(float delta) {
        // Draw your screen here. "delta" is the time since last render in seconds.
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
    }
}
