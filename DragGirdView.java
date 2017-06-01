package com.lovehouse.view;

import java.util.ArrayList;
import java.util.List;

import com.lovehouse.R;
import com.lovehouse.adapter.FindPurifierGridAdapter;
import com.lovehouse.util.Data;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

/**
*yang
*create 2014.10
**/
@SuppressLint("NewApi")
public class DragGirdView extends GridView implements
		android.widget.AdapterView.OnItemLongClickListener {
	private FindPurifierGridAdapter adapter;
	private boolean isDarging;
	private OnItemLongClickListener listener;
	private ImageView dragImg; // 拖动的img
	private WindowManager.LayoutParams dragImgLp; // 被拖动的img的layoutparams
	private WindowManager manager;

	// 全屏手指的坐标
	private float rawX;
	private float rawY;
	// 当前控件是指的坐标
	private float x;
	private float y;

	private int itemWidth; // item的宽
	private int itemHight; // item的高

	private View selectView; // 呗选中的view

	private LinearLayout llDelete;

	private WindowManager.LayoutParams delLp; // 删除

	private Animation animIn; // delbar 进入
	private Animation animOut; // delbar 出去
	private Animation animRock; // delbar 删除时晃动
	private RelativeLayout rl;

	private ImageView deleImg; // shanchu的图标

	private int selectedPosition;

	private int numColunm;
	// 动画是否正在播放
	private boolean isAnimCanPlay = true;
	// 是否删除
	private boolean isDelete = false;
	// 震动
	private Vibrator vibrator;

	public DragGirdView(Context context) {
		super(context);
		init();
	}

	public DragGirdView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DragGirdView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		manager = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
		// 震动初始化
		vibrator = (Vibrator) getContext().getSystemService(
				Context.VIBRATOR_SERVICE);
		animIn = AnimationUtils.loadAnimation(getContext(),
				R.anim.grid_delete_bar_in);
		animOut = AnimationUtils.loadAnimation(getContext(),
				R.anim.grid_delete_bar_out);
		animRock = AnimationUtils.loadAnimation(getContext(),
				R.anim.grid_delbar_rock);
		animRock.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				rl.startAnimation(animOut);
			}
		});
		animOut.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
			}

			@Override
			public void onAnimationEnd(Animation arg0) {
				manager.removeView(llDelete);
				deleImg.setImageResource(R.drawable.del_selected);
			}
		});
		initDeleteBar();
		super.setOnItemLongClickListener(this);

	}

	private void startDrag(View view) {
		// 会话中的缓存
		view.setDrawingCacheEnabled(true);
		// 获得item项的镜像位图
		Bitmap bitmap = view.getDrawingCache();
		// 把图片放大
		bitmap = Bitmap.createScaledBitmap(bitmap,
				(int) (bitmap.getWidth() * 1.2),
				(int) (bitmap.getHeight() * 1.2), true);
		itemWidth = bitmap.getWidth();
		itemHight = bitmap.getHeight();
		// 初始化拖动的图片
		dragImg = new ImageView(getContext());
		dragImg.setImageBitmap(bitmap);
		dragImgLp = new WindowManager.LayoutParams();
		// 设置显示背景为透明
		dragImgLp.format = PixelFormat.TRANSPARENT;
		// 显示位置
		dragImgLp.gravity = Gravity.TOP | Gravity.LEFT;
		// 设置宽高
		dragImgLp.width = WindowManager.LayoutParams.WRAP_CONTENT;
		dragImgLp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		// (FLAG_NOT_FOCUSABLE不能获得焦点，FLAG_NOT_TOUCHABLE不能触摸)
		dragImgLp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		dragImgLp.x = (int) (rawX - itemWidth / 2);
		dragImgLp.y = (int) (rawY - itemHight / 2);
		manager.addView(dragImg, dragImgLp);
		// 隐藏原本的被长按项
		view.setVisibility(View.GONE);
	}

	@Override
	public void setOnItemLongClickListener(
			android.widget.AdapterView.OnItemLongClickListener listener) {
		this.listener = listener;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if (arg2 == adapter.getCount() - 1) {
			return false;
		}
		isDarging = true;
		if (listener != null) {
			listener.onItemLongClick(arg0, arg1, arg2, arg3);
		}
		selectView = arg1;
		selectedPosition = arg2;
		adapter.exChangeHidenItem(arg2);
		// 判断是否在震动
		if (vibrator.hasVibrator()) {
			vibrator.vibrate(50);
		}
		showDelete();
		startDrag(arg1);
		return false;
	}

	private void initDeleteBar() {
		llDelete = new LinearLayout(getContext());
		rl = new RelativeLayout(getContext());
		deleImg = new ImageView(getContext());
		deleImg.setImageResource(R.drawable.del_selected);
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(50,
				50);
		rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
		// 添加图片到rl
		rl.addView(deleImg, rlp);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 60);
		rl.setBackgroundColor(Color.parseColor("#a0000000"));
		llDelete.addView(rl, llp);
		delLp = new WindowManager.LayoutParams();
		delLp.format = PixelFormat.TRANSPARENT;
		delLp.gravity = Gravity.TOP | Gravity.LEFT;
		delLp.width = WindowManager.LayoutParams.MATCH_PARENT;
		delLp.height = WindowManager.LayoutParams.WRAP_CONTENT;
	}

	private void showDelete() {
		manager.addView(llDelete, delLp);
		rl.startAnimation(animIn);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 获得手指按下的位置
			rawX = ev.getRawX();
			rawY = ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			if (isDarging) {
				float dx = ev.getRawX() - rawX;
				float dy = ev.getRawY() - rawY;
				if (dx != 0 && dy != 0) {
					rawX = ev.getRawX();
					rawY = ev.getRawY();
					x = ev.getX();
					y = ev.getY();
					draging();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			endDrag();
			break;
		}
		return super.onTouchEvent(ev);
	}

	private void endDrag() {
		if (isDarging) {
			isDarging = false;
			if (rawY < 60) {
				deleImg.startAnimation(animRock);
				if (selectedPosition == adapter.getCount() - 1) {
					adapter.removeItem(selectedPosition);
					selectedPosition = -1;
					adapter.exChangeHidenItem(-1);
				} else {
					isDelete = true;
					movePostion(adapter.getCount() - 1);
				}
			} else {
				rl.startAnimation(animOut);
				selectedPosition = -1;
				adapter.exChangeHidenItem(-1);
				;
			}
			manager.removeView(dragImg);
		}
	}

	private void draging() {
		dragImgLp.x = (int) (rawX - itemWidth / 2);
		dragImgLp.y = (int) (rawY - itemHight / 2);
		manager.updateViewLayout(dragImg, dragImgLp);
		if (rawY < 60) {
			deleImg.setImageResource(R.drawable.del_red);
		} else {
			deleImg.setImageResource(R.drawable.del_selected);
		}
		int newPosition = pointToPosition((int) x, (int) y);
		if (newPosition == adapter.getCount() - 1) {
			newPosition -= 1;
		}
		movePostion(newPosition);
	}

	private void movePostion(final int newPosition) {
		if (newPosition != selectedPosition && newPosition != -1
				&& isAnimCanPlay) {
			isAnimCanPlay = false;
			adapter.exChangePosition(selectedPosition, newPosition);
			adapter.exChangeHidenItem(newPosition);
			final ViewTreeObserver observer = getViewTreeObserver();
			observer.addOnPreDrawListener(new OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);
					startAnimation(selectedPosition, newPosition);
					selectedPosition = newPosition;
					return false;
				}
			});
		}
	}

	public void setAdapter(ListAdapter adapter) {
		if (adapter instanceof FindPurifierGridAdapter) {
			this.adapter = (FindPurifierGridAdapter) adapter;
		} else {
			throw new IllegalArgumentException(
					"adapter should extends GridAdapter");
		}
		super.setAdapter(adapter);
	}

	@Override
	// 得到宽、高、列数
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		numColunm = getNumColumns();
	}

	public void startAnimation(int oldPosition, int newPosition) {
		List<Animator> animators = new ArrayList<Animator>();
		if (newPosition > oldPosition) {
			for (int i = oldPosition; i < newPosition; i++) {
				View view = getChildAt(i - getFirstVisiblePosition());
				Animator anim = null;
				if (i % numColunm == numColunm - 1) {
					anim = createAnimation(view, -view.getWidth()
							* (numColunm - 1), 0, view.getHeight(), 0);
				} else {
					anim = createAnimation(view, view.getWidth(), 0, 0, 0);
				}
				animators.add(anim);
			}
		} else {
			for (int i = oldPosition; i > newPosition; i--) {
				View view = getChildAt(i - getFirstVisiblePosition());
				Animator anim = null;
				if (i % numColunm == 0) {
					anim = createAnimation(view, view.getWidth()
							* (numColunm - 1), 0, -view.getHeight(), 0);
				} else {
					anim = createAnimation(view, -view.getWidth(), 0, 0, 0);
				}
				animators.add(anim);
			}
		}
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(animators);
		animatorSet.setDuration(700);
		animatorSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator arg0) {

			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				isAnimCanPlay = true;
				Log.i("aaaa", "==ddd===" + isDelete);
				if (isDelete) {
					isDelete = false;
					adapter.removeItem(selectedPosition);
					selectedPosition = -1;
					adapter.exChangeHidenItem(-1);
				}
			}

			@Override
			public void onAnimationCancel(Animator arg0) {
			}
		});
		animatorSet.start();
	}

	private Animator createAnimation(View view, float fromX, float toX,
			float fromY, float toY) {
		ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX",
				fromX, toX);
		ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY",
				fromY, toY);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(animX, animY);
		return animatorSet;
	}

}
