package gash.router.server.raft;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;

import gash.router.client.CommInit;
import gash.router.database.DatabaseService;
import gash.router.database.Record;
import gash.router.logger.Logger;
import gash.router.server.ServerUtils;
import gash.router.server.WorkHandler;
import gash.router.server.WorkInit;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Request;
import pipe.common.Common.Response;
import pipe.common.Common.WriteBody;
import pipe.work.AppendEntriesRPC.AppendEntriesResponse.IsUpdated;
import pipe.work.VoteRPC.ResponseVoteRPC;
import pipe.work.Work.WorkMessage;
import redis.clients.jedis.Jedis;
import routing.Pipe.AddNodeRequest;
import routing.Pipe.CommandMessage;



public class LeaderState extends State implements Runnable {
    //making it singleton
	private static LeaderState INSTANCE = null;
	Thread heartBt = null;
	private Boolean writeStatus=true;
	private int totalNodes;
	private int totalUpdated;
	private static String clienthost;
	private static int clientport;
	private static ChannelFuture channel;
	
	private static EventLoopGroup group;
	
	private LeaderState() {
		// TODO Auto-generated constructor stub

	}

	public static LeaderState getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LeaderState();
		}
		return INSTANCE;
	}

	@Override
	public void run() {
		Logger.DEBUG("-----------------------LEADER SERVICE STARTED ----------------------------");
//		NodeState.currentTerm++;
		initLatestTimeStampOnUpdate();
		heartBt = new Thread(){
		    public void run(){
				while (running) {
					try {
						Thread.sleep(NodeState.getInstance().getServerState().getConf().getHeartbeatDt());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sendHeartBeat();
				}
		    }
		 };

		heartBt.start();
		//ServerQueueService.getInstance().createQueue();
	}

	private void initLatestTimeStampOnUpdate() {

		NodeState.setTimeStampOnLatestUpdate(DatabaseService.getInstance().getDb().getCurrentTimeStamp());

	}
	
	/**
	 * This is for Actual sending of the write request
	 */
	@Override
	public void sendAppendEntriesPacket(Request request, long timestamp) {
		WorkMessage workMessage = ServerMessageUtils.prepareAppendEntriesPacket(request, timestamp);
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap()
				.values()) {
            
			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null) {
                
				Logger.DEBUG("Sent AppendEntriesPacket to " + ei.getRef());

				ChannelFuture cf = ei.getChannel().writeAndFlush(workMessage);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send message (AppendEntriesPacket) to server");
				}
			}
		}
			
	}
/**
 * checks if the log is consistent by checking timestamp on latest update .
 * if not then sends append entries
 */
	public void handleHeartBeatResponse(WorkMessage wm) {

		long timeStampOnLatestUpdate = wm.getHeartBeatPacket().getHeartBeatResponse().getTimeStampOnLatestUpdate();

		if (DatabaseService.getInstance().getDb().getCurrentTimeStamp() > timeStampOnLatestUpdate) {
			List<Record> laterEntries = DatabaseService.getInstance().getDb().getNewEntries(timeStampOnLatestUpdate);

			for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap()
					.values()) {

				if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null
						&& ei.getRef() == wm.getHeartBeatPacket().getHeartBeatResponse().getNodeId()) {

					for (Record record : laterEntries) {
						
						WorkMessage workMessage = ServerMessageUtils.prepareAppendEntries(record);
						Logger.DEBUG("Sent AppendEntriesPacket to " + ei.getRef());
						ChannelFuture cf = ei.getChannel().writeAndFlush(workMessage);
						if (cf.isDone() && !cf.isSuccess()) {
							Logger.DEBUG("failed to send message (AppendEntriesPacket) to server");
						}
					}
				}
			}
		
		
		}
			

	}

	@Override
	public WorkMessage handleRequestVoteRPC(WorkMessage workMessage) {
		
		return ServerMessageUtils.prepareResponseVoteRPC(ResponseVoteRPC.IsVoteGranted.NO);
	}
	
	public void handleHeartBeat(WorkMessage wm) {
		Logger.DEBUG("HeartbeatPacket received from leader :" + wm.getHeartBeatPacket().getHeartbeat().getLeaderId());
		WorkMessage heartBeatResponse = ServerMessageUtils.prepareHeartBeatResponse();
		
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {

			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null
					&& ei.getRef() == wm.getHeartBeatPacket().getHeartbeat().getLeaderId()) {
					if(wm.getHeartBeatPacket().getHeartbeat().getTerm()>=NodeState.currentTerm) {
						NodeState.getInstance().setNodeState(NodeState.FOLLOWER);
					}

			}
		}

	}

	@Override
	public void sendHeartBeat() {
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null) {
				WorkMessage workMessage = ServerMessageUtils.prepareHeartBeat();
				Logger.DEBUG("Sent HeartBeatPacket to " + ei.getRef());
				ChannelFuture cf = ei.getChannel().writeAndFlush(workMessage);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send message (HeartBeatPacket) to server");
				}
			}
		}
			
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
			e.printStackTrace();
		}
		NodeState.setTimeStampOnLatestUpdate(timestamp);
		sendAppendEntriesPacket(request,timestamp);
		//send response back to client
		
	}

	
	@Override
	public void handleDelete(String key) {
		System.out.println("DELETE Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		NodeState.updateTaskCount();
		NodeState.setTimeStampOnLatestUpdate(System.currentTimeMillis());
	//	DatabaseService.getInstance().getDb().delete(key);
	//	WorkMessage wm = ServerMessageUtils.prepareAppendEntriesPacket(key, null, 0 ,RequestType.DELETE);
		//sendAppendEntriesPacket(wm);
	}	

	public void startService(State state) {
		
		//TODO notify the CLIENTAPI of new Leader
		running = Boolean.TRUE;
		cthread = new Thread((LeaderState) state);
		cthread.start();
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
	    //int i=1;
		
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null) {
				
				nodemap.put(ei.getRef(), ei);
				//i++;
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
	
	
	public void handleAddNodeRequest(AddNodeRequest request,long timestamp) {
		//TODO add the info got by the AddNodeRequest
		System.out.println("Inside handling Add node request in leader !");
		int ref = request.getRefId();
		String host= request.getHost();
		int port = request.getPort();
		//EdgeInfo ei = new EdgeInfo(ref, host, port);
		NodeState.getInstance().getServerState().getEmon().getOutboundEdges().addNode(ref,host, port);
		System.out.println("Add Node Request Processed by Node: " + NodeState.getInstance().getServerState().getConf().getNodeId());
		NodeState.updateTaskCount();
	//send append entries to other nodes	
		sendAddNodeRequestAppendEntry( request, timestamp);
}
	
public void sendAddNodeRequestAppendEntry(AddNodeRequest request,long timestamp) {
		//Build a work message to be sent
	WorkMessage addnoderequest= ServerMessageUtils.prepareAddNodeRequest(request);
	for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
		if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null && ei.getRef()!=request.getRefId()) {
			Logger.DEBUG("Sent Add node Request to " + ei.getRef());
			ChannelFuture cf = ei.getChannel().writeAndFlush(addnoderequest);
			if (cf.isDone() && !cf.isSuccess()) {
				Logger.DEBUG("failed to send message (AddNodeRequest) to server");
			}
		}
		else{
			Logger.DEBUG("No followers !!");
		}
	}
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
			//handleReadResponse(responsePacket);
			sendReadDataResponse(request.getClient().getHost(),request.getClient().getPort(),responsePacket);
		  return responsePacket;
		
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
			}
			
		
		} 
	

	public void stopService() {
		running = Boolean.FALSE;

	}
	

	public void sendReadDataResponse(String host, int port ,CommandMessage responseMessage){

		System.out.println("Trying to send the read response");
		//make response object and send back to the specific client
		if(host == clienthost && port == clientport && channel!=null && channel.channel().isActive()){
			
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
	
	public void handleAppendEntriesResponse(WorkMessage wm) {
		
		//check if the response is yes or not and then update the client if data stored or not by sending write response
		if(wm.getAppendEntriesPacket().getAppendEntriesResponse().getIsUpdated() == IsUpdated.YES){
			totalUpdated++;
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
	private  Jedis localhostJedis=new Jedis("169.254.214.175",6379); //during demo day IP will change
	//private static Jedis localhostJedis=new Jedis("127.0.0.1",6379); //during demo day IP will change
	public  Jedis getLocalhostJedis() {
		return localhostJedis;
	}
	
	public void handleGlobalPing(CommandMessage message){
	/**	if(message.getHeader().getDestination()==1){
			System.out.println("__________________________I HAVE RECIEVED THE PING AFTER ROUNDTRIP_________________________ ");
			Logger.DEBUG("TERMINATING THE PING");
		}**/
		//send to other cluster
		//read host port from redis server
		for (EdgeInfo ei : NodeState.getInstance().getServerState().getEmon().getOutboundEdges().getMap().values()) {
			if (ei.getRef()!=0 && ei.isActive() && ei.getChannel() != null) {
				WorkMessage workMessage = ServerMessageUtils.preparePingMessage(NodeState.getInstance().getServerState().getConf().getNodeId(),NodeState.getInstance().getServerState().getConf().getHost(),NodeState.getInstance().getServerState().getConf().getWorkPort());
				
				ChannelFuture cf = ei.getChannel().writeAndFlush(workMessage);
				if (cf.isDone() && !cf.isSuccess()) {
					Logger.DEBUG("failed to send message (PING) to server");
				}
				else {
					
					Logger.DEBUG("Sent PING to " + ei.getRef());
				}
			}
		}
		//code to send the global ping to external cluster
	/**	String host="";
		int port=0;
		
		try{
			  getLocalhostJedis().select(0);// uses default database 
			  
			String hostport =getLocalhostJedis().get("2");// use this to get leader for cluster from redis, 
			  //should be implementd on client
			String[] words=hostport.split(":");//splits the string based on whitespace 
			host=words[0];
			port=Integer.parseInt(words[1]);
			  System.out.println("---Redis read---");

			}catch(Exception e){
			  System.out.println("---Problem with redis---");
			}   
		
		
		createConnection(host,port);
		if(channel!=null && channel.channel().isActive()){
		channel.channel().writeAndFlush(message);
		
		if (channel.isDone() && channel.isSuccess()) {
			System.out.println("Msg sent succesfully:");
		}
		else{
			System.out.println("Msg sending failed:");
		}
		
	}
	
	else{
		
		Logger.DEBUG("Cannot make connection to external Cluster");
	}
	
	**/
	}
	
	}
