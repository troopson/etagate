package org.etagate;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.etagate.helper.YamlConfig;
import org.junit.Test;

public class TestYamlhelper {

	@Test
	public void test() throws MalformedURLException {
		InputStream is = this.getClass().getResourceAsStream("/application.yml");
		if(is== null)
			return;
		YamlConfig y=new YamlConfig();
		y.loadYaml(is);
		System.out.println(y.toString());
		System.out.println(y.get("page.pageSize"));
		System.out.println(y.get("spring.datasource.url"));
		System.out.println(y.get("spring.datasource.tomcat.max-wait"));
		
		YamlConfig y2=new YamlConfig();		
		URL url =  new URL("http://172.21.9.20:7777/read/bussa/devqjj/application.yml");
		y2.loadYaml(url);
		System.out.println(y2.toString());
	}

}
