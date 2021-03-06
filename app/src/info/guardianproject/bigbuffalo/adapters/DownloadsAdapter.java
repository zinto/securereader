package info.guardianproject.bigbuffalo.adapters;

import info.guardianproject.bigbuffalo.App;
import info.guardianproject.bigbuffalo.MainActivity;
import info.guardianproject.bigbuffalo.R;
import info.guardianproject.bigbuffalo.ui.MediaViewCollection;
import info.guardianproject.bigbuffalo.uiutil.AnimationHelpers;
import info.guardianproject.bigbuffalo.views.StoryMediaContentView;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tinymission.rss.Item;

public class DownloadsAdapter extends BaseAdapter
{
	private static final int VIEW_TYPE_HEADER_COMPLETE = 0;
	private static final int VIEW_TYPE_HEADER_IN_PROGRESS = 1;
	private static final int VIEW_TYPE_ITEM_COMPLETE = 2;
	private static final int VIEW_TYPE_ITEM_IN_PROGRESS = 3;

	private static final ArrayList<Long> gComplete = new ArrayList<Long>();
	private static final HashMap<Long, Integer> gInProgress = new HashMap<Long, Integer>();
	private static DownloadsAdapter gInstance;

	private final Context mContext;

	public static DownloadsAdapter getInstance(Context context)
	{
		if (gInstance == null)
			gInstance = new DownloadsAdapter(context);
		return gInstance;
	}

	private DownloadsAdapter(Context context)
	{
		mContext = context;
	}

	@Override
	public int getCount()
	{
		int num = 0;
		if (this.getNumComplete() > 0)
		{
			num += this.getNumComplete() + 1;
		}
		num += 1; // For "In progress" header
		num += this.getNumInProgress();
		return num;
	}

	@Override
	public int getItemViewType(int position)
	{
		int numComplete = this.getNumComplete();
		int numInProgress = this.getNumInProgress();
		if (numComplete > 0)
		{
			if (position == 0)
				return VIEW_TYPE_HEADER_COMPLETE;
			position -= 1;
			if (position < numComplete)
				return VIEW_TYPE_ITEM_COMPLETE;
			position -= numComplete;
		}
		if (position == 0)
			return VIEW_TYPE_HEADER_IN_PROGRESS;
		position -= 1;
		if (position < numInProgress)
			return VIEW_TYPE_ITEM_IN_PROGRESS;
		return -1;
	}

	@Override
	public int getViewTypeCount()
	{
		return 4;
	}

	@Override
	public Object getItem(int position)
	{
		int numComplete = this.getNumComplete();
		int numInProgress = this.getNumInProgress();

		if (numComplete > 0)
		{
			if (position == 0)
				return null; // "Complete" header
			position -= 1;
			if (position < numComplete)
				return this.getCompleteAtIndex(position);
			position -= numComplete;
		}
		if (position == 0)
			return null; // "In progress" header
		position -= 1;
		if (position < numInProgress)
			return this.getInProgressAtIndex(position);
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		Item item = (Item) getItem(position);
		if (item != null)
			return item.getDatabaseId();
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		int type = this.getItemViewType(position);
		if (convertView != null
				&& (convertView.getTag() == null || !(convertView.getTag() instanceof Integer) || ((Integer) convertView.getTag()).intValue() != type))
			convertView = null;

		View returnView = null;

		switch (type)
		{
		case VIEW_TYPE_HEADER_COMPLETE:
		{
			if (convertView == null)
				convertView = createHeaderView();
			TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
			tvTitle.setText(R.string.downloads_complete);
			returnView = convertView;
			break;
		}
		case VIEW_TYPE_HEADER_IN_PROGRESS:
		{
			if (convertView == null)
				convertView = createHeaderView();
			TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
			tvTitle.setText(R.string.downloads_in_progress);
			returnView = convertView;
			break;
		}
		case VIEW_TYPE_ITEM_COMPLETE:
		{
			if (convertView == null)
				convertView = createItemCompleteView();
			populateItemCompleteView(convertView, (Item) getItem(position));
			returnView = convertView;
			break;
		}
		case VIEW_TYPE_ITEM_IN_PROGRESS:
		{
			if (convertView == null)
				convertView = createItemInProgressView();
			populateItemInProgressView(convertView, (Item) getItem(position));
			returnView = convertView;
			break;
		}

		}

		if (returnView != null)
			returnView.setTag(Integer.valueOf(type));
		return returnView;
	}

	private View createHeaderView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_header, null);
		return view;
	}

	private View createItemCompleteView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_item_complete, null);
		return view;
	}

	private View createItemInProgressView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_item_in_progress, null);
		return view;
	}

	private void populateItemCompleteView(View view, Item item)
	{
		StoryMediaContentView mediaView = (StoryMediaContentView) view.findViewById(R.id.mediaContentView);
		MediaViewCollection collection = new MediaViewCollection(mContext, null, item, false);
		mediaView.setMediaCollection(collection, false, true);

		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		tvTitle.setText(item.getTitle());

		view.setOnClickListener(new View.OnClickListener()
		{
			private Item mItem;

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(mContext, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM, mItem.getDatabaseId());
				intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED, mItem.getFeedId());
				mContext.startActivity(intent);
			}

			public View.OnClickListener init(Item item)
			{
				mItem = item;
				return this;
			}
		}.init(item));
	}

	private void populateItemInProgressView(View view, Item item)
	{
		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		tvTitle.setText(item.getTitle());

		View operationButtons = view.findViewById(R.id.llOperationButtons);
		operationButtons.setVisibility(View.GONE);
		AnimationHelpers.fadeOut(operationButtons, 0, 0, false);

		View menuView = view.findViewById(R.id.ivMenu);
		menuView.setOnClickListener(new View.OnClickListener()
		{
			private View mOperationView;

			@Override
			public void onClick(View v)
			{
				if (mOperationView.getVisibility() == View.GONE)
				{
					mOperationView.setVisibility(View.VISIBLE);
					AnimationHelpers.fadeIn(mOperationView, 500, 5000, false);
				}
			}

			public View.OnClickListener init(View operationView)
			{
				mOperationView = operationView;
				return this;
			}
		}.init(operationButtons));
	}

	public static int getNumComplete()
	{
		Log.v("DownloadsAdapter", "getNumComplete");
		return gComplete.size();
	}

	private Item getCompleteAtIndex(int index)
	{
		Log.v("DownloadsAdapter", "getCompleteAtIndex " + index);
		Long l = gComplete.get(index);
		return App.getInstance().socialReader.getItemFromId(l.longValue());
	}

	public static int getNumInProgress()
	{
		Log.v("DownloadsAdapter", "getNumInProgress");
		return gInProgress.size();
	}

	private Item getInProgressAtIndex(int index)
	{
		Log.v("DownloadsAdapter", "getInProgressAtIndex " + index);
		Long l = (Long) gInProgress.keySet().toArray()[index];
		return App.getInstance().socialReader.getItemFromId(l.longValue());
	}

	public static void downloading(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		if (gComplete.contains(itemLong))
			gComplete.remove(itemLong);
		Integer current = gInProgress.get(itemLong);
		if (current == null)
			gInProgress.put(itemLong, Integer.valueOf(1));
		else
			gInProgress.put(itemLong, Integer.valueOf(current.intValue() + 1));
		if (gInstance != null)
			gInstance.notifyDataSetChanged();
	}

	public static void downloaded(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		Integer current = gInProgress.get(itemLong);
		if (current != null)
		{
			int i = current.intValue();
			if (i == 1)
			{
				// Done!
				gInProgress.remove(itemLong);
				gComplete.add(itemLong);
				if (gInstance != null)
					gInstance.notifyDataSetChanged();
			}
			else
			{
				gInProgress.put(itemLong, Integer.valueOf(i - 1));
			}
		}
	}

	public static void viewed(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		if (gComplete.contains(itemLong))
		{
			gComplete.remove(itemLong);
			if (gInstance != null)
				gInstance.notifyDataSetChanged();
		}
	}
}
