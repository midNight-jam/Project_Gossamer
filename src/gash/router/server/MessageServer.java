/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server;


import java.io.BufferedInputStream;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Request;
import pipe.common.Common.WriteBody;
//import pipe.common.Common.Request.RequestType;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.raft.NodeState;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Queue;
import java.util.LinkedList;

import routing.Pipe.CommandMessage;
import routing.Pipe.WorkStealingRequest;

public class MessageServer {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();

	// public static final String sPort = "port";
	// public static final String sPoolSize = "pool.size";

	protected RoutingConf conf;
	protected boolean background = false;
	static ServerState state;

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public static ServerState getServerState(){
		return state;
	}
	
	public MessageServer(File cfg) {
		init(cfg);
		state = new ServerState();
		state.setConf(conf);
	}

	public MessageServer(RoutingConf conf) {
		this.conf = conf;
		//state = new ServerState();
	}

	public void release() {
	}

	/*public void startWorkWatcher(){
		CommandMessage.Builder command = CommandMessage.newBuilder();
		WorkStealingRequest.Builder stealRequest = WorkStealingRequest.newBuilder();
		stealRequest.setHost("someIP");
		stealRequest.setPort(4567);
		command.setWsr(stealRequest); // setting the work stealing request
		
		CommandMessage stealCommandMessage = command.build();
		ServerState state = NodeState.getInstance().getServerState();
		EdgeMonitor emon = state.getEmon();
		
		EdgeList edgeList = emon.getOutboundEdges();
		
		EdgeInfo edgeInfo = edgeList.getNode(0);
		Channel channel = edgeInfo.getChannel();
		
		channel.writeAndFlush(stealCommandMessage);
		
		
		Thread queWatcher = new Thread(new Runnable(){
			public void run(){
				logger.info("...(@>@)... handling message Queue");
				try{
					while(true){
						// Should request work from the QueueServer
//						if(work, do it ){
//							 
//							logger.info("...(@>@)... Processed from message Que : " + receivedCommand.toString());
//						}
//						else{
//							// Request work from queueServer
//						}
						Thread.sleep(100);	
					}
				}
				catch(InterruptedException e){
					logger.info("...(@>@)... Some how the queueWathcher is gone ... ");
				}
			}
		});
		
		queWatcher.start();
		logger.info("...(@>@)... the wathching thread on messageQue started ");
	}*/
	public void broadcastAndAccept(){
		try{
			//Logger.DEBUG("DiscoveryResponseServer  starting");
			/*if(this.outboundEdges.map.size() != 0){
				Logger.DEBUG("Don't broadcast !!");
				return;
				//Thread.sleep(30000);
			}
			Logger.DEBUG("Broadcast !!");*/
			DatagramSocket c = new DatagramSocket();
			c.setBroadcast(true);
			//state.getConf().get
			String host = state.getConf().getHost();
			int port = state.getConf().getCommandPort();
			String request = "Add_Node_Request," + host + ","+ port;
			byte[] sendData = request.getBytes();
			
			try{
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
				c.send(sendPacket);
				logger.info(">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
			}
			catch(Exception e){
				
			}
			
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			 while (interfaces.hasMoreElements()) {
				 NetworkInterface networkInterface = interfaces.nextElement();
				 if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					 continue; 
				 }
				 for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					 InetAddress broadcast = interfaceAddress.getBroadcast();
					 if (broadcast == null) 
						 continue;
					 try {
							 DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
							 c.send(sendPacket);
						 } 
					 catch (Exception e) {
					 }
					 
				 }
			 }
//			 System.out.println(">>> Done looping over all network interfaces. Now waiting for a reply!");
//			 byte[] recvBuf = new byte[15000];
//			 DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
//			 c.receive(receivePacket);
////			 System.out.println(">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());
//			 String message = new String(receivePacket.getData()).trim();
//			 
//			 if (message.contains("Add_Node_Request_Response"))
//				 logger.info(" Edge list recevied  " + message);
			 
			 c.close();
		}
		catch(Exception e){
			logger.info("EXCEPTION ");
			e.printStackTrace();
		}
	}
	
	public void startServer() {
		
		broadcastAndAccept();
		StartWorkCommunication comm = new StartWorkCommunication(conf);
		logger.info("Work starting");
		Thread cthread = new Thread(comm);
		cthread.start();
		
		StartCommandCommunication comm2 = new StartCommandCommunication(state, conf);
		logger.info("Command starting");
		
		Thread cthread2 = new Thread(comm2);
		cthread2.start();
		
		
		

		/*if (background) {
			Thread cthread2 = new Thread(comm2);
			cthread2.start();
		} else
			comm2.run();
		
		logger.info("Work starting");
		StartWorkCommunication comm = new StartWorkCommunication(conf);
		
		if(state != null){
			logger.debug("State is created");
		}
		logger.info("Work starting");
		//startWorkWatcher();
		// We always start the worker in the background
		Thread cthread = new Thread(comm);
		cthread.start();*/

		//if (!conf.isInternalNode()) {
			
		//}
		
	}
	
	
	
	
	
	
	/*private static class DiscoveryResponseServer implements Runnable{
		protected static Logger logger = LoggerFactory.getLogger("(D.D) DiscoveryResponseServer");
		public void run(){
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			int port = 7000;
			try {
				ServerBootstrap b = new ServerBootstrap();

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				b.childHandler(new DiscoveryCommandInit());

				logger.info("Starting Discovery Response server");
				
				ChannelFuture f = b.bind(port).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				f.channel().closeFuture().sync();

			} catch (Exception ex) {
				logger.error("Failed to setup handler.", ex);
			} finally {
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}*/

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		logger.info("Server shutdown");
		System.exit(0);
	}

	private void init(File cfg) {
		if (!cfg.exists())
			throw new RuntimeException(cfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), RoutingConf.class);
			if (!verifyConf(conf))
				throw new RuntimeException("verification of configuration failed");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//LEADER ELECTION
       NodeState.getInstance().setNodeState(NodeState.FOLLOWER);
       System.out.println("Node State :"+NodeState.getNodestate());
		
	}

	private boolean verifyConf(RoutingConf conf) {
		return (conf != null);
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartCommandCommunication implements Runnable {
		RoutingConf conf;
		ServerState state;
		
		public StartCommandCommunication(ServerState state, RoutingConf conf) {
			this.conf = conf;
			this.state = state;
		}
		
		
		
		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			
			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getCommandPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new CommandInit(conf, state, compressComm));

				// Start the server.
				logger.info("Starting command server (" + conf.getNodeId() + "), listening on port = "
						+ conf.getCommandPort());
				ChannelFuture f = b.bind(conf.getCommandPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();

			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartWorkCommunication implements Runnable {
		
		public StartWorkCommunication(RoutingConf conf) {
			if (conf == null)
				throw new RuntimeException("missing conf");

			//LEADER ELECTION
			NodeState.getInstance().setServerState(state);
			
			
			EdgeMonitor emon = new EdgeMonitor(state);
			//state.setEmon(emon);
			Thread t = new Thread(emon);
			t.start();
			//emon.broadcastAndAccept();
		}
		
		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(state.getConf().getWorkPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new WorkInit(state, compressComm));

				// Start the server.
				logger.info("Starting work server (" + state.getConf().getNodeId() + "), listening on port = "
						+ state.getConf().getWorkPort());
				ChannelFuture f = b.bind(state.getConf().getWorkPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();

			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();

				// shutdown monitor
				EdgeMonitor emon = state.getEmon();
				if (emon != null)
					emon.shutdown();
			}
		}
	}

	/**
	 * help with processing the configuration information
	 * 
	 * @author gash
	 *
	 */
	public static class JsonUtil {
		private static JsonUtil instance;

		public static void init(File cfg) {

		}

		public static JsonUtil getInstance() {
			if (instance == null)
				throw new RuntimeException("Server has not been initialized");

			return instance;
		}

		public static String encode(Object data) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.writeValueAsString(data);
			} catch (Exception ex) {
				return null;
			}
		}

		public static <T> T decode(String data, Class<T> theClass) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readValue(data.getBytes(), theClass);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
	}

}
