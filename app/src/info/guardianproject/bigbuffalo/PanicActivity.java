package info.guardianproject.bigbuffalo;

import info.guardianproject.bigbuffalo.R;

import info.guardianproject.bigbuffalo.api.SocialReader;
import info.guardianproject.bigbuffalo.uiutil.AnimationHelpers;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class PanicActivity extends Activity implements OnTouchListener
{
	private View mArrow;
	private ImageView mSymbol;
	private boolean mOnlyTesting;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.activity_panic);

		mOnlyTesting = getIntent().getBooleanExtra("testing", false);

		mArrow = findViewById(R.id.arrowSymbolView);

		mSymbol = (ImageView) findViewById(R.id.radioactiveSymbolView);
		mSymbol.setOnTouchListener(this);

		View btnCancel = findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				PanicActivity.this.finish();
			}
		});

		View btnSettings = findViewById(R.id.btnSettings);
		btnSettings.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(PanicActivity.this, SettingsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra("go_to_group", R.id.groupPanicAction);
				PanicActivity.this.startActivity(intent);
				PanicActivity.this.finish();
			}
		});

		TextView textHint = (TextView) findViewById(R.id.textHint);
		if (App.getSettings().wipeApp())
			textHint.setText(R.string.panic_hint);
		else
			textHint.setText(R.string.panic_hint_wipe_content);
	}

	public int yMaxTranslation;
	public int yTranslationArrow;
	public int yCurrentTranslation;
	public int yDelta;
	public int yOriginal;
	public Rect mArrowRect;
	public boolean mIsOverArrow = false;

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		if (view == mSymbol)
		{
			final int X = (int) event.getRawX();
			final int Y = (int) event.getRawY();
			switch (event.getAction() & MotionEvent.ACTION_MASK)
			{
			case MotionEvent.ACTION_DOWN:
				FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams) view.getLayoutParams();
				yOriginal = lParams.topMargin;
				yDelta = Y - lParams.topMargin;
				mIsOverArrow = false;

				mArrowRect = new Rect();
				if (!mArrow.getGlobalVisibleRect(mArrowRect))
				{
					mArrowRect = null;
				}
				else
				{
					Rect symbolRect = new Rect();
					if (mSymbol.getGlobalVisibleRect(symbolRect))
					{
						yMaxTranslation = mArrowRect.bottom - symbolRect.bottom;
						yTranslationArrow = mArrowRect.top - symbolRect.bottom;
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			{
				mSymbol.setColorFilter(null);
				if (mIsOverArrow)
				{
					AnimationHelpers.scale(mSymbol, 1.0f, 0, 200, new Runnable()
					{
						@Override
						public void run()
						{
							doWipe();
						}
					});
				}
				else
				{
					AnimationHelpers.translateY(mSymbol, yCurrentTranslation, 0, 200);
				}
				mIsOverArrow = false;
				break;
			}

			case MotionEvent.ACTION_POINTER_DOWN:
				break;
			case MotionEvent.ACTION_POINTER_UP:

				break;
			case MotionEvent.ACTION_MOVE:
			{
				yCurrentTranslation = Math.max(0, Math.min(Y - yDelta, yMaxTranslation));
				AnimationHelpers.translateY(mSymbol, yCurrentTranslation, yCurrentTranslation, 0);

				if (yCurrentTranslation >= yTranslationArrow)
					mIsOverArrow = true;
				else
					mIsOverArrow = false;
				setSymbolColor(mIsOverArrow);
				break;
			}
			}
			view.invalidate();
			return true;
		}
		return false;
	}

	private void setSymbolColor(boolean isOverArrow)
	{
		if (isOverArrow)
			mSymbol.setColorFilter(0xffff0000);
		else
			mSymbol.setColorFilter(null);
	}

	private void doWipe()
	{
		if (mOnlyTesting)
		{
			Builder alert = new AlertDialog.Builder(this).setTitle(R.string.app_name)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
							PanicActivity.this.finish();
						}
					}).setMessage(R.string.panic_test_successful);
			alert.show();
		}
		else
		{
			// Do the wipe
			App.getInstance().wipe(App.getSettings().wipeApp() ? SocialReader.FULL_APP_WIPE : SocialReader.DATA_WIPE);

			Intent intent = new Intent(getApplicationContext(), KillActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			PanicActivity.this.finish();
		}
	}
}
