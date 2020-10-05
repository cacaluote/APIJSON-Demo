/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.demo.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.IFloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.ViewStateListener;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import apijson.demo.R;
import apijson.demo.ui.UIAutoActivity;
import apijson.demo.ui.UIAutoListActivity;
import unitauto.apk.UnitAutoApp;

/**Application
 * @author Lemon
 */
public class DemoApplication extends Application {
	private static final String TAG = "DemoApplication";

	private static final String SPLIT_X = "SPLIT_X";
	private static final String SPLIT_Y = "SPLIT_Y";
	private static final String SPLIT_HEIGHT = "SPLIT_HEIGHT";
	private static final String SPLIT_COLOR = "SPLIT_COLOR";


	private static DemoApplication instance;
	public static DemoApplication getInstance() {
		return instance;
	}


	private boolean isRecovering = false;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			if (isRecovering) {
				//通过遍历数组来实现
//                if (lastCurTime >= System.currentTimeMillis()) {
//                    isRecovering = false;
//                    pbUIAutoSplitY.setVisibility(View.GONE);
//                }
//
//                MotionEvent event = (MotionEvent) msg.obj;
//                dispatchEventToCurrentActivity(event);


				//根据递归链表来实现，能精准地实现两个事件之间的间隔，不受处理时间不一致，甚至卡顿等影响。还能及时终止
				Node<InputEvent> eventNode = (Node<InputEvent>) msg.obj;
				dispatchEventToCurrentActivity(eventNode.item);

				if (eventNode.next == null || eventNode.next.item == null) {
					isRecovering = false;
//					pbUIAutoSplitY.setVisibility(View.GONE);
					return;
				}

				floatBall.updateX(windowX + eventNode.splitX - splitSize/2);
				floatBall.updateY(windowY + eventNode.splitY - splitSize/2);

				msg = Message.obtain();
				msg.obj = eventNode.next;
				sendMessageDelayed(msg, eventNode.next.item.getEventTime() - eventNode.item.getEventTime());
			}
		}
	};



	private Activity activity;
	int screenWidth;
	int screenHeight;

	int windowWidth;
	int windowHeight;
	int windowX;
	int windowY;

	ViewGroup vFloatCover;
	View vFloatBall;
	ViewGroup vSplitX;
	ViewGroup vSplitY;

	private int splitX;
	private int splitY;
	private int splitSize;
	private boolean moved = false;

	private JSONArray touchList;

	SharedPreferences cache;
	private long flowId = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		UnitAutoApp.init(this);
		Log.d(TAG, "项目启动 >>>>>>>>>>>>>>>>>>>> \n\n");

		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {


			@Override
			public void onActivityStarted(Activity activity) {
				Log.v(TAG, "onActivityStarted  activity = " + activity.getClass().getName());
			}

			@Override
			public void onActivityStopped(Activity activity) {
				Log.v(TAG, "onActivityStopped  activity = " + activity.getClass().getName());
			}

			@Override
			public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
				Log.v(TAG, "onActivitySaveInstanceState  activity = " + activity.getClass().getName());
			}

			@Override
			public void onActivityResumed(Activity activity) {
				Log.v(TAG, "onActivityResumed  activity = " + activity.getClass().getName());
				setCurrentActivity(activity);
			}

			@Override
			public void onActivityPaused(Activity activity) {
				Log.v(TAG, "onActivityPaused  activity = " + activity.getClass().getName());
				setCurrentActivity(activityList.isEmpty() ? null : activityList.get(activityList.size() - 1));
			}

			@Override
			public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
				Log.v(TAG, "onActivityCreated  activity = " + activity.getClass().getName());
				activityList.add(activity);
				//TODO 按键、键盘监听拦截和转发
			}

			@Override
			public void onActivityDestroyed(Activity activity) {
				Log.v(TAG, "onActivityDestroyed  activity = " + activity.getClass().getName());
				activityList.remove(activity);
			}

		});

	}

	public void onUIAutoActivityCreate(Activity activity) {
		setCurrentActivity(activity);
		Window window = activity.getWindow();
		//反而让 vFloatCover 与底部差一个导航栏高度 window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		DisplayMetrics outMetrics = new DisplayMetrics();
		Display display = activity.getWindowManager().getDefaultDisplay();

		windowWidth = display.getWidth();
		windowHeight = display.getHeight();
		windowX = 0;
		windowY = getWindowY(activity);

		display.getRealMetrics(outMetrics);
		screenWidth = outMetrics.widthPixels;
		screenHeight = outMetrics.heightPixels;

		cache = getSharedPreferences(TAG, Context.MODE_PRIVATE);

		splitX = cache.getInt(SPLIT_X, 0);
		splitY = cache.getInt(SPLIT_Y, 0);
		splitSize = cache.getInt(SPLIT_HEIGHT, dip2px(24));

		if (splitX <= splitSize || splitX >= windowWidth - splitSize) {
			splitX = windowWidth - splitSize - dip2px(45);
		}
		if (splitY <= splitSize || splitY >= windowHeight - splitSize) {
			splitY = windowHeight - splitSize - dip2px(45);
		}

		if (touchList != null && touchList.isEmpty() == false) { //TODO 回放操作
			activity.startActivityForResult(UIAutoListActivity.createIntent(DemoApplication.getInstance(), touchList == null ? null : touchList.toJSONString()), UIAutoActivity.REQUEST_UI_AUTO_LIST);
		}

		vFloatCover = (ViewGroup) getLayoutInflater().inflate(R.layout.ui_auto_cover_layout, null);
		vFloatBall = getLayoutInflater().inflate(R.layout.ui_auto_split_ball_layout, null);
		vSplitX = (ViewGroup) getLayoutInflater().inflate(R.layout.ui_auto_split_x_layout, null);
		vSplitY = (ViewGroup) getLayoutInflater().inflate(R.layout.ui_auto_split_y_layout, null);


        vFloatCover.addView(vSplitX);
        vFloatCover.addView(vSplitY);

//        vSplitY.post(new Runnable() {
//            @Override
//            public void run() {
//                vSplitY.setY(splitY - vSplitY.getHeight()/2);
//                vFloatCover.setVisibility(View.GONE);
//            }
//        });

//		vSplitY.setBackgroundColor(Color.parseColor(cache.getString(SPLIT_COLOR, "#10000000")));

		vFloatCover.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouchEvent  " + Calendar.getInstance().getTime().toLocaleString() +  " action:" + (event.getAction()) + "; x:" + event.getX() + "; y:" + event.getY());
				dispatchEventToCurrentActivity(event);
//死循环                llTouch.dispatchTouchEvent(event);
//                vDispatchTouch.dispatchTouchEvent(event);
//                vDispatchTouch.dispatchTouchEvent(event);
				//onTouchEvent 不能处理事件 vDispatchTouch.onTouchEvent(event);
//                vTouch.setOnTouchListener(this);
				return true;  //连续记录只能 return true
			}
		});

		vFloatBall.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isRecovering = false;
//                ((ViewGroup) v.getParent()).removeView(v);

				if (touchList != null && touchList.isEmpty() == false) {
					String cacheKey = UIAutoListActivity.CACHE_TOUCH;
					SharedPreferences cache = getSharedPreferences(TAG, Context.MODE_PRIVATE);
					JSONArray allList = JSON.parseArray(cache.getString(cacheKey, null));

					if (allList == null || allList.isEmpty()) {
						allList = touchList;
					}
					else {
						allList.addAll(touchList);
					}
					cache.edit().remove(cacheKey).putString(cacheKey, JSON.toJSONString(allList)).commit();
				}

				isShowing = false;
				floatCover = null;
				floatBall = null;
				FloatWindow.destroy("floatCover");
				FloatWindow.destroy("floatBall");

//                startActivity(UIAutoListActivity.createIntent(DemoApplication.getInstance(), flowId));  // touchList == null ? null : touchList.toJSONString()));
//                startActivityForResult(UIAutoListActivity.createIntent(DemoApplication.getInstance(), touchList == null ? null : touchList.toJSONString()), REQUEST_UI_AUTO_LIST);
				startActivity(UIAutoActivity.createIntent(DemoApplication.getInstance(), touchList == null ? null : touchList.toJSONString()));
			}
		});
//		vFloatBall.setOnTouchListener(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
////                都不动了 if (event.getY() - event.getRawY() >= 10) {
//				if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
//					moved = true;
//					vSplitY.setY(event.getY());
////                    vSplitY.invalidate();
//				} else {
//					if (event.getAction() == MotionEvent.ACTION_DOWN) {
//						moved = false;
//					}
//					else if (event.getAction() == MotionEvent.ACTION_UP) {
//						if (! moved) {
//							ivUIAutoSplitY.performClick();
//						}
//					}
//				}
////                }
//				return true;
//			}
//		});

//        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        root.addView(vFloatCover, lp);

	}


	public void onUIAutoActivityDestroy(Activity activity) {
		cache.edit()
				.remove(SPLIT_X)
				.putInt(SPLIT_X, (int) (vSplitX.getX() + vSplitX.getWidth()/2))
				.remove(SPLIT_Y)
				.putInt(SPLIT_Y, (int) (vSplitY.getY() + vSplitY.getHeight()/2))
				.apply();
	}


	public LayoutInflater getLayoutInflater() {
		try {
			return LayoutInflater.from(this);
		} catch (Exception e) {
			return LayoutInflater.from(activity);
		}
	}

	/**获取应用名
	 * @return
	 */
	public String getAppName() {
		return getResources().getString(R.string.app_name);
	}
	/**获取应用版本名(显示给用户看的)
	 * @return
	 */
	public String getAppVersion() {
		return getResources().getString(R.string.app_version);
	}

	private List<Activity> activityList = new LinkedList<>();

	private WeakReference<Activity> sCurrentActivityWeakRef;
	public Activity getCurrentActivity() {
		Activity currentActivity = null;
		if (sCurrentActivityWeakRef != null) {
			currentActivity = sCurrentActivityWeakRef.get();
		}
		return currentActivity;
	}

	public void setCurrentActivity(Activity activity) {
		this.activity = activity;
		if (sCurrentActivityWeakRef == null || ! activity.equals(sCurrentActivityWeakRef.get())) {
			sCurrentActivityWeakRef = new WeakReference<>(activity);
		}

		UnitAutoApp.setCurrentActivity(activity);
	}





	public boolean onTouchEvent(MotionEvent event, Activity activity) {
		addInputEvent(event, activity);
		return true;
	}
	public boolean onKeyDown(int keyCode, KeyEvent event, Activity activity) {
		addInputEvent(event, activity);
		return true;
	}
	public boolean onKeyUp(int keyCode, KeyEvent event, Activity activity) {
		addInputEvent(event, activity);
		return true;
	}

	public void record() {
		flowId = - System.currentTimeMillis();

		vFloatCover.setVisibility(View.VISIBLE);
		showCover(true, activity);
	}


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isShowing) {
			int w = windowWidth;
			int h = windowHeight;
//			int x = windowX;
//			int y = windowY;

			int sx = splitX;
			int sy = splitY;
			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				windowWidth = Math.max(w, h);
				windowHeight = Math.min(w, h);

//				windowX = windowY;
//				windowY = x;

				splitX = splitY;
				splitY = sx;
			} else {
				windowWidth = Math.min(w, h);
				windowHeight = Math.max(w, h);

//				windowY = windowX;
//				windowX = y;

				splitY = splitX;
				splitX = sy;
			}

        	FloatWindow.destroy("floatCover");
        	FloatWindow.destroy("floatBall");

        	showCover(true, getCurrentActivity());
        }
    }

    private IFloatWindow floatCover;
	private IFloatWindow floatBall;

	private boolean isShowing;
	private void showCover(boolean show, Activity activity) {
		isShowing = show;

		floatCover = FloatWindow.get("floatCover");
		if (floatCover == null) {
			FloatWindow
					.with(getApplicationContext())
					.setTag("floatCover")
					.setView(vFloatCover)
					.setWidth(windowWidth)                               //设置控件宽高
					.setHeight(windowHeight)
					.setX(0)                                   //设置控件初始位置
					.setY(windowY)
					.setMoveType(MoveType.inactive)
					.setDesktopShow(true) //必须为 true，否则切换 Activity 就会自动隐藏                        //桌面显示
//                .setViewStateListener(mViewStateListener)    //监听悬浮控件状态改变
//                .setPermissionListener(mPermissionListener)  //监听权限申请结果
					.build();

			floatCover = FloatWindow.get("floatCover");
		}
		floatCover.show();


		floatBall = FloatWindow.get("floatBall");
		if (floatBall == null) {
			int x = (int) (windowX + splitX - splitSize/2);
			int y = (int) (windowY + splitY - splitSize/2);
			FloatWindow
					.with(getApplicationContext())
					.setTag("floatBall")
					.setView(vFloatBall)
					.setWidth(splitSize)                               //设置控件宽高
					.setHeight(splitSize)
					.setX(x)                                   //设置控件初始位置
					.setY(y)
					.setMoveType(MoveType.active)
					.setDesktopShow(true) //必须为 true，否则切换 Activity 就会自动隐藏                        //桌面显示
					.setViewStateListener(new ViewStateListener() {
						@Override
						public void onPositionUpdate(int x, int y) {
							if (vSplitX != null) {
								vSplitX.setX(x + splitSize/2 - dip2px(0.5f));
							}
							if (vSplitY != null) {
								vSplitY.setY(y + splitSize/2 - dip2px(0.5f));
							}
						}

						@Override
						public void onShow() {
							if (vSplitX != null) {
								vSplitX.setVisibility(View.VISIBLE);
							}
							if (vSplitY != null) {
								vSplitY.setVisibility(View.VISIBLE);
							}

							onPositionUpdate(x, y);
						}

						@Override
						public void onHide() {
							if (vSplitX != null) {
								vSplitX.setVisibility(View.GONE);
							}
							if (vSplitY != null) {
								vSplitY.setVisibility(View.GONE);
							}
						}

						@Override
						public void onDismiss() {
							onHide();
						}

						@Override
						public void onMoveAnimStart() {

						}

						@Override
						public void onMoveAnimEnd() {

						}

						@Override
						public void onBackToDesktop() {

						}
					})    //监听悬浮控件状态改变
//                .setPermissionListener(mPermissionListener)  //监听权限申请结果
					.build();

			floatBall = FloatWindow.get("floatBall");
		}
		floatBall.show();


	}


	public int getWindowY(Activity activity) {
		View decorView = activity.getWindow().getDecorView();

		Rect rectangle= new Rect();
		decorView.getWindowVisibleDisplayFrame(rectangle);
		return rectangle.top;
	}

	public boolean dispatchEventToCurrentActivity(InputEvent ie) {
		if (activity != null) {
			if (ie instanceof MotionEvent) {
				MotionEvent event = (MotionEvent) ie;
				int windowY = getWindowY(activity);

				if (windowY > 0) {
					event = MotionEvent.obtain(event);
					event.offsetLocation(0, windowY);
				}
				activity.dispatchTouchEvent(event);
			}
			else if (ie instanceof KeyEvent) {
				KeyEvent event = (KeyEvent) ie;
				activity.dispatchKeyEvent(event);
			}

		}

		addInputEvent(ie, activity);

		return activity != null;
	}



	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public int dip2px(float dpValue) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public int px2dip(float pxValue) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}


	private Node<InputEvent> firstEventNode;
	private Node<InputEvent> eventNode;

	private long firstTime = 0;
	private long lastTime = 0;
	private long firstCurTime = 0;
	private long lastCurTime = 0;
	public void recover(JSONArray touchList) {
		isRecovering = true;

//        List<InputEvent> list = new LinkedList<>();

		showCover(true, DemoApplication.getInstance().getCurrentActivity());

		JSONObject first = touchList == null || touchList.isEmpty() ? null : touchList.getJSONObject(0);
		firstTime = first == null ? 0 : first.getLongValue("time");

		firstCurTime = 0;
		if (firstTime > 0) {
			firstCurTime = System.currentTimeMillis();
//			pbUIAutoSplitY.setVisibility(View.VISIBLE);

			for (int i = 0; i < touchList.size(); i++) {
				JSONObject obj = touchList.getJSONObject(i);

				InputEvent event;
				if (obj.getIntValue("type") == 1) {
					/**
					 public KeyEvent(long downTime, long eventTime, int action,
					 int code, int repeat, int metaState,
					 int deviceId, int scancode, int flags, int source) {
					 mDownTime = downTime;
					 mEventTime = eventTime;
					 mAction = action;
					 mKeyCode = code;
					 mRepeatCount = repeat;
					 mMetaState = metaState;
					 mDeviceId = deviceId;
					 mScanCode = scancode;
					 mFlags = flags;
					 mSource = source;
					 mDisplayId = INVALID_DISPLAY;
					 }
					 */
					event = new KeyEvent(
							obj.getLongValue("downTime"),
							obj.getLongValue("eventTime"),
							obj.getIntValue("action"),
							obj.getIntValue("keyCode"),
							obj.getIntValue("repeatCount"),
							obj.getIntValue("metaState"),
							obj.getIntValue("deviceId"),
							obj.getIntValue("scanCode"),
							obj.getIntValue("flags"),
							obj.getIntValue("source")
					);
				}
				else {
					/**
					 public static MotionEvent obtain(long downTime, long eventTime, int action,
					 float x, float y, float pressure, float size, int metaState,
					 float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source,
					 int displayId)
					 */

					//居然编译报错，和
					// static public MotionEvent obtain(long downTime, long eventTime,
					//    int action, int pointerCount, PointerProperties[] pointerProperties,
					//    PointerCoords[] pointerCoords, int metaState, int buttonState,
					//    float xPrecision, float yPrecision, int deviceId,
					//    int edgeFlags, int source, int displayId, int flags)
					//冲突，实际上类型没传错

					//                    event = MotionEvent.obtain(obj.getLongValue("downTime"),  obj.getLongValue("eventTime"),  obj.getIntValue("action"),
					//                    obj.getFloatValue("x"),  obj.getFloatValue("y"),  obj.getFloatValue("pressure"),  obj.getFloatValue("size"),  obj.getIntValue("metaState"),
					//                    obj.getFloatValue("xPrecision"),  obj.getFloatValue("yPrecision"),  obj.getIntValue("deviceId"),  obj.getIntValue("edgeFlags"),  obj.getIntValue("source"),
					//                    obj.getIntValue("displayId"));

                    float x = obj.getFloatValue("x");
                    float y = obj.getFloatValue("y");
                    float sx = obj.getFloatValue("splitX");
                    float sy = obj.getFloatValue("splitY");
                    float ww = obj.getFloatValue("windowWidth");
                    float wh = obj.getFloatValue("windowHeight");

					event = MotionEvent.obtain(
							obj.getLongValue("downTime"),
							obj.getLongValue("eventTime"),
							obj.getIntValue("action"),
//                            obj.getIntValue("pointerCount"),
							windowX + (x <= sx ? x*windowWidth/ww : windowWidth - (ww - x)*windowWidth/ww),
                            windowY + (y <= sy ? y*windowHeight/wh : windowHeight - (wh - y)*windowHeight/wh),
							obj.getFloatValue("pressure"),
							obj.getFloatValue("size"),
							obj.getIntValue("metaState"),
							obj.getFloatValue("xPrecision"),
							obj.getFloatValue("yPrecision"),
							obj.getIntValue("deviceId"),
							obj.getIntValue("edgeFlags")
//                            obj.getIntValue("source"),
//                            obj.getIntValue("displayId")
					);
					((MotionEvent) event).setSource(obj.getIntValue("source"));
//                    ((MotionEvent) event).setEdgeFlags(obj.getIntValue("edgeFlags"));

				}


//                list.add(event);

				long time = obj.getIntValue("time");
				if (i <= 0) {
					firstEventNode = new Node<>(null, event, null);
					eventNode = firstEventNode;
				}
				else if (i >= touchList.size() - 1) {
					lastTime = time;
					lastCurTime = firstCurTime + lastTime - firstTime;
				}

				eventNode.id = obj.getLongValue("id");
				eventNode.flowId = obj.getLongValue("flowId");
				eventNode.time = obj.getLongValue("time");
				eventNode.splitX = obj.getIntValue("splitX");
				eventNode.splitY = obj.getIntValue("splitY");
				eventNode.splitSize = obj.getIntValue("splitSize");
				eventNode.windowX = obj.getIntValue("windowX");
				eventNode.windowY = obj.getIntValue("windowY");
				eventNode.orientation = obj.getIntValue("orientation");

				eventNode.next = new Node<>(eventNode, event, null);
				eventNode = eventNode.next;

				//通过遍历数组来实现
//                Message msg = handler.obtainMessage();
//                msg.obj = event;
//                handler.sendMessageDelayed(msg, i <= 0 ? 0 : time - firstTime);
			}

			//通过递归链表来实现
			Message msg = handler.obtainMessage();
			msg.obj = firstEventNode;
			handler.sendMessage(msg);

		}
	}


	public JSONArray addInputEvent(InputEvent ie, Activity activity) {
		if (vSplitX == null || vSplitY == null) {
			Log.e(TAG, "addInputEvent  vSplitX == null || vSplitY == null >> return null;");
			return null;
		}

		int splitX = (int) (vSplitX.getX() + vSplitX.getWidth()/2);
		int splitY = (int) (vSplitY.getY() + vSplitY.getHeight()/2);
		int orientation = activity == null ? Configuration.ORIENTATION_PORTRAIT : activity.getResources().getConfiguration().orientation;

		JSONObject obj = new JSONObject(true);
		obj.put("id", - System.currentTimeMillis());
		obj.put("flowId", flowId);
		obj.put("time", System.currentTimeMillis());
		obj.put("orientation", orientation);
		obj.put("splitX", splitX);
		obj.put("splitY", splitY);
		obj.put("windowWidth", windowWidth);
		obj.put("windowHeight", windowHeight);

		if (ie instanceof KeyEvent) {
			KeyEvent event = (KeyEvent) ie;
			obj.put("type", 1);

			//虽然 KeyEvent 和 MotionEvent 都有，但都不在父类 InputEvent 中 <<<<<<<<<<<<<<<<<<
			obj.put("action", event.getAction());
			obj.put("downTime", event.getDownTime());
			obj.put("eventTime", event.getEventTime());
			obj.put("metaState", event.getMetaState());
			obj.put("source", event.getSource());
			obj.put("deviceId", event.getDeviceId());
			//虽然 KeyEvent 和 MotionEvent 都有，但都不在父类 InputEvent 中 >>>>>>>>>>>>>>>>>>

			obj.put("keyCode", event.getKeyCode());
			obj.put("scanCode", event.getScanCode());
			obj.put("repeatCount", event.getRepeatCount());
			//通过 keyCode 获取的            obj.put("number", event.getNumber());
			obj.put("flags", event.getFlags());
			//通过 mMetaState 获取的 obj.put("modifiers", event.getModifiers());
			//通过 mKeyCode 获取的 obj.put("displayLabel", event.getDisplayLabel());
			//通过 mMetaState 获取的 obj.put("unicodeChar", event.getUnicodeChar());
		}
		else if (ie instanceof MotionEvent) {
			MotionEvent event = (MotionEvent) ie;
			obj.put("type", 0);

			//虽然 KeyEvent 和 MotionEvent 都有，但都不在父类 InputEvent 中 <<<<<<<<<<<<<<<<<<
			obj.put("action", event.getAction());
			obj.put("downTime", event.getDownTime());
			obj.put("eventTime", event.getEventTime());
			obj.put("metaState", event.getMetaState());
			obj.put("source", event.getSource());
			obj.put("deviceId", event.getDeviceId());
			//虽然 KeyEvent 和 MotionEvent 都有，但都不在父类 InputEvent 中 >>>>>>>>>>>>>>>>>>


			obj.put("x", (int) event.getX());
			obj.put("y", (int) event.getY());
			obj.put("rawX", (int) event.getRawX());
			obj.put("rawY", (int) event.getRawY());
			obj.put("size", event.getSize());
			obj.put("pressure", event.getPressure());
			obj.put("xPrecision", event.getXPrecision());
			obj.put("yPrecision", event.getYPrecision());
			obj.put("pointerCount", event.getPointerCount());
			obj.put("edgeFlags", event.getEdgeFlags());
		}

		if (touchList == null) {
			touchList = new JSONArray();
		}
		touchList.add(obj);

		return touchList;
	}




	private static class Node<E> {
		E item;
		Node<E> next;
		Node<E> prev;

		long id;
		long flowId;
		long time;
		int splitX;
		int splitY;
        int splitSize;
		int windowX;
		int windowY;
		int orientation;

        Node(Node<E> prev, E element, Node<E> next) {
			this.item = element;
			this.next = next;
			this.prev = prev;
		}
	}

}
