package doext.implement;

import java.util.Map;

import org.json.JSONObject;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageHandleHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoImageLoadHelper.OnPostExecuteListener;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.helper.cache.DoCacheManager;
import core.interfaces.DoIBitmap;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoUIModule;
import doext.define.do_ImageView_IMethod;
import doext.define.do_ImageView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_ImageView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ImageView_View extends ImageView implements DoIUIModuleView, do_ImageView_IMethod, OnClickListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ImageView_MAbstract model;
	private ColorDrawable bgColorDrawable = new ColorDrawable(Color.TRANSPARENT);
	private float radius;
	private String source;
	private String animation = "none";
	private double radiusZoom;
	private int borderSize;

	public do_ImageView_View(Context context) {
		super(context);
		this.setScaleType(ScaleType.FIT_XY);
		this.setEnabled(false);
		this.setFocusable(false);
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (enabled) {
			this.setOnClickListener(this);
		}
		super.setEnabled(enabled);
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		Bitmap newBitmap = null;
		if (bm != null) {
			int imageOriginalW = bm.getWidth();
			int imageOriginalH = bm.getHeight();
			try {
				String _scale = model.getPropertyValue("scale"); //默认值可能为空
				if (TextUtils.isEmpty(_scale) || "fillxy".equals(_scale)) {
					double _realWidth = model.getRealWidth();
					double _realHeight = model.getRealHeight();
					if (_realWidth > 0 && _realHeight == LayoutParams.WRAP_CONTENT) { //width 固定，height = -1；
						//view 的 width x XZoom ， height 也必须  x YZoom
						_realHeight = imageOriginalH * (_realWidth / imageOriginalW);
					} else if (_realHeight > 0 && _realWidth == LayoutParams.WRAP_CONTENT) {//height固定，width = -1；
						_realWidth = imageOriginalW * (_realHeight / imageOriginalH);
					}
					double scaleWidth = _realWidth / imageOriginalW;
					double scaleHeight = _realHeight / imageOriginalH;
					// 取得想要缩放的matrix参数
					Matrix matrix = new Matrix();
					matrix.postScale((float) scaleWidth, (float) scaleHeight);
					// 得到新的图片
					newBitmap = Bitmap.createBitmap(bm, 0, 0, imageOriginalW, imageOriginalH, matrix, true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (newBitmap != null) {
			bm = newBitmap;
		}
		super.setImageBitmap(bm);
		if ("fade".equals(animation)) {
			ValueAnimator animator = null;
			animator = ObjectAnimator.ofFloat(do_ImageView_View.this, "alpha", 0f, 1f);
			animator.setDuration(1000);
			animator.start();
		}
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		if ("fade".equals(animation)) {
			ValueAnimator animator = null;
			animator = ObjectAnimator.ofFloat(do_ImageView_View.this, "alpha", 0f, 1f);
			animator.setDuration(1000);
			animator.start();
		}
	}

	@Override
	public void setBackgroundColor(int color) {
		if (model.getRealWidth() == LayoutParams.WRAP_CONTENT || model.getRealHeight() == LayoutParams.WRAP_CONTENT || ScaleType.CENTER_CROP.equals(getScaleType()) || this.radius == 0f) {//暂时先解决不设置圆角处理，否者执行bgColor动画会有问题；
			super.setBackgroundColor(color);
		}
		if (color != Color.TRANSPARENT) {
			if (bgColorDrawable == null) {
				bgColorDrawable = new ColorDrawable(color);
			} else {
				bgColorDrawable.setColor(color);
			}
		}
		this.postInvalidate();
	}

	private int getImgWidth() {
		return this.getWidth() == 0 ? (int) model.getWidth() : this.getWidth();
	}

	private int getImgHeight() {
		return this.getHeight() == 0 ? (int) model.getHeight() : this.getHeight();
	}

	@SuppressLint({ "NewApi", "DrawAllocation" })
	@Override
	protected void onDraw(Canvas canvas) {
		//-1不做处理
		if ((bgColorDrawable.getColor() == Color.TRANSPARENT && this.radius == 0f) || model.getRealWidth() == LayoutParams.WRAP_CONTENT || model.getRealHeight() == LayoutParams.WRAP_CONTENT) {
			super.onDraw(canvas);
		} else if (ScaleType.CENTER_CROP.equals(getScaleType()) && this.radius == 0f) {
			super.onDraw(canvas);
		} else {
			int imgW = getImgWidth();
			int imgH = getImgHeight();
			Drawable mDrawable = getDrawable();
			if (imgW == 0 || imgH == 0) {
				return;
			}
			Bitmap bgBitmap = DoImageHandleHelper.drawableToBitmap(bgColorDrawable, imgW, imgH);
			int _newW = bgBitmap.getWidth() - (borderSize * 2);
			int _newH = bgBitmap.getHeight() - (borderSize * 2);
			if (_newW <= 0 || _newH <= 0) {
				return;
			}

			Bitmap newBitmap = Bitmap.createBitmap(_newW, _newH, bgBitmap.getConfig());
			Canvas newCanvas = new Canvas(newBitmap);
			newCanvas.drawBitmap(bgBitmap, 0, 0, null);
			if (mDrawable != null) {
				Bitmap imageBitmap = ((BitmapDrawable) mDrawable).getBitmap();
				if (imageBitmap != null) {
					Bitmap scaledBitmap = getScaledBitmap(imageBitmap);
					float left = Math.abs(bgBitmap.getWidth() - scaledBitmap.getWidth()) / 2;
					float top = Math.abs(bgBitmap.getHeight() - scaledBitmap.getHeight()) / 2;
					newCanvas.drawBitmap(scaledBitmap, left, top, null);
				}
			}
			if (this.radius > 0f) {
				canvas.drawBitmap(DoImageHandleHelper.createRoundBitmap(newBitmap, radius), borderSize, borderSize, null);
				return;
			}
			canvas.drawBitmap(newBitmap, borderSize, borderSize, null);
		}
	}

	private Bitmap getScaledBitmap(Bitmap imageBitmap) {
		// 获取原图真实宽、高
		Rect rect = getDrawable().getBounds();
		int dw = rect.width();
		int dh = rect.height();
		// 获得ImageView中Image的变换矩阵
		Matrix m = getImageMatrix();
		float[] values = new float[10];
		m.getValues(values);
		// Image在绘制过程中的变换矩阵，从中获得x和y方向的缩放系数
		float sx = values[0];
		float sy = values[4];
		// 计算Image在屏幕上实际绘制的宽高
		int cw = (int) (dw * sx);
		int ch = (int) (dh * sy);
		try {
			if ("center".equals(model.getPropertyValue("scale"))) {
				return createCenterTypeScaledBitmap(imageBitmap, cw, ch);
			}
			if ("centercrop".equals(model.getPropertyValue("scale"))) {
				Matrix matrix = new Matrix();
				matrix.postScale(sx, sy);
				imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, dw, dh, matrix, true);
				return createCenterCropScaledBitmap(imageBitmap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Bitmap.createScaledBitmap(imageBitmap, cw, ch, true);
	}

	private Bitmap createCenterTypeScaledBitmap(Bitmap bitmap, int cw, int ch) {
		//bitmap 原图
		Bitmap centerScaleBitmap = Bitmap.createBitmap(getImgWidth(), getImgHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(centerScaleBitmap);
		//src 绘画该图片需显示多少，也就是你想绘画该图片裁剪区域；
		Rect src = new Rect();
		int bx = (cw - getImgWidth()) / 2;
		int by = (ch - getImgHeight()) / 2;
		//by > bx > 0原图超出实际宽高度，从0,0位置开始画，否则从x,y点开始画；
		int x = bx > 0 ? 0 : Math.abs(bx);
		int y = by > 0 ? 0 : Math.abs(by);
		//by < bx < 0原图小于实际宽高度，从0,0位置开始截取原图片内容，否则从bx,by点开始截取；
		bx = bx < 0 ? 0 : bx;
		by = by < 0 ? 0 : by;
		src.left = bx;
		src.top = by;
		src.right = bx + cw;
		src.bottom = by + ch;
		//dst 表示在该画布上图片屏幕的裁剪区域；注，在src的基础上裁剪并显示图片
		Rect dst = new Rect();
		dst.left = x;
		dst.top = y;
		dst.right = x + cw;
		dst.bottom = y + ch;
		canvas.drawBitmap(bitmap, src, dst, null);
		return centerScaleBitmap;
	}

	private Bitmap createCenterCropScaledBitmap(Bitmap bitmap) {
		//bitmap 原图
		Bitmap centerScaleBitmap = Bitmap.createBitmap(getImgWidth(), getImgHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(centerScaleBitmap);
		//src 绘画bitmap图片需显示多少，也就是你想绘画该图片裁剪区域；
		Rect src = new Rect();
		int bx = (bitmap.getWidth() - getImgWidth()) / 2;
		int by = (bitmap.getHeight() - getImgHeight()) / 2;
		//by > bx > 0原图超出实际宽高度，从0,0位置开始画，否则从x,y点开始画；
		int x = bx > 0 ? 0 : Math.abs(bx);
		int y = by > 0 ? 0 : Math.abs(by);
		//by < bx < 0原图小于实际宽高度，从0,0位置开始截取原图片内容，否则从bx,by点开始截取；
		bx = bx < 0 ? 0 : bx;
		by = by < 0 ? 0 : by;
		src.left = bx;
		src.top = by;
		src.right = bitmap.getWidth();
		src.bottom = bitmap.getHeight();
		//dst 表示在该画布上图片屏幕的裁剪区域；注，在src的基础上裁剪并显示图片
		Rect dst = new Rect();
		dst.left = x;
		dst.top = y;
		dst.right = x + bitmap.getWidth() - bx;
		dst.bottom = y + bitmap.getHeight() - by;
		canvas.drawBitmap(bitmap, src, dst, null);
		return centerScaleBitmap;
	}

	@Override
	public void onClick(View v) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		this.model.getEventCenter().fireEvent("touch", _invokeResult);
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ImageView_MAbstract) _doUIModule;
		radiusZoom = (model.getXZoom() + model.getYZoom()) / 2;
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		if (_changedValues.containsKey("radius")) {
			this.radius = (float) (DoTextHelper.strToFloat(_changedValues.get("radius"), 0f) * radiusZoom);
		}
		if (_changedValues.containsKey("enabled")) {
			this.setEnabled(Boolean.parseBoolean(_changedValues.get("enabled")));
		}
		if (_changedValues.containsKey("scale")) {
			String value = _changedValues.get("scale");
			if ("center".equals(value)) {
				this.setScaleType(ScaleType.CENTER);
			} else if ("fillxory".equals(value)) {
				this.setScaleType(ScaleType.FIT_CENTER);
			} else if ("centercrop".equals(value)) {
				this.setScaleType(ScaleType.CENTER_CROP);
			} else {
				this.setScaleType(ScaleType.FIT_XY);
			}
		}
		if (_changedValues.containsKey("defaultImage")) {
			String defaultImageValue = _changedValues.get("defaultImage");
			try {
				setLocalImage(defaultImageValue);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_ImageView_View", e);
			}
		}

		if (_changedValues.containsKey("border")) {
			String _border = _changedValues.get("border"); //FF0000FF,1,[10,10,10,10]
			String[] _borderParam = DoUIModuleHelper.split(_border);
			if (_borderParam != null && _borderParam.length == 3) {
				// 颜色，宽度，圆角
				borderSize = (int) Math.ceil(DoTextHelper.strToInt(_borderParam[1], 0) * radiusZoom);
				float[] radii = DoUIModuleHelper.getRadii(_borderParam[2], radiusZoom, _border);
				this.radius = radii[0];
			}
		}

		if (_changedValues.containsKey("source")) {
			try {
				String cache = getCacheValue(_changedValues);
				source = _changedValues.get("source");
				if (null != DoIOHelper.getHttpUrlPath(source)) {
					Bitmap bitmap = DoCacheManager.getInstance().getBitmapFromMemoryCache(source, true);
					if (bitmap != null) {
						setImageBitmap(bitmap);
					} else {
						String defaultImageValue = this.model.getPropertyValue("defaultImage");
						setLocalImage(defaultImageValue);
					}
					if (bitmap == null || "temporary".equals(cache)) {
						int width = -1;
						int height = -1;
						if (!"center".equals(model.getPropertyValue("scale"))) {
							width = (int) model.getWidth();
							height = (int) model.getHeight();
						}
						DoImageLoadHelper.getInstance().loadURL(source, cache, width, height, new OnPostExecuteListener() {
							@Override
							public void onResultExecute(Bitmap bitmap, String url) {
								//url.equals(source)判断source等于最后请求结果URL并显示，忽略掉中间线程结果；
								if ((bitmap != null && url.equals(source))) {
									setImageBitmap(bitmap);
								}
							}
						});
					}
				} else {
					setLocalImage(source);
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_ImageView_View", e);
			}
		}
		if (_changedValues.containsKey("animation")) {
			animation = _changedValues.get("animation");
		}
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
	}

	private void setLocalImage(String local) throws Exception {
		if (local != null && !"".equals(local)) {
			String path = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), local);
			Bitmap bitmap = null;
			if ("center".equals(model.getPropertyValue("scale"))) {
				bitmap = DoImageLoadHelper.getInstance().loadLocal(path, -1, -1);
				if (bitmap != null && model.getRealWidth() != LayoutParams.WRAP_CONTENT && model.getRealHeight() != LayoutParams.WRAP_CONTENT) {
					Matrix matrix = new Matrix();
					matrix.postScale((float) model.getXZoom(), (float) model.getYZoom());
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				}
			} else {
				bitmap = DoImageLoadHelper.getInstance().loadLocal(path, (int) model.getWidth(), (int) model.getHeight());
			}
			setImageBitmap(bitmap);
		} else { //如果为空找defaultImage
			String defaultImageValue = this.model.getPropertyValue("defaultImage");
			if (TextUtils.isEmpty(defaultImageValue)) { //清空imageview 图片
				setImageBitmap(null);
			} else {
				setLocalImage(defaultImageValue);
			}

		}
	}

	private String getCacheValue(Map<String, String> _changedValues) {
		String cacheType = _changedValues.get("cacheType");
		if (cacheType == null) {
			try {
				cacheType = this.model.getPropertyValue("cacheType");
				if (cacheType == null || "".equals(cacheType)) {
					cacheType = "never";
				}
			} catch (Exception e) {
				cacheType = "never";
				e.printStackTrace();
			}
		}
		return cacheType;
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("setBitmap".equals(_methodName)) {
			this.setBitmap(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName
	 *                    ,_invokeResult);参数解释：
	 * @_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象； 获取DoInvokeResult对象方式new
	 *                                                   DoInvokeResult
	 *                                                   (this.model
	 *                                                   .getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// ...do something
		return false;
	}

	private void setBitmap(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "bitmap", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("bitmap参数不能为空！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("bitmap参数无效！");
		if (_multitonModule instanceof DoIBitmap) {
			DoIBitmap _bitmap = (DoIBitmap) _multitonModule;
			Bitmap _mData = _bitmap.getData();
			if (_mData != null && !_mData.isRecycled()) {
				this.setImageBitmap(_mData);
			}
		}
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!this.model.getEventCenter().containsEvent("touch")) {
			return false;
		}
		return super.onTouchEvent(event);
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}
}
