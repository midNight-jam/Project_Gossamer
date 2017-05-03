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
package gash.router.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.protobuf.ByteString;

import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import pipe.common.Common.ChunkLocation;
import pipe.common.Common.Failure;
import pipe.common.Common.Header;
import pipe.common.Common.Node;
import pipe.common.Common.ReadBody;
import pipe.common.Common.Request;
import pipe.common.Common.TaskType;
import pipe.common.Common.Response.Status;
import redis.clients.jedis.Jedis;
import routing.Pipe.CommandMessage;

public class DemoApp implements CommListener {
	
	static String host;
	static int port;
	static Map<Integer,ByteString> chunk_buffer= new HashMap<>();
	
	 static final int chunkSize = 1024*1024; // MAX BLOB STORAGE = Math,pow(2,15) -1 = 65535 Bytes 
	private MessageClient mc;
	 private static Jedis localhostJedis=new Jedis("192.168.1.20",6379); //during demo day IP will change
	 public static Jedis getLocalhostJedis() {
	 	return localhostJedis;
	 }
	public DemoApp(MessageClient mc) {
		init(mc);
	}

	private void init(MessageClient mc) {
		this.mc = mc;
		this.mc.addListener(this);
	}

	public  void ping(int destination){	

		mc.ping(destination);
		
	}

	@Override
	public String getListenerID() {
		return "demo";
	}

	@Override
	public void onMessage(CommandMessage msg) {
		System.out.println("---> " + msg);
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

         
		try {
			// TODO How can you implement this without if-else statements?
		
		    if (msg.hasResponse() == true && msg.getResponse().getResponseType()== TaskType.RESPONSEWRITEFILE) {
				
				
				 if(msg.getResponse().getStatus()==Status.SUCCESS){
					 System.out.println("File Written Successfully !");	 
				 }
				 else if (msg.getResponse().getStatus()==Status.ERROR){
				  List list=msg.getResponse().getWriteResponse().getChunkIdList();
				
					//TODO error handling
                      
					}
				 }
			
			else if (msg.hasResponse() == true && msg.getResponse().getResponseType() == TaskType.RESPONSEREADFILE && msg.getResponse().getReadResponse().hasChunk()){
				 System.out.println("OH i got response for read chunk data");
				 
				 System.out.println("Chunk Data : "+msg.getResponse().getReadResponse().getChunk().getChunkData().toStringUtf8());
				 handleChunkDataResponse(msg);	
			}
			else if (msg.hasResponse() == true && msg.getResponse().getResponseType()== TaskType.RESPONSEREADFILE){
				 System.out.println("OH i got response for read chunk location");
				handleChunkLocationResponse(msg);
			
		}
           

		} catch (Exception e) {
			// TODO add logging
		/**	Failure.Builder eb = Failure.newBuilder();
			eb.setId(conf.getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
			rb.setErr(eb);
			channel.write(rb.build());**/
		}

		System.out.flush();
		
	}


public  void handleChunkLocationResponse(CommandMessage message){
	System.out.println("Lets start finding where each chunk is");
	String filename = message.getResponse().getReadResponse().getFilename();
	long num_of_chunks=message.getResponse().getReadResponse().getNumOfChunks();
	 System.out.println("Num of chunks looping"+num_of_chunks);
	 chunk_buffer = new TreeMap<>();
    Map<Integer, List> hm = new HashMap<>();
	for(int i = 0; i<num_of_chunks; i++){
		ChunkLocation chunk_location= message.getResponse().getReadResponse().getChunkLocation(i);
		List nodelist = chunk_location.getNodeList();
		System.out.println("reading for chunk: "+chunk_location.getChunkid());
		hm.put(new Integer(chunk_location.getChunkid()),nodelist);
	}
	if(hm.size()==num_of_chunks){
		
		   Iterator it = hm.entrySet().iterator();
		    while (it.hasNext()) {
		
		        Map.Entry pair = (Map.Entry)it.next();
		        List list = (List) pair.getValue();
		        Node node = (Node) list.get(0);
		        int chunk_id=(int) pair.getKey();
		       
		  
		        readChunkData(filename,chunk_id ,node.getHost(), node.getPort());
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        it.remove(); // avoids a ConcurrentModificationException
		    }
	}
	
}
	
public void writeFile(File file){
	
	mc.writeFile(file, chunkSize);
}


public void readChunkData(String filename, int chunkId , String nodeHost , int nodePort){

	if(nodeHost != host || nodePort != port){
		host= nodeHost;
		port= nodePort;
		mc.readChunkData(filename, chunkId, nodeHost, nodePort);
	     
	}
	else{
		mc.readChunkData( filename,  chunkId ,  nodeHost ,  nodePort);
	}

}

public static void handleChunkDataResponse(CommandMessage message){
	System.out.println("File Writing starts");
	System.out.println("oh baby");
	System.out.println(message);
	
	String filename= message.getResponse().getReadResponse().getFilename();
	System.out.println(filename);
	chunk_buffer.put(message.getResponse().getReadResponse().getChunk().getChunkId(),message.getResponse().getReadResponse().getChunk().getChunkData());
    System.out.print("CHunkBufferSize : "+chunk_buffer.size());
    System.out.print("num_of_chunks : "+message.getResponse().getReadResponse().getNumOfChunks());
    if(chunk_buffer.size()==message.getResponse().getReadResponse().getNumOfChunks()){
    	File file = new File("/Users/seemarohilla/Desktop/275Gash/"+filename+"");
    	System.out.println("Creating");
		try {
			file.createNewFile();
		
		List<ByteString> byteString = new ArrayList<ByteString>();
		  FileOutputStream outputStream = new FileOutputStream(file);
			for(Integer i : chunk_buffer.keySet()){
				byteString.add(chunk_buffer.get(i));
			}
			ByteString bs = ByteString.copyFrom(byteString);
			outputStream.write(bs.toByteArray());
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	
    }
}

public void readChunkLocation(String filename){
	mc.readChunkLocation(filename);
}


	
	/**
	 * sample application (client) use of our messaging service
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
 
				String host ="127.0.0.1";
				int port =4068;
				
				//Client Side

				try{
				  String str= getLocalhostJedis().select(0);// uses default database 
				  
				String hostport =getLocalhostJedis().get("1");// use this to get leader for cluster from redis, 
			
				String[] words=hostport.split(":");//splits the string based on whitespace 
				host=words[0];
				System.out.println(hostport);
				port=Integer.parseInt(words[1]);
				  System.out.println("---Redis read---");

				}catch(Exception e){
				  System.out.println("---Problem with redis---");
				}    

		try {
		//	String clienthost="127.0.0.1";
			//int clientport=4030;
			MessageClient mc = new MessageClient(host, port);
			DemoApp da = new DemoApp(mc);

			// do stuff w/ the connection
		//	da.ping(2);
			File file = new File("/Users/seemarohilla/Desktop/App5.java");
			da.writeFile(file);
		//	da.readChunkLocation("App4.java");

			System.out.println("\n** exiting in 10 seconds. **");
			System.out.flush();
			//Thread.sleep(10 * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		//	CommConnection.getInstance().release();
		}
	
}
	
}