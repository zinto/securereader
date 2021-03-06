package info.guardianproject.bigbuffalo;

import info.guardianproject.bigbuffalo.adapters.PostDraftsListAdapter;
import info.guardianproject.bigbuffalo.adapters.PostOutgoingListAdapter;
import info.guardianproject.bigbuffalo.adapters.PostPublishedListAdapter;
import info.guardianproject.bigbuffalo.adapters.StoryListAdapter;
import info.guardianproject.bigbuffalo.adapters.PostDraftsListAdapter.PostDraftsListAdapterListener;
import info.guardianproject.bigbuffalo.adapters.StoryListAdapter.OnTagClickedListener;
import info.guardianproject.bigbuffalo.api.SocialReporter;
import info.guardianproject.bigbuffalo.uiutil.UIHelpers;
import info.guardianproject.bigbuffalo.views.StoryListView.StoryListListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import info.guardianproject.bigbuffalo.R;
import com.tinymission.rss.Item;

public class PostListFragment extends Fragment implements PostDraftsListAdapterListener
{
	public static final String ARG_POST_LIST_TYPE = "post_list_type";

	public enum PostListType
	{
		PUBLISHED, OUTGOING, DRAFTS
	};

	public PostListFragment()
	{
	}

	private PostListType mPostListType;
	private ListView mListPosts;
	SocialReporter socialReporter;
	private OnTagClickedListener mOnTagClickedListener;
	private StoryListListener mStoryListListener;
	private String mCurrentTagFilter;

	private TextView mTvTagResults;
	private View mViewTagSearch;
	private View mBtnCloseTagSearch;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		socialReporter = ((App) getActivity().getApplicationContext()).socialReporter;

		mPostListType = PostListType.valueOf((String) getArguments().get(ARG_POST_LIST_TYPE));

		View rootView = inflater.inflate(R.layout.post_list, container, false);

		mListPosts = (ListView) rootView.findViewById(R.id.lvPosts);

		updateListAdapter();

		// Controls for tag search
		mTvTagResults = (TextView) rootView.findViewById(R.id.tvTagResults);
		mViewTagSearch = rootView.findViewById(R.id.llTagSearch);
		mViewTagSearch.setVisibility(View.GONE);
		mBtnCloseTagSearch = rootView.findViewById(R.id.btnCloseTagSearch);
		mBtnCloseTagSearch.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mOnTagClickedListener != null)
					mOnTagClickedListener.onTagClicked(null);
			}
		});

		setTagFilter(mCurrentTagFilter);
		return rootView;
	}

	public void setStoryListListener(StoryListListener listener)
	{
		mStoryListListener = listener;
		if (mListPosts != null && mListPosts.getAdapter() != null)
		{
			if (mListPosts.getAdapter() instanceof StoryListAdapter)
				((StoryListAdapter) mListPosts.getAdapter()).setListener(mStoryListListener);
		}
	}

	public void setOnTagClickedListener(OnTagClickedListener onTagClickedListener)
	{
		mOnTagClickedListener = onTagClickedListener;
		if (mListPosts != null && mListPosts.getAdapter() != null)
		{
			if (mListPosts.getAdapter() instanceof PostPublishedListAdapter)
				((PostPublishedListAdapter) mListPosts.getAdapter()).setOnTagClickedListener(mOnTagClickedListener);
			if (mListPosts.getAdapter() instanceof PostOutgoingListAdapter)
				((PostOutgoingListAdapter) mListPosts.getAdapter()).setOnTagClickedListener(mOnTagClickedListener);
		}
	}

	public void setTagFilter(String tag)
	{
		mCurrentTagFilter = tag;
		if (mViewTagSearch != null)
		{
			if (mCurrentTagFilter == null)
			{
				mViewTagSearch.setVisibility(View.GONE);
			}
			else
			{
				mTvTagResults.setText(UIHelpers.setSpanBetweenTokens(getString(R.string.story_item_short_tag_results, tag), "##", new ForegroundColorSpan(
						getResources().getColor(R.color.accent))));
				mViewTagSearch.setVisibility(View.VISIBLE);
			}
		}

		if (mListPosts != null && mListPosts.getAdapter() != null)
		{
			if (mListPosts.getAdapter() instanceof PostPublishedListAdapter)
				((PostPublishedListAdapter) mListPosts.getAdapter()).setTagFilter(tag);
			if (mListPosts.getAdapter() instanceof PostOutgoingListAdapter)
				((PostOutgoingListAdapter) mListPosts.getAdapter()).setTagFilter(tag);
			if (mListPosts.getAdapter() instanceof PostDraftsListAdapter)
				((PostDraftsListAdapter) mListPosts.getAdapter()).setTagFilter(tag);
		}
	}

	public void updateListAdapter()
	{
		if (mPostListType == PostListType.PUBLISHED)
		{
			PostPublishedListAdapter adapter = new PostPublishedListAdapter(getActivity(), socialReporter.getPosts());
			adapter.setOnTagClickedListener(mOnTagClickedListener);
			adapter.setListener(mStoryListListener);
			adapter.setTagFilter(mCurrentTagFilter);
			mListPosts.setAdapter(adapter);

		}
		else if (mPostListType == PostListType.OUTGOING)
		{
			PostOutgoingListAdapter adapter = new PostOutgoingListAdapter(getActivity(), socialReporter.getDrafts());
			adapter.setOnTagClickedListener(mOnTagClickedListener);
			adapter.setListener(mStoryListListener);
			adapter.setTagFilter(mCurrentTagFilter);
			mListPosts.setAdapter(adapter);
		}
		else if (mPostListType == PostListType.DRAFTS)
		{
			PostDraftsListAdapter adapter = new PostDraftsListAdapter(getActivity(), socialReporter.getDrafts());
			adapter.setPostDraftsListAdapterListener(this);
			adapter.setOnTagClickedListener(mOnTagClickedListener);
			adapter.setListener(mStoryListListener);
			adapter.setTagFilter(mCurrentTagFilter);
			mListPosts.setAdapter(adapter);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateListAdapter();
	}

	@Override
	public void onEditDraft(Item item)
	{
		Intent intent = new Intent(getActivity(), AddPostActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("story", item.getDatabaseId());
		getActivity().startActivity(intent);
	}

	@Override
	public void onDeleteDraft(Item item)
	{
		App.getInstance().socialReporter.deleteDraft(item);
		updateListAdapter();
	}
}