/**
 * 
 */
package org.etagate.conf;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.etagate.app.AppInfo;
import org.etagate.app.AppObject;
import org.etagate.app.NodeStragegy;
import org.etagate.helper.S;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年5月27日
 */
public class GateSetting {

	public static final String APP_TAG = "app";
	public static final String NODE_TAG = "node";
	public static final String INCLUDE_TAG = "include";
	public static final String AUTH_TAG = "auth";
	public static final String EXCLUDE_TAG = "exclude";
	public static final String PROPERTY_TAG = "property";
	
	public static final String ResType = "*.bmp,*.gif,*.jpg,*.png,*.woff,*.woff2,*.css,*.js,*.ico";

	public static final Logger log = LoggerFactory.getLogger(GateSetting.class);

	public static JsonObject properties=new JsonObject();
	public static JsonObject authSetting = new JsonObject();
	public static boolean hasAuth=true;
	public static AppInfo appInfo =null;
	
	
	public static void parse(URL is) {
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
	private static void collectProperty(Element root){
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
	}
	
	
	public static boolean hasAuth(){
		return hasAuth;
	}
	
	@SuppressWarnings("unchecked")
	private static void parseAuth(Element root){
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
		
		List<Element> exclude = auth.elements(EXCLUDE_TAG);
		if(exclude==null)
			return;
		Set<String> end =new HashSet<>();
		Set<String> start = new HashSet<>();
		addExcludes(end, ResType);
		exclude.forEach(e ->{
			String ends = e.attributeValue("end");
			String starts = e.attributeValue("start");
			addExcludes(end, ends);
			addExcludes(start, starts);
		});
		authSetting.put("exclude.end", S.join(end, ","));
		authSetting.put("exclude.start", S.join(start, ","));
		
	}
	
	private static void addExcludes(Set<String> set, String def){
		if(S.isBlank(def))
			return;
		String[] ex = def.split(",");
		set.addAll(Arrays.asList(ex));		
	}
	
	@SuppressWarnings("unchecked")
	private static void parseApp(Element root){
		
		List<Element> l = root.elements(APP_TAG);
		// System.out.println("====================here=========="+l.size()+"
		// "+el.toString());
		appInfo = new AppInfo();
		
		l.forEach(app -> {
			// System.out.println(app.toString());
			buildAppObject(appInfo, app);
		});
		
	}
	
	
	@SuppressWarnings("unchecked")
	private static void buildAppObject(AppInfo appinfo, Element appNode) {

		String name = appNode.attributeValue("name");

		if (S.isBlank(name))
			return;

		
		
		boolean cutname = "true".equalsIgnoreCase(appNode.attributeValue("cutContextPath")) ? true : false;
		String timeout = appNode.attributeValue("timeout");

		List<Element> nodes = appNode.elements(NODE_TAG);
		List<Element> include = appNode.elements(INCLUDE_TAG);

		NodeStragegy ns = createNodeStrategy(appNode);
		
		AppObject a = new AppObject(name,ns);
		a.setCutAppName(cutname);
		if (S.isNotBlank(timeout))
			a.setTimeout(Long.parseLong(timeout));

		if (nodes != null && !nodes.isEmpty()) {
			nodes.forEach(n -> {
				String host = n.attributeValue("host");
				String port = n.attributeValue("port");
				String weight = n.attributeValue("weight");
				if(S.isBlank(weight))
					a.addNode(host, Integer.parseInt(port),1);
				else
					a.addNode(host, Integer.parseInt(port), Integer.parseInt(weight));
			});
		}

		if (include != null && !include.isEmpty()) {
			include.forEach(i -> {
				a.addRoutePath(i.attributeValue("path"));
			});
		}

		appinfo.addAppObject(a);

		log.info("add app route " + name);

	}

	private static NodeStragegy createNodeStrategy(Element appNode) {
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

}
