/**
 * 
 */
package org.etagate.app;

import java.util.HashMap;
import java.util.Map;

import org.etagate.helper.S;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年6月19日
 */
public class DevModeSupport {

	private static final Logger log = LoggerFactory.getLogger(DevModeSupport.class);
	
	public static Map<String,AppObject.Node> devMode = new HashMap<>(); 
	
	public AppObject.Node getDevNode(AppObject app,HttpServerRequest clientRequest){
		String hostport = clientRequest.getParam(app.name);
		String remoteip = clientRequest.remoteAddress().host();
		if(S.isNotBlank(hostport)){
			if("clear".equals(hostport)){
				devMode.remove(remoteip);
				log.debug("clear node of remote ip:"+remoteip);
				return null;
			}else{
				String[] ipp = hostport.split(":");
				AppObject.Node n = app.new Node(ipp[0], Integer.parseInt(ipp[1]),1);
				devMode.put(remoteip, n);
				log.debug("remote ip visit setting:"+remoteip+" ["+app.name+"->"+hostport+"]");
				return n;
			}
		}else{
			return devMode.get(remoteip);
		}
	}
	
}
