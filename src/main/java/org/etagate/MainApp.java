/**
 * 
 */
package org.etagate;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.etagate.conf.GateSetting;
import org.etagate.helper.Args;
import org.etagate.helper.S;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年3月15日
 * 
 * 
 * 
 */
public class MainApp extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(MainApp.class);
	
	public void start() throws Exception {
		
		String[] args =vertx.getOrCreateContext().processArgs().toArray(new String[]{});			
		Args argsObj =new Args(args);
		
		URL u = this.findgfile(argsObj);				
		
		vertx.<GateSetting>executeBlocking(v->{
			GateSetting gs = new GateSetting();
			gs.parse(u);
			v.complete(gs);
		}, r->{
			if(r.succeeded()){
				GateSetting gs = r.result();
				JsonObject conf = config();
				gs.eachProperties(e->{
					conf.put(e.getKey(), e.getValue());
				});
				
				int instance = Integer.parseInt(conf.getString("server.instance","1"));
				
				DeploymentOptions voptions = new DeploymentOptions();				
				voptions.setConfig(conf);
				for(int i=0;i<instance;i++){
					OutServerVerticle gv = new OutServerVerticle();
					gv.setGsetting(gs);
				    vertx.deployVerticle(gv, voptions);
				}	
				
				InsideServerVerticle inside = new InsideServerVerticle();
				inside.setGsetting(gs);
				vertx.deployVerticle(inside,voptions);
				
			}else{
				r.cause().printStackTrace();	
				log.error("init failed.");
				System.exit(-1);
			}
		});
		
				
	}
	
	
	private URL findgfile(Args argsObj) throws MalformedURLException{
		
		JsonObject conf = config();
		String routeConf = conf.getString("gfile");
		URL u = null;
		
		if(S.isBlank(routeConf))			
			routeConf = argsObj.get("gfile");
		
		
		if(S.isBlank(routeConf)){
			u = MainApp.class.getResource("gfile");
		}else{
			if(routeConf.indexOf("://")>0)
				u = new URL(routeConf);
			else{
				File f = new File(routeConf);
				u = f.toURI().toURL();
			}
		}
				
		log.info("load gate setting from "+u.toString());
		
		return u;
	}
	


}
