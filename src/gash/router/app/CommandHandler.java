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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import gash.router.container.RoutingConf;
import gash.router.server.raft.NodeState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Failure;
import pipe.common.Common.Response.Status;
import pipe.common.Common.TaskType;
import routing.Pipe.CommandMessage;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	protected RoutingConf conf;

	public CommandHandler(RoutingConf conf) {
		if (conf != null) {
			this.conf = conf;
		}
	}

	/**
	 * override this method to provide processing behavior. This implementation
	 * mimics the routing we see in annotating classes to support a RESTful-like
	 * behavior (e.g., jax-rs).
	 * 
	 * @param msg
	 */
	public void handleMessage(CommandMessage msg, Channel channel) {
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}
       // ServerApp.propagateMessage(msg);
	//	PrintUtil.printCommand(msg);
         
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
				 DemoApp.handleChunkDataResponse(msg);	
			}
			else if (msg.hasResponse() == true && msg.getResponse().getResponseType()== TaskType.RESPONSEREADFILE){
				 System.out.println("OH i got response for read chunk location");
				WriteClient.handleChunkLocationResponse(msg);
			
		}
           

		} catch (Exception e) {
			// TODO add logging
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(conf.getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
			rb.setErr(eb);
			channel.write(rb.build());
		}

		System.out.flush();
	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

}