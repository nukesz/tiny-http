package com.nukesz.tinyhttp;

public class Main {

    static void main(String[] args) {
        Server server = new Server(9090);
        server.start();
    }
}
