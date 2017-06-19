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
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年3月15日
 * 
 * 
 * 
 */
public class MainApp extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(MainApp.class);
	
	public void start() throws Exception {
	
		URL u = this.findgfile();
		
		vertx.executeBlocking(v->{
			GateSetting.parse(u);
			v.complete();
		}, r->{
			if(r.succeeded()){
				JsonObject conf = config();
				GateSetting.properties.forEach(e->{
					conf.put(e.getKey(), e.getValue());
				});
				
				int instance = Integer.parseInt(conf.getString("server.instance","1"));
				
				DeploymentOptions voptions = new DeploymentOptions().setInstances(instance);				
				voptions.setConfig(conf);
				vertx.deployVerticle(GateVerticle.class.getName(), voptions);
				
				
			}else{
				r.cause().printStackTrace();	
				log.error("init failed.");
				System.exit(-1);
			}
		});
		
				
	}
	
	
	private URL findgfile() throws MalformedURLException{
		
		JsonObject conf = config();
		String routeConf = conf.getString("gfile");
		URL u = null;
		
		if(S.isBlank(routeConf)){
			String[] args =vertx.getOrCreateContext().processArgs().toArray(new String[]{});			
			Args argsObj =new Args(args);
			routeConf = argsObj.get("gfile");
		}
		
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
	
//	
//	  @Before
//	    public void setUp(TestContext context) {
//	        VertxOptions options = new VertxOptions();
//	        options.setBlockedThreadCheckInterval(1000*60*60);
//	        vertx = Vertx.vertx(options);
//	        vertx.deployVerticle(MyFirstVerticle.class.getName(), context.asyncAssertSuccess());
//	    }


}
