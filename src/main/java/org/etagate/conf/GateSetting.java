/**
 * 
 */
package org.etagate.conf;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.etagate.app.App;
import org.etagate.app.AppContain;
import org.etagate.app.DevModeSupport;
import org.etagate.app.node.NodeStragegy;
import org.etagate.helper.S;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年5月27日
 */
public class GateSetting {

	public static final String APP_TAG = "app";
	public static final String NODE_TAG = "node";
	public static final String INCLUDE_TAG = "include";
	public static final String AUTH_TAG = "auth";
	public static final String EXCLUDE_TAG = "exclude";
	public static final String PROPERTY_TAG = "property";
	
	public static final String ResourceType = "*.bmp,*.gif,*.jpg,*.png,*.woff,*.woff2,*.css,*.js,*.ico";

	public static final Logger log = LoggerFactory.getLogger(GateSetting.class);

	private JsonObject properties=new JsonObject();
	private JsonObject authSetting = new JsonObject();
	private boolean hasAuth=true;
//	private AppContain appContain =null;
	
	private List<Element> app_tags=null;
	private String inside_address =null;

	
			
	public void parse(URL is) {
		SAXReader saxReader = new SAXReader();

		try {
			Document document = saxReader.read(is);
			Element root = document.getRootElement();
			
			collectProperty(root);
			parseAuth(root);
			parseApp(root);
//			
//			System.out.println("====================parse ok=============");
//			System.out.println(properties.toString());
//			System.out.println(authSetting.toString());
		} catch (DocumentException e) {
			log.error("Load route.xml error.");
			e.printStackTrace();
		}

	}
	
	@SuppressWarnings("unchecked")
	private void collectProperty(Element root){
		List<Element> nodes = root.elements(PROPERTY_TAG);
		if(nodes==null)
			return;
		nodes.forEach(e->{
			String name = e.attributeValue("name");
			if(S.isBlank(name))
				return;			
			String v = e.getStringValue();
			properties.put(name.trim(), v.trim());
		});
		
		String insidehost = properties.getString("inside.host");
		String insideport = properties.getString("inside.port");
		if(S.isNotBlank(insidehost) && S.isNotBlank(insideport))
			this.inside_address =insidehost+":"+insideport;
		
	}
	
	
	@SuppressWarnings("unchecked")
	private void parseAuth(Element root){
		Element auth = root.element(AUTH_TAG);
		if(auth==null){
			hasAuth=false;
			return;
		}
		
		authSetting.put("app", auth.attributeValue("app"));
		authSetting.put("authentication", auth.attributeValue("authentication"));
		authSetting.put("authorisation", auth.attributeValue("authorisation"));
		authSetting.put("successfield", auth.attributeValue("successfield"));
		authSetting.put("mainpage", auth.attributeValue("mainpage"));
		authSetting.put("loginpage", auth.attributeValue("loginpage"));
		
		List<Element> exclude = auth.elements(EXCLUDE_TAG);
		if(exclude==null)
			return;
		Set<String> end =new HashSet<>();
		Set<String> start = new HashSet<>();
		addExcludes(end, ResourceType);
		exclude.forEach(e ->{
			String ends = e.attributeValue("end");
			String starts = e.attributeValue("start");
			addExcludes(end, ends);
			addExcludes(start, starts);
		});
		authSetting.put("exclude.end", S.join(end, ","));
		authSetting.put("exclude.start", S.join(start, ","));
		
	}
	
	private void addExcludes(Set<String> set, String def){
		if(S.isBlank(def))
			return;
		String[] ex = def.split(",");
		set.addAll(Arrays.asList(ex));		
	}
	
	@SuppressWarnings("unchecked")
	private void parseApp(Element root){
		
		this.app_tags = root.elements(APP_TAG);
		
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	private void buildAppObject(Vertx vertx,WebClient webclient,AppContain appinfo, Element appNode) {

		String name = appNode.attributeValue("name");

		if (S.isBlank(name))
			return;

		
		
		boolean cutname = "true".equalsIgnoreCase(appNode.attributeValue("cutContextPath")) ? true : false;
		String timeout = appNode.attributeValue("timeout");
		String dev = appNode.attributeValue("dev");
		String circuit_maxfail = appNode.attributeValue("maxfail");
		String circuit_reset = appNode.attributeValue("resetsecond");
		String depend = appNode.attributeValue("depend");

//		System.out.println(circuit_maxfail+"  "+circuit_reset);
		
		List<Element> nodes = appNode.elements(NODE_TAG);
		List<Element> include = appNode.elements(INCLUDE_TAG);
		
		if(nodes==null || nodes.isEmpty())
			throw new NullPointerException("Tag <app> must contains at least one <node> tag. \r\n"+appNode.asXML());

		NodeStragegy ns = createNodeStrategy(appNode);
		DevModeSupport devmode = new DevModeSupport();
		
		App a = new App(vertx,webclient,name,ns,appinfo);
		a.setCutAppName(cutname);
		if (S.isNotBlank(timeout))
			a.setTimeout(Integer.parseInt(timeout));
		if("true".equals(dev))
			a.setDevmode(devmode);
		if(S.isNotBlank(circuit_maxfail))
			a.setCircuitMaxfail(Integer.parseInt(circuit_maxfail));
		if(S.isNotBlank(circuit_reset))
			a.setCircuitReset(Long.parseLong(circuit_reset)*1000);
		if(this.inside_address!=null)
			a.setInside_address(this.inside_address);
		if(S.isNotBlank(depend))
			a.setDepends(depend);

		if (nodes != null && !nodes.isEmpty()) {
			nodes.forEach(n -> {
				String host = n.attributeValue("host");
				String port = n.attributeValue("port");
				String weight = n.attributeValue("weight");				
				if(S.isBlank(weight))
					a.addNode(vertx,host, Integer.parseInt(port),1);
				else
					a.addNode(vertx,host, Integer.parseInt(port), Integer.parseInt(weight));
			});
		}

		if (include != null && !include.isEmpty()) {
			include.forEach(i -> {
				a.addRoutePath(i.attributeValue("path"));
			});
		}

		appinfo.addAppObject(a);

		log.info("[{}] route path /{} for [{}]" ,vertx.getOrCreateContext().deploymentID(), name, name);

	}

	private NodeStragegy createNodeStrategy(Element appNode) {
		String nodestrategy = appNode.attributeValue("balanceStrategy");
		NodeStragegy ns =null;
		if(S.isNotBlank(nodestrategy)){
			try {
				ns = (NodeStragegy) Class.forName(nodestrategy).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				log.error("error in attribute: balanceStrategy = "+nodestrategy, e);
				throw new RuntimeException(e);
			}
		}
		return ns;
	}
	
	public void eachProperties(Consumer<Entry<String,Object>> action){
		this.properties.forEach(action);
	}

	public boolean hasAuth() {
		return hasAuth;
	}

	public AppContain getAppContain(Vertx vertx,WebClient webclient) {
		// System.out.println("====================here=========="+l.size()+"
		// "+el.toString());
		AppContain appContain = new AppContain();
		
		this.app_tags.forEach(app -> {
			// System.out.println(app.toString());
			buildAppObject(vertx,webclient,appContain, app);
		});
		
		return appContain;
	}

	public JsonObject getAuthSetting() {
		return authSetting;
	}

}
