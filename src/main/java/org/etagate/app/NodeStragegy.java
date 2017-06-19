package org.etagate.app;

import java.util.List;
import java.util.Optional;

import org.etagate.app.AppObject.Node;

import io.vertx.core.http.HttpServerRequest;

public interface NodeStragegy {
	
	public void addNode(Node node);
	
	public void delNode(Node node);
		
	public Node getNode(Optional<HttpServerRequest> clientRequest);
	
	public List<Node> nodes();
	
	
}
