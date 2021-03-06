package info.guardianproject.bigbuffalo.api;

import android.os.AsyncTask;
import android.util.Log;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Reader;

/**
 * Class for fetching the feed content in the background.
 * 
 */
public class FeedFetcher extends AsyncTask<Feed, Integer, Feed>
{
	public final static String LOGTAG = "FeedFetcher";

	SocialReader socialReader;

	FeedFetchedCallback feedFetchedCallback;

	Feed originalFeed;

	public void setFeedUpdatedCallback(FeedFetchedCallback _feedFetchedCallback)
	{
		feedFetchedCallback = _feedFetchedCallback;
	}

	public interface FeedFetchedCallback
	{
		public void feedFetched(Feed _feed);
	}

	public FeedFetcher(SocialReader _socialReader)
	{
		super();
		socialReader = _socialReader;
	}

	@Override
	protected Feed doInBackground(Feed... params)
	{
		Feed feed = new Feed();
		if (params.length == 0)
		{
			Log.v(LOGTAG, "doInBackground params length is 0");
		}
		else
		{
			Log.v(LOGTAG, "doInBackground: " + params[0].getFeedURL());
			feed = params[0];
			originalFeed = feed;

			Reader reader = new Reader(socialReader, feed);
			feed = reader.fetchFeed();
		}

		socialReader.setFeedAndItemData(feed);

		return feed;
	}

	@Override
	protected void onPostExecute(Feed feed)
	{		
		if (feedFetchedCallback != null)
		{
			feedFetchedCallback.feedFetched(feed);
		}
	}
}
