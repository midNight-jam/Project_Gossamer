# ProjectGossamer

<b>Objective:</b> To build a fault tolerant and scalable system capable of storing, finding and doing stuff in a large, decentralized, network of servers.
<br/><br/>
<b>Technology Stack :</b><br/>
Languages used : Java, and Python<br/>
Core Packages: Protobuf , Netty. No MQ-tech used<br/>
Storage: Mysql and Redis<br/>
Challenges: Leader election (Raft), balance through stealing, discovery<br/></br>

<b>RAFT :</b>
Consensus algorithms allow a collection of machines to work as a coherent group that can survive the failures of some of its members. Because of this, they play a key role in building reliable large-scale software systems.
Raft is a consensus algorithm for managing a replicated log. Raft separates the key elements of consensus, such as leader election, log replication, and safety, and it enforces a stronger degree of coherency to reduce the number of states that must be considered. Raft also includes a new mechanism for changing the cluster membership, which uses overlapping majorities to guarantee safety.
 A Raft cluster contains several servers; five is a typical number, which allows the system to tolerate two failures. At any given time, each server is in one of three states: leader, follower, or candidate. In normal operation there is exactly one leader and all of the other servers are followers. Followers are passive: they issue no requests on their own but simply respond to requests from leaders and candidates. The leader handles all client requests (if a client contacts a follower, the follower redirects it to the leader). The third state, candidate, is used to elect a new leader. Figure 1 shows the states and their transitions.


<b>Raft implementation in Project:</b>
Used for: Maintaining consensus in 100 % Replicated System using Leader Election and Log Replication. </br></br>

<b>States:</b> The 3 states, Leader, follower and candidate state of a server implemented using State Design pattern where an object can change from one state to another depending on the stimulus. Every node starts as a follower. After a certain timeout it becomes a candidate and asks for votes. The node with maximum votes wins the election and becomes the leader, all other nodes become followers. </br></br>

<b>Heartbeat:</b> Leader sends a heartbeat messages to all the nodes in its edge list. On receiving the heartbeat, the nodes get updated with information of current leader, resets its election timeout and sends a response. In response they send the latest timestamp on their records. In response to this message leader sends the append entries if the follower’s data is stale.</br></br>

<b>Write Requests:</b> All the writes of the file go through the leader. The leader appends the data in its database and send append entries to all the follower. On Receiving the append entries packet, the followers save the data into their database and respond with success or failure message.</br></br>

<b>Read Requests:</b> The first read request goes to the leader. The leader responds with the location of each chunk of the file. Client then connects to each node to fetch the actual chunk data. </br></br>

<b>Global Ping:</b> A global ping message issued by the client is handled by the leader. The leader checks if the current cluster is the target of the ping. If not, then it will propagate the ping message to the next cluster. Leader will also send this ping internal to the cluster to all the followers node. For getting the Host and Ip of next cluster, Leader connects to the global redis database storing the information for each cluster.</br></br>

<b>Persistence of data:</b> The data is stored on MySQL database. Each node has its own database and 100 % replication of data on each server. Making the cluster highly available and fault tolerant.</br></br>






<b>Queue Server : </b></br>
Queue Server is like a the point of entrance for the read and write requests and also add node requests. The reason why we have named it as Queue Server because it holds two queues inside. Leader Queue and Non Leader Queue namely, these queues are used by the cluster nodes for work stealing. The sole reason of having these different queues is because in our cluster there are certain commands that has to be handled by the leader only. Thus, by segregating the work in different queues we are guaranteeing that each command message will be handled correctly depending on the message type. The following are the responsibility of the Queue Server : </br></br>
<b>Write File :</b> Ensures that all the chunks corresponding to the file has arrived correctly before sending the success acknowledgement to the client. And if the chunks are missing it has to send a response asking for resending of the missing chunks.  The amount of time Queue Server waits depends on the number of chunks and chunk size which varies for every file.</br></br>

<b>Enqueuing Write File Request to Leader Queue:</b> The purpose of leader queue is to hold any command messages that has to be handled only by Leader of the cluster. Thus, when the write file request arrives it is enqueued in the Leader Queue so that only leader can request this write file command message. Later when the Leader writes the file to its database it instructs to all of its followers to replicate the data by forwarding the write file request. </br></br>

<b>Enabling Work Stealing :</b> When the read file requests arrive they are enqueued in the Non Leader Queue. Any node be it Leader or Follower sends a work steal request to the Queue Server, and if it detects that the Non Leader Queue is not empty it polls the Non Leader Queue and assigns it to the earliest arriving work steal request. In this way read File request can be stolen by any available node  in the cluster and served by it.</br></br>

<b>Node Discovery:</b>
We are using cold start for our nodes in the cluster. When a node comes up, it sends a broadcast packet into the local LAN in which it is connected. </br></br>

<b>“Add_Node_Request” broadcast packet:</b> We have used DatagramPacket from java.net package to send the “Add_Node_Request”. The newly up node constructs this packet and send it on the broadcast address that is “255.255.255.255” using port 8888. This packet now floats in the local LAN and waits for a response to this broadcast.</br></br>

<b>Discovery Server:</b> When the Queue Server starts it spawns a small Discovery Server. This Discovery Server is responsible for listening any broadcast packet that is arriving at the port 8888. When the “Add_Node_Request” broadcast packet is received at the Discovery Server, it establishes a protobuf channel back to the “Add_Node_Request” sender using its address and command port that are found inside the data of the broadcast packet. After establishing the protobuf channel it uses the Queue Server’s Edge List to get all the outbound edges, all these edges are sent in the response for the newly up node to get fully connected with the rest of the cluster. </br></br>
