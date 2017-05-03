package gash.router.server;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Node;
import gash.router.client.CommInit;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import routing.Pipe.CommandMessage;

public class DiscoveryCommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
	
	private static HashMap<String, List<CommandMessage>> map = new HashMap<>();
	private static EdgeList outbound = new EdgeList();
	private static Node client;
	static EventLoopGroup group = new NioEventLoopGroup();
	protected static Logger logger = LoggerFactory.getLogger(" (D.D) Discovery Command");
	
	public static void init(EdgeInfo ei)
	{
		logger.info("Trying to connect to host ! " + ei.getHost());
		try {
			CommInit si = new CommInit(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			ChannelFuture cf = b.connect(ei.getHost(), ei.getPort()).syncUninterruptibly();
			ei.setChannel(cf.channel());
			ei.setActive(true);

			System.out.println(cf.channel().localAddress() + " -> open: " + cf.channel().isOpen()
					+ ", write: " + cf.channel().isWritable() + ", reg: " + cf.channel().isRegistered());

		} catch (Throwable ex) {
			System.out.println("failed to initialize the client connection " + ex.toString());
			ex.printStackTrace();
		}

	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		logger.info("Request arrived from : " + msg.getHeader().getNodeId());
		//handleMessage(msg, ctx.channel());
	}

}
