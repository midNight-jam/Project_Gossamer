package gash.router.app;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.protobuf.ByteString;

import gash.router.client.CommInit;
import gash.router.logger.Logger;
import gash.router.server.ServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Chunk;
import pipe.common.Common.ChunkLocation;
import pipe.common.Common.Header;
import pipe.common.Common.Node;
import pipe.common.Common.ReadBody;
import pipe.common.Common.Request;
import pipe.common.Common.TaskType;
import pipe.common.Common.WriteBody;
import pipe.work.Work.WorkMessage;
import redis.clients.jedis.Jedis;
import routing.Pipe.CommandMessage;


public class WriteClient {
	
	static String host;
	static int port;
	static ChannelFuture channel;
	static int chunk_buffer_size;
	
	 static EventLoopGroup group;
	 static final int chunkSize = 1024*1024; // MAX BLOB STORAGE = Math,pow(2,15) -1 = 65535 Bytes 
		
	// private static Jedis localhostJedis=new Jedis("169.254.214.175",6379); //during demo day IP will change
	 private static Jedis localhostJedis=new Jedis("127.0.0.1",6379); //during demo day IP will change
	 public static Jedis getLocalhostJedis() {
	 	return localhostJedis;
	 }

	
	public static void init(String host_received, int port_received)
	{
		host = host_received;
		port = port_received;
		group = new NioEventLoopGroup();
		try {
			CommInit si = new CommInit(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);


			// Make the connection attempt.
			 channel = b.connect(host, port).syncUninterruptibly();

			
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
/**
public static void writeFile(File f){
		
		byte[] data = new byte[chunkSize];
		//long fileSize = f.length();
		int partCounter = 0;
		int incompleteChunkData = (int) (f.length() % chunkSize);
		int completeChunks = (int) (f.length() / chunkSize);
		
		int chunks = completeChunks + 1;
		Chunk.Builder chunk = Chunk.newBuilder();
		
		try (BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(f))){
			String name = f.getName();
			ByteString byteStringData;
			while(completeChunks-- > 0){
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
				rwb.setNumOfChunks(chunks);
				Header.Builder header= Header.newBuilder();
				header.setNodeId(999); // 999 = Client
				header.setTime(System.currentTimeMillis());
				command.setHeader(header);
				
				rwb.setChunk(chunk);
				msg.setRwb(rwb);
				Node.Builder node = Node.newBuilder();
				node.setHost("localhost");
				node.setPort(4888);
				node.setNodeId(999);
				msg.setClient(node);
				command.setRequest(msg);
				partCounter++;
				CommandMessage commandMessage = command.build();
				
				channel.channel().writeAndFlush(commandMessage);
				
				if (channel.isDone() && channel.isSuccess()) {
					System.out.println("Msg sent succesfully:");
				}
			}
			data = new byte[incompleteChunkData];
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
			rwb.setNumOfChunks(chunks);
			Header.Builder header= Header.newBuilder();
			header.setNodeId(999); // 999 = Client
			header.setTime(System.currentTimeMillis());
			command.setHeader(header);
			
			rwb.setChunk(chunk);
			msg.setRwb(rwb);
			Node.Builder node = Node.newBuilder();
			node.setHost("localhost");
			node.setPort(4888);
			node.setNodeId(999);
			msg.setClient(node);
			command.setRequest(msg);
			partCounter++;
			CommandMessage commandMessage = command.build();
			
			channel.channel().writeAndFlush(commandMessage);
			
			if (channel.isDone() && channel.isSuccess()) {
				System.out.println("Msg sent succesfully:");
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}
**/
public static void writeFile(File f){
		
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
				node.setHost("192.168.10.11");
				node.setPort(4888);
				node.setNodeId(999);
				msg.setClient(node);
				command.setRequest(msg);
				partCounter++;
				CommandMessage commandMessage = command.build();
				
				channel.channel().writeAndFlush(commandMessage);
				
				if (channel.isDone() && channel.isSuccess()) {
					System.out.println("Msg sent succesfully:");
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

	
	public static void ReadChunkData(String filename, int chunkId , String nodeHost , int nodePort){
		System.out.println("Lets start reading each chunk");
		CommandMessage.Builder command = CommandMessage.newBuilder();
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTREADFILE);
		Node.Builder node = Node.newBuilder();
		node.setNodeId(-1);
		node.setHost("192.168.10.11");
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
		CommandMessage commandMessage = command.build();
		if(nodeHost != host || nodePort != port){
			host= nodeHost;
			port= nodePort;
		WriteClient.init(host, port);
		}
		channel.channel().writeAndFlush(commandMessage);
		
		if (channel.isDone() && channel.isSuccess()) {
			System.out.println("Msg sent succesfully:");
		}

	}
	
	public static void ReadChunkLocation(String filename){
		
		CommandMessage.Builder command = CommandMessage.newBuilder();
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTREADFILE);
		Node.Builder node = Node.newBuilder();
		node.setNodeId(-1);
		node.setHost("192.168.10.11");
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
		CommandMessage commandMessage = command.build();
		
		channel.channel().writeAndFlush(commandMessage);
		
		if (channel.isDone() && channel.isSuccess()) {
			System.out.println("Msg sent succesfully:");
		}

	}
	
	private class ReadNode{
		public ReadNode(String host, int port, long node_id) {
		
			this.host = host;
			this.port = port;
			this.node_id = node_id;
		}
		String host;
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public long getNode_id() {
			return node_id;
		}
		public void setNode_id(long node_id) {
			this.node_id = node_id;
		}
		int port;
		long node_id;	
	}
	
static Map<Integer,ByteString> chunk_buffer;

public static void handleChunkLocationResponse(CommandMessage message){
	System.out.println("Lets start finding where each chunk is");
	String filename = message.getResponse().getReadResponse().getFilename();
	long num_of_chunks=message.getResponse().getReadResponse().getNumOfChunks();
	 System.out.println("Num of chunks looping"+num_of_chunks);
	 chunk_buffer_size=(int) num_of_chunks;
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
		       
		  
		        ReadChunkData(filename,chunk_id ,node.getHost(), node.getPort());
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        it.remove(); // avoids a ConcurrentModificationException
		    }
	}
	
}

	
public static void ReadFile(String filename){
		
		CommandMessage.Builder command = CommandMessage.newBuilder();
		Request.Builder msg = Request.newBuilder();
		msg.setRequestType(TaskType.REQUESTREADFILE);
		Node.Builder node = Node.newBuilder();
		node.setNodeId(-1);
		node.setHost("192.168.10.11");
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
		CommandMessage commandMessage = command.build();
		
		channel.channel().writeAndFlush(commandMessage);
		
		if (channel.isDone() && channel.isSuccess()) {
			System.out.println("Msg sent succesfully:");
		}

	}

public static void handleChunkDataResponse(CommandMessage message){
	System.out.println("File Writing starts");
	String filename= message.getResponse().getReadResponse().getFilename();
	
	chunk_buffer.put(message.getResponse().getReadResponse().getChunk().getChunkId(),message.getResponse().getReadResponse().getChunk().getChunkData());
    System.out.print("CHunkBufferSize : "+chunk_buffer.size());
/**	if(chunk_buffer.size()==chunk_buffer_size){
		System.out.println("Actual PDF :"+ chunk_buffer.toString());
				//write to file
		for(Integer i : chunk_buffer.keySet()){
		//	List<String> lines = Arrays.asList(chunk_buffer.get(i).toStringUtf8());
			try {
			//Files.write(file, lines, Charset.forName("UTF-8"));
			
			File file = new File("/Users/seemarohilla/Desktop/275Gash/"+filename+"");
			 if (file.createNewFile()){
			        System.out.println("File is created!");
			      }else{
			        System.out.println("File already exists.");
			      }
			Path filepath = Paths.get("/Users/seemarohilla/Desktop/275Gash/"+filename+"");
			
			//	Files.write(filepath, lines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
//			Files.write(filepath, lines, Charset.forName("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
	}
	**/
	
    if(chunk_buffer.size()==chunk_buffer_size){
    	File file = new File("/Users/seemarohilla/Desktop/275Gash/"+filename+"");
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




public static void handleWriteResponse(List missingChunkList){
	List<Integer> list = missingChunkList;
	for(Integer i : list){
	System.out.print("Chunks to be sent :"+i.intValue());
	}
	//TODO handle 
}

public static void ping(){
	

	CommandMessage.Builder command = CommandMessage.newBuilder();
	Boolean ping=true;
	command.setPing(ping);
	
	Header.Builder header= Header.newBuilder();
	header.setNodeId(1);
	header.setTime(0);
	header.setMaxHops(10);
	header.setDestination(2);
	command.setHeader(header);
	
	
	CommandMessage commandMessage = command.build();
	
	channel.channel().writeAndFlush(commandMessage);
	
	if (channel.isDone() && channel.isSuccess()) {
		System.out.println("Msg sent succesfully:");
	}
	
}

	public static void main(String[] args) {
		//String host = "127.0.0.1";
		//int port = 4068;
		String host ="169.254.217.125";
		int port =4068;
		
		
		
		
		//Client Side

		try{
		  String str= getLocalhostJedis().select(0);// uses default database 
		  
		String hostport =getLocalhostJedis().get("2");// use this to get leader for cluster from redis, 
		  //should be implementd on client
		String[] words=hostport.split(":");//splits the string based on whitespace 
		host=words[0];
		System.out.println(hostport);
		port=Integer.parseInt(words[1]);
		  System.out.println("---Redis read---");

		}catch(Exception e){
		  System.out.println("---Problem with redis---");
		}    
	
		System.out.println("Sent the message");
		if(channel==null){
		WriteClient.init(host, port);}
		File file = new File("/Users/seemarohilla/Desktop/App.java");
	//	WriteClient.writeFile(file);
		WriteClient.ReadChunkLocation("App.java");
		WriteClient.ping();
	
		while(true){
			
		}
		// TODO Auto-generated method stub
		
	}

}