package info.guardianproject.bigbuffalo.api;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings
{
	private final SharedPreferences mPrefs;
	private final boolean mIsFirstRun;

	// Use these constants when listening to changes, to see what property has
	// changed!
	//
	public static final String KEY_LAST_ITEM_EXPIRATION_CHECK_TIME = "last_item_expiration_check_time";
	public static final String KEY_LAST_OPML_CHECK_TIME = "last_opml_check_time";
	public static final String KEY_REQUIRE_TOR = "require_tor";
	public static final String KEY_LAUNCH_REQUIRE_PASSPHRASE = "launch_require_passphrase";
	public static final String KEY_CONTENT_FONT_SIZE_ADJUSTMENT = "content_font_size_adjustment";
	public static final String KEY_WIPE_APP = "wipe_app";
	public static final String KEY_ARTICLE_EXPIRATION = "article_expiration";
	public static final String KEY_SYNC_MODE = "sync_mode";
	public static final String KEY_SYNC_FREQUENCY = "sync_frequency";
	public static final String KEY_SYNC_NETWORK = "sync_network";
	public static final String KEY_READER_SWIPE_DIRECTION = "reader_swipe_direction";
	public static final String KEY_UI_LANGUAGE = "ui_language";
	public static final String KEY_PASSWORD_ATTEMPTS = "num_password_attempts";
	public static final String KEY_CURRENT_PASSWORD_ATTEMPTS = "num_current_password_attempts";
	public static final String KEY_ACCEPTED_POST_PERMISSION = "accepted_post_permission";
	public static final String KEY_XMLRPC_USERNAME = "xmlrpc_username";
	public static final String KEY_XMLRPC_PASSWORD = "xmlrpc_password";
	public static final String KEY_USE_KILL_PASSPHRASE = "use_passphrase";
	public static final String KEY_KILL_PASSPHRASE = "passphrase";
	public static final String KEY_CHAT_SECURE_DIALOG_SHOWN = "chat_secure_dialog_shown";
	public static final String KEY_USERNAME_PASSWORD_CHAT_REGISTERED = "chat_username_password_registered";
		
	public Settings(Context context)
	{
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		mIsFirstRun = mPrefs.getBoolean("firstrunkey", true);

		// If this is first run, set the flag for next time
		if (mIsFirstRun)
			mPrefs.edit().putBoolean("firstrunkey", false).commit();
	}
	
	public void registerChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
	{
		mPrefs.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unregisterChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
	{
		mPrefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	
	/**
	 * @return returns true if this is the first time we start the app
	 * 
	 */
	public boolean isFirstRun()
	{
		return mIsFirstRun;
	}

	/**
	 * @return Gets whether or not a TOR connection is required
	 * 
	 */
	public boolean requireTor()
	{
		return mPrefs.getBoolean(KEY_REQUIRE_TOR, true);
	}

	/**
	 * @return Sets whether a TOR connection is required
	 * 
	 */
	public void setRequireTor(boolean require)
	{
		mPrefs.edit().putBoolean(KEY_REQUIRE_TOR, require).commit();
	}

	/**
	 * @return Gets whether or not a passphrase is required to launch the app
	 * 
	 */
	public boolean launchRequirePassphrase()
	{
		return mPrefs.getBoolean(KEY_LAUNCH_REQUIRE_PASSPHRASE, true);
	}

	/**
	 * @return Sets whether or not a passphrase is required to launch the app
	 * 
	 */
	public void setLaunchRequirePassphrase(boolean require)
	{
		mPrefs.edit().putBoolean(KEY_LAUNCH_REQUIRE_PASSPHRASE, require).commit();
	}

	/**
	 * @return gets the passphrase to unlock app
	 * 
	 */
	public String launchPassphrase()
	{
		return mPrefs.getString("launch_passphrase", null);
	}

	/**
	 * @return Sets the passphrase to unlock app
	 * 
	 */
	public void setLaunchPassphrase(String passphrase)
	{
		mPrefs.edit().putString("launch_passphrase", passphrase).commit();
	}

	/**
	 * @return returns true if we have shown the "swipe up to close story" hint
	 *         dialog
	 * 
	 */
	public boolean hasShownSwipeUpHint()
	{
		return mPrefs.getBoolean("hasshownswipeuphint", false);
	}

	/**
	 * Set (or reset) whether we have shown the "swipe up to close story" hint
	 * dialog.
	 * 
	 */
	public void setHasShownSwipeUpHint(boolean shown)
	{
		mPrefs.edit().putBoolean("hasshownswipeuphint", shown).commit();
	}

	/**
	 * @return returns true if we have shown the menu hint (bounced it out)
	 * 
	 */
	public boolean hasShownMenuHint()
	{
		return mPrefs.getBoolean("hasshownmenuhint", false);
	}

	/**
	 * Set (or reset) whether we have shown the menu hint (bounced out)
	 * 
	 */
	public void setHasShownMenuHint(boolean shown)
	{
		mPrefs.edit().putBoolean("hasshownmenuhint", shown).commit();
	}

	/**
	 * @return returns true if we have shown the help items
	 * 
	 */
	public boolean hasShownHelp()
	{
		return mPrefs.getBoolean("hasshownhelpitems", false);
	}

	/**
	 * Set (or reset) whether we have shown the help items
	 * 
	 */
	public void setHasShownHelp(boolean shown)
	{
		mPrefs.edit().putBoolean("hasshownhelpitems", shown).commit();
	}

	/**
	 * If we have made content text larger or smaller store the adjustment here
	 * (we don't store the absolute size here since we might change the default
	 * font and therefore need to change the default font size as well).
	 * 
	 * @return adjustment we have made to default font size (in sp units)
	 * 
	 */
	public int getContentFontSizeAdjustment()
	{
		return mPrefs.getInt(KEY_CONTENT_FONT_SIZE_ADJUSTMENT, 0);
	}

	/**
	 * Set the adjustment for default font size in sp units (positive means
	 * larger, negative means smaller)
	 * 
	 */
	public void setContentFontSizeAdjustment(int adjustment)
	{
		mPrefs.edit().putInt(KEY_CONTENT_FONT_SIZE_ADJUSTMENT, adjustment).commit();
	}

	/**
	 * @return Gets whether we should wipe the entire app (as opposed to only
	 *         the user data)
	 * 
	 */
	public boolean wipeApp()
	{
		return mPrefs.getBoolean(KEY_WIPE_APP, false);
	}

	/**
	 * @return Sets whether we should wipe the entire app (as opposed to only
	 *         the user data)
	 * 
	 */
	public void setWipeApp(boolean wipeApp)
	{
		mPrefs.edit().putBoolean(KEY_WIPE_APP, wipeApp).commit();
	}

	public enum ArticleExpiration
	{
		Never, OneDay, OneWeek, OneMonth
	}

	/**
	 * @return Gets when articles are expired
	 * 
	 */
	public ArticleExpiration articleExpiration()
	{
		return Enum.valueOf(ArticleExpiration.class, mPrefs.getString(KEY_ARTICLE_EXPIRATION, ArticleExpiration.Never.name()));
	}
	
	public long articleExpirationMillis() {
		if (articleExpiration() == ArticleExpiration.OneDay) {
			return   86400000L;
		} else if (articleExpiration() == ArticleExpiration.OneWeek) {
			return  604800000L;
		} else if (articleExpiration() == ArticleExpiration.OneWeek) {
			return 2592000000L;
		} else {
			return -1L;
		}
	}

	/**
	 * @return Sets when articles are expired
	 * 
	 */
	public void setArticleExpiration(ArticleExpiration articleExpiration)
	{
		mPrefs.edit().putString(KEY_ARTICLE_EXPIRATION, articleExpiration.name()).commit();
	}

	public enum SyncFrequency
	{
		Manual, WhenRunning, InBackground
	}

	/**
	 * @return Gets when to sync feeds
	 * 
	 */
	public SyncFrequency syncFrequency()
	{
		// return Enum.valueOf(SyncFrequency.class,
		// mPrefs.getString(KEY_SYNC_FREQUENCY, SyncFrequency.Manual.name()));
		return Enum.valueOf(SyncFrequency.class, mPrefs.getString(KEY_SYNC_FREQUENCY, SyncFrequency.WhenRunning.name()));
	}

	/**
	 * @return Sets when to sync feeds. Can be manual, when app is running or
	 *         when in background.
	 * 
	 */
	public void setSyncFrequency(SyncFrequency syncFreqency)
	{
		mPrefs.edit().putString(KEY_SYNC_FREQUENCY, syncFreqency.name()).commit();
	}

	public enum SyncMode
	{
		BitWise, LetItFlow
	}

	/**
	 * @return Gets whether or not to download media automatically or only on
	 *         demand
	 * 
	 */
	public SyncMode syncMode()
	{
		// return Enum.valueOf(SyncMode.class, mPrefs.getString(KEY_SYNC_MODE,
		// SyncMode.LetItFlow.name()));
		return Enum.valueOf(SyncMode.class, mPrefs.getString(KEY_SYNC_MODE, SyncMode.BitWise.name()));
	}

	/**
	 * @return Sets whether or not to download media automatically or only on
	 *         demand
	 * 
	 */
	public void setSyncMode(SyncMode syncMode)
	{
		mPrefs.edit().putString(KEY_SYNC_MODE, syncMode.name()).commit();
	}

	public enum SyncNetwork
	{
		WifiAndMobile, WifiOnly
	}

	/**
	 * @return Gets over which networks that sync is available
	 * 
	 */
	public SyncNetwork syncNetwork()
	{
		return Enum.valueOf(SyncNetwork.class, mPrefs.getString(KEY_SYNC_NETWORK, SyncNetwork.WifiAndMobile.name()));
	}

	/**
	 * @return Sets over which networks that sync is available
	 * 
	 */
	public void setSyncNetwork(SyncNetwork syncNetwork)
	{
		mPrefs.edit().putString(KEY_SYNC_NETWORK, syncNetwork.name()).commit();
	}

	public enum ReaderSwipeDirection
	{
		Rtl, Ltr, Automatic
	}

	/**
	 * @return Gets how swipes are handled in full screen story view
	 * 
	 */
	public ReaderSwipeDirection readerSwipeDirection()
	{
		return Enum.valueOf(ReaderSwipeDirection.class, mPrefs.getString(KEY_READER_SWIPE_DIRECTION, ReaderSwipeDirection.Automatic.name()));
	}

	/**
	 * @return Sets how swipes are handled in full screen story view
	 * 
	 */
	public void setReaderSwipeDirection(ReaderSwipeDirection readerSwipeDirection)
	{
		mPrefs.edit().putString(KEY_READER_SWIPE_DIRECTION, readerSwipeDirection.name()).commit();
	}

	public enum UiLanguage
	{
		English, Farsi
	}

	/**
	 * @return gets the app ui language
	 * 
	 */
	public UiLanguage uiLanguage()
	{
		String ret = mPrefs.getString(KEY_UI_LANGUAGE, null);
		if (ret != null)
		{
			return Enum.valueOf(UiLanguage.class, ret);
		}
		
		// Is default system language arabic?
		String defaultLanguage = Locale.getDefault().getLanguage();
		if (defaultLanguage == "ar")
			return UiLanguage.Farsi;
		return UiLanguage.English;
	}

	/**
	 * @return Sets the app ui language
	 * 
	 */
	public void setUiLanguage(UiLanguage language)
	{
		mPrefs.edit().putString(KEY_UI_LANGUAGE, language.name()).commit();
	}

	/**
	 * @return Get number of failed password attempts before content is wiped!
	 * 
	 */
	public int numberOfPasswordAttempts()
	{
		return mPrefs.getInt(KEY_PASSWORD_ATTEMPTS, 3);
	}

	/**
	 * Set number of failed password attempts before content is wiped!
	 * 
	 */
	public void setNumberOfPasswordAttempts(int attempts)
	{
		mPrefs.edit().putInt(KEY_PASSWORD_ATTEMPTS, attempts).commit();
	}

	/**
	 * @return Get current number of failed password attempts
	 * 
	 */
	public int currentNumberOfPasswordAttempts()
	{
		return mPrefs.getInt(KEY_CURRENT_PASSWORD_ATTEMPTS, 0);
	}

	/**
	 * Set current number of failed password attempts
	 * 
	 */
	public void setCurrentNumberOfPasswordAttempts(int attempts)
	{
		mPrefs.edit().putInt(KEY_CURRENT_PASSWORD_ATTEMPTS, attempts).commit();
	}

	/**
	 * @return Gets whether or not we have accepted post permission
	 * 
	 */
	public boolean acceptedPostPermission()
	{
		return mPrefs.getBoolean(KEY_ACCEPTED_POST_PERMISSION, false);
	}

	/**
	 * @return Sets whether or not we have accepted post permission
	 * 
	 */
	public void setAcceptedPostPermission(boolean accepted)
	{
		mPrefs.edit().putBoolean(KEY_ACCEPTED_POST_PERMISSION, accepted).commit();
	}
	
	/**
	 * @return Gets whether or not to use a kill passphrase that will wipe the app on login
	 * 
	 */
	public boolean useKillPassphrase()
	{
		return mPrefs.getBoolean(KEY_USE_KILL_PASSPHRASE, false);
	}

	/**
	 * @return Sets whether or not to use a kill passphrase that will wipe the app on login
	 * 
	 */
	public void setUseKillPassphrase(boolean use)
	{
		mPrefs.edit().putBoolean(KEY_USE_KILL_PASSPHRASE, use).commit();
	}

	/**
	 * @return gets the kill passphrase that will wipe the app on login
	 * 
	 */
	public String killPassphrase()
	{
		return mPrefs.getString(KEY_KILL_PASSPHRASE, null);
	}

	/**
	 * @return Sets the kill passphrase that will wipe the app on login
	 * 
	 */
	public void setKillPassphrase(String passphrase)
	{
		mPrefs.edit().putString(KEY_KILL_PASSPHRASE, passphrase).commit();
	}
	
	public long lastOPMLCheckTime() {
		return mPrefs.getLong(KEY_LAST_OPML_CHECK_TIME, 0);
	}
	
	public void setLastOPMLCheckTime(long lastCheckTime) {
		mPrefs.edit().putLong(KEY_LAST_OPML_CHECK_TIME, lastCheckTime).commit();
	}
	
	public long lastItemExpirationCheckTime() {
		return mPrefs.getLong(KEY_LAST_ITEM_EXPIRATION_CHECK_TIME, 0);		
	}
	
	public void setLastItemExpirationCheckTime(long lastCheckTime) {
		mPrefs.edit().putLong(KEY_LAST_ITEM_EXPIRATION_CHECK_TIME, lastCheckTime).commit();
	}

	/**
	 * @return number of times we have shown ChatSecure download dialog
	 * 
	 */
	public int chatSecureDialogShown()
	{
		return mPrefs.getInt(KEY_CHAT_SECURE_DIALOG_SHOWN, 0);
	}
	
	/**
	 * Set number of times we have shown ShatSecure download dialog
	 * 
	 */
	public void setChatSecureDialogShown(int numTimes)
	{
		mPrefs.edit().putInt(KEY_CHAT_SECURE_DIALOG_SHOWN, numTimes).commit();
	}
	
	public boolean chatUsernamePasswordSet() {
		return mPrefs.getBoolean(KEY_USERNAME_PASSWORD_CHAT_REGISTERED, false);
	}
	
	public void setChatUsernamePasswordSet() {
		mPrefs.edit().putBoolean(KEY_USERNAME_PASSWORD_CHAT_REGISTERED, true).commit();
	}
}