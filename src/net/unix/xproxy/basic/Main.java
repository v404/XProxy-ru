package net.unix.xproxy.basic;

public class Main {
    public static void main(String... args) {
        final Proxy proxy = new Proxy("127.0.0.1", 25565);
        final Thread proxyThread = new Thread(proxy::onLoad);
        proxyThread.start();
    }
}
