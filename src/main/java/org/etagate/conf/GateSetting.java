/**
 * 
 */
package org.etagate.conf;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.etagate.app.AppInfo;
import org.etagate.app.AppObject;
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
	public static final String PROPERTY_TAG = "exclude";

	public static final Logger log = LoggerFactory.getLogger(GateSetting.class);

	public static JsonObject properties=new JsonObject();
	public static JsonObject authSetting = new JsonObject();
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
		List<Element> nodes = root.elements("property");
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
	
	
	@SuppressWarnings("unchecked")
	private static void parseAuth(Element root){
		Element auth = root.element("auth");
		if(auth==null)
			return;
		
		authSetting.put("app", auth.attributeValue("app"));
		authSetting.put("authentication", auth.attributeValue("authentication"));
		authSetting.put("authorisation", auth.attributeValue("authorisation"));
		authSetting.put("successfield", auth.attributeValue("successfield"));
		
		List<Element> exclude = auth.elements("exclude");
		if(exclude==null)
			return;
		Set<String> end =new HashSet<>();
		Set<String> start = new HashSet<>();
		exclude.forEach(e ->{
			String ends = e.attributeValue("end");
			String starts = e.attributeValue("start");
			if(S.isNotBlank(ends))
				end.add(ends);
			if(S.isNotBlank(starts))
				start.add(starts);
		});
		authSetting.put("exclude.end", S.join(end, ","));
		authSetting.put("exclude.start", S.join(start, ","));
		
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

		List<Element> nodes = appNode.elements("node");
		List<Element> include = appNode.elements("include");

		AppObject a = new AppObject(name);
		a.setCutAppName(cutname);
		if (S.isNotBlank(timeout))
			a.setTimeout(Long.parseLong(timeout));

		if (nodes != null && !nodes.isEmpty()) {
			nodes.forEach(n -> {
				String host = n.attributeValue("host");
				String port = n.attributeValue("port");
				a.addNode(host, Integer.parseInt(port));
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

}
