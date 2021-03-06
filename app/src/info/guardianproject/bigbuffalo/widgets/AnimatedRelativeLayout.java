package info.guardianproject.bigbuffalo.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

public class AnimatedRelativeLayout extends RelativeLayout
{
	private SparseArray<Rect> mStartPositions;
	private SparseArray<Rect> mEndPositions;
	private float mAnimationValue;
	private boolean mAnimating;

	public AnimatedRelativeLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public AnimatedRelativeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public AnimatedRelativeLayout(Context context)
	{
		super(context);
	}

	public void setStartPositions(SparseArray<Rect> startPositions)
	{
		mStartPositions = startPositions;
	}

	public boolean isAnimating()
	{
		return mAnimating;
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (mStartPositions != null && mStartPositions.size() > 0)
		{
			mEndPositions = new SparseArray<Rect>();

			// Since the RelativeLayout clips children, set the "real" position
			// of the view to the
			// leftmost and topmost coordinate and size to the biggest size.
			// Then use
			// translation for the animations. When the animation is done we can
			// reset the actual
			// view size and position again.
			for (int index = 0; index < mStartPositions.size(); index++)
			{
				int id = mStartPositions.keyAt(index);

				View view = findViewById(id);
				if (view != null)
				{
					LayoutParams lpView = (LayoutParams) view.getLayoutParams();
					Rect currentRect = new Rect(lpView.leftMargin, lpView.topMargin, lpView.leftMargin + view.getWidth(), lpView.topMargin + view.getHeight());
					mEndPositions.put(id, currentRect);

					Rect startRect = mStartPositions.get(id);
					LayoutParams lp = (LayoutParams) view.getLayoutParams();
					lp.height = Math.max(currentRect.height(), startRect.height());
					lp.leftMargin = Math.min(currentRect.left, startRect.left);
					lp.topMargin = Math.min(currentRect.top, startRect.top);
					view.setLayoutParams(lp);
				}
			}

			final LayoutAnim anim = new LayoutAnim();
			anim.setDuration(1000);
			anim.setFillBefore(true);
			anim.setFillAfter(true);
			anim.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationEnd(Animation animation)
				{
					post(new Runnable()
					{
						@Override
						public void run()
						{
							resetAnimatedProperties();
						}
					});
				}

				@Override
				public void onAnimationRepeat(Animation animation)
				{
				}

				@Override
				public void onAnimationStart(Animation animation)
				{
				}
			});
			mAnimationValue = 0;
			mAnimating = true;
			this.startAnimation(anim);
		}
	}

	private void resetAnimatedProperties()
	{
		mAnimating = false;
		for (int index = 0; index < mStartPositions.size(); index++)
		{
			int id = mStartPositions.keyAt(index);

			View view = findViewById(id);
			if (view != null)
			{
				Rect endRect = mEndPositions.get(id);
				LayoutParams lp = (LayoutParams) view.getLayoutParams();
				lp.height = endRect.height();
				lp.leftMargin = endRect.left;
				lp.topMargin = endRect.top;
				view.setLayoutParams(lp);
			}
		}
		mStartPositions = null;
		this.clearAnimation();
	}

	public class LayoutAnim extends Animation
	{
		public LayoutAnim()
		{
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			mAnimationValue = interpolatedTime;
			invalidate();
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
		}

		@Override
		public boolean willChangeBounds()
		{
			return false;
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime)
	{
		int sc = Integer.MAX_VALUE;

		if (isAnimating())
		{
			if (child.getId() != View.NO_ID)
			{
				if (mStartPositions.indexOfKey(child.getId()) >= 0)
				{
					sc = canvas.save();

					Rect startRect = mStartPositions.get(child.getId());
					Rect endRect = mEndPositions.get(child.getId());

					boolean invertedLeft = endRect.left < startRect.left;
					boolean invertedTop = endRect.top < startRect.top;

					int height = (int) (startRect.height() + (mAnimationValue * (endRect.height() - startRect.height())));

					int leftDelta = (int) ((invertedLeft ? (1 - mAnimationValue) : (mAnimationValue)) * (endRect.left - startRect.left));
					int topDelta = (int) ((invertedTop ? (1 - mAnimationValue) : (mAnimationValue)) * (endRect.top - startRect.top));

					if (invertedLeft)
						leftDelta = -leftDelta;
					if (invertedTop)
						topDelta = -topDelta;
					canvas.clipRect(child.getLeft() + leftDelta, child.getTop() + topDelta, child.getRight() + leftDelta, child.getTop() + topDelta + height,
							Op.INTERSECT);
					canvas.translate(leftDelta, topDelta);
				}
			}
		}
		boolean ret = super.drawChild(canvas, child, drawingTime);

		// Restore to count?
		if (sc != Integer.MAX_VALUE)
			canvas.restoreToCount(sc);

		return ret;
	}
}
