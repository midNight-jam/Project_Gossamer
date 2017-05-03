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
package gash.router.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.protobuf.ByteString;

import gash.router.logger.Logger;
import pipe.common.Common.Chunk;
import pipe.common.Common.Header;
import pipe.common.Common.Node;
import pipe.common.Common.ReadBody;
import pipe.common.Common.Request;
import pipe.common.Common.TaskType;
import pipe.common.Common.WriteBody;
import routing.Pipe.CommandMessage;

/**
 * front-end (proxy) to our service - functional-based
 * 
 * @author gash
 * 
 */
public class MessageClient {
	// track requests
	private long curID = 0;

	public MessageClient(String host, int port) {
		init(host, port);
	}

	private void init(String host, int port) {
		CommConnection.initConnection(host, port);
	}

	public void addListener(CommListener listener) {
		CommConnection.getInstance().addListener(listener);
	}
	
	public void ping(int destination) {
		// construct the message to send
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(11);
		hb.setTime(System.currentTimeMillis());
		hb.setMaxHops(10);
		hb.setDestination(destination);

		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setPing(true);
		


		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(rb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(CommandMessage msg) {
	
		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void release() {
		CommConnection.getInstance().release();
	}

	/**
	 * Since the service/server is asychronous we need a unique ID to associate
	 * our requests with the server's reply
	 * 
	 * @return
	 */
	private synchronized long nextId() {
		return ++curID;
	}
	
	
	public void readChunkData(String filename, int chunkId){
		System.out.println("Lets start reading each chunk");
		CommandMessage.Builder command = CommandMessage.newBuilder();
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTREADFILE);
		Node.Builder node = Node.newBuilder();
		node.setNodeId(11);
		node.setHost("127.0.0.1");
		node.setPort(4888);
		msg.setClient(node);
		
		
		Header.Builder header= Header.newBuilder();
		header.setNodeId(1);
		header.setTime(0);
		command.setHeader(header);
		
		ReadBody.Builder rrb  = ReadBody.newBuilder();
		rrb.setFilename(filename);
		rrb.setChunkId(chunkId);
		rrb.setChunkSize(100);
		msg.setRrb(rrb);
		
		command.setRequest(msg);
		
		
		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(command.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		

	}
	
	public void readChunkData(String filename, int chunkId,String nodeHost , int nodePort){
		System.out.println("Lets start reading each chunk");
		CommandMessage.Builder command = CommandMessage.newBuilder();
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTREADFILE);
		Node.Builder node = Node.newBuilder();
		node.setNodeId(11);
		node.setHost("127.0.0.1");
		node.setPort(4888);
		msg.setClient(node);
		
		
		Header.Builder header= Header.newBuilder();
		header.setNodeId(1);
		header.setTime(0);
		command.setHeader(header);
		
		ReadBody.Builder rrb  = ReadBody.newBuilder();
		rrb.setFilename(filename);
		rrb.setChunkId(chunkId);
		rrb.setChunkSize(100);
		msg.setRrb(rrb);
		
		command.setRequest(msg);
		init(nodeHost,nodePort);
		
		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(command.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		

	}
	
	
public static void writeFile(File f,int chunkSize){
		
		byte[] data;
		ByteString byteStringData;
		int partCounter = 0;
		int chunks;
		
		if(f.length() < chunkSize){
			data = new byte[(int) f.length()];
			chunks = 1;
		}
		else{
			data = new byte[chunkSize];
			chunks = (int) f.length();
		}
		
		Chunk.Builder chunk = Chunk.newBuilder();
		
		try(BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f))){
			while(chunks > 0){
				bis.read(data);
				byteStringData = ByteString.copyFrom(data);
				System.out.println(byteStringData.toStringUtf8());
				chunk.setChunkId(partCounter);
				chunk.setChunkSize(data.length);
				chunk.setChunkData(byteStringData);
				
				Logger.DEBUG(byteStringData.toStringUtf8());
				
				CommandMessage.Builder command = CommandMessage.newBuilder();
				Request.Builder msg = Request.newBuilder();
				msg.setRequestType(TaskType.REQUESTWRITEFILE);
				WriteBody.Builder rwb  = WriteBody.newBuilder();
				rwb.setFileExt(f.getName().substring(f.getName().lastIndexOf(".") + 1));
				rwb.setFilename(f.getName());
				rwb.setNumOfChunks(chunks--);
				Header.Builder header= Header.newBuilder();
				header.setNodeId(999); // 999 = Client
				header.setTime(System.currentTimeMillis());
				command.setHeader(header);
				
				rwb.setChunk(chunk);
				msg.setRwb(rwb);
				Node.Builder node = Node.newBuilder();
				node.setHost("192.168.1.10");
				node.setPort(4888);
				node.setNodeId(999);
				msg.setClient(node);
				command.setRequest(msg);
				partCounter++;
				
				
				try {
					// direct no queue
					// CommConnection.getInstance().write(rb.build());

					// using queue
					CommConnection.getInstance().enqueue(command.build());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
public void readChunkLocation(String filename){
	
	CommandMessage.Builder command = CommandMessage.newBuilder();
	Request.Builder msg = Request.newBuilder();
	msg.setRequestType(TaskType.REQUESTREADFILE);
	Node.Builder node = Node.newBuilder();
	node.setNodeId(-1);
	node.setHost("192.168.1.10");
	node.setPort(4888);
	msg.setClient(node);
	
	
	Header.Builder header= Header.newBuilder();
	header.setNodeId(1);
	header.setTime(0);
	command.setHeader(header);
	
	ReadBody.Builder rrb  = ReadBody.newBuilder();
	rrb.setFilename(filename);
	msg.setRrb(rrb);
	
	command.setRequest(msg);

	try {
		// direct no queue
		// CommConnection.getInstance().write(rb.build());

		// using queue
		CommConnection.getInstance().enqueue(command.build());
	} catch (Exception e) {
		e.printStackTrace();
	}
	
}
	
}
