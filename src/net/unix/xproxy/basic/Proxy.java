package net.unix.xproxy.basic;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.protocol.MinecraftConstants;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.message.TextMessage;
import org.spacehq.mc.protocol.data.status.PlayerInfo;
import org.spacehq.mc.protocol.data.status.ServerStatusInfo;
import org.spacehq.mc.protocol.data.status.VersionInfo;
import org.spacehq.mc.protocol.data.status.handler.ServerInfoBuilder;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.util.UUID;

public class Proxy implements ProxyHelper {
    private final String host;
    private final int port;
    private Server server;
    private final String version = "v2.49 BCNuker";
    public Proxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void onLoad() {
        this.server = new Server(this.host, this.port, MinecraftProtocol.class, new TcpSessionFactory(java.net.Proxy.NO_PROXY));
        this.server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, java.net.Proxy.NO_PROXY);
        this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
        this.server.bind();
        this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
            @Override
            public ServerStatusInfo buildInfo(Session session) {
                return new ServerStatusInfo(
                        new VersionInfo(fixColor("&6Najlepsze proxy w sieci!                                                       "+"                  &70&8/&71"), 48),
                        new PlayerInfo(1, 0,
                        new GameProfile[] { new GameProfile(UUID.randomUUID(), getSubMotd()) }),
                        new TextMessage(" &6XProxy.ru &8| &7"+version+" &8| &7Dostep: &ewww.example.com\n &7Narzedzie do testowania Twojego serwera &eMinecraft&7!"), null);
            }
        });
    }

    public String fixColor(String text) {
        return text.replace("&", "§");
    }

    private String getSubMotd() {
        return fixColor(
                "§6Y88b   d88P 8888888b."+
        "\n§6 Y88b d88P  888   Y88b"+
        "\n§6  Y88o88P   888    888"+
        "\n§6  Y88o88P   888    888"+
        "\n§6  Y88o88P   888    888"+
        "\n§6   Y888P    888   d88P 888d888  .d88b.  888  888 888  888"+
        "\n§6   d888b    8888888P   888P    d88  88b  Y8bd8P  888  888"+
        "\n§6  d88888b   888        888     888  888   X88K   888  888"+
        "\n§6 d88P Y88b  888        888     Y88..88P .d8  8b. Y88b 888"+
        "\n§6d88P   Y88b 888        888       Y88P   888  888   Y88888"+
        "\n§6                                                               888"+
        "\n§6                                                          Y8b d88P"+
        "\n§6                                                            Y88P"
        + "\n\n&7Niniejsze proxy jest wlasnoscia &6Unix&7."
        + "\n&7Aktualna wersja: &6"+this.version+" &7[cpu: 0.0%, players: 0, bots: 0]"
        + "\n&7Uzywanie niniejszego oprogramowania na nieswoich uslugach na wlasna odpowiedzialnosc."
        + "\n\n&7Autor &6XProxy &7nie ponosi odpowiedzialnosci za"
        + "\n&7wykorzystywanie tego oprogramowania niezgodnie z jego przeznaczeniem."
        + "\n\n&6Kontakt: &7discord.gg/cvFQdAw"
        + "\n&6Zakup: &7www.example.com"
        + "\n&6Dostepne platnosci: &7PayPal/PaySafeCard");
    }
}
