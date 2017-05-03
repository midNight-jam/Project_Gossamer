package gash.router.server;

import gash.router.client.CommInit;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.raft.State;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Header;
import pipe.common.Common.Node;
import pipe.common.Common.Request;
import pipe.common.Common.TaskType;
import routing.Pipe.AddNodeRequest;
import routing.Pipe.AddNodeResponse;
import routing.Pipe.CommandMessage;
import routing.Pipe.CommandMessage.Builder;

public class DiscoveryServer implements Runnable{

	protected static Logger logger = LoggerFactory.getLogger("(D.D) Discovery");
	
	static QueueCommandHandler queueHandler;
	DatagramSocket socket;
	
	public DiscoveryServer(QueueCommandHandler queueHandler){
		// TODO Auto-generated constructor stub
		DiscoveryServer.queueHandler = queueHandler;
	}
	@Override
	public void run() {
		
		try {
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			System.out.println(getClass().getName() + ">>>Ready to receive broadcast packets! at port : " + socket.getLocalPort());
			while (true) {
//				byte[] recvBuf = "DISCOVER_FUIFSERVER_REQUEST".getBytes();		
				byte[] recvBuf = new byte [500] ;
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				socket.receive(packet);
				logger.info(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
				logger.info(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()));
				String message = new String(packet.getData()).trim();
				String host = message.split(",")[1];
				int port = Integer.parseInt(message.split(",")[2]);
				
				
				if (message.contains("Add_Node_Request")) {
					logger.info("preparing to create channerl and send response of Add_Node_Request");
					//String response = "Add_Node_Request_Response [will be some data]";
					//byte[] sendData = response.getBytes();
					//DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					//socket.send(sendPacket);
//					System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
					//Thread.sleep(5000);
					//logger.info("initiating protobuff channel ....");
					Thread.sleep(6000);
					writeAddNodeResponse(host, port);
				}
			}
		}
		catch(Exception e){
			logger.info("Some Error at Discovery Server");
			e.printStackTrace();
		}
	}
	
	protected CommandMessage createAddNodeRequest(int ref, String host, int port){
		CommandMessage.Builder command = CommandMessage.newBuilder();
		
		Header.Builder header = Header.newBuilder();
		header.setNodeId(queueHandler.nodeId);
		header.setTime(System.currentTimeMillis());
	
		command.setHeader(header);
		
		AddNodeRequest.Builder anr = AddNodeRequest.newBuilder();
		anr.setRefId(ref);
		anr.setHost(host);
		anr.setPort(port);
		
		command.setAnr(anr);
		
		CommandMessage cmd = command.build();
		return cmd;
	}
	
	protected Builder createCommandMessage(){
		CommandMessage.Builder command = CommandMessage.newBuilder();
		
		Header.Builder header = Header.newBuilder();
		header.setNodeId(queueHandler.nodeId);
		header.setTime(System.currentTimeMillis());
	
		command.setHeader(header);
		
		return command;
	}
	
	protected void writeAddNodeResponse(String host, int port){
		
		EdgeList outbound = queueHandler.outbound;
		if(!outbound.getMap().isEmpty()){
			logger.info("Map is not empty !");
			for(EdgeInfo ei : outbound.getMap().values()){
				if(ei.getPort() == port){
					//TODO : Handle duplicate ANR requests
					return;
				}
			}
		}
		
		logger.info("Before changing nodeId : " + queueHandler.nodeId);
		queueHandler.nodeId += 1;
		int newNodeId = queueHandler.nodeId;
		logger.info("After changing nodeId : " + queueHandler.nodeId);
		
		AddNodeResponse.Builder response = AddNodeResponse.newBuilder();
		Node.Builder node = Node.newBuilder();
		if(!outbound.getMap().isEmpty()){
			CommandMessage msg = createAddNodeRequest(newNodeId, host, port);
			for(EdgeInfo ei : outbound.getMap().values()){
				node.setHost(ei.getHost());
				node.setPort(ei.getPort());
				node.setNodeId(ei.getRef());
				response.addEdgeList(node);
			}
			
			/*
			 * Push the addNodeRequest to the leaderQueue for cluster update
			 */
			queueHandler.leaderMessageQue.offer(msg);
		}
		
		node.setHost(queueHandler.conf.getHost());
		node.setNodeId(queueHandler.conf.getNodeId());
		node.setPort(queueHandler.conf.getCommandPort());
		response.addEdgeList(node);
		
		logger.info("Before init");
		queueHandler.init(outbound.addNode(newNodeId, host, port));
		logger.info("Size after adding edge !" + outbound.getMap().size());
		logger.info("After Init");
		
		/*
		 * Send AddNodeResponse back to the new node after updating self EdgeList 
		 * and pushing request to the leaderQueue;
		 */
		response.setNodeId(newNodeId);
		
		CommandMessage.Builder command = createCommandMessage();
		
		command.setNodeResp(response);

		CommandMessage cmd = command.build();
		logger.info("Response to node :  " + cmd);
		logger.info("Before writing to channel ");
		
		EdgeInfo ei = outbound.getMap().get(newNodeId);
		System.out.println(ei);
		if(ei.isActive() && ei.getChannel() != null){
			ChannelFuture channel1 = ei.getChannel().writeAndFlush(cmd);
			if (channel1.isDone() && channel1.isSuccess()) {
				System.out.println("Add Node Response sent succesfully:");
			}
		}
		else{
			logger.info("Channel not active!");
		}
		
		
		
		/*String host = packet.getAddress().getHostAddress();
		int port = packet.getPort();	
		EventLoopGroup group = new NioEventLoopGroup();
		ChannelFuture channel;
		while(true){
			try{
				CommInit si = new CommInit(false);
				Bootstrap b = new Bootstrap();
				b.group(group).channel(NioSocketChannel.class).handler(si);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				
				channel = b.connect(host, port).syncUninterruptibly();
				
				CommandMessage.Builder command = CommandMessage.newBuilder();
				Request.Builder msg = Request.newBuilder();
				msg.setRequestType(TaskType.WRITEFILE);
				
				Header.Builder header= Header.newBuilder();
				header.setNodeId(7777); 
				header.setTime(System.currentTimeMillis());
				command.setHeader(header);
				
				Node.Builder node = Node.newBuilder();
				node.setHost(host);
				node.setPort(port);
				node.setNodeId(7777);
				msg.setClient(node);
				command.setRequest(msg);
				CommandMessage commandMessage = command.build();
				
				channel.channel().writeAndFlush(commandMessage);
				
				if (channel.isDone() && channel.isSuccess()) {
					logger.info(" {* _ *} Discovery response attempt success for port : "+port);
					break;
				}
				else
					logger.info(" [-_-] Discovery response attempt failed for port : "+port);
				
				
			}
			catch (Throwable ex) {
				logger.info(" [-_-] Discovery Response attempt EXCEPTION for port : "+port);
				ex.printStackTrace();
			}
			try{
				Thread.sleep(1500);
			}
			catch(InterruptedException e){
				logger.info(" (D.D) --^^^-- Discovery Response thread went wrong" );
			}
		}*/
	}
}