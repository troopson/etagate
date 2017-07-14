package org.etagate;

import org.etagate.app.App;
import org.etagate.app.AppContain;
import org.etagate.conf.GateSetting;
import org.etagate.helper.S;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月11日
 */
public class InsideServerVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(InsideServerVerticle.class);

	private String upload_dir;

	private Router router;
	
	private WebClient webclient;
	
	private GateSetting gsetting;
	
	private AppContain appContain;
	
	

	public void start() throws Exception {

		JsonObject conf = vertx.getOrCreateContext().config();
		
		HttpServerOptions options =  new HttpServerOptions();
		options.setCompressionSupported(true);		
		
		HttpServer server = vertx.createHttpServer(options);
				
		/*
		    <property name="inside.host">172.18.7.20</property>
   			<property name="inside.port">8999</property>
		 */
		String host = conf.getString("inside.host");
		String port = conf.getString("inside.port");		
		
		if(S.isBlank(host) || S.isBlank(port))
			return;

		// ============初始化======================

		this.upload_dir = conf.getString("upload.dir");

		this.webclient = WebClient.create(vertx);

		this.appContain = gsetting.getAppContain(vertx,this.webclient);
						 
		this.initRoutes();
		
		server.requestHandler(router::accept);
		server.listen(Integer.parseInt(port), host,ar -> {
			if (ar.succeeded()) {
				log.info("InsideServer listen on " + port);
			} else {
				log.error("InsideServer Failed to start!", ar.cause());
			}
		});		

	}

	private void initRoutes() {
		this.router = Router.router(this.vertx);
		
		BodyHandler bh = BodyHandler.create();
		bh.setMergeFormAttributes(false);
		bh.setUploadsDirectory(this.upload_dir);
		router.route().handler(bh);	
		
		router.route("/node/:appname").method(HttpMethod.GET).handler(rc->{
			HttpServerRequest request = rc.request();
			String appname = request.getParam("appname");
			if(S.isNotBlank(appname)){
				App app  = this.appContain.getAppInfo(appname);
				JsonObject json = app.retrieveOneNode(request);
				request.response().end(json.encode());
			}
		 });
		router.route("/nodes/:appname").method(HttpMethod.GET).handler(rc->{
			HttpServerRequest request = rc.request();
			String appname = request.getParam("appname");
			if(S.isNotBlank(appname)){
				App app  = this.appContain.getAppInfo(appname);
				JsonArray arry = app.retrieveNodes();
				request.response().end(arry.encode());
			}
		 });
		
		

	}


	// =================================================


	public void setGsetting(GateSetting gsetting) {
		this.gsetting = gsetting;
	}
	



}
