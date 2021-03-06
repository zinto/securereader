package info.guardianproject.bigbuffalo;

import info.guardianproject.bigbuffalo.api.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.bigbuffalo.api.Settings.SyncMode;
import info.guardianproject.bigbuffalo.api.SocialReader;
import info.guardianproject.bigbuffalo.api.SyncService;
import info.guardianproject.bigbuffalo.models.FeedFilterType;
import info.guardianproject.bigbuffalo.ui.ActionProviderFeedFilter;
import info.guardianproject.bigbuffalo.ui.UICallbackListener;
import info.guardianproject.bigbuffalo.ui.UICallbacks;
import info.guardianproject.bigbuffalo.ui.UICallbacks.OnCallbackListener;
import info.guardianproject.bigbuffalo.views.StoryListHintTorView;
import info.guardianproject.bigbuffalo.views.StoryListHintTorView.OnButtonClickedListener;
import info.guardianproject.bigbuffalo.views.StoryListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class MainActivity extends ItemExpandActivity implements OnSharedPreferenceChangeListener
{
	public static String HOCKEY_APP_ID = "629f97e71c12c857c0201e374e394bb7";

	public static String INTENT_EXTRA_SHOW_THIS_TYPE = "info.guardianproject.bigbuffalo.showThisFeedType";
	public static String INTENT_EXTRA_SHOW_THIS_FEED = "info.guardianproject.bigbuffalo.showThisFeedId";
	public static String INTENT_EXTRA_SHOW_THIS_ITEM = "info.guardianproject.bigbuffalo.showThisItemId";

	public static String LOGTAG = "MainActivity";

	private boolean mIsInitialized;
	private long mShowItemId;
	private long mShowFeedId;
	private FeedFilterType mShowFeedFilterType;
	SocialReader socialReader;

	OnCallbackListener mCallbackListener;

	/*
	 * The action bar menu item for the "TAG" option. Only show this when a feed
	 * filter is set.
	 */
	MenuItem mMenuItemTag;
	boolean mShowTagMenuItem;
	MenuItem mMenuItemShare;
	MenuItem mMenuItemFeed;

	ActionProviderFeedFilter mAPFeedFilter;
	FeedFilterType mFeedFilterType;

	StoryListView mStoryListView;
	ArrayList<Item> mRecentItems;

	ArrayList<Item> mPopularItems;
	boolean mPopularItemsAreFaked; // True if the popular stories are only
									// "examples"

	ArrayList<Item> mFavoriteItems;
	boolean mFavoriteItemsAreFaked; // True if it is only "examples", not
									// our real favorites

	ArrayList<Item> mSharedItems;

	boolean mIsLoading;
	private SyncMode mCurrentSyncMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mCurrentSyncMode = App.getSettings().syncMode();

		// We do a little song and dance number here - This activity's theme is
		// set to NoActionBar in the manifest, but here we change to default app
		// theme again and request the action bar. This is because, at first
		// startup, the system will show a screen with default action bar and
		// default background. We don't want that. Instead we want to show solid
		// color (same as lock screen background) and no action bar. See
		// AppThemeNoActionBar theme for more information.
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setTheme(R.style.AppTheme);

		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();

		addUICallbackListener();

		setContentView(R.layout.activity_main);
		setMenuIdentifier(R.menu.activity_main);

		mStoryListView = (StoryListView) findViewById(R.id.storyList);
		mStoryListView.setListener(this);

		socialReader = ((App) getApplicationContext()).socialReader;
		socialReader.setSyncServiceListener(new SyncService.SyncServiceListener()
		{
			@Override
			public void syncEvent(SyncService.SyncTask syncTask)
			{
				Log.v(LOGTAG, "Got a syncEvent");
				if (syncTask.type == SyncService.SyncTask.TYPE_FEED && syncTask.status == SyncService.SyncTask.FINISHED)
				{
					refreshSelectedFeedOrAll(false);
				}
			}
		});

		// socialReader.goOnline(this);
		showRecent(false);
		createFeedSpinner();

		checkForUpdates();

	}

	private void createFeedSpinner()
	{
		mAPFeedFilter = new ActionProviderFeedFilter(this);
		getSupportActionBar().setCustomView(mAPFeedFilter.onCreateActionView());
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		removeUICallbackListener();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// If we are in the process of displaying the lock screen isFinishing is
		// actually
		// true, so avoid extra work!
		if (!isFinishing())
		{
			// If we have not shown help yet, open that on top
			if (!App.getSettings().hasShownHelp())
			{
				Intent intent = new Intent(this, HelpActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra("useLeftSideMenu", false);
				startActivity(intent);
			}
			else
			{
				if (!mIsInitialized)
				{
					mIsInitialized = true;
					UICallbacks.setFeedFilter(FeedFilterType.ALL_FEEDS, 0, MainActivity.this);
					getSupportActionBar().show();
				}
			}

		}

		addSettingsChangeListener();

		// Called with flags of which item to show?
		Intent intent = getIntent();
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_ITEM) && intent.hasExtra(INTENT_EXTRA_SHOW_THIS_FEED))
		{
			this.mShowFeedId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_FEED, 0);
			this.mShowItemId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_ITEM, 0);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_FEED);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_ITEM);
		}
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_TYPE))
		{
			this.mShowFeedFilterType = (FeedFilterType) intent.getSerializableExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
		}
		else if (socialReader.getDefaultFeedId() >= 0) 
		{
			this.mShowFeedId = socialReader.getDefaultFeedId();
		}
		else
		{
			this.mShowFeedFilterType = null;
		}

		if (this.mShowFeedFilterType != null)
		{
			Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_TYPE was set, show type " + this.mShowFeedFilterType.toString());
			UICallbacks.setFeedFilter(this.mShowFeedFilterType, -1, MainActivity.this);
			this.mShowFeedFilterType = null;
		}
		else if (this.mShowFeedId != 0)
		{
			Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_FEED was set, show feed id " + this.mShowFeedId);
			UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, this.mShowFeedId, MainActivity.this);
		}

		// Resume sync if we are back from Orbot
		updateTorView();

		checkForCrashes();

	}

	private void checkForCrashes()
	{
		CrashManager.register(this, HOCKEY_APP_ID);
	}

	private void checkForUpdates()
	{
		// Remove this for store builds!
		UpdateManager.register(this, HOCKEY_APP_ID);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		removeSettingsChangeListener();
	}

	@Override
	protected void onAfterResumeAnimation()
	{
		super.onAfterResumeAnimation();
		if (!isFinishing() && App.getSettings().hasShownHelp())
		{
			boolean willShowMenuHint = false;
			if (mLeftSideMenu != null)
				willShowMenuHint = mLeftSideMenu.showMenuHintIfNotShown();
			if (socialReader.getFeedsList().size() > 0)
			{
				if (willShowMenuHint)
				{
					// Allow the menu animation some time before we start the
					// heavy work!
					mStoryListView.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							refreshSelectedFeedOrAll(true);
						}
					}, 6000);
				}
				else
				{
					refreshSelectedFeedOrAll(true);
				}
			}
		}

	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void syncSpinnerToCurrentItem()
	{
		if (mFeedFilterType == FeedFilterType.ALL_FEEDS)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_all_feeds));
		else if (mFeedFilterType == FeedFilterType.POPULAR)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_popular));
		else if (mFeedFilterType == FeedFilterType.SHARED)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_shared_stories));
		else if (mFeedFilterType == FeedFilterType.FAVORITES)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_favorites));
		else
			mAPFeedFilter.setCurrentTitle(App.getInstance().m_activeFeed.getTitle());
	}

	private Feed getFeedById(long idFeed)
	{
		ArrayList<Feed> items = socialReader.getSubscribedFeedsList();
		for (Feed feed : items)
		{
			if (feed.getDatabaseId() == idFeed)
				return feed;
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		// Find the tag menu item. Only to be shown when feed filter is set!
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		mMenuItemTag.setVisible(mShowTagMenuItem);

		mMenuItemShare = menu.findItem(R.id.menu_share);

		// Locate MenuItem with ShareActionProvider
		// mMenuItemFeed = menu.findItem(R.id.menu_feed);
		// if (mMenuItemFeed != null)
		// {
		// mMenuItemFeed.setActionProvider(new ActionProviderFeedFilter(this));
		// }

		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_tag:
		{
			showTagSearchPopup(getSupportActionBar().getCustomView());
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	private void setTagItemVisible(boolean bVisible)
	{
		mShowTagMenuItem = bVisible && App.UI_ENABLE_TAGS;
		if (mMenuItemTag != null)
		{
			mMenuItemTag.setVisible(mShowTagMenuItem);
			if (Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
		}
	}

	@SuppressLint("NewApi")
	private void addUICallbackListener()
	{
		mCallbackListener = new UICallbackListener()
		{
			@SuppressLint("NewApi")
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				if (type == FeedFilterType.ALL_FEEDS)
				{
					setTagItemVisible(false);
					App.getInstance().m_activeFeed = null;
					showRecent(false);
					refreshSelectedFeedOrAll(true);
				}
				else if (type == FeedFilterType.POPULAR)
				{
					setTagItemVisible(false);
					App.getInstance().m_activeFeed = null;
					showPopular(false);
				}
				else if (type == FeedFilterType.FAVORITES)
				{
					setTagItemVisible(false);
					App.getInstance().m_activeFeed = null;
					showFavorites(false);
				}
				else if (type == FeedFilterType.SHARED)
				{
					setTagItemVisible(false);
					App.getInstance().m_activeFeed = null;
					showShared(false);
					refreshSelectedFeedOrAll(true);
				}
				else if (type == FeedFilterType.SINGLE_FEED)
				{
					Feed feed = getFeedById(feedId);
					if (feed != null)
					{
						setTagItemVisible(true);
						App.getInstance().m_activeFeed = feed;
						if (mFeedFilterType != FeedFilterType.SINGLE_FEED)
							showRecent(false);
						refreshSelectedFeedOrAll(true);
					}
					else
					{
						// Null and show all feeds
						setTagItemVisible(false);
						App.getInstance().m_activeFeed = null;
						mRecentItems = null;
						showRecent(false);
						mFeedFilterType = FeedFilterType.ALL_FEEDS;
					}
				}
				syncSpinnerToCurrentItem();

				// TEMP TEMP TEMP
				// showError("Error message!!!");
			}

			@Override
			public void onItemFavoriteStatusChanged(Item item)
			{
				// An item has been marked/unmarked as favorite. Update the list
				// of favorites to pick
				// up this change!
				updateFavoriteItems();
			}

			@Override
			public void onCommand(int command, Bundle commandParameters)
			{
				switch (command)
				{
				case R.integer.command_add_feed_manual:
				{
					// First add it to reader!
					App.getInstance().socialReader.addFeedByURL(commandParameters.getString("uri"), MainActivity.this.mFeedFetchedCallback);
					showRecent(false);
					break;
				}
				}
			}

			@Override
			public void onRequestResync(Feed feed)
			{
				onResync(feed, (mFeedFilterType == FeedFilterType.ALL_FEEDS || mFeedFilterType == FeedFilterType.SINGLE_FEED)
						&& App.getInstance().m_activeFeed == feed); // Only show
																	// spinner
																	// if
																	// updating
																	// current
																	// feed
			}
		};
		UICallbacks.getInstance().addListener(mCallbackListener);
	}

	private void removeUICallbackListener()
	{
		if (mCallbackListener != null)
			UICallbacks.getInstance().removeListener(mCallbackListener);
		mCallbackListener = null;
	}

	private void addSettingsChangeListener()
	{
		App.getSettings().registerChangeListener(this);
	}

	private void removeSettingsChangeListener()
	{
		App.getSettings().unregisterChangeListener(this);
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = (LayoutInflater) this.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.story_search_by_tag, null, false), this.mStoryListView.getWidth(),
					this.mStoryListView.getHeight(), true);

			ListView lvTags = (ListView) mMenuPopup.getContentView().findViewById(R.id.lvTags);

			String[] rgTags = new String[0];
			// rgTags[0] = "#one";
			// rgTags[1] = "#two";
			// rgTags[2] = "#three";
			// rgTags[3] = "#four";

			ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.story_search_by_tag_item, R.id.tvTag, rgTags);
			lvTags.setAdapter(adapter);
			lvTags.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
				{
					String tag = (String) arg0.getAdapter().getItem(position);
					UICallbacks.setTagFilter(tag, null);
					mMenuPopup.dismiss();
				}
			});

			EditText editTag = (EditText) mMenuPopup.getContentView().findViewById(R.id.editTag);
			editTag.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
				{
					if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH)
					{
						UICallbacks.setTagFilter(v.getText().toString(), null);
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			mMenuPopup.showAsDropDown(anchorView);
			mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuPopup.dismiss();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onResync()
	{
		if (mFeedFilterType == FeedFilterType.SHARED)
			this.refreshSelectedFeedOrAll(true);
		else
			onResync(App.getInstance().m_activeFeed, true);
	}

	private void onResync(Feed feed, boolean showLoadingSpinner)
	{
		if (socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_TOR)
		{
			socialReader.connectTor(this);
		}

		if (socialReader.isOnline() == SocialReader.ONLINE)
		{
			setIsLoading(showLoadingSpinner);
			if (feed == null)
				socialReader.manualSyncSubscribedFeeds(mFeedFetchedCallback);
			else
				socialReader.manualSyncFeed(feed, mFeedFetchedCallback);
		}
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		if (mMenuItemFeed != null)
			mMenuItemFeed.setVisible(!fullscreen);
		if (mMenuItemShare != null)
			mMenuItemShare.setVisible(!fullscreen);

		if (!fullscreen)
		{
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			setDisplayHomeAsUp(false);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			setDisplayHomeAsUp(true);
		}
	}

	class RefreshFeedsTask extends AsyncTask<Object, Void, Void>
	{
		FeedFilterType type;
		ArrayList<Feed> listOfFeeds;

		@Override
		protected Void doInBackground(Object... values)
		{
			type = (FeedFilterType) values[0];

			Log.v(LOGTAG, "RefreshFeedsTask: doInBackground");
			if (type == FeedFilterType.SHARED)
			{
				listOfFeeds = socialReader.getAllShared();
			}
			else if (type == FeedFilterType.FAVORITES)
			{
				listOfFeeds = socialReader.getAllFavorites();
			}
			else if (type == FeedFilterType.ALL_FEEDS || App.getInstance().m_activeFeed == null)
			{
				Log.v(LOGTAG, "RefreshFeedsTask: all subscribed");
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getSubscribedFeedItems());
			}
			else
			{
				Log.v(LOGTAG, "RefreshFeedsTask: " + App.getInstance().m_activeFeed.getTitle());
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getFeed(App.getInstance().m_activeFeed));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			Log.v(LOGTAG, "RefreshFeedsTask: finished");
			processFeed(type, listOfFeeds);
		}
	}

	/**
	 * Depending on if "All feeds" or a specific feed is selected in the feed
	 * filter go ahead and call "getFeed" or "getAllFeeds" accordingly. Also,
	 * make sure to show loading indicator.
	 */
	@SuppressLint("NewApi")
	private void refreshSelectedFeedOrAll(boolean showProgressIndicator)
	{
		// If we are on a specific feed, make sure that it is still "valid",
		// i.e. that we are still subscribing
		// to it (we might have removed it since last time here)
		if (mFeedFilterType == FeedFilterType.SINGLE_FEED && App.getInstance().m_activeFeed != null)
		{
			long activeFeed = App.getInstance().m_activeFeed.getDatabaseId();
			boolean bFoundIt = false;

			ArrayList<Feed> subscribedFeeds = App.getInstance().socialReader.getSubscribedFeedsList();
			for (Feed feed : subscribedFeeds)
			{
				if (feed.getDatabaseId() == activeFeed)
				{
					bFoundIt = true;
					break;
				}
			}
			if (!bFoundIt)
			{
				// No longer subscribed. We'll fall back to "All items"
				mFeedFilterType = FeedFilterType.ALL_FEEDS;
				App.getInstance().m_activeFeed = null;
				syncSpinnerToCurrentItem();
			}
		}

		if (showProgressIndicator)
			setIsLoading(true);

		RefreshFeedsTask refreshTask = new RefreshFeedsTask();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			refreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedFilterType);
		else
			refreshTask.execute(mFeedFilterType);
	}

	private void setIsLoading(boolean isLoading)
	{
		mIsLoading = isLoading;
		if (mStoryListView != null)
			mStoryListView.setIsLoading(mIsLoading);
		updateTorView();
	}

	private void showError(String error)
	{
		if (mStoryListView != null)
		{
			if (TextUtils.isEmpty(error))
				mStoryListView.hideError();
			else
				mStoryListView.showError(error);
		}
	}

	private void showRecent(boolean isUpdate)
	{
		Log.v(LOGTAG, "showRecent " + isUpdate);

		if (App.getInstance().m_activeFeed != null)
			mFeedFilterType = FeedFilterType.SINGLE_FEED;
		else
			mFeedFilterType = FeedFilterType.ALL_FEEDS;
		if (mStoryListView != null)
		{
			int headerViewId = 0;
			if (mRecentItems == null || mRecentItems.size() == 0)
			{
				headerViewId = R.layout.story_list_hint_tor;
			}
			mStoryListView.updateItems(this, mRecentItems, headerViewId, isUpdate);
		}
	}

	public void showFavorites(boolean isUpdate)
	{
		mFeedFilterType = FeedFilterType.FAVORITES;
		boolean shouldShowAddFavoriteHint = mFavoriteItemsAreFaked || mFavoriteItems == null || mFavoriteItems.size() == 0;
		if (mStoryListView != null)
			mStoryListView.updateItems(this, mFavoriteItems, shouldShowAddFavoriteHint ? R.layout.story_list_hint_add_favorite : 0, isUpdate);
	}

	public void showPopular(boolean isUpdate)
	{
		mFeedFilterType = FeedFilterType.POPULAR;
		boolean shouldShowNoPopularHint = mPopularItemsAreFaked || mPopularItems == null || mPopularItems.size() == 0;
		if (mStoryListView != null)
			mStoryListView.updateItems(this, mPopularItems, shouldShowNoPopularHint ? R.layout.story_list_hint_no_popular : 0, isUpdate);
	}

	public void showShared(boolean isUpdate)
	{
		mFeedFilterType = FeedFilterType.SHARED;
		boolean shouldShowNoSharedHint = mSharedItems == null || mSharedItems.size() == 0;
		if (mStoryListView != null)
			mStoryListView.updateItems(this, mSharedItems, shouldShowNoSharedHint ? R.layout.story_list_hint_no_shared : 0, isUpdate);
	}

	private void processFeed(FeedFilterType type, ArrayList<Feed> listOfFeeds)
	{
		Log.v(LOGTAG, "feedFetched");

		if (type == FeedFilterType.ALL_FEEDS || type == FeedFilterType.SINGLE_FEED)
		{
			Feed _feed = listOfFeeds.get(0);
			if (App.getInstance().m_activeFeed != null && App.getInstance().m_activeFeed.getDatabaseId() == _feed.getDatabaseId())
				App.getInstance().m_activeFeed = _feed; // need to update to get
														// NetworkPullDate

			boolean isAllFeeds = (mFeedFilterType == FeedFilterType.ALL_FEEDS && type == FeedFilterType.ALL_FEEDS && _feed.getDatabaseId() == -1);
			boolean isThisFeed = (type == FeedFilterType.SINGLE_FEED && App.getInstance().m_activeFeed != null && _feed.getDatabaseId() == App.getInstance().m_activeFeed
					.getDatabaseId());

			Log.v(LOGTAG, "processFeed - isAllFeeds:" + isAllFeeds + " isThisFeed:" + isThisFeed);
			if (isAllFeeds || isThisFeed)
			{
				mRecentItems = _feed.getItems();
				Log.v(LOGTAG, "have " + mRecentItems.size() + " items");
				if (mFeedFilterType == FeedFilterType.ALL_FEEDS || mFeedFilterType == FeedFilterType.SINGLE_FEED)
				{
					showRecent(true);
					checkShowStoryFullScreen(mRecentItems);
					if (!isAllFeeds)
					{
						showErrorForFeed(_feed, !mIsLoading);
					}
					else
					{
						this.showError(null);
						for (Feed feed : App.getInstance().socialReader.getSubscribedFeedsList())
						{
							if (showErrorForFeed(feed, !mIsLoading))
								break;
						}
					}
				}
			}
			else
			{
				// Ignore, we are not showing this feed right now!
				Log.v(LOGTAG, "No ui update for feed");
			}
		}
		else if (type == FeedFilterType.FAVORITES)
		{
			updateFavoriteItems();
		}
		else if (type == FeedFilterType.POPULAR)
		{
			updatePopularItems();
		}
		else if (type == FeedFilterType.SHARED)
		{
			this.mSharedItems = flattenFeedArray(listOfFeeds);
			if (mFeedFilterType == FeedFilterType.SHARED)
			{
				showShared(true);
				checkShowStoryFullScreen(mSharedItems);
			}
		}

		setIsLoading(false);
	}

	private void checkShowStoryFullScreen(ArrayList<Item> items)
	{
		if (mShowItemId != 0)
		{
			Log.v(LOGTAG, "Loaded feed and INTENT_EXTRA_SHOW_THIS_ITEM was set to " + mShowItemId + ". Try to show it");
			for (int itemIndex = 0; itemIndex < items.size(); itemIndex++)
			{
				Item item = items.get(itemIndex);
				if (item.getDatabaseId() == mShowItemId)
				{
					Log.v(LOGTAG, "Found item at index " + itemIndex);
					this.openStoryFullscreen(items, itemIndex, mStoryListView.getListView(), null);
				}
			}
			mShowFeedId = 0;
			mShowItemId = 0;
		}
	}

	private ArrayList<Item> flattenFeedArray(ArrayList<Feed> listOfFeeds)
	{
		ArrayList<Item> items = new ArrayList<Item>();
		if (listOfFeeds != null)
		{
			Iterator<Feed> itFeed = listOfFeeds.iterator();
			while (itFeed.hasNext())
			{
				Feed feed = itFeed.next();
				Log.v(LOGTAG, "Adding " + feed.getItemCount() + " items");
				items.addAll(feed.getItems());
			}
		}
		Log.v(LOGTAG, "There are " + items.size() + " items total");
		return items;
	}

	private boolean showErrorForFeed(Feed feed, boolean onlyRemoveIfAllOk)
	{
		if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_404)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_404));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_bad_url));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_unknown));
		}
		else
		{
			this.showError(null);
			return false;
		}
		return true;
	}

	private final FeedFetchedCallback mFeedFetchedCallback = new FeedFetchedCallback()
	{
		@Override
		public void feedFetched(Feed _feed)
		{
			Log.v(LOGTAG, "feedFetched Callback");
			ArrayList<Feed> listOfFeeds = new ArrayList<Feed>();
			listOfFeeds.add(_feed);
			processFeed((_feed == null || _feed.getDatabaseId() == Feed.DEFAULT_DATABASE_ID) ? FeedFilterType.ALL_FEEDS : FeedFilterType.SINGLE_FEED,
					listOfFeeds);
		}
	};
	private StoryListHintTorView mTorView;

	private void updateFavoriteItems()
	{
		ArrayList<Item> favoriteItems = new ArrayList<Item>();

		// Get our favorites from the reader.
		//
		ArrayList<Feed> favItemsPerFeed = App.getInstance().socialReader.getAllFavorites();
		if (favItemsPerFeed != null)
		{
			Iterator<Feed> itFeed = favItemsPerFeed.iterator();
			while (itFeed.hasNext())
			{
				Feed feed = itFeed.next();
				favoriteItems.addAll(feed.getItems());
			}
		}

		boolean fakedItems = false;
		if (favoriteItems.size() == 0)
		{
			// No real favorites, so we fake some by randomly picking Items from
			// the "all items" feed
			fakedItems = true;

			Feed allSubscribed = App.getInstance().socialReader.getSubscribedFeedItems();
			if (allSubscribed != null)
			{
				ArrayList<Item> allSubscribedItems = allSubscribed.getItems();

				// Truncate to random 5 items
				Collections.shuffle(allSubscribedItems);
				favoriteItems.addAll(allSubscribedItems.subList(0, Math.min(5, allSubscribedItems.size())));
			}
		}

		mFavoriteItems = favoriteItems;
		mFavoriteItemsAreFaked = fakedItems;

		if (mFeedFilterType == FeedFilterType.FAVORITES)
			showFavorites(true);
	}

	private void updatePopularItems()
	{
		ArrayList<Item> items = null;

		Feed allSubscribed = App.getInstance().socialReader.getSubscribedFeedItems();
		if (allSubscribed != null)
		{
			items = allSubscribed.getItems();
		}

		ArrayList<Item> popularItems = new ArrayList<Item>(items);
		Iterator<Item> it = popularItems.iterator();
		while (it.hasNext())
		{
			Item item = it.next();
			// TODO - implementing comments we need to fix this
			// if (item.getComments().size() == 0)
			it.remove();
		}

		boolean fakedPopular = false;

		// TODO - how many popular should we show? Ordered by what, number of
		// comments?

		if (popularItems.size() == 0)
		{
			// No comments for any story. Let's add some dummies based on title
			// length! =)
			fakedPopular = true;

			popularItems = (ArrayList<Item>) items.clone();
			Collections.sort(popularItems, new Comparator<Item>()
			{
				@Override
				public int compare(Item i1, Item i2)
				{
					if (i1.equals(i2))
						return 0;
					else if (i1.getTitle() == null && i2.getTitle() == null)
						return 0;
					else if (i1.getTitle() == null)
						return 1;
					else if (i2.getTitle() == null)
						return -1;
					return (i1.getTitle().length() >= i2.getTitle().length()) ? -1 : 0;
				}
			});

			// Truncate to 5 items
			for (int i = popularItems.size() - 1; i >= 5; i--)
			{
				popularItems.remove(i);
			}
		}

		mPopularItems = popularItems;
		mPopularItemsAreFaked = fakedPopular;
	}

	@Override
	protected boolean onCommand(int command, Bundle commandParameters)
	{
		if (command == R.integer.command_resync)
		{
			onResync();
			return true;
		}
		return super.onCommand(command, commandParameters);
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
		if (resIdHeader == R.layout.story_list_hint_tor)
		{
			mTorView = (StoryListHintTorView) headerView;
			updateTorView();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.v(LOGTAG, "The setting " + key + " has changed.");
		// if (Settings.KEY_SYNC_MODE.equals(key))
		// {
		// }
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		// Probably opening a popup (Feed Spinner). Remember what sync mode was
		// set to when we open.
		if (!hasFocus)
		{
			mCurrentSyncMode = App.getSettings().syncMode();
		}
		else
		{
			if (mCurrentSyncMode != App.getSettings().syncMode())
			{
				mCurrentSyncMode = App.getSettings().syncMode();
				refreshSelectedFeedOrAll(false);
			}
		}
	}

	private void updateTorView()
	{
		if (mTorView == null)
			return;

		if (!App.getSettings().requireTor() || mIsLoading)
		{
			mTorView.setVisibility(View.GONE);
		}
		else
		{
			mTorView.setOnButtonClickedListener(new OnButtonClickedListener()
			{
				private StoryListHintTorView mView;

				@Override
				public void onNoNetClicked()
				{
					int onlineMode = App.getInstance().socialReader.isOnline();
					mView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
							onlineMode == SocialReader.ONLINE);
				}

				@Override
				public void onGoOnlineClicked()
				{
					onResync();
				}

				public OnButtonClickedListener init(StoryListHintTorView view)
				{
					mView = view;
					return this;
				}
			}.init(mTorView));
			int onlineMode = App.getInstance().socialReader.isOnline();
			mTorView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
					onlineMode == SocialReader.ONLINE);
			mTorView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, -1, this);
	}

	@Override
	public void onCacheWordOpened()
	{
		super.onCacheWordOpened();
		socialReader = ((App) getApplicationContext()).socialReader;
		// socialReader.goOnline(this);

		createFeedSpinner();
		syncSpinnerToCurrentItem();
		// setActionBarTitle(getString(R.string.feed_filter_all_feeds));
	}
}
