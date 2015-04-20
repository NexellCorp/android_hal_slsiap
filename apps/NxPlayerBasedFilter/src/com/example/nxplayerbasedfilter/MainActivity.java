// THIS IS FITLER NEW VERSION

//------------------------------------------------------------------------------
//
//	Copyright (C) 2014 Nexell Co. All Rights Reserved
//	Nexell Co. Proprietary & Confidential
//
//	NEXELL INFORMS THAT THIS CODE AND INFORMATION IS PROVIDED "AS IS" BASE
//  AND	WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING
//  BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
//  FOR A PARTICULAR PURPOSE.
//
//	Module		:
//	File		:
//	Description	:
//	Author		: 
//	Export		:
//	History		:
//
//------------------------------------------------------------------------------

package com.example.nxplayerbasedfilter;

import android.os.Build;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.util.Log;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.text.Collator;

import android.content.Intent;
import android.content.Context;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.res.Resources;

import android.app.ListActivity;
import android.view.LayoutInflater;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.drawable.Drawable;
import android.widget.RelativeLayout;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends ListActivity {
	private static final String DBG_TAG = "MainActivity";

	private static final boolean SUPPORT_THUMBNAIL	= true;
	private static final boolean DEBUG_ISTVOUT		= false;

	private static final String[] STORAGE_PATH 		= {
		"/storage/sdcard0",
		"/storage/sdcard1",
	};

	private static final String THUMBNAIL_PATH		= "/storage/sdcard0/.nxthumbnail/";
	private static final int 	THUMBNAIL_WIDTH		= 128;
	private static final int 	THUMBNAIL_HEIGHT	=  72;

	private static final String TVOUT_STATUS_FILE	= "/sys/class/switch/tvout/state";
	private static final String TVOUT_CONTROL_FILE	= "/sys/devices/platform/tvout/enable";
	
	private static final String[] VIDEO_EXTENSION = { 
		".avi",		".wmv",		".wmp",		".wm",		".asf",
		".mpg",		".mpeg",	".mpe",		".m1v",		".m2v",
		".mpv2",	".mp2v",	/*".dat",*/	".ts",		".tp",
		".tpr",		".trp", 	".vob", 	".ifo", 	".ogm",
		".ogv",		".mp4",		".m4v",		".m4p",		".m4b",
		".3gp",		".3gpp",	".3g2",		".3gp2",	".mkv",
		".rm",		".ram",		".rmvb",	".rpm",		".flv",
		".swf",		".mov",		".qt",		".amr",		".nsv",
		".dpg",		".m2ts",	".m2t",		".mts",		".dvr-ms",
		".k3g",		".skm",		".evo",		".nsr",		".amv",
		".divx",	".webm",	".wtv",		".f4v",	
	};
	
	private static final String[] AUDIO_EXTENSION = {
		"wav",		"wma",		"mpa",		"mp2",		"m1a",
		"m2a",		"mp3",		"ogg",		"m4a",		"aac",
		"mka",		"ra",		"flac",		"ape",		"mpc",
		"mod",		"ac3",		"eac3",		"dts",		"dtshd",
		"wv",		"tak", 
	};
	
	public static Context mContext;
	
	ArrayList<MediaInfo> mMediaInfo;
	static int	mItemPos;

	String 	mNetworkStreamName;
	boolean mNetworkStream;
	
	static boolean bVisibleVersion = false;
	
	// boolean mScroll		= false;
	int		mViewItemHeadPos = 0;
	int		mViewItemTailPos = 0;

	ThumbnailThread mThumbnailThread;
	MediaInfoAdapter mAdapter;

	private Handler	mHandler;

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// Overriding Function
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		FUNCIN();
		
		super.onCreate(savedInstanceState);
		setContentView( R.layout.activity_main );
		
	    // TODO Auto-generated method stub
		if( !bVisibleVersion ) {
			try {
				String packageVersion;
				PackageInfo info = this.getPackageManager().getPackageInfo( this.getPackageName(), 0 );
				packageVersion = info.versionName;
				
				Toast.makeText(MainActivity.this, "Application Version : " + packageVersion, Toast.LENGTH_LONG).show();
				Log.i(DBG_TAG, "Application Version : " + packageVersion);
			} catch( NameNotFoundException e ) {
				Toast.makeText(MainActivity.this, "Invalid Package name", Toast.LENGTH_LONG).show();
			}
			
			bVisibleVersion = true;
		}
		
		mContext = this;
		mMediaInfo = new ArrayList<MediaInfo>();

		for( int i = 0; i < STORAGE_PATH.length; i++ ) {
			File dir = new File( STORAGE_PATH[i] );
			if( dir.isDirectory() )
				UpdateFileList( STORAGE_PATH[i] );
		}

		Collections.sort( mMediaInfo , mComparator );

		if( SUPPORT_THUMBNAIL )
		{
		 	// Check Thumbnail Directory
		 	File dirThumbnail = new File( THUMBNAIL_PATH );
		 	if( !dirThumbnail.exists() )
		 	{
		 		Log.v( DBG_TAG, "Create Thumbnail directory : " + THUMBNAIL_PATH );
		 		dirThumbnail.mkdir();
		 	}
		}
		
		mAdapter = new MediaInfoAdapter( this, R.layout.listview_row, mMediaInfo );
		setListAdapter( mAdapter );
		setSelection( mItemPos );

		if( SUPPORT_THUMBNAIL )
		{
			mHandler = new Handler() {
				public void handleMessage( Message msg )
				{
					mAdapter.notifyDataSetChanged();
				}
			};

			mThumbnailThread = new ThumbnailThread();
			mThumbnailThread.start();
		}

		FUNCOUT();
	}

	private final static Comparator<MediaInfo> mComparator = new Comparator<MediaInfo>() {
		private final Collator collator = Collator.getInstance();
		
		@Override
		public int compare(MediaInfo object1, MediaInfo object2) {
			// Sorting File Name 
			//return collator.compare(object1.GetFileName(), object2.GetFileName());
			
			// Sorting Full path
			return collator.compare(object1.GetFilePath(), object2.GetFilePath());
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean enable = IsTVOut(); 
		menu.findItem(R.id.main_tvout).setChecked(enable);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if( id == R.id.main_networkstream ) {
			AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
			final EditText editText = new EditText(this);
			
			alertDlg.setTitle( "Network Stream" );
			alertDlg.setView(editText);
			alertDlg.setMessage(
				"Input URL:\n  Example: http://www.example.com/sample.mkv")
				.setCancelable(false)
				.setPositiveButton("Open", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// Open Action
						mNetworkStreamName = editText.getText().toString().trim();
						if( mNetworkStreamName.isEmpty() != true ) {
							mNetworkStream = true;

							if( SUPPORT_THUMBNAIL )
							{
								if( mThumbnailThread.isAlive() ){
									mThumbnailThread.interrupt();
									try {  
										mThumbnailThread.join();
									} catch(InterruptedException e) {
									}
								}
							}

							Toast.makeText(MainActivity.this, mNetworkStreamName, Toast.LENGTH_SHORT).show();
							Intent intent = new Intent( MainActivity.this, PlayerActivity.class);
							startActivity(intent);
							finish();
						}
						else {
							Toast.makeText(MainActivity.this, "Invalid Netwrok URL.", Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					}
				})
				.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
			alertDlg.show();
			return true;
		}
		else if( id == R.id.main_tvout ) {
			boolean enable = !IsTVOut(); 
			EnableTVOut( enable );
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		FUNCIN();
		
		mItemPos = position;
		mNetworkStream = false;

		if( SUPPORT_THUMBNAIL )
		{
			if( mThumbnailThread.isAlive() ){
				mThumbnailThread.interrupt();
				try {  
					mThumbnailThread.join();
				} catch(InterruptedException e) {
				}
			}
		}

		Toast.makeText(MainActivity.this, mMediaInfo.get(position).GetFilePath(), Toast.LENGTH_SHORT).show();
		
		Intent intent = new Intent( MainActivity.this, PlayerActivity.class );
		startActivity(intent);
		finish();
		
		FUNCOUT();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		FUNCIN();
		FUNCOUT();
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	FileList
	//
	private void UpdateFileList( String rootPath )
	{
		File rootFile = new File( rootPath );
		
		if( !rootFile.exists() ) {
			Log.w(DBG_TAG, "Invalid Directory Path. (" + rootPath + ")" );
			return;
		}
		
		if( rootFile.isDirectory() ) {
			String[] rootList = rootFile.list();
			
			if( null == rootList )
				return ;

			for( int i = 0; i < rootList.length; i++ )
			{
				String subPath = rootPath + "/" + rootList[i];
				File subFile = new File( subPath );
				String[] subList = subFile.list();
				
				if( subFile.isDirectory() )
				{
					for( int j = 0; j < subList.length; j++ )
					{
						UpdateFileList( subPath + "/" + subList[j] );
					}
				}
				else {
					if( isVideo(subPath) ) {
						MediaInfo info = new MediaInfo( subPath, subFile.getName(), null, MEDIA_TAG.VIDEO );
						mMediaInfo.add(info);
					}
					else if( isAudio(subPath) ) {
						MediaInfo info = new MediaInfo( subPath, subFile.getName(), null, MEDIA_TAG.AUDIO );
						mMediaInfo.add(info);
					}
				}
			}
		}
		else {
			if( isVideo(rootPath) ) {
				MediaInfo info = new MediaInfo( rootPath, rootFile.getName(), null, MEDIA_TAG.VIDEO );
				mMediaInfo.add(info);
			}
			else if( isAudio(rootPath) ) {
				MediaInfo info = new MediaInfo( rootPath, rootFile.getName(), null, MEDIA_TAG.AUDIO );
				mMediaInfo.add(info);
			}
		}
	}
	
	public boolean isVideo( String fileName )
	{
		String strTemp = fileName.toLowerCase();
		
		for( int i = 0; i < VIDEO_EXTENSION.length; i++ ) {
			if( strTemp.endsWith(VIDEO_EXTENSION[i]) ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isAudio( String fileName )
	{
		String strTemp = fileName.toLowerCase();
		
		for( int i = 0; i < AUDIO_EXTENSION.length; i++ ) {
			if( strTemp.endsWith(AUDIO_EXTENSION[i]) ) {
				return true;
			}
		}
		return false;
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Interface Function
	//
	public String GetFileName() {
		if( mNetworkStream )
		{
			// Log.i(DBG_TAG, "Network( " + mNetworkStreamName +" )" );
			return mNetworkStreamName;
		}
		else
		{
			// Log.i(DBG_TAG, "Pos( " + String.valueOf(mItemPos) + " ), File( " + mMediaInfo.get( mItemPos ).GetFilePath() +" )" );
			return mMediaInfo.get( mItemPos ).GetFilePath();
		}
	}

	public String GetNextFileName() {
		mItemPos++;
		if( mItemPos >= mMediaInfo.size() ) mItemPos = 0;

		return mMediaInfo.get( mItemPos ).GetFilePath();
	}
	
	public String GetPrevFileName() {
		mItemPos--;
		if( mItemPos < 0 ) mItemPos = mMediaInfo.size() - 1;

		return mMediaInfo.get( mItemPos ).GetFilePath();
	}
	
	public boolean IsNetworkStream()
	{
		return mNetworkStream;
	}
	
	public boolean IsTVOut()
	{
		if( DEBUG_ISTVOUT )
		{
		}
		else
		{
			File file = new File( TVOUT_STATUS_FILE );
			if( !file.exists() )
				return false;

			try {
				BufferedReader br = new BufferedReader(new FileReader(TVOUT_STATUS_FILE));
				String line = br.readLine();
				br.close();
				if ("1".equals(line)) {
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		return true;
	}

	public void EnableTVOut( boolean enable )
	{
		File file = new File( TVOUT_CONTROL_FILE );
		if( !file.exists() ) {
			Log.w(DBG_TAG, "Not Support TVOut.\n");
			return;
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(TVOUT_CONTROL_FILE));
			bw.write( enable ? "1" : "0" );
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Adapter of Media Information
	//
	private class MediaInfoAdapter extends ArrayAdapter<MediaInfo> {
		private ArrayList<MediaInfo> mItems;
		
		public MediaInfoAdapter(Context context, int textViewResourceId, ArrayList<MediaInfo> items) {
			super(context, textViewResourceId, items);
			mItems = items;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if( convertView == null ) {
				LayoutInflater li = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				
				// View inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot)
				convertView = li.inflate( R.layout.listview_row, parent, false );
			}
			
			MediaInfo mediaInfo = mItems.get(position);

			if( mediaInfo != null )
			{
				ImageView imageView1 = (ImageView)convertView.findViewById(R.id.imageView1);
				TextView textView1 = (TextView)convertView.findViewById(R.id.textView1);
			
				if( imageView1 != null )
				{
					if( SUPPORT_THUMBNAIL )
					{
						if( Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB ) {
							new ThumbnailTask(imageView1, mediaInfo, position).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
						} else {
							new ThumbnailTask(imageView1, mediaInfo, position).execute();
						}
					}
					else
					{
						RelativeLayout.LayoutParams param  = new RelativeLayout.LayoutParams( 0, 40 );
						imageView1.setLayoutParams( param );
						Drawable alpha = imageView1.getDrawable();
						alpha.setAlpha( 0 );
					}
				}

				if( textView1 != null ) {
					textView1.setText( mediaInfo.GetFileName() );
				}
			}

			return convertView;
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
		private ImageView 	mImgView;
		private MediaInfo 	mMediaInfo;
		
		public ThumbnailTask(ImageView imgView, MediaInfo mediaInfo, int position) {
			mImgView 	= imgView;
			mMediaInfo	= mediaInfo;
		}
		
		@Override
		protected Bitmap doInBackground(Void... params) {
			// TODO Auto-generated method stub
			Bitmap srcBitmap = null;
			Bitmap dstBitmap = null;

			// a. video case
			if( mMediaInfo.GetFileTag() == MEDIA_TAG.VIDEO ) {
				//Log.v(DBG_TAG, "VIDEO: " + mMediaInfo.GetName());

				String uriPath = THUMBNAIL_PATH + mMediaInfo.GetFileName() + ".jpg";
				File file = new File( uriPath );
				if( file.exists() == true )
				{
					srcBitmap = BitmapFactory.decodeFile( uriPath );
					dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
				}
				else
				{
					Resources res = mContext.getResources();	
					srcBitmap = BitmapFactory.decodeResource(res, R.drawable.thumbnail_init);
					dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
				}
			}
			// b. audio case
			else {
				//Log.v(DBG_TAG, "AUDIO: " + mMediaInfo.GetName());
				Resources res = mContext.getResources();
				srcBitmap = BitmapFactory.decodeResource(res, R.drawable.audio_only);
				dstBitmap = Bitmap.createScaledBitmap(srcBitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
			}

			if( srcBitmap != null ) srcBitmap.recycle();
			return dstBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			if( result != null ) {
				mImgView.setImageBitmap( result );
			}
		}
	}
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Thumbnail Thread
	//	
	class ThumbnailThread extends Thread {
		public void run() {
			MoviePlayer moviePlayer = MoviePlayer.GetInstance();

			for( int i = 0; i < mMediaInfo.size(); i++ )
			{
				if( mMediaInfo.get(i).GetFileTag() == MEDIA_TAG.VIDEO ) {
					String uriPath = THUMBNAIL_PATH + mMediaInfo.get(i).GetFileName() + ".jpg";
					File file = new File( uriPath );

					if( !file.exists() )
					{
						if( 0 > moviePlayer.MakeThumbnail( mMediaInfo.get(i).GetFilePath(), uriPath, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT ) )
							Log.e(DBG_TAG, "Fail, Create Thumbnail. (" + uriPath + ")" );	
						else {
							//Log.e(DBG_TAG, "Create Thumbnail Done. (" + uriPath + ")" );
							Message msg = mHandler.obtainMessage();
							mHandler.sendMessage(msg);
						}
					}
				}

				if( Thread.currentThread().isInterrupted() )
					break;
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Data of Media Information
	//
	enum MEDIA_TAG 			{ VIDEO, AUDIO }

	class MediaInfo {
		private String 		mFilePath;
		private String		mFileName;
		private String		mFileSize;
		private MEDIA_TAG	mFileTag;
		
		public MediaInfo( String path, String name, String size, MEDIA_TAG tag ) {
			this.mFilePath	= path;
			this.mFileName	= name;
			this.mFileSize	= size;
			this.mFileTag	= tag;
		}
		public String GetFilePath() {
			return mFilePath;
		}
		public String GetFileName() {
			return mFileName;
		}
		public String GetFileSize() {
			return mFileSize;
		}
		public MEDIA_TAG GetFileTag() {
			return mFileTag;
		}
	}	


	////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//	Log Message : Function IN / OUT 
	//
	private void FUNCIN() {
		// java.lang.Exception e = new java.lang.Exception();
		// StackTraceElement trace[] = e.getStackTrace();
		// Log.v( DBG_TAG, trace[1].getMethodName() + "()++" );
	}

	private void FUNCOUT() {
		// java.lang.Exception e = new java.lang.Exception();
		// StackTraceElement trace[] = e.getStackTrace();
		// Log.v( DBG_TAG, trace[1].getMethodName() + "()--" );
	}
}
