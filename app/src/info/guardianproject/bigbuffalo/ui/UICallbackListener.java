package info.guardianproject.bigbuffalo.ui;

import info.guardianproject.bigbuffalo.models.FeedFilterType;
import info.guardianproject.bigbuffalo.ui.UICallbacks.OnCallbackListener;
import android.os.Bundle;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class UICallbackListener implements OnCallbackListener
{
	/**
	 * Empty implementation of onFeedSelect.
	 * 
	 * @see OnCallbackListener#onFeedSelect(boolean, int, Object).
	 */
	@Override
	public void onFeedSelect(FeedFilterType type, long feedId, Object source)
	{
	}

	@Override
	public void onTagSelect(String tag)
	{
	}

	@Override
	public void onRequestResync(Feed feed)
	{
	}

	@Override
	public void onItemFavoriteStatusChanged(Item item)
	{
	}

	@Override
	public void onCommand(int command, Bundle commandParameters)
	{
	}
}
