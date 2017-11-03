package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_ImageView_MAbstract extends DoUIModule{

	protected do_ImageView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("cacheType", PropertyDataType.String, "never", true));
		this.registProperty(new DoProperty("enabled", PropertyDataType.Bool, "false", false));
		this.registProperty(new DoProperty("radius", PropertyDataType.Number, "0", true));
		this.registProperty(new DoProperty("scale", PropertyDataType.String, "fillxy", true));
		this.registProperty(new DoProperty("source", PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("defaultImage",PropertyDataType.String, "", false));
		this.registProperty(new DoProperty("animation",PropertyDataType.String, "none", false));
		
	}
}