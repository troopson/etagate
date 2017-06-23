package org.etagate.app.node;

import java.util.List;
import java.util.Optional;

import io.vertx.core.http.HttpServerRequest;

public interface NodeStragegy {
	
	public void addNode(Node node);
	
	public void delNode(Node node);
		
	public Node getNode(Optional<HttpServerRequest> clientRequest);
		
	public List<Node> nodes();
	
	public int size();
	
	
	
			
}
