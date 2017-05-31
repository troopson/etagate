package org.etagate.app;

import java.util.List;

import org.etagate.app.AppObject.Node;

public interface NodeStragegy {
	
	public void addNode(Node node);
	
	public void delNode(Node node);
		
	public Node getNode();
	
	public List<Node> nodes();
}
