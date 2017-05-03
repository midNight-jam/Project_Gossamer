package gash.router.server.raft;


import pipe.common.Common.Request;
import pipe.common.Common.Response;
import pipe.common.Common.WriteBody;
import pipe.work.Work.WorkMessage;
import routing.Pipe.AddNodeRequest;
import routing.Pipe.CommandMessage;


/**
 * State pattern : Abstract class for leader , follower , candidate 
 * @author seemarohilla
 *
 */
public class State {
	
	

	protected volatile Boolean running = Boolean.TRUE;
	static Thread cthread;
	
	public void startService(State state) {

	}
	
	public void handleAddNodeRequest(AddNodeRequest request,long timestamp) {
		
	}

	public void stopService() {
		// TODO Auto-generated method stub

	}

	public void handleResponseVoteRPCs(WorkMessage workMessage) {
		// TODO Auto-generated method stub

	}

	public WorkMessage handleRequestVoteRPC(WorkMessage workMessage) {
		// TODO Auto-generated method stub
		return null;
	}

	public void sendHeartBeat() {

	}

	
	public void sendAppendEntriesResponse() {

	}
	
	public void handleAppendEntriesResponse(WorkMessage wm) {

	}
	
	
	public void handleHeartBeat(WorkMessage wm) {

	}
	
	public void handleHeartBeatResponse(WorkMessage wm) {

	}

	public void handleAppendEntries(WorkMessage wm) {

	}
	
	
	
	public byte[] handleGetMessage(String key) {
		return new byte[1];
	}
	
	public String handlePostMessage(byte[] image, long timestamp) {
		return null;
	}

	public void handlePutMessage(String key, byte[] image, long timestamp) {
		
	}
	
	public void handleDelete(String key) {
		
	}
	
	public void sendAppendEntriesPacket(Request request,long timestamp) {
		
	}
	

	public void handleWriteFile(Request msg, long timestamp) {
		
	}
	public void handleGetFile(Request msg, long timestamp){
		
	}
	
	public CommandMessage handleGetChunkData(Request request,long timestamp){
		return null;
	}
	
	public void handleGetChunkLocation(Request request,long timestamp){
		
	}
	public void handleAddNodeRequestAppendEntry(WorkMessage message) {
		
	}
public void sendAddNodeRequestAppendEntry(AddNodeRequest request,long timestamp) {
		
	}
	public void handleGlobalPing(CommandMessage message){
		
	}
    public void handleInternalPing(){
    	
    }
    
}
