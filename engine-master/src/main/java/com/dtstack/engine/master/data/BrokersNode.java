package com.dtstack.engine.master.data;


/**
 * 
 *
 * Date: 2017年03月01日 下午1:16:37
 * Company: www.dtstack.com
 * @author sishu.yss
 *
 */
public class BrokersNode {

	private String master;

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}
	
	public static BrokersNode initBrokersNode(){
		return new BrokersNode();
	}
}