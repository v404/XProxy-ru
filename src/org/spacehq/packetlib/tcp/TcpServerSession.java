package org.spacehq.packetlib.tcp;

import io.netty.channel.ChannelHandlerContext;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.protocol.MinecraftConstants;
import org.spacehq.mc.protocol.data.game.values.MessageType;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerTitlePacket;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.packet.PacketProtocol;

import java.util.Map;

public class TcpServerSession extends TcpSession {
    private Server server;

    public TcpServerSession(String host, int port, PacketProtocol protocol, Server server) {
        super(host, port, protocol);
        this.server = server;
    }

    @Override
    public Map<String, Object> getFlags() {
        Map<String, Object> ret = super.getFlags();
        ret.putAll(this.server.getGlobalFlags());
        return ret;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.server.addSession(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        this.server.removeSession(this);
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
