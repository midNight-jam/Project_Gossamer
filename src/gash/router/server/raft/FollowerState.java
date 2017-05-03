package gash.router.server.raft;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gash.router.client.CommInit;
import gash.router.database.DatabaseService;
import gash.router.database.Record;
import gash.router.logger.Logger;
import gash.router.server.ServerUtils;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.timer.NodeTimer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.work.Work.WorkMessage;
import routing.Pipe.AddNodeRequest;
import routing.Pipe.CommandMessage;
import pipe.common.Common.Request;
import pipe.common.Common.Response;
import pipe.common.Common.TaskType;
import pipe.common.Common.WriteBody;

import pipe.work.VoteRPC.ResponseVoteRPC;





public class FollowerState extends State implements Runnable{
	public static int leaderId;
	public static Boolean isHeartBeatRecieved = Boolean.FALSE;
	NodeTimer timer;
	private Boolean writeStatus=true;
	private static String clienthost;
	private static int clientport;
	private static ChannelFuture channel;
	
	private static EventLoopGroup group;
	
	private static FollowerState INSTANCE = null;
	Thread fThread = null;
	private FollowerState() {
		// TODO Auto-generated constructor stub
	}
	
	public static FollowerState getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new FollowerState();

		}
		return INSTANCE;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Follower state");
		gash.router.logger.Logger.DEBUG("-----------------------FOLLOWER SERVICE STARTED ----------------------------");
		initFollower();

		fThread = new Thread(){
		    public void run(){
				while (running) {
					while (NodeState.getInstance().getNodestate() == NodeState.FOLLOWER) {
					}
				}

		    }
		 };

		fThread.start();
		
		
		
		
	}

	public void handleGetChunkLocation(Request request,long timestamp) {
		String filename= request.getRrb().getFilename();
		String file_id = request.getRrb().getFileId();
		//TODO
		String file_ext="";
		System.out.println("GET Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		NodeState.updateTaskCount();
		List<Record> list=DatabaseService.getInstance().getDb().get(filename,file_id,file_ext);
		Record record = list.get(0);
		System.out.println("I am preparing response for"+record.getFilename());
		 Map<Integer, EdgeInfo> nodemap = new HashMap<>();
			
			for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
				if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null) {	
					nodemap.put(ei.getRef(), ei);
					}
				}
			EdgeInfo selfei= new EdgeInfo(NodeState.getInstance().getServerState().getConf().getNodeId(),NodeState.getInstance().getServerState().getConf().getHost(),NodeState.getInstance().getServerState().getConf().getCommandPort());
			nodemap.put(NodeState.getInstance().getServerState().getConf().getNodeId(),selfei);
		   //send the list of nodes with data in response
		    //CommandMessage responsePacket= ServerMessageUtils.prepareChunkLocationResponse(record,NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap());
			CommandMessage responsePacket= ServerMessageUtils.prepareChunkLocationResponse(record,nodemap);
			//to be sent to the API CLIENT
			sendReadResponse(responsePacket);
		
	
	
}
	public void sendReadResponse(CommandMessage responseMessage){
		//make response object and send back to the APICLient
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap()
				.values()) {
            
			if (ei.getRef()==0 && ei.isActive() && ei.getChannel() != null) {
                
				Logger.DEBUG("Sending the readResponse back to " + ei.getRef());

				ChannelFuture cf = ei.getChannel().writeAndFlush(responseMessage);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send response back to APICLIENT");
				}
			}
		}}
	private void initFollower() {
		// TODO Auto-generated method stub

		timer = new NodeTimer();
         
		timer.schedule(new Runnable() {
			@Override
			public void run() {
				gash.router.logger.Logger.DEBUG("CHanging state");
				NodeState.getInstance().setNodeState(NodeState.CANDIDATE);
			}
		}, ServerUtils.getElectionTimeout());

	}

	
	
	public void onReceivingHeartBeatPacket() {
		timer.reschedule(ServerUtils.getElectionTimeout());
	}

	@Override
	public WorkMessage handleRequestVoteRPC(WorkMessage workMessage) {
		 WorkMessage response;
		if (workMessage.getVoteRPCPacket().getRequestVoteRPC().getTimeStampOnLatestUpdate() < NodeState.getTimeStampOnLatestUpdate()) {
			Logger.DEBUG(NodeState.getInstance().getServerState().getConf().getNodeId() + " has replied NO");
			response=ServerMessageUtils.prepareResponseVoteRPC(ResponseVoteRPC.IsVoteGranted.NO);
			return response;

		}
		Logger.DEBUG(NodeState.getInstance().getServerState().getConf().getNodeId() + " has replied YES");
		response= ServerMessageUtils.prepareResponseVoteRPC(ResponseVoteRPC.IsVoteGranted.YES);
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null && ei.getRef() ==Integer.parseInt(workMessage.getVoteRPCPacket().getRequestVoteRPC().getCandidateId())){
				ChannelFuture cf = ei.getChannel().writeAndFlush(response);
				Logger.DEBUG("Vote Channel created and response send !");
			}
			}
			return ServerMessageUtils.prepareResponseVoteRPC(ResponseVoteRPC.IsVoteGranted.YES);
	}

	public void handleHeartBeat(WorkMessage wm) {
		Logger.DEBUG("HeartbeatPacket received from leader :" + wm.getHeartBeatPacket().getHeartbeat().getLeaderId());
		NodeState.currentTerm = wm.getHeartBeatPacket().getHeartbeat().getTerm();
		onReceivingHeartBeatPacket();
		WorkMessage heartBeatResponse = ServerMessageUtils.prepareHeartBeatResponse();

		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {

			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null
					&& ei.getRef() == wm.getHeartBeatPacket().getHeartbeat().getLeaderId()) {

				Logger.DEBUG("Sent HeartBeatResponse to " + ei.getRef());
				ChannelFuture cf = ei.getChannel().writeAndFlush(heartBeatResponse);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send message (HeartBeatResponse) to server");
				}
			}
		}

	}
/**
 * Actual Append
 */
	@Override
	public void handleAppendEntries(WorkMessage wm) {
		Request request= wm.getAppendEntriesPacket().getAppendEntries().getRequest();
		leaderId=wm.getHeartBeatPacket().getHeartbeat().getLeaderId();
		long unixTimeStamp = wm.getAppendEntriesPacket().getAppendEntries().getTimeStampOnLatestUpdate();
		if(request.getRequestType()== TaskType.REQUESTWRITEFILE){
		handleWriteFile(request, unixTimeStamp);
		}
		Logger.DEBUG("Inserted entry with received from "
				+ wm.getAppendEntriesPacket().getAppendEntries().getLeaderId());

	}
	
	
	
	
	
	
	@Override
	public byte[] handleGetMessage(String key) {
		System.out.println("GET Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		NodeState.updateTaskCount();
	//	return DatabaseService.getInstance().getDb().get(key);
		return null;
	}


	@Override
	public void startService(State state) {

		running = Boolean.TRUE;
		cthread = new Thread((FollowerState) state);
		cthread.start();

	}

	@Override
	public void stopService() {
		running = Boolean.FALSE;
	}
	
	public void handleWriteFile(Request request,long timestamp) {
		WriteBody msg = request.getRwb();
		System.out.println("POST Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		Logger.DEBUG("Data for File : "+msg.getFilename());
		Logger.DEBUG("Data : "+msg.getChunk().getChunkData().toStringUtf8());
		
		NodeState.updateTaskCount();
		try {
			DatabaseService.getInstance().getDb().post(msg.getFileId(), msg.getFilename(), msg.getFileExt(), msg.getChunk().getChunkId(),msg.getChunk().getChunkData(),msg.getChunk().getChunkSize(),msg.getNumOfChunks(),timestamp);
		    writeStatus=true;
		} catch (SQLException e) {
			writeStatus=false;
			//e.printStackTrace();
		}
		if(writeStatus){
			NodeState.setTimeStampOnLatestUpdate(timestamp);
			
		}
		sendAppendEntriesResponse(writeStatus);
		
	}
	
	
	
	public CommandMessage handleGetChunkData(Request request,long timestamp) {
		String filename= request.getRrb().getFilename();
		long file_id = 0;
		long chunk_id= request.getRrb().getChunkId();
	
		String file_ext="";		
		
		System.out.println("GET Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		NodeState.updateTaskCount();
		
		Record record=DatabaseService.getInstance().getDb().getChunkData(filename,file_id,chunk_id);
		System.out.println("Succesfully read : "+record.getFilename());
		CommandMessage responsePacket= ServerMessageUtils.prepareChunkDataResponse(record);
		System.out.println("Succesfully read");
		//TO BE SENT TO SPECIFIC CLIENT MAKING THE REQUEST
		sendReadDataResponse(request.getClient().getHost(),request.getClient().getPort(),responsePacket);
		return responsePacket;
	  
	
}
public void sendReadDataResponse(String host, int port ,CommandMessage responseMessage){
	System.out.println("Trying to send the read response");
		
		//make response object and send back to the specific client
		if(host== clienthost && port == clientport && channel!=null && channel.channel().isActive()){
			
			channel.channel().writeAndFlush(responseMessage);
			
			if (channel.isDone() && channel.isSuccess()) {
				System.out.println("Msg sent succesfully:");
			}
			else{
				System.out.println("Msg sending failed:");
			}
		}
		else{
			createConnection(host,port);
			channel.channel().writeAndFlush(responseMessage);
			
			if (channel.isDone() && channel.isSuccess()) {
				System.out.println("Msg sent succesfully:");
			}
			else{
				System.out.println("Msg sending failed:");
			}
		}
		
	}
		
		
		public void sendAppendEntriesResponse(Boolean status) {
			WorkMessage appendEntriesResponse = ServerMessageUtils.prepareAppendEntriesResponse(status);

			for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {

				if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null
						&& ei.getRef() == leaderId) {

					Logger.DEBUG("Sent appendEntriesResponse to " + ei.getRef());
					ChannelFuture cf = ei.getChannel().writeAndFlush(appendEntriesResponse);
					if (cf.isDone() && !cf.isSuccess()) {
						Logger.DEBUG("failed to send message (appendEntriesResponse) to server");
					}
				}
			}
			

		}
		
		public void createConnection(String host, int port){	
			clienthost=host;
			clientport=port;
			
			group = new NioEventLoopGroup();
			try { 
				CommInit si = new CommInit(false);
				Bootstrap b = new Bootstrap();
				b.group(group).channel(NioSocketChannel.class).handler(si);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);


				// Make the connection attempt.
				 channel = b.connect(clienthost, clientport).syncUninterruptibly();

				
				// want to monitor the connection to the server s.t. if we loose the
				// connection, we can try to re-establish it.
				// ClientClosedListener ccl = new ClientClosedListener(this);
				// channel.channel().closeFuture().addListener(ccl);

				System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
						+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());

			} catch (Throwable ex) {
				System.out.println("failed to initialize the client connection " + ex.toString());
				ex.printStackTrace();
			}
			}
		
		


		public void handleAddNodeRequestAppendEntry(WorkMessage message) {
			
			//TODO add the info got by the AddNodeRequest
			int ref = message.getAddNodeRequest().getRefId();
			String host = message.getAddNodeRequest().getHost();
			int port = message.getAddNodeRequest().getPort();
		//	EdgeInfo ei = new EdgeInfo(ref, host, port);
			
			//NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values().add(ei);
			NodeState.getInstance().getServerState().getEmon().getOutboundEdges().addNode(ref,host, port);
			System.out.println("Add Node Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
			NodeState.updateTaskCount();
	
		
	}
		public void handleInternalPing(){
			//send Ping response
		System.out.println("****************************I have recieved the Ping******************************** ");
			
			
		}
	
	
	

}
