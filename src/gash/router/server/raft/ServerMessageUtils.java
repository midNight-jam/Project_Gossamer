package gash.router.server.raft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.protobuf.ByteString;

import gash.router.database.DatabaseService;
import gash.router.database.Record;
import gash.router.server.ServerUtils;
import gash.router.server.edges.EdgeInfo;
import pipe.common.Common.Chunk;
import pipe.common.Common.ChunkLocation;
import pipe.common.Common.Header;
import pipe.common.Common.Node;
import pipe.common.Common.ReadResponse;
import pipe.common.Common.Request;
import pipe.common.Common.Response;
import pipe.common.Common.Response.Status;
import pipe.common.Common.TaskType;
import pipe.common.Common.WriteBody;
import pipe.work.AppendEntriesRPC;
import pipe.work.AppendEntriesRPC.AppendEntries;

import pipe.work.AppendEntriesRPC.AppendEntriesPacket;
import pipe.work.AppendEntriesRPC.AppendEntriesResponse;
import pipe.work.AppendEntriesRPC.AppendEntriesResponse.IsUpdated;
import pipe.work.HeartBeatRPC.HeartBeat;
import pipe.work.HeartBeatRPC.HeartBeatPacket;
import pipe.work.HeartBeatRPC.HeartBeatResponse;
import pipe.work.Ping.PingMessage;
import pipe.work.VoteRPC.RequestVoteRPC;
import pipe.work.VoteRPC.ResponseVoteRPC;
import pipe.work.VoteRPC.ResponseVoteRPC.IsVoteGranted;
import pipe.work.VoteRPC.VoteRPCPacket;
import pipe.work.Work.WorkMessage;
import routing.Pipe.AddNodeRequest;
import routing.Pipe.CommandMessage;

public class ServerMessageUtils {

	public static WorkMessage prepareRequestVoteRPC() {
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		RequestVoteRPC.Builder requestVoteRPC = RequestVoteRPC.newBuilder();
		requestVoteRPC.setTerm(NodeState.getInstance().getServerState().getConf().getNodeId());
		requestVoteRPC.setCandidateId("" + NodeState.getInstance().getServerState().getConf().getNodeId());
		requestVoteRPC.setTerm(NodeState.currentTerm);
		requestVoteRPC.setTimeStampOnLatestUpdate(NodeState.getTimeStampOnLatestUpdate());
	    requestVoteRPC.setTimeStampOnLatestUpdate(DatabaseService.getInstance().getDb().getCurrentTimeStamp());

		VoteRPCPacket.Builder voteRPCPacket = VoteRPCPacket.newBuilder();
		voteRPCPacket.setUnixTimestamp(ServerUtils.getCurrentUnixTimeStamp());
		voteRPCPacket.setRequestVoteRPC(requestVoteRPC);
		
		work.setVoteRPCPacket(voteRPCPacket);

		return work.build(); 
		
	}
	
	
	public static CommandMessage prepareChunkLocationResponse(Record record, Map nodemap) {
		CommandMessage.Builder response = CommandMessage.newBuilder();
		System.out.print("Making chunk location response");
		Response.Builder msg = Response.newBuilder();
		msg.setResponseType(TaskType.RESPONSEREADFILE);
		msg.setStatus(Status.SUCCESS);
	    ReadResponse.Builder readResponse = ReadResponse.newBuilder();
		readResponse.setFilename(record.getFilename());
		System.out.println("CHUNK_ NUMBER"+record.getNum_of_chunks());
		readResponse.setNumOfChunks((int)record.getNum_of_chunks());
		 
	
		 
		 
		for(int i = 0; i< record.getNum_of_chunks(); i++){ 
			Random rand = new Random();
		
			int  key = rand.nextInt(nodemap.keySet().size())+1;
			
			ChunkLocation.Builder chunkLocation = ChunkLocation.newBuilder();
			chunkLocation.setChunkid(i);
			//set the node from where u can read the chunks
			Node.Builder node = Node.newBuilder();
			System.out.print("Making chunk location response inside loop : set node");
	
			
			
			System.out.print("KEY VALUE:"+key);
			EdgeInfo ei = (EdgeInfo) nodemap.get(key);
			node.setHost(ei.getHost());
			node.setPort(ei.getPort());
		//	node.setPort(4168);
			node.setNodeId(ei.getRef());
			
			System.out.print("Making chunk location response inside loop : set nodeId");
			chunkLocation.addNode(node);
			System.out.print("Making chunk location response inside loop : create setting node to chunklocation ");
			//set the chunklocation object for that chunk
			readResponse.addChunkLocation(chunkLocation);
			System.out.print("Making chunk location response inside loop : setting chunklocation to the readresponse");
			
		}
		msg.setReadResponse(readResponse);
		Header.Builder header= Header.newBuilder();
		header.setNodeId(1);
		header.setTime(ServerUtils.getCurrentUnixTimeStamp());
		response.setHeader(header);
		
		response.setResponse(msg);
		
		CommandMessage commandMessage = response.build();
		System.out.print("Build");
		return commandMessage;

	}
	

	public static WorkMessage preparePingMessage(int nodeId,String host , int port ){
		
		
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());
		PingMessage.Builder pingMessage = PingMessage.newBuilder();
		pingMessage.setNodeId(nodeId);
		pingMessage.setIP(host);
		pingMessage.setPort(port);
		work.setTrivialPing(pingMessage);
		return work.build();
		
	}
	
	public static CommandMessage prepareChunkDataResponse(Record record) {
		CommandMessage.Builder response = CommandMessage.newBuilder();
		System.out.println("i am making a response");
		Response.Builder msg = Response.newBuilder();
		msg.setResponseType(TaskType.RESPONSEREADFILE);
	    ReadResponse.Builder readResponse = ReadResponse.newBuilder();
		readResponse.setFilename(record.getFilename());
		readResponse.setNumOfChunks((int)record.getNum_of_chunks());
		Chunk.Builder chunk = Chunk.newBuilder();
		chunk.setChunkData(record.getChunk_data());
		int chunk_id=(int) record.getChunk_id();
		chunk.setChunkId(chunk_id);
		int chunk_size=(int) record.getChunk_size();
		chunk.setChunkSize(chunk_size);
		readResponse.setChunk(chunk);
	    msg.setReadResponse(readResponse);
	    msg.setStatus(Status.SUCCESS);
		Header.Builder header= Header.newBuilder();
		header.setNodeId(1);
		header.setTime(0);
		response.setHeader(header);
		response.setResponse(msg);
		CommandMessage commandMessage = response.build();
		System.out.println("Made a resposne");
		return commandMessage;

	}
	
	

	public static WorkMessage prepareAppendEntriesResponse(Boolean status) {
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		AppendEntriesPacket.Builder appendEntriesPacket = AppendEntriesPacket.newBuilder();
		appendEntriesPacket.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		AppendEntriesResponse.Builder appendEntriesResponse = AppendEntriesResponse.newBuilder();
        if(status){
		appendEntriesResponse.setIsUpdated(IsUpdated.YES);}
        else{
        appendEntriesResponse.setIsUpdated(IsUpdated.NO);
        }

		appendEntriesPacket.setAppendEntriesResponse(appendEntriesResponse);

		work.setAppendEntriesPacket(appendEntriesPacket);

		return work.build();

	}

	public static WorkMessage prepareHeartBeatResponse() {
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		HeartBeatResponse.Builder heartbeatResponse = HeartBeatResponse.newBuilder();
		heartbeatResponse.setNodeId(NodeState.getInstance().getServerState().getConf().getNodeId());
		heartbeatResponse.setTerm(NodeState.currentTerm);
		heartbeatResponse.setTimeStampOnLatestUpdate(NodeState.getTimeStampOnLatestUpdate());
	   heartbeatResponse.setTimeStampOnLatestUpdate(DatabaseService.getInstance().getDb().getCurrentTimeStamp());
		HeartBeatPacket.Builder heartBeatPacket = HeartBeatPacket.newBuilder();
		heartBeatPacket.setUnixTimestamp(ServerUtils.getCurrentUnixTimeStamp());
		heartBeatPacket.setHeartBeatResponse(heartbeatResponse);
		
		work.setHeartBeatPacket(heartBeatPacket);

		return work.build();

	}
	
	


	public static WorkMessage prepareHeartBeat() {
		
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		HeartBeat.Builder heartbeat = HeartBeat.newBuilder();
		heartbeat.setLeaderId(NodeState.getInstance().getServerState().getConf().getNodeId());
		heartbeat.setTerm(NodeState.currentTerm);
		// Optional

		heartbeat.setTimeStampOnLatestUpdate(NodeState.getTimeStampOnLatestUpdate());

		// heartbeat.setTimeStampOnLatestUpdate(DatabaseService.getInstance().getDb().getCurrentTimeStamp());
		HeartBeatPacket.Builder heartBeatPacket = HeartBeatPacket.newBuilder();
		heartBeatPacket.setUnixTimestamp(ServerUtils.getCurrentUnixTimeStamp());
		heartBeatPacket.setHeartbeat(heartbeat);

		work.setHeartBeatPacket(heartBeatPacket);

		return work.build();
		
	}

	public static WorkMessage prepareAppendEntriesPacket(Request wm, long timestamp
			) {

		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		AppendEntriesPacket.Builder appendEntriesPacket = AppendEntriesPacket.newBuilder();
		appendEntriesPacket.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		

		AppendEntries.Builder appendEntries = AppendEntries.newBuilder();
		appendEntries.setTimeStampOnLatestUpdate(timestamp);
		appendEntries.setRequest(wm);
		appendEntries.setLeaderId(NodeState.getInstance().getServerState().getConf().getNodeId());

	
		appendEntriesPacket.setAppendEntries(appendEntries);

		work.setAppendEntriesPacket(appendEntriesPacket);

		return work.build();

	}
	
	
	
	public static WorkMessage prepareAppendEntries(Record record) {
		
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTWRITEFILE);
		WriteBody.Builder rwb  = WriteBody.newBuilder();
		rwb.setFileId(record.getFile_id());
		rwb.setFileExt(record.getFile_ext());
		rwb.setFilename(record.getFilename());
		rwb.setNumOfChunks((int)record.getNum_of_chunks());
		Chunk.Builder chunk = Chunk.newBuilder();
		chunk.setChunkId((int)record.getChunk_id());
		chunk.setChunkSize((int)record.getChunk_size());
		chunk.setChunkData(record.getChunk_data());
		rwb.setChunk(chunk);
		msg.setRwb(rwb);
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());
		AppendEntriesPacket.Builder appendEntriesPacket = AppendEntriesPacket.newBuilder();
		appendEntriesPacket.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());
		AppendEntries.Builder appendEntries = AppendEntries.newBuilder();
		appendEntries.setTimeStampOnLatestUpdate(record.getTimestamp());
		appendEntries.setRequest(msg);
		appendEntries.setLeaderId(NodeState.getInstance().getServerState().getConf().getNodeId());

	
		appendEntriesPacket.setAppendEntries(appendEntries);

		work.setAppendEntriesPacket(appendEntriesPacket);

		return work.build();

	}
	
	
	

	public static WorkMessage prepareResponseVoteRPC(IsVoteGranted decision) {
		
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		VoteRPCPacket.Builder voteRPCPacket = VoteRPCPacket.newBuilder();
		voteRPCPacket.setUnixTimestamp(ServerUtils.getCurrentUnixTimeStamp());

		ResponseVoteRPC.Builder responseVoteRPC = ResponseVoteRPC.newBuilder();
		responseVoteRPC.setTerm(NodeState.getInstance().getServerState().getConf().getNodeId());
		responseVoteRPC.setIsVoteGranted(decision);

		voteRPCPacket.setResponseVoteRPC(responseVoteRPC);

		work.setVoteRPCPacket(voteRPCPacket);

		return work.build();
	}
	
public static WorkMessage prepareAddNodeRequest(AddNodeRequest request) {
		
		WorkMessage.Builder work = WorkMessage.newBuilder();
		work.setUnixTimeStamp(ServerUtils.getCurrentUnixTimeStamp());

		work.setAddNodeRequest(request);

		return work.build();
	}


	
}
