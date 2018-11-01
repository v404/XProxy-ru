package org.spacehq.packetlib.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.net.Proxy;
import java.util.Hashtable;

import javax.naming.directory.InitialDirContext;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.protocol.MinecraftConstants;
import org.spacehq.mc.protocol.data.game.values.MessageType;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerTitlePacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.packet.PacketProtocol;

public class TcpClientSession extends TcpSession {
    private Client client;
    private Proxy proxy;
    private EventLoopGroup group;

    public TcpClientSession(String host, int port, PacketProtocol protocol, Client client, Proxy proxy) {
        super(host, port, protocol);
        this.client = client;
        this.proxy = proxy;
    }

    @Override
    public void connect(boolean wait) {
        if(this.disconnected) {
            throw new IllegalStateException("Session has already been disconnected.");
        } else if(this.group != null) {
            return;
        }

        try {
            final Bootstrap bootstrap = new Bootstrap();
            if(this.proxy != null) {
                this.group = new OioEventLoopGroup();
                bootstrap.channelFactory(new ProxyOioChannelFactory(this.proxy));
            } else {
                this.group = new NioEventLoopGroup();
                bootstrap.channel(NioSocketChannel.class);
            }

            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                public void initChannel(Channel channel) throws Exception {
                    getPacketProtocol().newClientSession(client, TcpClientSession.this);

                    channel.config().setOption(ChannelOption.IP_TOS, 0x18);
                    channel.config().setOption(ChannelOption.TCP_NODELAY, false);

                    ChannelPipeline pipeline = channel.pipeline();

                    refreshReadTimeoutHandler(channel);
                    refreshWriteTimeoutHandler(channel);

                    pipeline.addLast("encryption", new TcpPacketEncryptor(TcpClientSession.this));
                    pipeline.addLast("sizer", new TcpPacketSizer(TcpClientSession.this));
                    pipeline.addLast("codec", new TcpPacketCodec(TcpClientSession.this));
                    pipeline.addLast("manager", TcpClientSession.this);
                }
            }).group(this.group).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout() * 1000);

            Runnable connectTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        String host = getHost();
                        int port = getPort();

                        try {
                            Hashtable<String, String> environment = new Hashtable<String, String>();
                            environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
                            environment.put("java.naming.provider.url", "dns:");

                            String[] result = new InitialDirContext(environment).getAttributes(getPacketProtocol().getSRVRecordPrefix() + "._tcp." + host, new String[] { "SRV" }).get("srv").get().toString().split(" ", 4);
                            host = result[3];
                            port = Integer.parseInt(result[2]);
                        } catch(Throwable t) {
                        }

                        bootstrap.remoteAddress(host, port);

                        ChannelFuture future = bootstrap.connect().sync();
                        if(future.isSuccess()) {
                            while(!isConnected() && !disconnected) {
                            }
                        }
                    } catch(Throwable t) {
                        exceptionCaught(null, t);
                    }
                }
            };

            if(wait) {
                connectTask.run();
            } else {
                new Thread(connectTask).start();
            }
        } catch(Throwable t) {
            exceptionCaught(null, t);
        }
    }

    @Override
    public void disconnect(String reason, Throwable cause, boolean wait) {
        super.disconnect(reason, cause, wait);
        if(this.group != null) {
            Future<?> future = this.group.shutdownGracefully();
            if(wait) {
                try {
                    future.await();
                } catch(InterruptedException e) {
                }
            }

            this.group = null;
        }
    }

    @Override
    public boolean sendMessage(String message) {
        this.send(new ServerChatPacket(message.replace("&", "§").replace(">>", "»").replace("<<", "«").replaceAll("%p", "§8[§6Proxy§8]")));
        return false;
    }

    @Override
    public boolean sendTitle(String title, String subtitle) {
        this.send(new ServerTitlePacket(title.replace("&", "§").replace(">>", "»").replace("<<", "«").replaceAll("%p", "§8[§6Proxy§8]"), false));
        this.send(new ServerTitlePacket(subtitle.replace("&", "§").replace(">>", "»").replace("<<", "«").replaceAll("%p", "§8[§6Proxy§8]"), true));
        return false;
    }

    @Override
    public boolean sendActionBar(String message) {
        this.send(new ServerChatPacket(message.replace("&", "§").replace(">>", "»").replace("<<", "«").replaceAll("%p", "§8[§6Proxy§8]"), MessageType.NOTIFICATION));
        return false;
    }

    @Override
    public String getName() {
        GameProfile profile = this.getFlag(MinecraftConstants.PROFILE_KEY);
        return profile.getName();
    }
	
}
