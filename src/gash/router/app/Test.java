package gash.router.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import pipe.common.Common.ChunkLocation;
import pipe.common.Common.Node;

public class Test {

	public static void main(String[] args) {
		Map<Integer,Map<String,Integer>> nodemap1 = new HashMap<>();
		 Map<String, Integer> map1 = new HashMap<>();
		 map1.put("192.168.10.12", 4568);
		 Map<String, Integer> map2 = new HashMap<>();
		 map2.put("192.168.10.12", 4570);
		 Map<String, Integer> map3 = new HashMap<>();
		 map3.put("192.168.10.10", 4168);
		 Map<String, Integer> map4 = new HashMap<>();
		 map4.put("192.168.10.11", 4268);
		 nodemap1.put(1,map1);
		 nodemap1.put(2,map2);
		 nodemap1.put(3,map3);
		 nodemap1.put(4,map4);
		 Random rand = new Random();
			//	int  key = rand.nextInt(nodemap.size()) + 1;
				int  key = rand.nextInt(4) + 1;
				Map map= (Map) nodemap1.get(key);
				
				
				String host="";
				Set<String> set = map.keySet();
				for(String key1:set){
					//System.out.print("Some Keys");
					 host=key1;
				}
				System.out.println("HOst :"+host);
			//	node.setHost(host);
				//node.setHost("localhost");
				//System.out.print(" set host to : "+host);
				
				Integer port1=(Integer) map.get(host);
				int port= port1.intValue();
				System.out.println("Port :"+port);
				
	}
}
