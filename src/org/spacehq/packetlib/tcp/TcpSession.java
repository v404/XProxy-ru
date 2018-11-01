package org.spacehq.packetlib.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectingEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.PacketSentEvent;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class TcpSession
extends SimpleChannelInboundHandler<Packet>
implements Session
{
private String host;
private int port;
private PacketProtocol protocol;
private int compressionThreshold = -1;
private int connectTimeout = 30;
private int readTimeout = 30;
private int writeTimeout = 0;
@SuppressWarnings({ "unchecked", "rawtypes" })
private Map<String, Object> flags = new HashMap();
@SuppressWarnings({ "unchecked", "rawtypes" })
private List<SessionListener> listeners = new CopyOnWriteArrayList();
private Channel channel;
protected boolean disconnected = false;
@SuppressWarnings({ "unchecked", "rawtypes" })
private BlockingQueue<Packet> packets = new LinkedBlockingQueue();
private Thread packetHandleThread;

public TcpSession(String host, int port, PacketProtocol protocol)
{
  this.host = host;
  this.port = port;
  this.protocol = protocol;
}

public void connect()
{
  connect(true);
}

public void connect(boolean wait) {}

public String getHost()
{
  return this.host;
}

public int getPort()
{
  return this.port;
}

public SocketAddress getLocalAddress()
{
  return this.channel != null ? this.channel.localAddress() : null;
}

public SocketAddress getRemoteAddress()
{
  return this.channel != null ? this.channel.remoteAddress() : null;
}

public PacketProtocol getPacketProtocol()
{
  return this.protocol;
}

@SuppressWarnings({ "unchecked", "rawtypes" })
public Map<String, Object> getFlags()
{
  return new HashMap(this.flags);
}

public boolean hasFlag(String key)
{
  return getFlags().containsKey(key);
}

@SuppressWarnings("unchecked")
public <T> T getFlag(String key)
{
  Object value = getFlags().get(key);
  if (value == null) {
    return null;
  }
  try
  {
    return (T)value;
  }
  catch (ClassCastException e)
  {
    throw new IllegalStateException("Tried to get flag \"" + key + "\" as the wrong type. Actual type: " + value.getClass().getName());
  }
}

public void setFlag(String key, Object value)
{
  this.flags.put(key, value);
}

@SuppressWarnings({ "unchecked", "rawtypes" })
public List<SessionListener> getListeners()
{
  return new ArrayList(this.listeners);
}

public void addListener(SessionListener listener)
{
  this.listeners.add(listener);
}

public void removeListener(SessionListener listener)
{
  this.listeners.remove(listener);
}

public void callEvent(SessionEvent event)
{
  try
  {
    for (SessionListener listener : this.listeners) {
      event.call(listener);
    }
  }
  catch (Throwable t)
  {
    exceptionCaught(null, t);
  }
}

public int getCompressionThreshold()
{
  return this.compressionThreshold;
}

public void setCompressionThreshold(int threshold)
{
  this.compressionThreshold = threshold;
  if (this.channel != null) {
    if (this.compressionThreshold >= 0)
    {
      if (this.channel.pipeline().get("compression") == null) {
        this.channel.pipeline().addBefore("codec", "compression", new TcpPacketCompression(this));
      }
    }
    else if (this.channel.pipeline().get("compression") != null) {
      this.channel.pipeline().remove("compression");
    }
  }
}

public int getConnectTimeout()
{
  return this.connectTimeout;
}

public void setConnectTimeout(int timeout)
{
  this.connectTimeout = timeout;
}

public int getReadTimeout()
{
  return this.readTimeout;
}

public void setReadTimeout(int timeout)
{
  this.readTimeout = timeout;
  refreshReadTimeoutHandler();
}

public int getWriteTimeout()
{
  return this.writeTimeout;
}

public void setWriteTimeout(int timeout)
{
  this.writeTimeout = timeout;
  refreshWriteTimeoutHandler();
}

public boolean isConnected()
{
  return (this.channel != null) && (this.channel.isOpen()) && (!this.disconnected);
}

public void send(final Packet packet)
{
  if (this.channel == null) {
    return;
  }
  ChannelFuture future = this.channel.writeAndFlush(packet).addListener(new ChannelFutureListener()
  {
    public void operationComplete(ChannelFuture future)
      throws Exception
    {
      if (future.isSuccess()) {
        TcpSession.this.callEvent(new PacketSentEvent(TcpSession.this, packet));
      } else {
        TcpSession.this.exceptionCaught(null, future.cause());
      }
    }
  });
  if (packet.isPriority()) {
    try
    {
      future.await();
    }
    catch (InterruptedException e) {}
  }
}

public void disconnect(String reason)
{
  disconnect(reason, false);
}

public void disconnect(String reason, boolean wait)
{
  disconnect(reason, null, wait);
}

public void disconnect(String reason, Throwable cause)
{
  disconnect(reason, cause, false);
}

public void disconnect(final String reason, final Throwable cause, boolean wait)
{
  if (this.disconnected) {
    return;
  }
  this.disconnected = true;
  if (this.packetHandleThread != null)
  {
    this.packetHandleThread.interrupt();
    this.packetHandleThread = null;
  }
  if ((this.channel != null) && (this.channel.isOpen()))
  {
    callEvent(new DisconnectingEvent(this, reason, cause));
    ChannelFuture future = this.channel.flush().close().addListener(new ChannelFutureListener()
    {
      public void operationComplete(ChannelFuture future)
        throws Exception
      {
        TcpSession.this.callEvent(new DisconnectedEvent(TcpSession.this, reason != null ? reason : "Connection closed.", cause));
      }
    });
    if (wait) {
      try
      {
        future.await();
      }
      catch (InterruptedException e) {}
    }
  }
  else
  {
    callEvent(new DisconnectedEvent(this, reason != null ? reason : "Connection closed.", cause));
  }
  this.channel = null;
}

protected void refreshReadTimeoutHandler()
{
  refreshReadTimeoutHandler(this.channel);
}

protected void refreshReadTimeoutHandler(Channel channel)
{
  if (channel != null) {
    if (this.readTimeout <= 0)
    {
      if (channel.pipeline().get("readTimeout") != null) {
        channel.pipeline().remove("readTimeout");
      }
    }
    else if (channel.pipeline().get("readTimeout") == null) {
      channel.pipeline().addFirst("readTimeout", new ReadTimeoutHandler(this.readTimeout));
    } else {
      channel.pipeline().replace("readTimeout", "readTimeout", new ReadTimeoutHandler(this.readTimeout));
    }
  }
}

protected void refreshWriteTimeoutHandler()
{
  refreshWriteTimeoutHandler(this.channel);
}

protected void refreshWriteTimeoutHandler(Channel channel)
{
  if (channel != null) {
    if (this.writeTimeout <= 0)
    {
      if (channel.pipeline().get("writeTimeout") != null) {
        channel.pipeline().remove("writeTimeout");
      }
    }
    else if (channel.pipeline().get("writeTimeout") == null) {
      channel.pipeline().addFirst("writeTimeout", new WriteTimeoutHandler(this.writeTimeout));
    } else {
      channel.pipeline().replace("writeTimeout", "writeTimeout", new WriteTimeoutHandler(this.writeTimeout));
    }
  }
}

public void channelActive(ChannelHandlerContext ctx)
  throws Exception
{
  if ((this.disconnected) || (this.channel != null))
  {
    ctx.channel().close();
    return;
  }
  this.channel = ctx.channel();
  
  this.packetHandleThread = new Thread(new Runnable()
  {
    public void run()
    {
      try
      {
        Packet packet;
        while ((packet = (Packet)TcpSession.this.packets.take()) != null) {
          TcpSession.this.callEvent(new PacketReceivedEvent(TcpSession.this, packet));
        }
      }
      catch (InterruptedException e) {}catch (Throwable t)
      {
        TcpSession.this.exceptionCaught(null, t);
      }
    }
  });
  this.packetHandleThread.start();
  
  callEvent(new ConnectedEvent(this));
}

public void channelInactive(ChannelHandlerContext ctx)
  throws Exception
{
  if (ctx.channel() == this.channel) {
    disconnect("Connection closed.");
  }
}

public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
{
  @SuppressWarnings("unused")
String message = null;
  if (((cause instanceof ConnectTimeoutException)) || (((cause instanceof ConnectException)) && (cause.getMessage().contains("connection timed out")))) {
    message = "Connection timed out.";
  } else if ((cause instanceof ReadTimeoutException)) {
    message = "Read timed out.";
  } else if ((cause instanceof WriteTimeoutException)) {
    message = "Write timed out.";
  } else {
    message = cause.toString();
  }

  if(cause.getMessage() != null && cause.getMessage().contains("Packet")){
      return;
  }
  return;
}

protected void channelRead0(ChannelHandlerContext ctx, Packet packet)
  throws Exception
{
  if (!packet.isPriority()) {
    this.packets.add(packet);
  }
}
}
